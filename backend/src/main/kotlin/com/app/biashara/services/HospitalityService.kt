package com.app.biashara.services

import com.app.biashara.auth.generateId
import com.app.biashara.db.*
import com.app.biashara.models.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class HospitalityService(private val orderService: OrderService) {
    internal companion object {
        val ACTIVE_TAB_STATUSES = listOf("OPEN", "AWAITING_PAYMENT")
    }

    private val logger = LoggerFactory.getLogger(HospitalityService::class.java)
    fun isEnabled(businessId: String): Boolean = transaction {
        val row = BusinessesTable.select { BusinessesTable.id eq businessId }.firstOrNull() ?: return@transaction false
        val enabledMenusList = row[BusinessesTable.enabledMenus].split(',').map { it.trim().uppercase() }
        row[BusinessesTable.hospitalityEnabled] == true ||
            row[BusinessesTable.type].equals("HOSPITALITY", ignoreCase = true) ||
            enabledMenusList.contains("HOSPITALITY") ||
            enabledMenusList.contains("HOSPITALITY_OPS")
    }
    fun dashboard(businessId: String): HospitalityDashboardResponse = transaction {
        val enabled = isEnabled(businessId)
        HospitalityDashboardResponse(enabled, tables(businessId), openTabs(businessId), tickets(businessId))
    }

    fun setEnabled(businessId: String, enabled: Boolean): HospitalityDashboardResponse = transaction {
        val isHospitalityType = BusinessesTable.select { BusinessesTable.id eq businessId }.firstOrNull()?.get(BusinessesTable.type)?.equals("HOSPITALITY", ignoreCase = true) == true
        if (!enabled && !isHospitalityType) {
            require(OrdersTable.select {
                (OrdersTable.businessId eq businessId) and
                    (OrdersTable.tabStatus inList ACTIVE_TAB_STATUSES)
            }.none()) { "Settle all open and awaiting-payment tabs before disabling hospitality mode" }
            require(KitchenTicketsTable.select {
                (KitchenTicketsTable.businessId eq businessId) and
                    (KitchenTicketsTable.status inList listOf("NEW", "PREPARING", "READY", "DELAYED"))
            }.none()) { "Complete or cancel all active kitchen and bar tickets before disabling hospitality mode" }
        }
        BusinessesTable.update({ BusinessesTable.id eq businessId }) {
            it[hospitalityEnabled] = enabled
            if (enabled || isHospitalityType) {
                val currentMenus = BusinessesTable.select { BusinessesTable.id eq businessId }.first()[BusinessesTable.enabledMenus]
                    .split(',').map(String::trim).filter(String::isNotBlank).toMutableSet()
                currentMenus += setOf("HOSPITALITY", "HOSPITALITY_OPS", "OPEN_TABS")
                it[enabledMenus] = currentMenus.joinToString(",")
            }
            it[updatedAt] = Clock.System.now()
        }
        logger.info("""{"event":"hospitality_mode_changed","business_id":"$businessId","enabled":$enabled}""")
        dashboard(businessId)
    }

    fun createTable(businessId: String, request: CreateHospitalityTableRequest): HospitalityTableResponse = transaction {
        requireEnabled(businessId)
        val name = request.name.trim()
        require(name.length in 1..60) { "Table name must be between 1 and 60 characters" }
        require(request.capacity in 1..100) { "Capacity must be between 1 and 100" }
        require(HospitalityTablesTable.select { HospitalityTablesTable.businessId eq businessId }.none { it[HospitalityTablesTable.name].equals(name, true) }) { "A table with this name already exists" }
        val id=generateId(); val now=Clock.System.now()
        HospitalityTablesTable.insert { it[HospitalityTablesTable.id]=id; it[HospitalityTablesTable.businessId]=businessId; it[HospitalityTablesTable.name]=name; it[area]=request.area.trim().ifBlank { "Main Floor" }.take(80); it[capacity]=request.capacity; it[status]="AVAILABLE"; it[isActive]=true; it[createdAt]=now; it[updatedAt]=now }
        logger.info("""{"event":"hospitality_table_created","business_id":"$businessId","table_id":"$id","capacity":${request.capacity}}""")
        HospitalityTableResponse(id,name,request.area.trim().ifBlank { "Main Floor" },request.capacity,"AVAILABLE")
    }

    fun updateTable(businessId: String, tableId: String, request: UpdateHospitalityTableRequest): HospitalityTableResponse = transaction {
        requireEnabled(businessId)
        val name = request.name.trim()
        val area = request.area.trim().ifBlank { "Main Floor" }
        require(name.length in 1..60) { "Table name must be between 1 and 60 characters" }
        require(area.length <= 80) { "Area must not exceed 80 characters" }
        require(request.capacity in 1..100) { "Capacity must be between 1 and 100" }
        require(HospitalityTablesTable.select {
            (HospitalityTablesTable.businessId eq businessId) and (HospitalityTablesTable.id neq tableId)
        }.none { it[HospitalityTablesTable.name].equals(name, true) }) { "A table with this name already exists" }
        require(HospitalityTablesTable.update({
            (HospitalityTablesTable.id eq tableId) and (HospitalityTablesTable.businessId eq businessId)
        }) {
            it[HospitalityTablesTable.name] = name
            it[HospitalityTablesTable.area] = area
            it[capacity] = request.capacity
            it[updatedAt] = Clock.System.now()
        } == 1) { "Table not found" }
        OrdersTable.update({
            (OrdersTable.businessId eq businessId) and
                (OrdersTable.hospitalityTableId eq tableId) and
                (OrdersTable.tabStatus inList ACTIVE_TAB_STATUSES)
        }) { it[deliveryLocation] = name; it[updatedAt] = Clock.System.now() }
        logger.info("""{"event":"hospitality_table_updated","business_id":"$businessId","table_id":"$tableId","capacity":${request.capacity}}""")
        tables(businessId).first { it.id == tableId }
    }

    fun createOrder(businessId: String, serverUserId: String?, request: HospitalityOrderRequest): ApiResponse<OrderResponse> {
        val enabled = transaction { BusinessesTable.select { BusinessesTable.id eq businessId }.firstOrNull()?.get(BusinessesTable.hospitalityEnabled) == true }
        if (!enabled) return ApiResponse(false, message = "Hospitality mode is disabled")
        val serviceType=request.serviceType.trim().uppercase()
        if (serviceType !in setOf("DINE_IN","TAKEAWAY","DELIVERY")) return ApiResponse(false,message="Invalid service type")
        if (request.guestCount !in 1..100) return ApiResponse(false,message="Guest count must be between 1 and 100")
        val table = request.tableId?.let { tableId -> transaction { HospitalityTablesTable.select { (HospitalityTablesTable.id eq tableId) and (HospitalityTablesTable.businessId eq businessId) and (HospitalityTablesTable.isActive eq true) }.firstOrNull() } }
        if (serviceType == "DINE_IN" && table == null) return ApiResponse(false,message="Select a table for dine-in service")
        val productRows=transaction{ProductsTable.select{(ProductsTable.businessId eq businessId) and (ProductsTable.id inList request.items.map{it.productId})}.associateBy{it[ProductsTable.id]}}
        val profiles=transaction{HospitalityMenuProfilesTable.select{(HospitalityMenuProfilesTable.businessId eq businessId) and (HospitalityMenuProfilesTable.productId inList request.items.map{it.productId})}.associateBy{it[HospitalityMenuProfilesTable.productId]}}
        if(request.items.any{profiles[it.productId]?.get(HospitalityMenuProfilesTable.soldOut)==true}) return ApiResponse(false,message="One or more menu items are sold out")
        if(!request.ageVerified&&request.items.any{profiles[it.productId]?.get(HospitalityMenuProfilesTable.ageRestricted)==true}) return ApiResponse(false,message="Age verification is required for this order")
        val currentTime=Clock.System.now().toLocalDateTime(TimeZone.of("Africa/Nairobi")).time.toString().take(5)
        val pricedItems=request.items.map{item->val product=productRows[item.productId]?:return ApiResponse(false,message="Product not found");val profile=profiles[item.productId];val happy=profile?.get(HospitalityMenuProfilesTable.happyHourPrice);val start=profile?.get(HospitalityMenuProfilesTable.happyHourStart);val end=profile?.get(HospitalityMenuProfilesTable.happyHourEnd);val active=happy!=null&&start!=null&&end!=null&&if(start<=end)currentTime in start..end else currentTime>=start||currentTime<=end;item.copy(unitPrice=if(active)happy!! else product[ProductsTable.sellingPrice])}
        val insufficient=transaction{pricedItems.firstOrNull{item->ProductRecipesTable.select{ProductRecipesTable.productId eq item.productId}.any{line->val stock=InventoryIngredientsTable.select{InventoryIngredientsTable.id eq line[ProductRecipesTable.ingredientId]}.firstOrNull()?.get(InventoryIngredientsTable.quantity)?:0.0;stock<line[ProductRecipesTable.quantity]*item.quantity}}}
        if(insufficient!=null)return ApiResponse(false,message="Insufficient ingredients for ${productRows[insufficient.productId]?.get(ProductsTable.name)?:"menu item"}")
        val result=orderService.create(businessId, CreateOrderRequest(
            customerName=request.customerName.trim().ifBlank { "Walk-in Guest" }, customerPhone=request.customerPhone.trim(),
            deliveryLocation=table?.get(HospitalityTablesTable.name) ?: serviceType.replace('_',' '), items=pricedItems,
            paymentMethod="TAB", paymentStatus="PENDING", deliveryStatus="PROCESSING", notes=request.notes.trim().take(1000),
            serviceType=serviceType, hospitalityTableId=table?.get(HospitalityTablesTable.id), serverUserId=serverUserId,
            guestCount=request.guestCount, tabStatus="OPEN"
        ), "WEB")
        val order=result.data ?: return result
        logger.info("""{"event":"hospitality_tab_opened","business_id":"$businessId","order_id":"${order.id}","service_type":"$serviceType","guest_count":${request.guestCount}}""")
        transaction {
            table?.let { HospitalityTablesTable.update({ HospitalityTablesTable.id eq it[HospitalityTablesTable.id] }) { row -> row[status]="OCCUPIED"; row[updatedAt]=Clock.System.now() } }
            pricedItems.forEach{item->ProductRecipesTable.select{ProductRecipesTable.productId eq item.productId}.forEach{line->InventoryIngredientsTable.update({InventoryIngredientsTable.id eq line[ProductRecipesTable.ingredientId]}){with(SqlExpressionBuilder){it.update(quantity,quantity-line[ProductRecipesTable.quantity]*item.quantity)};it[updatedAt]=Clock.System.now()}}}
            val stations=pricedItems.mapNotNull { item -> profiles[item.productId]?.get(HospitalityMenuProfilesTable.preparationStation) ?: hospitalityStationFor(productRows[item.productId]?.get(ProductsTable.category).orEmpty()) }.toSet()
            val now=Clock.System.now()
            stations.forEach { station -> KitchenTicketsTable.insert { it[id]=generateId(); it[KitchenTicketsTable.businessId]=businessId; it[orderId]=order.id; it[KitchenTicketsTable.station]=station; it[status]="NEW"; it[notes]=request.notes.take(500); it[createdAt]=now; it[updatedAt]=now } }
        }
        return ApiResponse(true,data=orderService.getById(order.id,businessId),message="Tab ${order.orderNumber} opened")
    }

    fun updateTicket(businessId: String, ticketId: String, request: UpdateTicketStatusRequest): KitchenTicketResponse = transaction {
        val status=request.status.trim().uppercase(); require(status in setOf("NEW","PREPARING","READY","SERVED","DELAYED","CANCELLED")) { "Invalid ticket status" }
        require(KitchenTicketsTable.update({ (KitchenTicketsTable.id eq ticketId) and (KitchenTicketsTable.businessId eq businessId) }) { it[KitchenTicketsTable.status]=status; it[updatedAt]=Clock.System.now() } == 1) { "Ticket not found" }
        logger.info("""{"event":"hospitality_ticket_status_changed","business_id":"$businessId","ticket_id":"$ticketId","status":"$status"}""")
        tickets(businessId).first { it.id == ticketId }
    }

    fun closeTab(businessId: String, orderId: String, request: CloseHospitalityTabRequest): OrderResponse = transaction {
        val method=request.paymentMethod.trim().uppercase(); require(method in setOf("CASH","CARD","MPESA")) { "Payment method must be CASH, CARD, or MPESA" }
        val order=OrdersTable.select {
            (OrdersTable.id eq orderId) and
                (OrdersTable.businessId eq businessId) and
                (OrdersTable.tabStatus inList ACTIVE_TAB_STATUSES)
        }.firstOrNull() ?: error("Active tab not found")
        val paid=method == "CASH"; val now=Clock.System.now()
        OrdersTable.update({ OrdersTable.id eq orderId }) {
            it[paymentMethod]=method
            it[paymentStatus]=if(paid) "PAID" else "PENDING"
            if (paid) it[deliveryStatus]="DELIVERED"
            it[tabStatus]=if(paid) "CLOSED" else "AWAITING_PAYMENT"
            it[updatedAt]=now
        }
        logger.info("""{"event":"hospitality_tab_settlement_started","business_id":"$businessId","order_id":"$orderId","payment_method":"$method","paid":$paid}""")
        if (paid) PaymentsTable.insert {
            it[id]=generateId(); it[PaymentsTable.businessId]=businessId; it[PaymentsTable.orderId]=orderId
            it[transactionCode]="${method}-${order[OrdersTable.orderNumber]}"; it[amount]=order[OrdersTable.subtotal]
            it[payerPhone]=order[OrdersTable.customerPhone]; it[payerName]=order[OrdersTable.customerName]
            it[PaymentsTable.method]=method; it[status]="SUCCESS"; it[channel]="HOSPITALITY_POS"
            it[reconciled]=true; it[transactionDate]=now
        }
        if (paid) order[OrdersTable.hospitalityTableId]?.let { tableId ->
            val otherOpen=OrdersTable.select { (OrdersTable.hospitalityTableId eq tableId) and (OrdersTable.id neq orderId) and (OrdersTable.tabStatus inList ACTIVE_TAB_STATUSES) }.any()
            if(!otherOpen) HospitalityTablesTable.update({ HospitalityTablesTable.id eq tableId }) { it[status]="AVAILABLE"; it[updatedAt]=now }
        }
        orderService.getById(orderId,businessId)!!
    }

    fun transferTab(businessId: String, orderId: String, request: TransferHospitalityTabRequest): OrderResponse = transaction {
        val order = OrdersTable.select {
            (OrdersTable.id eq orderId) and
                (OrdersTable.businessId eq businessId) and
                (OrdersTable.tabStatus inList ACTIVE_TAB_STATUSES)
        }.firstOrNull() ?: error("Active tab not found")
        val target = HospitalityTablesTable.select {
            (HospitalityTablesTable.id eq request.tableId) and
                (HospitalityTablesTable.businessId eq businessId) and
                (HospitalityTablesTable.isActive eq true)
        }.firstOrNull() ?: error("Target table not found")
        require(order[OrdersTable.hospitalityTableId] != request.tableId) { "Select a different table" }
        val now = Clock.System.now()
        val previousTableId = order[OrdersTable.hospitalityTableId]
        OrdersTable.update({ OrdersTable.id eq orderId }) {
            it[hospitalityTableId] = request.tableId
            it[deliveryLocation] = target[HospitalityTablesTable.name]
            it[updatedAt] = now
        }
        logger.info("""{"event":"hospitality_tab_transferred","business_id":"$businessId","order_id":"$orderId","target_table_id":"${request.tableId}"}""")
        HospitalityTablesTable.update({ HospitalityTablesTable.id eq request.tableId }) { it[status] = "OCCUPIED"; it[updatedAt] = now }
        previousTableId?.let { oldId ->
            val otherOpen = OrdersTable.select {
                (OrdersTable.hospitalityTableId eq oldId) and
                    (OrdersTable.id neq orderId) and
                    (OrdersTable.tabStatus inList ACTIVE_TAB_STATUSES)
            }.any()
            if (!otherOpen) HospitalityTablesTable.update({ HospitalityTablesTable.id eq oldId }) { it[status] = "AVAILABLE"; it[updatedAt] = now }
        }
        orderService.getById(orderId, businessId)!!
    }

    private fun tables(businessId: String): List<HospitalityTableResponse> {
        val open=OrdersTable.select { (OrdersTable.businessId eq businessId) and (OrdersTable.tabStatus inList ACTIVE_TAB_STATUSES) }
            .filter { it[OrdersTable.hospitalityTableId] != null }
            .groupBy { it[OrdersTable.hospitalityTableId]!! }
        return HospitalityTablesTable.select { (HospitalityTablesTable.businessId eq businessId) and (HospitalityTablesTable.isActive eq true) }
            .orderBy(HospitalityTablesTable.area to SortOrder.ASC, HospitalityTablesTable.name to SortOrder.ASC).map { row ->
            val tabs=open[row[HospitalityTablesTable.id]].orEmpty()
            HospitalityTableResponse(
                row[HospitalityTablesTable.id], row[HospitalityTablesTable.name], row[HospitalityTablesTable.area], row[HospitalityTablesTable.capacity],
                if(tabs.isEmpty()) row[HospitalityTablesTable.status] else "OCCUPIED",
                tabs.firstOrNull()?.get(OrdersTable.id), tabs.sumOf { it[OrdersTable.subtotal] }, tabs.size,
                row[HospitalityTablesTable.waiterUserId],row[HospitalityTablesTable.mergedIntoTableId],row[HospitalityTablesTable.positionX],row[HospitalityTablesTable.positionY],row[HospitalityTablesTable.shape]
            )
        }
    }
    private fun openTabs(businessId: String)=OrdersTable.select { (OrdersTable.businessId eq businessId) and (OrdersTable.tabStatus inList ACTIVE_TAB_STATUSES) }.orderBy(OrdersTable.createdAt,SortOrder.DESC).mapNotNull { orderService.getById(it[OrdersTable.id],businessId) }
    private fun tickets(businessId: String): List<KitchenTicketResponse> {
        val rows=KitchenTicketsTable.select { KitchenTicketsTable.businessId eq businessId }.orderBy(KitchenTicketsTable.createdAt,SortOrder.ASC).toList()
        return rows.mapNotNull { ticket ->
            val order=orderService.getById(ticket[KitchenTicketsTable.orderId],businessId)!!
            val tableName=order.hospitalityTableId?.let { id -> HospitalityTablesTable.select { HospitalityTablesTable.id eq id }.firstOrNull()?.get(HospitalityTablesTable.name) }
            val productCategories=ProductsTable.select { ProductsTable.id inList order.items.map { it.productId } }.associate { it[ProductsTable.id] to it[ProductsTable.category] }
            val stationItems=order.items.filter { hospitalityStationFor(productCategories[it.productId].orEmpty()) == ticket[KitchenTicketsTable.station] }
            if (stationItems.isEmpty()) return@mapNotNull null
            KitchenTicketResponse(ticket[KitchenTicketsTable.id],order.id,order.orderNumber,tableName,ticket[KitchenTicketsTable.station],ticket[KitchenTicketsTable.status],ticket[KitchenTicketsTable.notes],stationItems,ticket[KitchenTicketsTable.createdAt].toString())
        }
    }
    private fun requireEnabled(businessId: String) {
        require(BusinessesTable.select {
            (BusinessesTable.id eq businessId) and (BusinessesTable.hospitalityEnabled eq true)
        }.any()) { "Hospitality mode is disabled" }
    }
}

internal fun hospitalityStationFor(category: String): String? {
    val value = category.trim().lowercase()
    val kitchenKeywords = listOf("food", "meal", "dish", "snack", "bakery", "breakfast", "lunch", "dinner", "restaurant", "kitchen")
    val barKeywords = listOf("drink", "beverage", "beer", "wine", "spirit", "cocktail", "bar", "juice", "soda", "water")
    return when {
        kitchenKeywords.any(value::contains) -> "KITCHEN"
        barKeywords.any(value::contains) -> "BAR"
        else -> null
    }
}

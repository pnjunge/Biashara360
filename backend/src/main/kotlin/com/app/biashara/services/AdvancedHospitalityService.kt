package com.app.biashara.services

import com.app.biashara.auth.generateId
import com.app.biashara.db.*
import com.app.biashara.models.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class AdvancedHospitalityService(private val orderService: OrderService) {
    private val json=Json { ignoreUnknownKeys=true }

    fun staff(businessId:String)=transaction {UsersTable.select{(UsersTable.businessId eq businessId) and (UsersTable.isActive eq true)}.map{mapOf("id" to it[UsersTable.id],"name" to it[UsersTable.name],"role" to it[UsersTable.role])}}
    fun dashboard(businessId:String)=transaction { HospitalityOperationsResponse(reservations(businessId),menuProfiles(businessId),ingredients(businessId),shifts(businessId),suppliers(businessId),purchaseOrders(businessId),approvals(businessId),HospitalityShiftsTable.select{(HospitalityShiftsTable.businessId eq businessId) and (HospitalityShiftsTable.status eq "OPEN")}.any()) }

    fun saveReservation(businessId:String,userId:String,request:ReservationRequest)=transaction {
        require(request.customerName.trim().isNotBlank()){"Customer name is required"}; require(request.guestCount in 1..100){"Guest count must be between 1 and 100"}
        BusinessesTable.select { BusinessesTable.id eq businessId }.forUpdate().first()
        val at=Instant.parse(request.reservedAt);val durationMinutes=request.durationMinutes.coerceIn(15,1440);request.tableId?.let { id -> require(HospitalityTablesTable.select{(HospitalityTablesTable.id eq id) and (HospitalityTablesTable.businessId eq businessId) and (HospitalityTablesTable.isActive eq true) and HospitalityTablesTable.mergedIntoTableId.isNull()}.any()){"Table not found"};val requestedEnd=at.plus(durationMinutes.toLong(),DateTimeUnit.MINUTE);val overlaps=HospitalityReservationsTable.select{(HospitalityReservationsTable.businessId eq businessId) and (HospitalityReservationsTable.tableId eq id) and (HospitalityReservationsTable.status inList listOf("BOOKED","SEATED")) and (HospitalityReservationsTable.reservedAt less requestedEnd)}.any{row->row[HospitalityReservationsTable.reservedAt].plus(row[HospitalityReservationsTable.durationMinutes].toLong(),DateTimeUnit.MINUTE)>at};require(!overlaps){"Table is already reserved for that time"} }
        val id=generateId(); val now=Clock.System.now(); HospitalityReservationsTable.insert { it[HospitalityReservationsTable.id]=id;it[HospitalityReservationsTable.businessId]=businessId;it[tableId]=request.tableId;it[customerName]=request.customerName.trim();it[customerPhone]=request.customerPhone.trim();it[guestCount]=request.guestCount;it[reservedAt]=at;it[HospitalityReservationsTable.durationMinutes]=durationMinutes;it[status]="BOOKED";it[notes]=request.notes.take(500);it[createdAt]=now;it[updatedAt]=now }; audit(businessId,userId,"RESERVATION_CREATED","RESERVATION",id); reservations(businessId).first{it.id==id}
    }
    fun updateReservationStatus(businessId:String,userId:String,id:String,status:String)=transaction { val value=status.uppercase();require(value in setOf("BOOKED","SEATED","COMPLETED","CANCELLED","NO_SHOW")){"Invalid reservation status"};val current=HospitalityReservationsTable.select{(HospitalityReservationsTable.id eq id) and (HospitalityReservationsTable.businessId eq businessId)}.firstOrNull()?:error("Reservation not found");val allowed=when(current[HospitalityReservationsTable.status]){"BOOKED"->setOf("BOOKED","SEATED","CANCELLED","NO_SHOW");"SEATED"->setOf("SEATED","COMPLETED");else->setOf(current[HospitalityReservationsTable.status])};require(value in allowed){"Cannot change a ${current[HospitalityReservationsTable.status].lowercase()} reservation to $value"};require(HospitalityReservationsTable.update({(HospitalityReservationsTable.id eq id) and (HospitalityReservationsTable.businessId eq businessId)}){it[HospitalityReservationsTable.status]=value;it[updatedAt]=Clock.System.now()}==1){"Reservation not found"};audit(businessId,userId,"RESERVATION_$value","RESERVATION",id);reservations(businessId).first{it.id==id} }
    fun updateTableOperations(businessId:String,userId:String,id:String,r:TableOperationsRequest)=transaction {
        BusinessesTable.select{BusinessesTable.id eq businessId}.forUpdate().first()
        require(r.mergeIntoTableId==null || HospitalityTablesTable.select{HospitalityTablesTable.mergedIntoTableId eq id}.none()){ "Unmerge child tables before merging this table" }
        require(r.positionX==null||r.positionX>=0){"Table X position cannot be negative"};require(r.positionY==null||r.positionY>=0){"Table Y position cannot be negative"};require(r.shape==null||r.shape.uppercase() in setOf("RECTANGLE","CIRCLE")){"Invalid table shape"}
        r.waiterUserId?.let { require(UsersTable.select{(UsersTable.id eq it) and (UsersTable.businessId eq businessId) and (UsersTable.isActive eq true)}.any()){"Waiter not found"} }
        r.mergeIntoTableId?.let { require(it!=id){"A table cannot be merged into itself"};require(HospitalityTablesTable.select{(HospitalityTablesTable.id eq it) and (HospitalityTablesTable.businessId eq businessId) and (HospitalityTablesTable.isActive eq true) and HospitalityTablesTable.mergedIntoTableId.isNull()}.any()){"Merge target not found"} }
        require(HospitalityTablesTable.update({(HospitalityTablesTable.id eq id) and (HospitalityTablesTable.businessId eq businessId)}){it[waiterUserId]=r.waiterUserId;it[mergedIntoTableId]=r.mergeIntoTableId;r.positionX?.let{v->it[positionX]=v};r.positionY?.let{v->it[positionY]=v};r.shape?.let{v->it[shape]=v.uppercase()};it[updatedAt]=Clock.System.now()}==1){"Table not found"}
        if(r.mergeIntoTableId!=null) OrdersTable.update({(OrdersTable.businessId eq businessId) and (OrdersTable.hospitalityTableId eq id) and (OrdersTable.tabStatus inList HospitalityService.ACTIVE_TAB_STATUSES)}){it[hospitalityTableId]=r.mergeIntoTableId;it[updatedAt]=Clock.System.now()}
        val affected=listOfNotNull(id,r.mergeIntoTableId)
        affected.forEach { tableId ->
            val active=OrdersTable.select{(OrdersTable.businessId eq businessId) and (OrdersTable.hospitalityTableId eq tableId) and (OrdersTable.tabStatus inList HospitalityService.ACTIVE_TAB_STATUSES)}.any()
            HospitalityTablesTable.update({HospitalityTablesTable.id eq tableId}){it[status]=if(active)"OCCUPIED" else "AVAILABLE";it[updatedAt]=Clock.System.now()}
        }
        audit(businessId,userId,"TABLE_OPERATIONS_UPDATED","TABLE",id)
    }
    fun saveMenuProfile(businessId:String,userId:String,productId:String,r:MenuProfileRequest)=transaction {
        require(r.preparationStation==null || r.preparationStation=="KITCHEN"){"Invalid preparation station"}
        if(r.preparationStation=="KITCHEN") {
            val product=ProductsTable.select { (ProductsTable.id eq productId) and (ProductsTable.businessId eq businessId) }.firstOrNull() ?: error("Product not found")
            require(hospitalityStationFor(product[ProductsTable.category])=="KITCHEN") { "Only food categories can go for preparation. Use no preparation ticket for drinks and other items" }
        }
        require((r.sizes+r.extras+r.variants).all{it.name.isNotBlank()&&it.priceDelta.isFinite()&&it.priceDelta>=0}){"Invalid menu option"}
        require(ProductsTable.select{(ProductsTable.id eq productId) and (ProductsTable.businessId eq businessId)}.any()){"Product not found"};require(!r.ageRestricted || (r.minimumAge?:0) in 18..100){"Minimum age must be at least 18"};require(r.happyHourPrice==null||r.happyHourPrice>=0){"Happy-hour price cannot be negative"};val comboIds=r.comboProductIds.distinct();require(comboIds.isEmpty()||ProductsTable.select{(ProductsTable.businessId eq businessId) and (ProductsTable.id inList comboIds)}.count().toInt()==comboIds.size){"Combo product not found"}
        val exists=HospitalityMenuProfilesTable.select{HospitalityMenuProfilesTable.productId eq productId}.any();val write:HospitalityMenuProfilesTable.(UpdateBuilder<*>) -> Unit = { row -> row[HospitalityMenuProfilesTable.businessId]=businessId;row[preparationStation]=r.preparationStation?.uppercase();row[mealPeriods]=r.mealPeriods.joinToString(",");row[sizesJson]=json.encodeToString(r.sizes);row[extrasJson]=json.encodeToString(r.extras);row[variantsJson]=json.encodeToString(r.variants);row[comboJson]=json.encodeToString(r.comboProductIds);row[soldOut]=r.soldOut;row[happyHourPrice]=r.happyHourPrice;row[happyHourStart]=r.happyHourStart;row[happyHourEnd]=r.happyHourEnd;row[ageRestricted]=r.ageRestricted;row[minimumAge]=r.minimumAge;row[updatedAt]=Clock.System.now() }
        if(exists) HospitalityMenuProfilesTable.update({HospitalityMenuProfilesTable.productId eq productId}){write(it)} else HospitalityMenuProfilesTable.insert{it[HospitalityMenuProfilesTable.productId]=productId;write(it)};audit(businessId,userId,"MENU_PROFILE_SAVED","PRODUCT",productId);menuProfiles(businessId).first{it.productId==productId}
    }
    fun createIngredient(businessId:String,userId:String,r:IngredientRequest)=transaction {
        require(r.name.trim().isNotBlank() && r.unit.trim().length in 1..20) { "Ingredient name and unit are required" }
        require(listOf(r.quantity,r.reorderLevel,r.unitCost).all { it.isFinite() && it >= 0 }) { "Ingredient quantities and costs must be finite and non-negative" }
        val unit=r.unit.trim().uppercase(); val purchaseUnit=r.purchaseUnit?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
        validatePurchaseUnit(unit,purchaseUnit,r.purchaseUnitSize)
        val id=generateId(); val now=Clock.System.now()
        InventoryIngredientsTable.insert {
            it[InventoryIngredientsTable.id]=id; it[InventoryIngredientsTable.businessId]=businessId; it[name]=r.name.trim().take(160)
            it[InventoryIngredientsTable.unit]=unit; it[quantity]=r.quantity; it[reorderLevel]=r.reorderLevel; it[unitCost]=r.unitCost
            it[InventoryIngredientsTable.purchaseUnit]=purchaseUnit; it[purchaseUnitSize]=r.purchaseUnitSize
            it[isActive]=true; it[createdAt]=now; it[updatedAt]=now
        }
        audit(businessId,userId,"INGREDIENT_CREATED","INGREDIENT",id); ingredients(businessId).first { it.id==id }
    }
    fun configurePurchaseUnit(businessId:String,userId:String,id:String,r:IngredientPurchaseUnitRequest)=transaction {
        BusinessesTable.select { BusinessesTable.id eq businessId }.forUpdate().first()
        val ingredient=InventoryIngredientsTable.select { (InventoryIngredientsTable.id eq id) and (InventoryIngredientsTable.businessId eq businessId) }.firstOrNull() ?: error("Ingredient not found")
        val unit=r.purchaseUnit?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
        validatePurchaseUnit(ingredient[InventoryIngredientsTable.unit],unit,r.purchaseUnitSize)
        InventoryIngredientsTable.update({ InventoryIngredientsTable.id eq id }) { it[purchaseUnit]=unit; it[purchaseUnitSize]=r.purchaseUnitSize; it[updatedAt]=Clock.System.now() }
        audit(businessId,userId,"INGREDIENT_PURCHASE_UNIT_UPDATED","INGREDIENT",id)
        ingredients(businessId).first { it.id==id }
    }
    private fun validatePurchaseUnit(base:String,unit:String?,size:Double) {
        require(size.isFinite() && size>0) { "Pack size must be finite and positive" }
        if(unit!=null) purchaseConversion(base,unit,unit,size)
    }
    fun recipe(businessId:String,productId:String)=transaction {require(ProductsTable.select{(ProductsTable.id eq productId) and (ProductsTable.businessId eq businessId)}.any()){ "Product not found" };ProductRecipesTable.select{ProductRecipesTable.productId eq productId}.map{RecipeLine(it[ProductRecipesTable.ingredientId],it[ProductRecipesTable.quantity])}}
    fun saveRecipe(businessId:String,userId:String,productId:String,r:SaveRecipeRequest)=transaction { BusinessesTable.select{BusinessesTable.id eq businessId}.forUpdate().first(); require(ProductsTable.select{(ProductsTable.id eq productId) and (ProductsTable.businessId eq businessId)}.any()){"Product not found"};require(r.lines.map{it.ingredientId}.distinct().size==r.lines.size){"Duplicate recipe ingredient"};require(r.lines.all{it.quantity.isFinite()&&it.quantity>0}){"Recipe quantities must be positive"};val ids=r.lines.map{it.ingredientId}.distinct();require(ids.isEmpty()||InventoryIngredientsTable.select{(InventoryIngredientsTable.businessId eq businessId) and (InventoryIngredientsTable.id inList ids)}.count().toInt()==ids.size){"Invalid ingredient"};ProductRecipesTable.deleteWhere{ProductRecipesTable.productId eq productId};r.lines.forEach{line->ProductRecipesTable.insert{it[ProductRecipesTable.productId]=productId;it[ingredientId]=line.ingredientId;it[quantity]=line.quantity}};audit(businessId,userId,"RECIPE_SAVED","PRODUCT",productId);r.lines }
    fun recordBarEvent(businessId:String,userId:String,r:BarStockEventRequest)=transaction { BusinessesTable.select{BusinessesTable.id eq businessId}.forUpdate().first(); val type=r.eventType.uppercase();require(type in setOf("OPEN_BOTTLE","SHOT","WASTAGE","BREAKAGE","SPILLAGE","COMPLIMENTARY","STOCK_IN","VARIANCE")){"Invalid bar event"};require(r.quantity.isFinite()&&r.quantity>0){"Quantity must be positive"};require(r.productId!=null||r.ingredientId!=null){"A product or ingredient is required"};r.productId?.let{id->require(ProductsTable.select{(ProductsTable.id eq id) and (ProductsTable.businessId eq businessId)}.any()){"Product not found"}};val delta=if(type=="STOCK_IN")r.quantity else if(type=="OPEN_BOTTLE")0.0 else -r.quantity;r.ingredientId?.let{id->val ingredient=InventoryIngredientsTable.select{(InventoryIngredientsTable.id eq id) and (InventoryIngredientsTable.businessId eq businessId)}.forUpdate().firstOrNull()?:error("Ingredient not found");require(ingredient[InventoryIngredientsTable.quantity]+delta>=0){"Insufficient ingredient stock"};require(ingredient[InventoryIngredientsTable.unit].equals(r.unit,true)){"Stock unit does not match ingredient"};require(InventoryIngredientsTable.update({(InventoryIngredientsTable.id eq id) and (InventoryIngredientsTable.businessId eq businessId)}){with(SqlExpressionBuilder){it.update(quantity,quantity+delta)};it[updatedAt]=Clock.System.now()}==1){"Ingredient not found"}};val id=generateId();BarStockEventsTable.insert{it[BarStockEventsTable.id]=id;it[BarStockEventsTable.businessId]=businessId;it[productId]=r.productId;it[ingredientId]=r.ingredientId;it[eventType]=type;it[quantity]=r.quantity;it[unit]=r.unit.uppercase();it[reason]=r.reason.take(500);it[recordedBy]=userId;it[recordedAt]=Clock.System.now()};audit(businessId,userId,"BAR_$type","BAR_STOCK_EVENT",id);id }
    fun openShift(businessId:String,userId:String,r:ShiftOpenRequest)=transaction { BusinessesTable.select{BusinessesTable.id eq businessId}.forUpdate().first(); require(r.openingFloat>=0){"Opening float cannot be negative"};require(HospitalityShiftsTable.select{(HospitalityShiftsTable.businessId eq businessId) and (HospitalityShiftsTable.status eq "OPEN")}.none()){"A shift is already open"};val id=generateId();HospitalityShiftsTable.insert{it[HospitalityShiftsTable.id]=id;it[HospitalityShiftsTable.businessId]=businessId;it[openedBy]=userId;it[openedAt]=Clock.System.now();it[openingFloat]=r.openingFloat;it[tipsTotal]=0.0;it[expensesTotal]=0.0;it[status]="OPEN";it[notes]=r.notes.take(500)};audit(businessId,userId,"SHIFT_OPENED","SHIFT",id);shifts(businessId).first{it.id==id} }
    fun closeShift(businessId:String,userId:String,id:String,r:ShiftCloseRequest)=transaction {
        BusinessesTable.select { BusinessesTable.id eq businessId }.forUpdate().first()
        require(r.actualCash >= 0 && (r.actualMpesa ?: -1.0) >= 0 && (r.actualCard ?: -1.0) >= 0 && r.tipsTotal >= 0 && r.expensesTotal >= 0) {
            "Cash, M-Pesa, card, tips, and expenses must be provided as non-negative amounts"
        }
        val actualMpesa = r.actualMpesa ?: error("Enter the M-Pesa amount counted before closing the shift")
        val actualCard = r.actualCard ?: error("Enter the card amount counted before closing the shift")
        val shift = HospitalityShiftsTable.select {
            (HospitalityShiftsTable.id eq id) and
                (HospitalityShiftsTable.businessId eq businessId) and
                (HospitalityShiftsTable.status eq "OPEN")
        }.firstOrNull() ?: error("Open shift not found")
        require(OrdersTable.select {
            (OrdersTable.businessId eq businessId) and
                (OrdersTable.tabStatus inList HospitalityService.ACTIVE_TAB_STATUSES)
        }.none()) { "Settle all open tabs before ending the day" }
        val closedAt = Clock.System.now()
        HospitalityDutyService().closeStaffShiftsForDay(businessId,closedAt)
        val hospitalityIds=OrdersTable.select{(OrdersTable.businessId eq businessId) and (OrdersTable.serviceType inList listOf("DINE_IN","TAKEAWAY","DELIVERY"))}.map{it[OrdersTable.id]}
        val payments = PaymentsTable.select {
            (PaymentsTable.businessId eq businessId) and (PaymentsTable.orderId inList hospitalityIds) and
                (PaymentsTable.transactionDate greaterEq shift[HospitalityShiftsTable.openedAt]) and
                (PaymentsTable.transactionDate less closedAt) and
                (PaymentsTable.status eq "SUCCESS")
        }.toList()
        val unreconciled = payments.count { !it[PaymentsTable.reconciled] }
        require(unreconciled == 0) { "Reconcile all successful payments before ending the day ($unreconciled remaining)" }
        val cash = payments.filter { it[PaymentsTable.method] == "CASH" }.sumOf { it[PaymentsTable.amount] }
        val mpesa = payments.filter { it[PaymentsTable.method] == "MPESA" }.sumOf { it[PaymentsTable.amount] }
        val card = payments.filter { it[PaymentsTable.method] == "CARD" }.sumOf { it[PaymentsTable.amount] }
        val supplierCash=IngredientPurchasePaymentsTable.select{(IngredientPurchasePaymentsTable.businessId eq businessId) and (IngredientPurchasePaymentsTable.method eq "CASH") and (IngredientPurchasePaymentsTable.paidFromTill eq true) and (IngredientPurchasePaymentsTable.paidAt greaterEq shift[HospitalityShiftsTable.openedAt]) and (IngredientPurchasePaymentsTable.paidAt less closedAt)}.sumOf{it[IngredientPurchasePaymentsTable.amount]}
        val cashExpenses=r.expensesTotal+supplierCash
        val expectedCashAmount = shift[HospitalityShiftsTable.openingFloat] + cash - cashExpenses
        val cashVariance = r.actualCash - expectedCashAmount
        val mpesaVariance = actualMpesa - mpesa
        val cardVariance = actualCard - card
        HospitalityShiftsTable.update({ HospitalityShiftsTable.id eq id }) {
            it[closedBy] = userId
            it[HospitalityShiftsTable.closedAt] = closedAt
            it[HospitalityShiftsTable.expectedCash] = expectedCashAmount
            it[actualCash] = r.actualCash
            it[mpesaTotal] = mpesa
            it[cardTotal] = card
            it[mpesaActual] = actualMpesa
            it[cardActual] = actualCard
            it[tipsTotal] = r.tipsTotal
            it[expensesTotal] = cashExpenses
            it[status] = "CLOSED"
            it[notes] = r.notes.take(500)
        }
        audit(businessId, userId, "SHIFT_CLOSED", "SHIFT", id, "{\"cashVariance\":$cashVariance,\"mpesaVariance\":$mpesaVariance,\"cardVariance\":$cardVariance}")
        shifts(businessId).first { it.id == id }
    }
    fun createSupplier(businessId:String,userId:String,r:SupplierRequest)=transaction { require(r.name.trim().isNotBlank()){"Supplier name is required"};val id=generateId();val now=Clock.System.now();SuppliersTable.insert{it[SuppliersTable.id]=id;it[SuppliersTable.businessId]=businessId;it[name]=r.name.trim();it[phone]=r.phone.trim();it[email]=r.email;it[address]=r.address;it[isActive]=true;it[createdAt]=now;it[updatedAt]=now};audit(businessId,userId,"SUPPLIER_CREATED","SUPPLIER",id);suppliers(businessId).first{it.id==id} }
    fun createPurchaseOrder(businessId:String,userId:String,r:PurchaseOrderRequest)=transaction {
        BusinessesTable.select { BusinessesTable.id eq businessId }.forUpdate().first()
        require(SuppliersTable.select { (SuppliersTable.id eq r.supplierId) and (SuppliersTable.businessId eq businessId) and (SuppliersTable.isActive eq true) }.any()) { "Supplier not found" }
        require(r.items.isNotEmpty() && r.items.size<=100 && r.items.all { it.quantity.isFinite() && it.quantity>0 && it.unitCost.isFinite() && it.unitCost>=0 }) { "Valid purchase items are required" }
        require(r.items.map { it.ingredientId }.distinct().size==r.items.size) { "Combine duplicate ingredient lines" }
        val converted=r.items.map { line ->
            val ingredient=InventoryIngredientsTable.select { (InventoryIngredientsTable.id eq line.ingredientId) and (InventoryIngredientsTable.businessId eq businessId) and (InventoryIngredientsTable.isActive eq true) }.firstOrNull() ?: error("Ingredient not found")
            val unit=line.purchaseUnit?.trim()?.uppercase()?.takeIf { it.isNotEmpty() } ?: ingredient[InventoryIngredientsTable.unit]
            val factor=purchaseConversion(ingredient[InventoryIngredientsTable.unit],unit,ingredient[InventoryIngredientsTable.purchaseUnit],ingredient[InventoryIngredientsTable.purchaseUnitSize])
            require((line.quantity*factor).isFinite() && (line.quantity*line.unitCost).isFinite() && (line.unitCost/factor).isFinite()) { "Purchase quantity or cost is too large" }
            Triple(line,unit,factor)
        }
        val id=generateId(); val now=Clock.System.now(); val number="PO-${generateId().take(12)}"
        val total=kotlin.math.round(r.items.sumOf { it.quantity*it.unitCost }*100)/100; require(total.isFinite()) { "Purchase total is too large" }
        PurchaseOrdersTable.insert {
            it[PurchaseOrdersTable.id]=id; it[PurchaseOrdersTable.businessId]=businessId; it[supplierId]=r.supplierId; it[orderNumber]=number
            it[status]="ORDERED"; it[orderedAt]=now; it[totalCost]=total; it[notes]=r.notes.take(500); it[createdBy]=userId
        }
        converted.forEach { (line,unit,factor) -> PurchaseOrderItemsTable.insert {
            it[PurchaseOrderItemsTable.id]=generateId(); it[purchaseOrderId]=id; it[ingredientId]=line.ingredientId
            it[orderedQuantity]=line.quantity*factor; it[receivedQuantity]=0.0; it[unitCost]=line.unitCost/factor
            it[purchaseQuantity]=line.quantity; it[purchaseUnit]=unit; it[purchaseUnitCost]=line.unitCost; it[conversionFactor]=factor
        } }
        audit(businessId,userId,"PURCHASE_ORDER_CREATED","PURCHASE_ORDER",id); purchaseOrders(businessId).first { it.id==id }
    }
    fun receivePurchaseOrder(businessId:String,userId:String,id:String,request:ReceiveIngredientPurchaseRequest=ReceiveIngredientPurchaseRequest())=transaction {
        BusinessesTable.select{BusinessesTable.id eq businessId}.forUpdate().first()
        val po=PurchaseOrdersTable.select{(PurchaseOrdersTable.id eq id) and (PurchaseOrdersTable.businessId eq businessId) and (PurchaseOrdersTable.status eq "ORDERED")}.forUpdate().firstOrNull()?:error("Purchase order not found or already received")
        val now=Clock.System.now()
        PurchaseOrderItemsTable.select{PurchaseOrderItemsTable.purchaseOrderId eq id}.forEach{line->
            val ingredient=InventoryIngredientsTable.select{(InventoryIngredientsTable.id eq line[PurchaseOrderItemsTable.ingredientId]) and (InventoryIngredientsTable.businessId eq businessId)}.forUpdate().firstOrNull()?:error("Ingredient not found")
            val quantity=ingredient[InventoryIngredientsTable.quantity]+line[PurchaseOrderItemsTable.orderedQuantity]
            val value=ingredient[InventoryIngredientsTable.quantity]*ingredient[InventoryIngredientsTable.unitCost]+line[PurchaseOrderItemsTable.orderedQuantity]*line[PurchaseOrderItemsTable.unitCost]
            require(quantity.isFinite() && value.isFinite() && quantity>0){"Received stock value is too large"}
            InventoryIngredientsTable.update({InventoryIngredientsTable.id eq line[PurchaseOrderItemsTable.ingredientId]}){it[InventoryIngredientsTable.quantity]=quantity;it[unitCost]=value/quantity;it[updatedAt]=now}
            PurchaseOrderItemsTable.update({PurchaseOrderItemsTable.id eq line[PurchaseOrderItemsTable.id]}){it[receivedQuantity]=line[PurchaseOrderItemsTable.orderedQuantity]}
        }
        PurchaseOrdersTable.update({PurchaseOrdersTable.id eq id}){it[status]="RECEIVED";it[receivedAt]=now}
        recordIngredientPurchaseExpense(po,now)
        if(request.payment!=null)recordIngredientPayment(businessId,userId,PurchaseOrdersTable.select{PurchaseOrdersTable.id eq id}.first(),request.payment)
        audit(businessId,userId,"PURCHASE_ORDER_RECEIVED","PURCHASE_ORDER",id)
        purchaseOrders(businessId).first{it.id==id}
    }
    fun payPurchaseOrder(businessId:String,userId:String,id:String,request:IngredientPurchasePaymentRequest)=transaction {
        BusinessesTable.select{BusinessesTable.id eq businessId}.forUpdate().first()
        val po=PurchaseOrdersTable.select{(PurchaseOrdersTable.id eq id) and (PurchaseOrdersTable.businessId eq businessId)}.forUpdate().firstOrNull()?:error("Purchase order not found")
        recordIngredientPayment(businessId,userId,po,request)
        purchaseOrders(businessId).first{it.id==id}
    }
    private fun validateApproval(businessId:String,r:ApprovalRequest) {
        require(r.reason.trim().isNotBlank()) { "A reason is required" }
        when(r.actionType) {
            "CANCEL_ORDER","DISCOUNT_ORDER","COMPLIMENTARY_ORDER" -> {
                require(r.entityType=="ORDER") { "Order target required" }
                val order=OrdersTable.select { (OrdersTable.id eq r.entityId) and (OrdersTable.businessId eq businessId) and (OrdersTable.serviceType inList listOf("DINE_IN","TAKEAWAY","DELIVERY")) and (OrdersTable.tabStatus eq "OPEN") and (OrdersTable.paymentStatus eq "PENDING") }.forUpdate().firstOrNull()?:error("Open unpaid hospitality order not found")
                require(PaymentsTable.select{(PaymentsTable.orderId eq r.entityId) and (PaymentsTable.status eq "SUCCESS")}.none()){ "Order already has a payment" }
                if(r.actionType=="DISCOUNT_ORDER")require(r.amount!=null&&r.amount.isFinite()&&r.amount>0&&r.amount<=order[OrdersTable.baseAmount]) { "Discount must be positive and within the order base amount" }
            }
            "STOCK_EVENT" -> {
                require(r.entityType=="INGREDIENT") { "Ingredient target required" }
                require(InventoryIngredientsTable.select{(InventoryIngredientsTable.id eq r.entityId) and (InventoryIngredientsTable.businessId eq businessId)}.any()) { "Ingredient not found" }
                require(r.quantity!=null&&r.quantity.isFinite()&&r.quantity>0&&r.eventType in setOf("STOCK_IN","WASTAGE","BREAKAGE","SPILLAGE","COMPLIMENTARY","VARIANCE")) { "Valid stock event and quantity required" }
            }
            "RECEIVE_PURCHASE_ORDER" -> {
                require(r.entityType=="PURCHASE_ORDER") { "Purchase order target required" }
                require(PurchaseOrdersTable.select{(PurchaseOrdersTable.id eq r.entityId) and (PurchaseOrdersTable.businessId eq businessId) and (PurchaseOrdersTable.status eq "ORDERED")}.any()) { "Unreceived purchase order not found" }
            }
            else -> error("Unsupported approval action")
        }
    }
    fun requestApproval(businessId:String,userId:String,r:ApprovalRequest)=transaction {
        BusinessesTable.select{BusinessesTable.id eq businessId}.forUpdate().first()
        validateApproval(businessId,r)
        require(ManagerApprovalsTable.select{(ManagerApprovalsTable.businessId eq businessId) and (ManagerApprovalsTable.entityId eq r.entityId) and (ManagerApprovalsTable.actionType eq r.actionType) and (ManagerApprovalsTable.status eq "PENDING")}.none()){ "This action already has a pending request" }
        val id=generateId();ManagerApprovalsTable.insert{it[ManagerApprovalsTable.id]=id;it[ManagerApprovalsTable.businessId]=businessId;it[actionType]=r.actionType;it[entityType]=r.entityType;it[entityId]=r.entityId;it[requestedBy]=userId;it[status]="PENDING";it[reason]=r.reason.trim().take(500);it[payload]=json.encodeToString(r);it[requestedAt]=Clock.System.now()}
        audit(businessId,userId,"APPROVAL_REQUESTED",r.entityType,id);approvals(businessId).first{it.id==id}
    }
    fun decideApproval(businessId:String,userId:String,id:String,approved:Boolean)=transaction {
        BusinessesTable.select{BusinessesTable.id eq businessId}.forUpdate().first()
        val row=ManagerApprovalsTable.select{(ManagerApprovalsTable.id eq id) and (ManagerApprovalsTable.businessId eq businessId) and (ManagerApprovalsTable.status eq "PENDING")}.forUpdate().firstOrNull()?:error("Pending approval not found")
        require(row[ManagerApprovalsTable.requestedBy]!=userId){ "You cannot decide your own approval request" }
        if(approved) {
            val r=runCatching{json.decodeFromString<ApprovalRequest>(row[ManagerApprovalsTable.payload])}.getOrElse{error("Legacy request lacks action details; reject and submit a new request")}
            validateApproval(businessId,r)
            when(r.actionType) {
                "CANCEL_ORDER" -> {val result=orderService.cancel(r.entityId,businessId);require(result.success){result.message?:"Cancellation failed"}}
                "DISCOUNT_ORDER","COMPLIMENTARY_ORDER" -> {
                    val order=OrdersTable.select{OrdersTable.id eq r.entityId}.first()
                    val items=OrderItemsTable.select{OrderItemsTable.orderId eq r.entityId}.toList()
                    var remaining=r.amount?:0.0
                    items.forEach { item ->
                        val gross=item[OrderItemsTable.quantity]*(item[OrderItemsTable.unitPrice]+json.decodeFromString<List<MenuOption>>(item[OrderItemsTable.modifiersJson]).sumOf{it.priceDelta})
                        val available=if(item[OrderItemsTable.complimentary])0.0 else (gross-item[OrderItemsTable.discountAmount]).coerceAtLeast(0.0)
                        val reduction=minOf(remaining,available);remaining-=reduction
                        OrderItemsTable.update({OrderItemsTable.id eq item[OrderItemsTable.id]}) { if(r.actionType=="COMPLIMENTARY_ORDER")it[complimentary]=true else it[discountAmount]=item[OrderItemsTable.discountAmount]+reduction }
                    }
                    val base=if(r.actionType=="COMPLIMENTARY_ORDER")0.0 else (order[OrdersTable.baseAmount]-(r.amount?:0.0)).coerceAtLeast(0.0)
                    val totals=calculateOrderTotals(base,order[OrdersTable.taxIncluded],order[OrdersTable.taxRate])
                    OrdersTable.update({OrdersTable.id eq r.entityId}) {it[baseAmount]=totals.baseAmount;it[taxAmount]=totals.taxAmount;it[subtotal]=totals.total;it[updatedAt]=Clock.System.now()}
                }
                "STOCK_EVENT" -> { val unit=InventoryIngredientsTable.select{InventoryIngredientsTable.id eq r.entityId}.first()[InventoryIngredientsTable.unit];recordBarEvent(businessId,userId,BarStockEventRequest(ingredientId=r.entityId,eventType=r.eventType!!,quantity=r.quantity!!,unit=unit,reason=r.reason)) }
                "RECEIVE_PURCHASE_ORDER" -> receivePurchaseOrder(businessId,userId,r.entityId)
            }
        }
        ManagerApprovalsTable.update({ManagerApprovalsTable.id eq id}){it[approvedBy]=userId;it[status]=if(approved)"APPROVED" else "REJECTED";it[decidedAt]=Clock.System.now()}
        audit(businessId,userId,if(approved)"APPROVAL_APPLIED" else "APPROVAL_REJECTED","APPROVAL",id)
        approvals(businessId).first{it.id==id}
    }
    fun splitBill(businessId:String,userId:String,orderId:String,r:SplitBillRequest)=transaction {
        BusinessesTable.select{BusinessesTable.id eq businessId}.forUpdate().first()
        requireStaffOnDuty(businessId,userId)
        require(HospitalityShiftsTable.select {
            (HospitalityShiftsTable.businessId eq businessId) and (HospitalityShiftsTable.status eq "OPEN")
        }.any()) { "Open a shift before settling hospitality payments" }
        val order=OrdersTable.select{
            (OrdersTable.id eq orderId) and (OrdersTable.businessId eq businessId) and
                (OrdersTable.serviceType inList listOf("DINE_IN","TAKEAWAY","DELIVERY")) and (OrdersTable.tabStatus inList HospitalityService.ACTIVE_TAB_STATUSES)
        }.forUpdate().firstOrNull()?:error("Active hospitality tab not found")
        require(OrderSplitPaymentsTable.select{OrderSplitPaymentsTable.orderId eq orderId}.none()){"This tab has already been split"}
        require(r.payments.size>=2){"At least two payment lines are required"}
        require(r.payments.all{it.amount.isFinite()&&it.amount>0&&it.method.equals("CASH",true)}){"Only verified cash split payments are currently supported"}
        val alreadyPaid=PaymentsTable.select { (PaymentsTable.businessId eq businessId) and (PaymentsTable.orderId eq orderId) and (PaymentsTable.status eq "SUCCESS") }.sumOf { it[PaymentsTable.amount] }
        val remaining=(order[OrdersTable.subtotal]-alreadyPaid).coerceAtLeast(0.0)
        require(kotlin.math.abs(r.payments.sumOf{it.amount}-remaining)<0.01){"Split payments must equal the outstanding balance"}
        val now=Clock.System.now()
        r.payments.forEach{line->
            OrderSplitPaymentsTable.insert{it[id]=generateId();it[OrderSplitPaymentsTable.businessId]=businessId;it[OrderSplitPaymentsTable.orderId]=orderId;it[amount]=line.amount;it[method]="CASH";it[status]="SUCCESS";it[createdBy]=userId;it[createdAt]=now}
            PaymentsTable.insert{it[id]=generateId();it[PaymentsTable.businessId]=businessId;it[PaymentsTable.orderId]=orderId;it[transactionCode]="SPLIT-CASH-${generateId().take(8)}";it[amount]=line.amount;it[payerPhone]=line.phone.orEmpty();it[payerName]=order[OrdersTable.customerName];it[method]="CASH";it[status]="SUCCESS";it[channel]="HOSPITALITY_SPLIT";it[collectedByUserId]=userId;it[reconciled]=true;it[transactionDate]=now}
        }
        OrdersTable.update({OrdersTable.id eq orderId}){it[paymentMethod]="SPLIT";it[paymentStatus]="PAID";it[completedAt]=now;it[settledByUserId]=userId;it[deliveryStatus]="DELIVERED";it[tabStatus]="CLOSED";it[updatedAt]=now}
        order[OrdersTable.hospitalityTableId]?.let{tableId->
            val another=OrdersTable.select{(OrdersTable.businessId eq businessId) and (OrdersTable.hospitalityTableId eq tableId) and (OrdersTable.id neq orderId) and (OrdersTable.tabStatus inList HospitalityService.ACTIVE_TAB_STATUSES)}.any()
            if(!another)HospitalityTablesTable.update({(HospitalityTablesTable.id eq tableId) and (HospitalityTablesTable.businessId eq businessId)}){it[status]="AVAILABLE";it[updatedAt]=now}
        }
        audit(businessId,userId,"BILL_SPLIT","ORDER",orderId);r.payments
    }
    fun report(businessId:String,startDate:String,endDate:String)=transaction {
        val zone=TimeZone.of("Africa/Nairobi");val start=LocalDate.parse(startDate).atStartOfDayIn(zone);val end=LocalDate.parse(endDate).plus(1,DateTimeUnit.DAY).atStartOfDayIn(zone)
        val orders=OrdersTable.select{(OrdersTable.businessId eq businessId) and (OrdersTable.serviceType inList listOf("DINE_IN","TAKEAWAY","DELIVERY")) and (OrdersTable.paymentStatus eq "PAID") and (OrdersTable.deliveryStatus neq "CANCELLED") and (OrdersTable.createdAt greaterEq start) and (OrdersTable.createdAt less end)}.toList();val ids=orders.map{it[OrdersTable.id]};val items=if(ids.isEmpty())emptyList() else OrderItemsTable.select{OrderItemsTable.orderId inList ids}.toList();val products=if(items.isEmpty())emptyMap() else ProductsTable.select{ProductsTable.id inList items.map{it[OrderItemsTable.productId]}}.associateBy{it[ProductsTable.id]};val users=UsersTable.select{UsersTable.businessId eq businessId}.associate{it[UsersTable.id] to it[UsersTable.name]};val tables=HospitalityTablesTable.select{HospitalityTablesTable.businessId eq businessId}.associate{it[HospitalityTablesTable.id] to it[HospitalityTablesTable.name]}
        fun orderBreakdown(key:(ResultRow)->String)=orders.groupBy(key).map{(label,rows)->ReportBreakdown(label,rows.size,rows.sumOf{it[OrdersTable.subtotal]})}.sortedByDescending{it.amount}
        fun itemBreakdown(key:(ResultRow)->String)=items.groupBy(key).map{(label,rows)->ReportBreakdown(label,rows.sumOf{it[OrderItemsTable.quantity]},rows.sumOf{if(it[OrderItemsTable.complimentary])0.0 else (it[OrderItemsTable.quantity]*(it[OrderItemsTable.unitPrice]+json.decodeFromString<List<MenuOption>>(it[OrderItemsTable.modifiersJson]).sumOf{m->m.priceDelta})-it[OrderItemsTable.discountAmount]).coerceAtLeast(0.0)})}.sortedByDescending{it.amount}
        var food=0.0;var beverage=0.0;items.forEach{item->val cost=item[OrderItemsTable.buyingPrice]*item[OrderItemsTable.quantity];if(isBeverageCategory(products[item[OrderItemsTable.productId]]?.get(ProductsTable.category).orEmpty()))beverage+=cost else food+=cost}
        val waste=BarStockEventsTable.select{(BarStockEventsTable.businessId eq businessId) and (BarStockEventsTable.recordedAt greaterEq start) and (BarStockEventsTable.recordedAt less end) and (BarStockEventsTable.eventType inList listOf("WASTAGE","BREAKAGE","SPILLAGE"))}.sumOf{event->event[BarStockEventsTable.quantity]*(event[BarStockEventsTable.ingredientId]?.let{id->InventoryIngredientsTable.select{InventoryIngredientsTable.id eq id}.firstOrNull()?.get(InventoryIngredientsTable.unitCost)}?:0.0)}
        val revenue=orders.sumOf{it[OrdersTable.subtotal]};val closed=orders.filter{it[OrdersTable.tabStatus]=="CLOSED"};val turnover=if(closed.isEmpty())0.0 else closed.map{(it[OrdersTable.updatedAt].toEpochMilliseconds()-it[OrdersTable.createdAt].toEpochMilliseconds())/60000.0}.average();val shiftRows=HospitalityShiftsTable.select{(HospitalityShiftsTable.businessId eq businessId) and (HospitalityShiftsTable.openedAt less end)}.toList()
        HospitalityReportResponse(orderBreakdown{users[it[OrdersTable.serverUserId]]?:"Unassigned"},orderBreakdown{tables[it[OrdersTable.hospitalityTableId]]?:it[OrdersTable.serviceType]},itemBreakdown{it[OrderItemsTable.productName]},itemBreakdown{products[it[OrderItemsTable.productId]]?.get(ProductsTable.category)?.ifBlank{"Uncategorised"}?:"Uncategorised"},orderBreakdown{"${it[OrdersTable.createdAt].toLocalDateTime(zone).hour.toString().padStart(2,'0')}:00"},orderBreakdown{it[OrdersTable.paymentMethod]},orderBreakdown{it[OrdersTable.salesChannel]},orderBreakdown{o->shiftRows.firstOrNull{s->o[OrdersTable.createdAt]>=s[HospitalityShiftsTable.openedAt]&&(s[HospitalityShiftsTable.closedAt]?.let{o[OrdersTable.createdAt]<it}?:true)}?.get(HospitalityShiftsTable.id)?.take(8)?:"No shift"},orderBreakdown{it[OrdersTable.serviceType]},food,beverage,waste,if(revenue>0)(revenue-food-beverage-waste)/revenue*100 else 0.0,turnover)
    }

    private fun reservations(b:String)=HospitalityReservationsTable.select{HospitalityReservationsTable.businessId eq b}.orderBy(HospitalityReservationsTable.reservedAt).map{ReservationResponse(it[HospitalityReservationsTable.id],it[HospitalityReservationsTable.tableId],it[HospitalityReservationsTable.customerName],it[HospitalityReservationsTable.customerPhone],it[HospitalityReservationsTable.guestCount],it[HospitalityReservationsTable.reservedAt].toString(),it[HospitalityReservationsTable.durationMinutes],it[HospitalityReservationsTable.status],it[HospitalityReservationsTable.notes])}
    private fun menuProfiles(b:String)=HospitalityMenuProfilesTable.select{HospitalityMenuProfilesTable.businessId eq b}.map{MenuProfileResponse(it[HospitalityMenuProfilesTable.productId],it[HospitalityMenuProfilesTable.preparationStation],it[HospitalityMenuProfilesTable.mealPeriods].split(',').filter(String::isNotBlank),json.decodeFromString(it[HospitalityMenuProfilesTable.sizesJson]),json.decodeFromString(it[HospitalityMenuProfilesTable.extrasJson]),json.decodeFromString(it[HospitalityMenuProfilesTable.variantsJson]),json.decodeFromString(it[HospitalityMenuProfilesTable.comboJson]),it[HospitalityMenuProfilesTable.soldOut],it[HospitalityMenuProfilesTable.happyHourPrice],it[HospitalityMenuProfilesTable.happyHourStart],it[HospitalityMenuProfilesTable.happyHourEnd],it[HospitalityMenuProfilesTable.ageRestricted],it[HospitalityMenuProfilesTable.minimumAge])}
    private fun ingredients(b:String)=InventoryIngredientsTable.select{InventoryIngredientsTable.businessId eq b}.orderBy(InventoryIngredientsTable.name).map{IngredientResponse(it[InventoryIngredientsTable.id],it[InventoryIngredientsTable.name],it[InventoryIngredientsTable.unit],it[InventoryIngredientsTable.quantity],it[InventoryIngredientsTable.reorderLevel],it[InventoryIngredientsTable.unitCost],it[InventoryIngredientsTable.quantity]<=it[InventoryIngredientsTable.reorderLevel],it[InventoryIngredientsTable.purchaseUnit],it[InventoryIngredientsTable.purchaseUnitSize])}
    private fun shifts(b:String)=HospitalityShiftsTable.select{HospitalityShiftsTable.businessId eq b}.orderBy(HospitalityShiftsTable.openedAt,SortOrder.DESC).map {
        val supplierCash = IngredientPurchasePaymentsTable.select { (IngredientPurchasePaymentsTable.businessId eq b) and (IngredientPurchasePaymentsTable.paidFromTill eq true) and (IngredientPurchasePaymentsTable.method eq "CASH") and (IngredientPurchasePaymentsTable.paidAt greaterEq it[HospitalityShiftsTable.openedAt]) and (IngredientPurchasePaymentsTable.paidAt less (it[HospitalityShiftsTable.closedAt] ?: Clock.System.now())) }.sumOf { payment -> payment[IngredientPurchasePaymentsTable.amount] }
        val expected = it[HospitalityShiftsTable.expectedCash]
        val actual = it[HospitalityShiftsTable.actualCash]
        val mpesa = it[HospitalityShiftsTable.mpesaTotal]
        val card = it[HospitalityShiftsTable.cardTotal]
        val actualMpesa = it[HospitalityShiftsTable.mpesaActual]
        val actualCard = it[HospitalityShiftsTable.cardActual]
        val cashVariance = if (expected != null && actual != null) actual - expected else null
        val mpesaVariance = if (mpesa != null && actualMpesa != null) actualMpesa - mpesa else null
        val cardVariance = if (card != null && actualCard != null) actualCard - card else null
        ShiftResponse(it[HospitalityShiftsTable.id],it[HospitalityShiftsTable.openedBy],it[HospitalityShiftsTable.openedAt].toString(),it[HospitalityShiftsTable.closedAt]?.toString(),it[HospitalityShiftsTable.openingFloat],expected,actual,mpesa,card,it[HospitalityShiftsTable.tipsTotal],it[HospitalityShiftsTable.expensesTotal],it[HospitalityShiftsTable.status],cashVariance,actualMpesa,actualCard,mpesaVariance,cardVariance,listOfNotNull(cashVariance,mpesaVariance,cardVariance).sum(),supplierCash)
    }
    private fun suppliers(b:String)=SuppliersTable.select{SuppliersTable.businessId eq b}.orderBy(SuppliersTable.name).map{SupplierResponse(it[SuppliersTable.id],it[SuppliersTable.name],it[SuppliersTable.phone],it[SuppliersTable.email],it[SuppliersTable.address],it[SuppliersTable.isActive])}
    private fun purchaseOrders(b:String)=PurchaseOrdersTable.select { PurchaseOrdersTable.businessId eq b }.orderBy(PurchaseOrdersTable.orderedAt,SortOrder.DESC).map { po ->
        val lines=PurchaseOrderItemsTable.select { PurchaseOrderItemsTable.purchaseOrderId eq po[PurchaseOrdersTable.id] }.map { line ->
            val ingredient=InventoryIngredientsTable.select { (InventoryIngredientsTable.id eq line[PurchaseOrderItemsTable.ingredientId]) and (InventoryIngredientsTable.businessId eq b) }.first()
            PurchaseOrderLineResponse(line[PurchaseOrderItemsTable.ingredientId],ingredient[InventoryIngredientsTable.name],ingredient[InventoryIngredientsTable.unit],line[PurchaseOrderItemsTable.purchaseQuantity]?:line[PurchaseOrderItemsTable.orderedQuantity],line[PurchaseOrderItemsTable.purchaseUnit]?:ingredient[InventoryIngredientsTable.unit],line[PurchaseOrderItemsTable.purchaseUnitCost]?:line[PurchaseOrderItemsTable.unitCost],line[PurchaseOrderItemsTable.conversionFactor],line[PurchaseOrderItemsTable.orderedQuantity],line[PurchaseOrderItemsTable.unitCost])
        }
        ingredientPaymentTotals(po).let { totals -> PurchaseOrderResponse(po[PurchaseOrdersTable.id],po[PurchaseOrdersTable.orderNumber],po[PurchaseOrdersTable.supplierId],po[PurchaseOrdersTable.status],po[PurchaseOrdersTable.totalCost],po[PurchaseOrdersTable.orderedAt].toString(),po[PurchaseOrdersTable.receivedAt]?.toString(),lines,
            expenseId=ExpensesTable.select{(ExpensesTable.purchaseOrderId eq po[PurchaseOrdersTable.id]) and (ExpensesTable.businessId eq b)}.firstOrNull()?.get(ExpensesTable.id),paidAmount=totals.paid,outstandingAmount=totals.outstanding,paymentStatus=totals.status,
            payments=IngredientPurchasePaymentsTable.select{(IngredientPurchasePaymentsTable.purchaseOrderId eq po[PurchaseOrdersTable.id]) and (IngredientPurchasePaymentsTable.businessId eq b)}.orderBy(IngredientPurchasePaymentsTable.paidAt).map{IngredientPurchasePaymentResponse(it[IngredientPurchasePaymentsTable.id],it[IngredientPurchasePaymentsTable.amount],it[IngredientPurchasePaymentsTable.method],it[IngredientPurchasePaymentsTable.reference],it[IngredientPurchasePaymentsTable.paidAt].toString(),it[IngredientPurchasePaymentsTable.recordedBy],it[IngredientPurchasePaymentsTable.paidFromTill])}) }
    }
    private fun approvals(b:String)=ManagerApprovalsTable.select{ManagerApprovalsTable.businessId eq b}.orderBy(ManagerApprovalsTable.requestedAt,SortOrder.DESC).map{ApprovalResponse(it[ManagerApprovalsTable.id],it[ManagerApprovalsTable.actionType],it[ManagerApprovalsTable.entityType],it[ManagerApprovalsTable.entityId],it[ManagerApprovalsTable.requestedBy],it[ManagerApprovalsTable.approvedBy],it[ManagerApprovalsTable.status],it[ManagerApprovalsTable.reason],it[ManagerApprovalsTable.requestedAt].toString(),runCatching{json.decodeFromString<ApprovalRequest>(it[ManagerApprovalsTable.payload]).amount}.getOrNull(),runCatching{json.decodeFromString<ApprovalRequest>(it[ManagerApprovalsTable.payload]).quantity}.getOrNull(),runCatching{json.decodeFromString<ApprovalRequest>(it[ManagerApprovalsTable.payload]).eventType}.getOrNull())}
    private fun audit(b:String,u:String,a:String,t:String,id:String?,d:String="{}"){AuditEventsTable.insert{it[AuditEventsTable.id]=generateId();it[businessId]=b;it[userId]=u;it[action]=a;it[entityType]=t;it[entityId]=id;it[details]=d;it[occurredAt]=Clock.System.now()}}
}

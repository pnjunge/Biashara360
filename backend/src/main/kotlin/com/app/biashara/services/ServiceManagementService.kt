package com.app.biashara.services

import com.app.biashara.auth.generateId
import com.app.biashara.db.*
import com.app.biashara.models.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class ServiceManagementService(private val orderService: OrderService) {
    fun isEnabled(businessId: String): Boolean = transaction {
        BusinessesTable.select { BusinessesTable.id eq businessId }.firstOrNull()?.get(BusinessesTable.servicesEnabled) == true
    }

    fun setEnabled(businessId: String, enabled: Boolean): Map<String, Boolean> = transaction {
        require(BusinessesTable.update({ BusinessesTable.id eq businessId }) {
            it[servicesEnabled] = enabled
            it[updatedAt] = Clock.System.now()
        } == 1) { "Business not found" }
        mapOf("enabled" to enabled)
    }

    private val activeStatuses = setOf("BOOKED", "CONFIRMED", "CHECKED_IN", "IN_PROGRESS")
    private val statuses = setOf("BOOKED", "CONFIRMED", "CHECKED_IN", "IN_PROGRESS", "COMPLETED", "CANCELLED", "NO_SHOW")

    fun schedule(businessId: String, from: Instant? = null, to: Instant? = null): ServiceScheduleResponse = transaction {
        ServiceScheduleResponse(
            services = listServices(businessId),
            resources = listResources(businessId),
            appointments = listAppointments(businessId, from, to),
        )
    }

    fun listServices(businessId: String): List<ServiceCatalogResponse> = transaction {
        BusinessServicesTable.select { BusinessServicesTable.businessId eq businessId }
            .orderBy(BusinessServicesTable.name to SortOrder.ASC)
            .map(::serviceResponse)
    }

    fun createService(businessId: String, request: ServiceCatalogRequest): ServiceCatalogResponse = transaction {
        val name = request.name.trim()
        validateService(name, request.durationMinutes, request.price)
        require(BusinessServicesTable.select {
            (BusinessServicesTable.businessId eq businessId) and (BusinessServicesTable.name eq name)
        }.empty()) { "A service with this name already exists" }
        val now = Clock.System.now()
        val id = generateId()
        BusinessServicesTable.insert {
            it[BusinessServicesTable.id] = id
            it[BusinessServicesTable.businessId] = businessId
            it[BusinessServicesTable.name] = name
            it[description] = request.description.trim().take(500)
            it[category] = request.category.trim().take(80)
            it[durationMinutes] = request.durationMinutes
            it[price] = request.price
            it[isActive] = request.isActive
            it[createdAt] = now
            it[updatedAt] = now
        }
        serviceResponse(BusinessServicesTable.select { BusinessServicesTable.id eq id }.single())
    }

    fun updateService(businessId: String, id: String, request: ServiceCatalogRequest): ServiceCatalogResponse = transaction {
        val name = request.name.trim()
        validateService(name, request.durationMinutes, request.price)
        require(BusinessServicesTable.select {
            (BusinessServicesTable.businessId eq businessId) and
                (BusinessServicesTable.name eq name) and (BusinessServicesTable.id neq id)
        }.empty()) { "A service with this name already exists" }
        require(BusinessServicesTable.update({
            (BusinessServicesTable.id eq id) and (BusinessServicesTable.businessId eq businessId)
        }) {
            it[BusinessServicesTable.name] = name
            it[description] = request.description.trim().take(500)
            it[category] = request.category.trim().take(80)
            it[durationMinutes] = request.durationMinutes
            it[price] = request.price
            it[isActive] = request.isActive
            it[updatedAt] = Clock.System.now()
        } == 1) { "Service not found" }
        serviceResponse(BusinessServicesTable.select { BusinessServicesTable.id eq id }.single())
    }

    fun listResources(businessId: String): List<ServiceResourceResponse> = transaction {
        ServiceResourcesTable.select { ServiceResourcesTable.businessId eq businessId }
            .orderBy(ServiceResourcesTable.name to SortOrder.ASC)
            .map(::resourceResponse)
    }

    fun createResource(businessId: String, request: ServiceResourceRequest): ServiceResourceResponse = transaction {
        val name = request.name.trim()
        require(name.isNotBlank() && name.length <= 120) { "Resource name is required and must be 120 characters or fewer" }
        require(ServiceResourcesTable.select {
            (ServiceResourcesTable.businessId eq businessId) and (ServiceResourcesTable.name eq name)
        }.empty()) { "A resource with this name already exists" }
        val now = Clock.System.now()
        val id = generateId()
        ServiceResourcesTable.insert {
            it[ServiceResourcesTable.id] = id
            it[ServiceResourcesTable.businessId] = businessId
            it[ServiceResourcesTable.name] = name
            it[type] = request.type.trim().ifBlank { "RESOURCE" }.uppercase().take(50)
            it[isActive] = request.isActive
            it[createdAt] = now
            it[updatedAt] = now
        }
        resourceResponse(ServiceResourcesTable.select { ServiceResourcesTable.id eq id }.single())
    }

    fun updateResource(businessId: String, id: String, request: ServiceResourceRequest): ServiceResourceResponse = transaction {
        val name = request.name.trim()
        require(name.isNotBlank() && name.length <= 120) { "Resource name is required and must be 120 characters or fewer" }
        require(ServiceResourcesTable.select {
            (ServiceResourcesTable.businessId eq businessId) and
                (ServiceResourcesTable.name eq name) and (ServiceResourcesTable.id neq id)
        }.empty()) { "A resource with this name already exists" }
        require(ServiceResourcesTable.update({
            (ServiceResourcesTable.id eq id) and (ServiceResourcesTable.businessId eq businessId)
        }) {
            it[ServiceResourcesTable.name] = name
            it[type] = request.type.trim().ifBlank { "RESOURCE" }.uppercase().take(50)
            it[isActive] = request.isActive
            it[updatedAt] = Clock.System.now()
        } == 1) { "Resource not found" }
        resourceResponse(ServiceResourcesTable.select { ServiceResourcesTable.id eq id }.single())
    }

    fun listAppointments(businessId: String, from: Instant? = null, to: Instant? = null): List<ServiceAppointmentResponse> = transaction {
        val services = BusinessServicesTable.select { BusinessServicesTable.businessId eq businessId }.associateBy { it[BusinessServicesTable.id] }
        val resources = ServiceResourcesTable.select { ServiceResourcesTable.businessId eq businessId }.associateBy { it[ServiceResourcesTable.id] }
        ServiceAppointmentsTable.select { ServiceAppointmentsTable.businessId eq businessId }
            .orderBy(ServiceAppointmentsTable.startsAt to SortOrder.ASC)
            .mapNotNull { row ->
                val starts = row[ServiceAppointmentsTable.startsAt]
                if (from != null && starts < from || to != null && starts > to) return@mapNotNull null
                appointmentResponse(row, services, resources)
            }
    }

    fun createAppointment(businessId: String, userId: String?, request: ServiceAppointmentRequest): ServiceAppointmentResponse = transaction {
        val service = BusinessServicesTable.select {
            (BusinessServicesTable.id eq request.serviceId) and (BusinessServicesTable.businessId eq businessId)
        }.singleOrNull() ?: error("Service not found")
        require(service[BusinessServicesTable.isActive]) { "This service is not active" }
        val starts = parseStart(request.startsAt)
        val duration = request.durationMinutes ?: service[BusinessServicesTable.durationMinutes]
        validateAppointment(request.customerName, duration, request.customerPhone)
        val resource = request.resourceId?.let {
            ServiceResourcesTable.select {
                (ServiceResourcesTable.id eq it) and
                    (ServiceResourcesTable.businessId eq businessId) and
                    (ServiceResourcesTable.isActive eq true)
            }.singleOrNull() ?: error("Resource not found")
        }
        request.customerId?.let {
            require(CustomersTable.select { (CustomersTable.id eq it) and (CustomersTable.businessId eq businessId) }.any()) { "Customer not found" }
        }
        request.staffUserId?.let {
            require(UsersTable.select { (UsersTable.id eq it) and (UsersTable.businessId eq businessId) and (UsersTable.isActive eq true) }.any()) { "Staff member not found" }
        }
        ensureAvailable(businessId, starts, duration, request.staffUserId, request.resourceId, null)
        val now = Clock.System.now()
        val id = generateId()
        ServiceAppointmentsTable.insert {
            it[ServiceAppointmentsTable.id] = id
            it[ServiceAppointmentsTable.businessId] = businessId
            it[serviceId] = request.serviceId
            it[resourceId] = resource?.get(ServiceResourcesTable.id)
            it[customerId] = request.customerId
            it[staffUserId] = request.staffUserId
            it[customerName] = request.customerName.trim().take(255)
            it[customerPhone] = request.customerPhone.trim().take(20)
            it[startsAt] = starts
            it[durationMinutes] = duration
            it[status] = "BOOKED"
            it[notes] = request.notes.trim().take(500)
            it[createdBy] = userId
            it[createdAt] = now
            it[updatedAt] = now
        }
        appointmentResponse(
            ServiceAppointmentsTable.select { ServiceAppointmentsTable.id eq id }.single(),
            mapOf(service[BusinessServicesTable.id] to service),
            resource?.let { mapOf(it[ServiceResourcesTable.id] to it) } ?: emptyMap(),
        )
    }

    fun updateAppointment(businessId: String, id: String, request: ServiceAppointmentRequest): ServiceAppointmentResponse = transaction {
        require(ServiceAppointmentsTable.select {
            (ServiceAppointmentsTable.id eq id) and (ServiceAppointmentsTable.businessId eq businessId)
        }.any()) { "Appointment not found" }
        val service = BusinessServicesTable.select {
            (BusinessServicesTable.id eq request.serviceId) and (BusinessServicesTable.businessId eq businessId)
        }.singleOrNull() ?: error("Service not found")
        val starts = parseStart(request.startsAt)
        val duration = request.durationMinutes ?: service[BusinessServicesTable.durationMinutes]
        validateAppointment(request.customerName, duration, request.customerPhone)
        request.resourceId?.let {
            require(ServiceResourcesTable.select { (ServiceResourcesTable.id eq it) and (ServiceResourcesTable.businessId eq businessId) and (ServiceResourcesTable.isActive eq true) }.any()) { "Resource not found" }
        }
        ensureAvailable(businessId, starts, duration, request.staffUserId, request.resourceId, id)
        ServiceAppointmentsTable.update({ ServiceAppointmentsTable.id eq id }) {
            it[serviceId] = request.serviceId
            it[resourceId] = request.resourceId
            it[customerId] = request.customerId
            it[staffUserId] = request.staffUserId
            it[customerName] = request.customerName.trim().take(255)
            it[customerPhone] = request.customerPhone.trim().take(20)
            it[startsAt] = starts
            it[durationMinutes] = duration
            it[notes] = request.notes.trim().take(500)
            it[updatedAt] = Clock.System.now()
        }
        appointmentResponseById(businessId, id)
    }

    fun updateAppointmentStatus(businessId: String, id: String, status: String): ServiceAppointmentResponse = transaction {
        val normalized = status.trim().uppercase()
        require(normalized in statuses) { "Unsupported appointment status" }
        require(ServiceAppointmentsTable.update({
            (ServiceAppointmentsTable.id eq id) and (ServiceAppointmentsTable.businessId eq businessId)
        }) {
            it[ServiceAppointmentsTable.status] = normalized
            it[updatedAt] = Clock.System.now()
        } == 1) { "Appointment not found" }
        appointmentResponseById(businessId, id)
    }

    fun checkoutAppointment(businessId: String, id: String, request: ServiceAppointmentCheckoutRequest): OrderResponse = transaction {
        val method = request.paymentMethod.trim().uppercase()
        require(method in setOf("CASH", "MPESA", "CARD")) { "Payment method must be CASH, MPESA, or CARD" }
        val appointment = ServiceAppointmentsTable.select {
            (ServiceAppointmentsTable.id eq id) and (ServiceAppointmentsTable.businessId eq businessId)
        }.forUpdate().singleOrNull() ?: error("Appointment not found")
        appointment[ServiceAppointmentsTable.orderId]?.let { existing ->
            val existingOrder = orderService.getById(existing, businessId) ?: error("Appointment order not found")
            if (existingOrder.paymentStatus != "PAID" && method == "CASH") {
                val now = Clock.System.now()
                OrdersTable.update({ (OrdersTable.id eq existing) and (OrdersTable.paymentStatus neq "PAID") }) {
                    it[paymentStatus] = "PAID"; it[paymentMethod] = "CASH"; it[deliveryStatus] = "DELIVERED"; it[updatedAt] = now
                }
                if (PaymentsTable.select { (PaymentsTable.orderId eq existing) and (PaymentsTable.status eq "SUCCESS") }.empty()) {
                    PaymentsTable.insert {
                        it[PaymentsTable.id] = generateId(); it[PaymentsTable.businessId] = businessId; it[PaymentsTable.orderId] = existing
                        it[transactionCode] = "CASH-${existingOrder.orderNumber}"; it[amount] = existingOrder.subtotal
                        it[payerPhone] = appointment[ServiceAppointmentsTable.customerPhone]; it[payerName] = appointment[ServiceAppointmentsTable.customerName]
                        it[PaymentsTable.method] = "CASH"; it[status] = "SUCCESS"; it[channel] = "SERVICE_CHECKOUT"
                        it[reconciled] = true; it[transactionDate] = now
                    }
                }
                ServiceAppointmentsTable.update({ ServiceAppointmentsTable.id eq id }) { it[status] = "COMPLETED"; it[updatedAt] = now }
            } else if (existingOrder.paymentStatus != "PAID" && existingOrder.paymentMethod != method) {
                OrdersTable.update({ OrdersTable.id eq existing }) { it[paymentMethod] = method; it[updatedAt] = Clock.System.now() }
            }
            return@transaction orderService.getById(existing, businessId) ?: error("Appointment order not found")
        }
        require(appointment[ServiceAppointmentsTable.status] in setOf("CHECKED_IN", "IN_PROGRESS")) { "Check in the appointment before checkout" }
        require(request.discountAmount.isFinite() && request.discountAmount >= 0.0) { "Discount must be zero or greater" }
        require(request.addOns.all { it.quantity in 1..10_000 }) { "Add-on quantity must be between 1 and 10,000" }
        require(request.addOns.map { it.productId }.distinct().size == request.addOns.size) { "Each add-on may only be selected once" }

        val service = BusinessServicesTable.select {
            (BusinessServicesTable.id eq appointment[ServiceAppointmentsTable.serviceId]) and
                (BusinessServicesTable.businessId eq businessId)
        }.singleOrNull() ?: error("Service not found")
        val products = if (request.addOns.isEmpty()) emptyMap() else ProductsTable.select {
            (ProductsTable.businessId eq businessId) and
                (ProductsTable.id inList request.addOns.map { it.productId }) and
                (ProductsTable.isActive eq true)
        }.forUpdate().associateBy { it[ProductsTable.id] }
        require(products.size == request.addOns.size) { "One or more add-ons are unavailable" }
        request.addOns.forEach { item ->
            require(products.getValue(item.productId)[ProductsTable.currentStock] >= item.quantity) {
                "Insufficient stock for ${products.getValue(item.productId)[ProductsTable.name]}"
            }
        }
        val gross = service[BusinessServicesTable.price] + request.addOns.sumOf { products.getValue(it.productId)[ProductsTable.sellingPrice] * it.quantity }
        require(request.discountAmount <= gross) { "Discount cannot exceed the checkout total" }
        val total = (gross - request.discountAmount).coerceAtLeast(0.0)
        val now = Clock.System.now()
        val orderId = generateId()
        val orderNumber = "B360-SVC-${java.util.UUID.randomUUID().toString().replace("-", "").take(8).uppercase()}"
        val paid = method == "CASH" || total == 0.0
        OrdersTable.insert {
            it[OrdersTable.id] = orderId; it[OrdersTable.orderNumber] = orderNumber; it[OrdersTable.businessId] = businessId
            it[clientReference] = "appointment:$id"; it[customerId] = appointment[ServiceAppointmentsTable.customerId]
            it[customerName] = appointment[ServiceAppointmentsTable.customerName]; it[customerPhone] = appointment[ServiceAppointmentsTable.customerPhone]
            it[deliveryLocation] = "Appointment"; it[paymentStatus] = if (paid) "PAID" else "PENDING"
            it[deliveryStatus] = if (paid) "DELIVERED" else "PROCESSING"; it[paymentMethod] = method; it[salesChannel] = "WEB"
            it[serviceType] = "SERVICE"; it[serverUserId] = appointment[ServiceAppointmentsTable.staffUserId]; it[tabStatus] = "CLOSED"
            it[baseAmount] = total; it[taxIncluded] = false; it[taxRate] = 0.0; it[taxAmount] = 0.0; it[subtotal] = total
            it[notes] = "Appointment $id"; it[createdAt] = now; it[updatedAt] = now
        }
        OrderItemsTable.insert {
            it[OrderItemsTable.id] = generateId(); it[OrderItemsTable.orderId] = orderId; it[OrderItemsTable.productId] = service[BusinessServicesTable.id]
            it[OrderItemsTable.productName] = service[BusinessServicesTable.name]; it[OrderItemsTable.quantity] = 1; it[OrderItemsTable.unitPrice] = service[BusinessServicesTable.price]
            it[OrderItemsTable.buyingPrice] = 0.0; it[OrderItemsTable.discountAmount] = request.discountAmount; it[OrderItemsTable.complimentary] = total == 0.0
        }
        request.addOns.forEach { item ->
            val product = products.getValue(item.productId)
            OrderItemsTable.insert {
                it[OrderItemsTable.id] = generateId(); it[OrderItemsTable.orderId] = orderId; it[OrderItemsTable.productId] = item.productId
                it[OrderItemsTable.productName] = product[ProductsTable.name]; it[OrderItemsTable.quantity] = item.quantity; it[OrderItemsTable.unitPrice] = product[ProductsTable.sellingPrice]
                it[OrderItemsTable.buyingPrice] = product[ProductsTable.buyingPrice]
            }
            require(ProductsTable.update({ (ProductsTable.id eq item.productId) and (ProductsTable.currentStock greaterEq item.quantity) }) {
                with(SqlExpressionBuilder) { it.update(currentStock, currentStock - item.quantity) }; it[updatedAt] = now
            } == 1) { "Insufficient stock for ${product[ProductsTable.name]}" }
        }
        ServiceAppointmentsTable.update({ ServiceAppointmentsTable.id eq id }) {
            it[ServiceAppointmentsTable.orderId] = orderId
            if (paid) it[status] = "COMPLETED"
            it[updatedAt] = now
        }
        if (paid) PaymentsTable.insert {
            it[PaymentsTable.id] = generateId(); it[PaymentsTable.businessId] = businessId; it[PaymentsTable.orderId] = orderId
            it[transactionCode] = if (total == 0.0) "DISCOUNT-$orderNumber" else "CASH-$orderNumber"; it[amount] = total
            it[payerPhone] = appointment[ServiceAppointmentsTable.customerPhone]; it[payerName] = appointment[ServiceAppointmentsTable.customerName]
            it[PaymentsTable.method] = if (total == 0.0) "DISCOUNT" else "CASH"; it[status] = "SUCCESS"; it[channel] = "SERVICE_CHECKOUT"
            it[reconciled] = true; it[transactionDate] = now
        }
        orderService.getById(orderId, businessId) ?: error("Checkout order not found")
    }

    fun seedTemplates(businessId: String): ServiceScheduleResponse = transaction {
        val type = BusinessesTable.select { BusinessesTable.id eq businessId }.singleOrNull()?.get(BusinessesTable.type)?.uppercase() ?: "SERVICE"
        templatesFor(type).forEach { template ->
            if (BusinessServicesTable.select {
                (BusinessServicesTable.businessId eq businessId) and (BusinessServicesTable.name eq template.name)
            }.empty()) {
                val now = Clock.System.now()
                BusinessServicesTable.insert {
                    it[id] = generateId()
                    it[BusinessServicesTable.businessId] = businessId
                    it[name] = template.name
                    it[description] = template.description
                    it[category] = template.category
                    it[durationMinutes] = template.durationMinutes
                    it[price] = template.price
                    it[isActive] = true
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }
        }
        schedule(businessId)
    }

    private fun appointmentResponseById(businessId: String, id: String): ServiceAppointmentResponse {
        val services = BusinessServicesTable.select { BusinessServicesTable.businessId eq businessId }.associateBy { it[BusinessServicesTable.id] }
        val resources = ServiceResourcesTable.select { ServiceResourcesTable.businessId eq businessId }.associateBy { it[ServiceResourcesTable.id] }
        return appointmentResponse(ServiceAppointmentsTable.select { ServiceAppointmentsTable.id eq id }.single(), services, resources)
    }

    private fun ensureAvailable(businessId: String, starts: Instant, duration: Int, staffUserId: String?, resourceId: String?, excludedId: String?) {
        if (staffUserId == null && resourceId == null) return
        val startSeconds = starts.epochSeconds
        val endSeconds = startSeconds + duration * 60L
        ServiceAppointmentsTable.select { ServiceAppointmentsTable.businessId eq businessId }.forEach { existing ->
            if (existing[ServiceAppointmentsTable.id] == excludedId || existing[ServiceAppointmentsTable.status] !in activeStatuses) return@forEach
            if (staffUserId != null && existing[ServiceAppointmentsTable.staffUserId] != staffUserId && resourceId == null) return@forEach
            if (resourceId != null && existing[ServiceAppointmentsTable.resourceId] != resourceId && staffUserId == null) return@forEach
            if (staffUserId != null && resourceId != null && existing[ServiceAppointmentsTable.staffUserId] != staffUserId && existing[ServiceAppointmentsTable.resourceId] != resourceId) return@forEach
            val existingStart = existing[ServiceAppointmentsTable.startsAt].epochSeconds
            val existingEnd = existingStart + existing[ServiceAppointmentsTable.durationMinutes] * 60L
            require(startSeconds >= existingEnd || endSeconds <= existingStart) { "The selected staff member or resource is already booked for this time" }
        }
    }

    private fun validateService(name: String, duration: Int, price: Double) {
        require(name.isNotBlank() && name.length <= 160) { "Service name is required and must be 160 characters or fewer" }
        require(duration in 5..10080) { "Duration must be between 5 minutes and 7 days" }
        require(price >= 0.0) { "Price cannot be negative" }
    }

    private fun validateAppointment(customerName: String, duration: Int, phone: String) {
        require(customerName.trim().isNotBlank()) { "Customer name is required" }
        require(duration in 5..10080) { "Duration must be between 5 minutes and 7 days" }
        require(phone.length <= 20) { "Customer phone is too long" }
    }

    private fun parseStart(value: String): Instant = runCatching { Instant.parse(value) }.getOrElse { error("startsAt must be a valid ISO-8601 timestamp") }

    private fun serviceResponse(row: ResultRow) = ServiceCatalogResponse(
        id = row[BusinessServicesTable.id], name = row[BusinessServicesTable.name], description = row[BusinessServicesTable.description],
        category = row[BusinessServicesTable.category], durationMinutes = row[BusinessServicesTable.durationMinutes], price = row[BusinessServicesTable.price],
        isActive = row[BusinessServicesTable.isActive], createdAt = row[BusinessServicesTable.createdAt].toString(), updatedAt = row[BusinessServicesTable.updatedAt].toString(),
    )

    private fun resourceResponse(row: ResultRow) = ServiceResourceResponse(row[ServiceResourcesTable.id], row[ServiceResourcesTable.name], row[ServiceResourcesTable.type], row[ServiceResourcesTable.isActive])

    private fun appointmentResponse(row: ResultRow, services: Map<String, ResultRow>, resources: Map<String, ResultRow>): ServiceAppointmentResponse {
        val service = services[row[ServiceAppointmentsTable.serviceId]]
        val resource = row[ServiceAppointmentsTable.resourceId]?.let(resources::get)
        return ServiceAppointmentResponse(
            id = row[ServiceAppointmentsTable.id], serviceId = row[ServiceAppointmentsTable.serviceId], serviceName = service?.get(BusinessServicesTable.name) ?: "Service",
            resourceId = row[ServiceAppointmentsTable.resourceId], resourceName = resource?.get(ServiceResourcesTable.name), customerId = row[ServiceAppointmentsTable.customerId],
            customerName = row[ServiceAppointmentsTable.customerName], customerPhone = row[ServiceAppointmentsTable.customerPhone], staffUserId = row[ServiceAppointmentsTable.staffUserId],
            startsAt = row[ServiceAppointmentsTable.startsAt].toString(), durationMinutes = row[ServiceAppointmentsTable.durationMinutes], status = row[ServiceAppointmentsTable.status],
            notes = row[ServiceAppointmentsTable.notes], orderId = row[ServiceAppointmentsTable.orderId], createdAt = row[ServiceAppointmentsTable.createdAt].toString(), updatedAt = row[ServiceAppointmentsTable.updatedAt].toString(),
        )
    }

    private fun templatesFor(type: String): List<ServiceCatalogRequest> = when {
        type.contains("SALON") || type.contains("BARBER") -> listOf(
            ServiceCatalogRequest("Haircut", category = "Hair", durationMinutes = 45), ServiceCatalogRequest("Hair wash & style", category = "Hair", durationMinutes = 60),
            ServiceCatalogRequest("Beard trim", category = "Grooming", durationMinutes = 30), ServiceCatalogRequest("Color treatment", category = "Hair", durationMinutes = 120),
        )
        type.contains("SPA") -> listOf(
            ServiceCatalogRequest("Massage", category = "Wellness", durationMinutes = 60), ServiceCatalogRequest("Facial", category = "Wellness", durationMinutes = 45),
            ServiceCatalogRequest("Manicure", category = "Beauty", durationMinutes = 45), ServiceCatalogRequest("Pedicure", category = "Beauty", durationMinutes = 60),
        )
        type.contains("LAUNDRY") -> listOf(
            ServiceCatalogRequest("Wash and fold", category = "Laundry", durationMinutes = 1440), ServiceCatalogRequest("Dry cleaning", category = "Laundry", durationMinutes = 2880),
            ServiceCatalogRequest("Express ironing", category = "Laundry", durationMinutes = 120),
        )
        type.contains("CAR") && type.contains("WASH") -> listOf(
            ServiceCatalogRequest("Basic wash", category = "Wash", durationMinutes = 30), ServiceCatalogRequest("Premium detail", category = "Detailing", durationMinutes = 90),
            ServiceCatalogRequest("Interior cleaning", category = "Detailing", durationMinutes = 60),
        )
        type.contains("GYM") -> listOf(
            ServiceCatalogRequest("Personal training", category = "Training", durationMinutes = 60), ServiceCatalogRequest("Group class", category = "Classes", durationMinutes = 60),
            ServiceCatalogRequest("Fitness assessment", category = "Membership", durationMinutes = 45),
        )
        type.contains("CLINIC") -> listOf(
            ServiceCatalogRequest("Consultation", category = "Clinical", durationMinutes = 30), ServiceCatalogRequest("Follow-up visit", category = "Clinical", durationMinutes = 20),
            ServiceCatalogRequest("Procedure", category = "Clinical", durationMinutes = 60),
        )
        type.contains("REPAIR") -> listOf(
            ServiceCatalogRequest("Diagnostics", category = "Repair", durationMinutes = 30), ServiceCatalogRequest("Repair slot", category = "Repair", durationMinutes = 120),
            ServiceCatalogRequest("Collection and delivery", category = "Fulfilment", durationMinutes = 30),
        )
        type.contains("HOTEL") || type.contains("LODGE") -> listOf(
            ServiceCatalogRequest("Room cleaning", category = "Housekeeping", durationMinutes = 60), ServiceCatalogRequest("Airport transfer", category = "Guest services", durationMinutes = 90),
            ServiceCatalogRequest("Spa treatment", category = "Guest services", durationMinutes = 60),
        )
        type.contains("WHOLESALE") || type.contains("DISTRIBUT") || type.contains("GROCERY") || type.contains("BOUTIQUE") || type.contains("RETAIL") -> listOf(
            ServiceCatalogRequest("Product consultation", category = "Sales", durationMinutes = 30), ServiceCatalogRequest("Delivery slot", category = "Fulfilment", durationMinutes = 60),
            ServiceCatalogRequest("Custom order", category = "Sales", durationMinutes = 30),
        )
        else -> listOf(ServiceCatalogRequest("Consultation", category = "General", durationMinutes = 30), ServiceCatalogRequest("General service", category = "General", durationMinutes = 60))
    }
}

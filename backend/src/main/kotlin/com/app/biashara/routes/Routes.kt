package com.app.biashara.routes

import com.app.biashara.auth.generateId
import com.app.biashara.db.*
import com.app.biashara.models.*
import com.app.biashara.services.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.ktor.ext.inject

// ─── Auth Routes ──────────────────────────────────────────────────────────────

fun Route.authRoutes() {
    val authService: AuthService by inject()

    route("/auth") {
        post("/register") {
            val req = call.receive<RegisterRequest>()
            if (req.name.isBlank() || req.email.isBlank() || req.password.length < 6) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "Invalid registration data"))
                return@post
            }
            val result = authService.register(req)
            call.respond(if (result.success) HttpStatusCode.Created else HttpStatusCode.BadRequest, result)
        }

        post("/login") {
            val req = call.receive<LoginRequest>()
            val result = authService.login(req)
            call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.Unauthorized, result)
        }

        post("/verify-otp") {
            val req = call.receive<OtpVerifyRequest>()
            val result = authService.verifyOtp(req)
            call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.Unauthorized, result)
        }

        post("/refresh") {
            val req = call.receive<RefreshTokenRequest>()
            val result = authService.refreshToken(req)
            call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.Unauthorized, result)
        }
    }
}

// ─── Product Routes ───────────────────────────────────────────────────────────

fun Route.productRoutes() {
    val productService: ProductService by inject()

    route("/products") {
        moduleGuard("INVENTORY")
        get {
            val businessId = call.businessId()
            val query = call.request.queryParameters["q"]
            val lowStock = call.request.queryParameters["lowStock"]?.toBoolean() ?: false
            val products = productService.getAll(businessId, query, lowStock)
            call.respond(ApiResponse(true, data = products))
        }

        post {
            val businessId = call.businessId()
            val req = call.receive<ProductRequest>()
            val result = productService.create(businessId, req)
            call.respond(if (result.success) HttpStatusCode.Created else HttpStatusCode.BadRequest, result)
        }

        route("/{id}") {
            get {
                val businessId = call.businessId()
                val id = call.parameters["id"]!!
                val product = productService.getById(id, businessId)
                if (product == null) call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Product not found"))
                else call.respond(ApiResponse(true, data = product))
            }

            put {
                val businessId = call.businessId()
                val id = call.parameters["id"]!!
                val req = call.receive<ProductRequest>()
                val result = productService.update(id, businessId, req)
                call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.NotFound, result)
            }

            delete {
                val businessId = call.businessId()
                if (!call.hasRole("ADMIN")) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Admin access required"))
                    return@delete
                }
                val id = call.parameters["id"]!!
                val result = productService.delete(id, businessId)
                call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.NotFound, result)
            }

            post("/stock") {
                val businessId = call.businessId()
                val id = call.parameters["id"]!!
                val req = call.receive<StockUpdateRequest>()
                val result = productService.updateStock(id, businessId, req)
                call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, result)
            }
        }
    }
}

// ─── Order Routes ─────────────────────────────────────────────────────────────

fun Route.orderRoutes() {
    val orderService: OrderService by inject()

    route("/orders") {
        moduleGuard("SALES")
        get {
            val businessId = call.businessId()
            val status = call.request.queryParameters["status"]
            val page = call.request.queryParameters["page"]?.toInt() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toInt() ?: 20
            val result = orderService.getAll(businessId, status, page, pageSize)
            call.respond(ApiResponse(true, data = result))
        }

        post {
            val businessId = call.businessId()
            val req = call.receive<CreateOrderRequest>()
            val result = orderService.create(businessId, req)
            call.respond(if (result.success) HttpStatusCode.Created else HttpStatusCode.BadRequest, result)
        }

        route("/{id}") {
            get {
                val businessId = call.businessId()
                val id = call.parameters["id"]!!
                val order = orderService.getById(id, businessId)
                if (order == null) call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Order not found"))
                else call.respond(ApiResponse(true, data = order))
            }

            patch("/payment-status") {
                val businessId = call.businessId()
                val id = call.parameters["id"]!!
                val req = call.receive<UpdatePaymentStatusRequest>()
                val result = orderService.updatePaymentStatus(id, businessId, req)
                call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.NotFound, result)
            }

            patch("/delivery-status") {
                val businessId = call.businessId()
                val id = call.parameters["id"]!!
                val req = call.receive<UpdateDeliveryStatusRequest>()
                val result = orderService.updateDeliveryStatus(id, businessId, req)
                call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.NotFound, result)
            }
        }
    }
}

// ─── Customer Routes ──────────────────────────────────────────────────────────

fun Route.customerRoutes() {
    val customerService: CustomerService by inject()

    route("/customers") {
        moduleGuard("CRM")
        get {
            val businessId = call.businessId()
            val query = call.request.queryParameters["q"]
            call.respond(ApiResponse(true, data = customerService.getAll(businessId, query)))
        }

        get("/top") {
            val businessId = call.businessId()
            val limit = call.request.queryParameters["limit"]?.toInt() ?: 10
            call.respond(ApiResponse(true, data = customerService.getTopCustomers(businessId, limit)))
        }

        post {
            val businessId = call.businessId()
            val req = call.receive<CustomerRequest>()
            val result = customerService.create(businessId, req)
            call.respond(if (result.success) HttpStatusCode.Created else HttpStatusCode.BadRequest, result)
        }

        route("/{id}") {
            get {
                val businessId = call.businessId()
                val id = call.parameters["id"]!!
                val customer = customerService.getById(id, businessId)
                if (customer == null) call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Customer not found"))
                else call.respond(ApiResponse(true, data = customer))
            }

            put {
                val businessId = call.businessId()
                val id = call.parameters["id"]!!
                val req = call.receive<CustomerRequest>()
                val result = customerService.update(id, businessId, req)
                call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.NotFound, result)
            }
        }
    }
}

// ─── Expense Routes ───────────────────────────────────────────────────────────

fun Route.expenseRoutes() {
    val expenseService: ExpenseService by inject()

    route("/expenses") {
        moduleGuard("EXPENSES")
        get {
            val businessId = call.businessId()
            val category = call.request.queryParameters["category"]
            val startDate = call.request.queryParameters["startDate"]
            val endDate = call.request.queryParameters["endDate"]
            call.respond(ApiResponse(true, data = expenseService.getAll(businessId, category, startDate, endDate)))
        }

        post {
            val businessId = call.businessId()
            val req = call.receive<ExpenseRequest>()
            val result = expenseService.create(businessId, req)
            call.respond(if (result.success) HttpStatusCode.Created else HttpStatusCode.BadRequest, result)
        }

        delete("/{id}") {
            val businessId = call.businessId()
            if (!call.hasRole("ADMIN")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Admin access required"))
                return@delete
            }
            val id = call.parameters["id"]!!
            val result = expenseService.delete(id, businessId)
            call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.NotFound, result)
        }
    }
}

// ─── Payment Routes ───────────────────────────────────────────────────────────

fun Route.paymentRoutes() {
    val paymentService: PaymentService by inject()
    val mpesaService: MpesaService by inject()
    val orderService: OrderService by inject()

    route("/payments") {
        moduleGuard("PAYMENTS")

        get {
            val businessId = call.businessId()
            val unreconciled = call.request.queryParameters["unreconciled"]?.toBoolean()
            call.respond(ApiResponse(true, data = paymentService.getAll(businessId, unreconciled?.let { !it })))
        }

        post("/initiate") {
            val businessId = call.businessId()
            val req = call.receive<InitiatePaymentRequest>()
            val order = orderService.getById(req.orderId, businessId)
                ?: run {
                    call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Order not found"))
                    return@post
                }

            val result = mpesaService.initiateSTKPush(
                phoneNumber = req.phoneNumber.normalizePhone(),
                amount = order.subtotal,
                accountReference = order.orderNumber,
                transactionDesc = "Payment for ${order.orderNumber}",
                businessId = businessId
            )

            when (result) {
                is StkPushResult.Success -> {
                    // Persist checkoutRequestId on the order so the Safaricom callback
                    // can resolve the tenant and update the order status.
                    transaction {
                        OrdersTable.update({
                            (OrdersTable.id eq req.orderId) and (OrdersTable.businessId eq businessId)
                        }) {
                            it[OrdersTable.stkCheckoutRequestId] = result.checkoutRequestId
                            it[OrdersTable.updatedAt] = Clock.System.now()
                        }
                    }
                    call.respond(ApiResponse(true, data = mapOf(
                        "merchantRequestId" to result.merchantRequestId,
                        "checkoutRequestId" to result.checkoutRequestId,
                        "responseCode" to result.responseCode,
                        "responseDescription" to "Success",
                        "customerMessage" to result.customerMessage
                    )))
                }
                is StkPushResult.Error -> call.respond(HttpStatusCode.BadGateway,
                    ApiResponse<Unit>(false, message = result.message))
            }
        }

        post("/{id}/reconcile") {
            val businessId = call.businessId()
            val id = call.parameters["id"]!!
            val req = call.receive<ReconcileRequest>()
            val result = paymentService.reconcile(id, businessId, req)
            call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.NotFound, result)
        }
    }
}

// ─── M-Pesa Daraja Callback (public — no JWT) ─────────────────────────────────
//
// Safaricom calls this endpoint after an STK push completes. We look up the
// pending order via stkCheckoutRequestId, persist the payment, and mark the
// order as PAID. We always respond 200 so Safaricom does not retry.

fun Route.mpesaCallbackRoute() {
    post("/payments/mpesa/callback") {
        val callback = try { call.receive<MpesaCallbackRequest>() } catch (e: Exception) {
            println("[MpesaCallback] Failed to deserialize callback payload: ${e.message}")
            call.respond(HttpStatusCode.OK, mapOf("ResultCode" to 0, "ResultDesc" to "Received"))
            return@post
        }
        val stkCallback = callback.Body.stkCallback
        val checkoutRequestId = stkCallback.CheckoutRequestID

        if (stkCallback.ResultCode == 0) {
            val metadata = stkCallback.CallbackMetadata?.Item ?: emptyList()
            val amount   = metadata.find { it.Name == "Amount" }?.Value?.toDoubleOrNull() ?: 0.0
            val txCode   = metadata.find { it.Name == "MpesaReceiptNumber" }?.Value ?: ""
            val phone    = metadata.find { it.Name == "PhoneNumber" }?.Value ?: ""
            val payerName = metadata.find { it.Name == "FirstName" }?.Value ?: "Unknown"

            // Resolve tenant + order from the checkoutRequestId stored at initiate time
            transaction {
                val orderRow = OrdersTable
                    .select { OrdersTable.stkCheckoutRequestId eq checkoutRequestId }
                    .firstOrNull()

                if (orderRow != null) {
                    val businessId   = orderRow[OrdersTable.businessId]
                    val orderId      = orderRow[OrdersTable.id]
                    val orderSubtotal = orderRow[OrdersTable.subtotal]
                    val now          = Clock.System.now()

                    // Warn on amount discrepancy (log but still accept — partial payments are valid)
                    if (Math.abs(amount - orderSubtotal) > orderSubtotal * 0.01) {
                        println("[MpesaCallback] Amount mismatch for order $orderId: " +
                            "expected $orderSubtotal, received $amount (txCode=$txCode)")
                    }

                    // Save payment record (auto-reconciled since it came from the callback)
                    PaymentsTable.insert {
                        it[PaymentsTable.id]              = generateId()
                        it[PaymentsTable.businessId]      = businessId
                        it[PaymentsTable.orderId]         = orderId
                        it[PaymentsTable.transactionCode] = txCode
                        it[PaymentsTable.amount]          = amount
                        it[PaymentsTable.payerPhone]      = phone
                        it[PaymentsTable.payerName]       = payerName
                        it[PaymentsTable.method]          = "MPESA"
                        it[PaymentsTable.status]          = "SUCCESS"
                        it[PaymentsTable.channel]         = "STK_PUSH"
                        it[PaymentsTable.reconciled]      = true
                        it[PaymentsTable.transactionDate] = now
                    }

                    // Mark order as paid
                    OrdersTable.update({ OrdersTable.id eq orderId }) {
                        it[OrdersTable.paymentStatus]         = "PAID"
                        it[OrdersTable.mpesaTransactionCode]  = txCode
                        it[OrdersTable.updatedAt]             = now
                    }
                } else {
                    println("[MpesaCallback] No order found for checkoutRequestId=$checkoutRequestId (txCode=$txCode)")
                }
            }
        }

        call.respond(HttpStatusCode.OK, mapOf("ResultCode" to 0, "ResultDesc" to "Accepted"))
    }
}

// ─── Reports Routes ───────────────────────────────────────────────────────────

fun Route.reportRoutes() {
    val expenseService: ExpenseService by inject()

    route("/reports") {
        moduleGuard("REPORTS")
        get("/profit-summary") {
            val businessId = call.businessId()
            if (!call.hasRole("ADMIN")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Admin access required"))
                return@get
            }
            val startDate = call.request.queryParameters["startDate"] ?: run {
                call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "startDate required"))
                return@get
            }
            val endDate = call.request.queryParameters["endDate"] ?: run {
                call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "endDate required"))
                return@get
            }
            val summary = expenseService.getProfitSummary(businessId, startDate, endDate)
            call.respond(ApiResponse(true, data = summary))
        }
    }
}

// ─── User Management Routes ───────────────────────────────────────────────────

fun Route.userRoutes() {
    val userService: UserManagementService by inject()

    route("/users") {
        get {
            val businessId = call.businessId()
            if (!call.hasRole("ADMIN")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Admin access required"))
                return@get
            }
            call.respond(ApiResponse(true, data = userService.listUsers(businessId)))
        }

        post {
            val businessId = call.businessId()
            if (!call.hasRole("ADMIN")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Admin access required"))
                return@post
            }
            val req = call.receive<InviteUserRequest>()
            val result = userService.inviteUser(businessId, req)
            call.respond(if (result.success) HttpStatusCode.Created else HttpStatusCode.BadRequest, result)
        }

        route("/{id}") {
            patch("/role") {
                val businessId = call.businessId()
                if (!call.hasRole("ADMIN")) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Admin access required"))
                    return@patch
                }
                val userId = call.parameters["id"]!!
                val req = call.receive<UpdateUserRoleRequest>()
                val result = userService.updateRole(userId, businessId, req)
                call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, result)
            }

            patch("/status") {
                val businessId = call.businessId()
                if (!call.hasRole("ADMIN")) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Admin access required"))
                    return@patch
                }
                val userId = call.parameters["id"]!!
                val req = call.receive<UpdateUserStatusRequest>()
                val result = userService.setActiveStatus(userId, businessId, req)
                call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.NotFound, result)
            }
        }
    }
}

// ─── Dashboard Route ──────────────────────────────────────────────────────────

fun Route.dashboardRoute() {
    val dashboardService: DashboardService by inject()

    get("/dashboard") {
        val businessId = call.businessId()
        val data = dashboardService.getDashboard(businessId)
        call.respond(ApiResponse(true, data = data))
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

fun ApplicationCall.businessId(): String =
    principal<JWTPrincipal>()?.payload?.getClaim("businessId")?.asString()
        ?: throw IllegalArgumentException("No businessId associated with this token")

fun ApplicationCall.userRole(): String =
    principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString() ?: ""

fun ApplicationCall.hasRole(vararg roles: String): Boolean =
    userRole() in roles

/**
 * Returns true when the business's enabledModules list contains [module].
 * SUPERADMIN users (no businessId) always pass the check.
 *
 * Results are cached for 60 s per (businessId, module) pair to avoid a DB
 * hit on every guarded request. The cache is process-scoped; a restart or a
 * 60-second wait is required to pick up module changes.
 */
private val moduleCache = java.util.concurrent.ConcurrentHashMap<Pair<String, String>, Pair<Long, Boolean>>()
private const val MODULE_CACHE_TTL_MS = 60_000L

fun ApplicationCall.hasModule(module: String): Boolean {
    if (userRole() == "SUPERADMIN") return true
    val bId = principal<JWTPrincipal>()?.payload?.getClaim("businessId")?.asString()
        ?: return false
    val cacheKey = bId to module.uppercase()
    val cached = moduleCache[cacheKey]
    if (cached != null && System.currentTimeMillis() - cached.first < MODULE_CACHE_TTL_MS) {
        return cached.second
    }
    val result = transaction {
        BusinessesTable.select { BusinessesTable.id eq bId }
            .firstOrNull()
            ?.get(BusinessesTable.enabledModules)
            ?.split(",")
            ?.any { it.trim().equals(module, ignoreCase = true) }
            ?: false
    }
    moduleCache[cacheKey] = System.currentTimeMillis() to result
    return result
}

/**
 * Route-level module guard. Intercepts every call under the current route and
 * responds 403 when [module] is not in the business's enabledModules.
 */
fun Route.moduleGuard(module: String) {
    intercept(ApplicationCallPipeline.Plugins) {
        if (!call.hasModule(module)) {
            call.respond(
                HttpStatusCode.Forbidden,
                ApiResponse<Unit>(false, message = "Module '$module' is not enabled for this business")
            )
            finish()
        }
    }
}

fun String.normalizePhone(): String = when {
    startsWith("07") -> "254${substring(1)}"
    startsWith("01") -> "254${substring(1)}"
    startsWith("+254") -> substring(1)
    else -> this
}

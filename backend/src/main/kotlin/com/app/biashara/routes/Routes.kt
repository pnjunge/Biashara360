package com.app.biashara.routes

import com.app.biashara.models.*
import com.app.biashara.services.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
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
                if (!call.hasRole("ADMIN", "SUPERADMIN")) {
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
            if (!call.hasRole("ADMIN", "SUPERADMIN")) {
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
                is StkPushResult.Success -> call.respond(ApiResponse(true, data = mapOf(
                    "merchantRequestId" to result.merchantRequestId,
                    "checkoutRequestId" to result.checkoutRequestId,
                    "responseCode" to result.responseCode,
                    "responseDescription" to "Success",
                    "customerMessage" to result.customerMessage
                )))
                is StkPushResult.Error -> call.respond(HttpStatusCode.BadGateway,
                    ApiResponse<Unit>(false, message = result.message))
            }
        }

        // Mpesa Daraja callback — no auth required
        post("/mpesa/callback") {
            val callback = call.receive<MpesaCallbackRequest>()
            when (val result = mpesaService.processCallback(callback)) {
                is MpesaCallbackResult.Success -> {
                    // Save payment record — reconcile with order later
                    // TODO: look up pending order by checkoutRequestId
                    call.respond(HttpStatusCode.OK, mapOf("ResultCode" to 0, "ResultDesc" to "Success"))
                }
                is MpesaCallbackResult.Failed -> {
                    call.respond(HttpStatusCode.OK, mapOf("ResultCode" to 0, "ResultDesc" to "Received"))
                }
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

// ─── Reports Routes ───────────────────────────────────────────────────────────

fun Route.reportRoutes() {
    val expenseService: ExpenseService by inject()

    route("/reports") {
        get("/profit-summary") {
            val businessId = call.businessId()
            if (!call.hasRole("ADMIN", "SUPERADMIN")) {
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
            if (!call.hasRole("ADMIN", "SUPERADMIN")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Admin access required"))
                return@get
            }
            call.respond(ApiResponse(true, data = userService.listUsers(businessId)))
        }

        post {
            val businessId = call.businessId()
            if (!call.hasRole("ADMIN", "SUPERADMIN")) {
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
                if (!call.hasRole("ADMIN", "SUPERADMIN")) {
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
                if (!call.hasRole("ADMIN", "SUPERADMIN")) {
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

// ─── Helpers ──────────────────────────────────────────────────────────────────

fun ApplicationCall.businessId(): String =
    principal<JWTPrincipal>()!!.payload.getClaim("businessId").asString()

fun ApplicationCall.userRole(): String =
    principal<JWTPrincipal>()!!.payload.getClaim("role").asString() ?: ""

fun ApplicationCall.hasRole(vararg roles: String): Boolean =
    userRole() in roles

fun String.normalizePhone(): String = when {
    startsWith("07") -> "254${substring(1)}"
    startsWith("01") -> "254${substring(1)}"
    startsWith("+254") -> substring(1)
    else -> this
}

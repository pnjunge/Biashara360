package com.app.biashara.routes

import com.app.biashara.models.*
import com.app.biashara.services.HospitalityService
import com.app.biashara.services.AdvancedHospitalityService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import org.koin.ktor.ext.inject

fun Route.hospitalityRoutes() {
    val service: HospitalityService by inject()
    val operations: AdvancedHospitalityService by inject()
    route("/hospitality") {
        intercept(ApplicationCallPipeline.Call) {
            val isToggleRequest = call.request.httpMethod == HttpMethod.Put && call.request.path().endsWith("/hospitality/enabled")
            val isStatusRequest = call.request.httpMethod == HttpMethod.Get && call.request.path().endsWith("/hospitality/status")
            if (!isToggleRequest && !isStatusRequest && !call.hasAnyMenu("HOSPITALITY", "HOSPITALITY_OPS", "OPEN_TABS")) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Hospitality access is not enabled for this user"))
                finish()
                return@intercept
            }
            if (!isToggleRequest && !isStatusRequest) {
                val path=call.request.path().substringAfter("/hospitality")
                val permission=when {
                    path.endsWith("/decision") -> "approvals"
                    path=="/operations/approvals" -> "view"
                    path=="/operations/report" -> "reports"
                    path.startsWith("/operations/reservations") -> "reservations"
                    path.startsWith("/operations/menu") || path.startsWith("/operations/recipes") -> "menu"
                    path.startsWith("/operations/ingredients") || path.startsWith("/operations/bar-stock") -> "stock"
                    path.startsWith("/operations/shifts") -> "shifts"
                    path.startsWith("/operations/suppliers") || path.startsWith("/operations/purchase-orders") -> "purchasing"
                    path=="/operations/staff" || path.startsWith("/operations/tables") || path.startsWith("/tables") -> "floor"
                    path=="/orders" || path.endsWith("/transfer") -> "orders"
                    path.startsWith("/tickets") -> "kitchen"
                    path.endsWith("/close") || path.endsWith("/split") -> "billing"
                    else -> "view"
                }
                if(!call.hasPermission("hospitality.$permission")) {
                    call.respond(HttpStatusCode.Forbidden,ApiResponse<Unit>(false,message="Hospitality $permission permission required"));finish();return@intercept
                }
            }
            if (!isToggleRequest && !isStatusRequest && !service.isEnabled(call.businessId())) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Hospitality mode is disabled for this business"))
                finish()
            }
        }
        get("/status") {
            val bId = call.businessId()
            call.respond(ApiResponse(true, data = mapOf(
                "enabled" to service.isEnabled(bId),
                "shiftOpen" to service.hasOpenShift(bId)
            )))
        }
        get { call.respond(ApiResponse(true,data=service.dashboard(call.businessId()))) }
        put("/enabled") {
            if(!call.hasRole("ADMIN")) return@put call.respond(HttpStatusCode.Forbidden,ApiResponse<Unit>(false,message="Admin access required"))
            val request=call.receive<Map<String,Boolean>>(); call.respond(ApiResponse(true,data=service.setEnabled(call.businessId(),request["enabled"]?:false)))
        }
        post("/tables") {
            call.respondHospitality(HttpStatusCode.Created) { service.createTable(call.businessId(),call.receive()) }
        }
        put("/tables/{id}") {
            call.respondHospitality { service.updateTable(call.businessId(),call.parameters["id"].orEmpty(),call.receive()) }
        }
        post("/orders") {
            val userId=call.principal<JWTPrincipal>()!!.payload.subject
            val request=call.receive<HospitalityOrderRequest>()
            if(request.items.any{it.complimentary||it.discountAmount>0}) return@post call.respond(HttpStatusCode.Forbidden,ApiResponse<Unit>(false,message="Create the tab at full price, then request a discount or complimentary approval"))
            val result=service.createOrder(call.businessId(),userId,request,call.request.headers["X-Client-Platform"]); call.respond(if(result.success) HttpStatusCode.Created else HttpStatusCode.BadRequest,result)
        }
        patch("/tickets/{id}") { call.respondHospitality { service.updateTicket(call.businessId(),call.parameters["id"].orEmpty(),call.receive()) } }
        post("/tabs/{orderId}/transfer") { call.respondHospitality { service.transferTab(call.businessId(),call.parameters["orderId"].orEmpty(),call.receive()) } }
        post("/tabs/{orderId}/close") { call.respondHospitality { service.closeTab(call.businessId(),call.parameters["orderId"].orEmpty(),call.receive()) } }
        route("/operations") {
            intercept(ApplicationCallPipeline.Call) {
                val isSplitBill = call.request.httpMethod == HttpMethod.Post && call.request.path().contains("/hospitality/operations/tabs/") && call.request.path().endsWith("/split")
                if (!isSplitBill && !call.hasAnyMenu("HOSPITALITY", "HOSPITALITY_OPS", "OPEN_TABS")) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Hospitality management access is required"))
                    finish()
                    return@intercept
                }
            }
            fun ApplicationCall.userId()=principal<JWTPrincipal>()!!.payload.subject
            get {
                val data=operations.dashboard(call.businessId())
                call.respond(ApiResponse(true,data=data.copy(
                    reservations=if(call.hasPermission("hospitality.reservations"))data.reservations else emptyList(),
                    menuProfiles=if(call.hasPermission("hospitality.view"))data.menuProfiles else emptyList(),
                    ingredients=if(call.hasPermission("hospitality.stock")||call.hasPermission("hospitality.menu")||call.hasPermission("hospitality.purchasing"))data.ingredients else emptyList(),
                    shifts=if(call.hasPermission("hospitality.shifts"))data.shifts else emptyList(),
                    suppliers=if(call.hasPermission("hospitality.purchasing"))data.suppliers else emptyList(),
                    purchaseOrders=if(call.hasPermission("hospitality.purchasing"))data.purchaseOrders else emptyList(),
                    approvals=if(call.hasPermission("hospitality.approvals"))data.approvals else data.approvals.filter{it.requestedBy==call.userId()}
                )))
            }
            get("/staff") { call.respondHospitality{operations.staff(call.businessId())} }
            get("/recipes/{productId}") { call.respondHospitality{operations.recipe(call.businessId(),call.parameters["productId"].orEmpty())} }
            get("/report") { val start=call.request.queryParameters["startDate"]?:return@get call.respond(HttpStatusCode.BadRequest,ApiResponse<Unit>(false,message="startDate required"));val end=call.request.queryParameters["endDate"]?:return@get call.respond(HttpStatusCode.BadRequest,ApiResponse<Unit>(false,message="endDate required"));call.respondHospitality{operations.report(call.businessId(),start,end)} }
            post("/reservations") { call.respondHospitality(HttpStatusCode.Created){operations.saveReservation(call.businessId(),call.userId(),call.receive())} }
            patch("/reservations/{id}/{status}") { call.respondHospitality{operations.updateReservationStatus(call.businessId(),call.userId(),call.parameters["id"].orEmpty(),call.parameters["status"].orEmpty())} }
            put("/tables/{id}") { call.respondHospitality{operations.updateTableOperations(call.businessId(),call.userId(),call.parameters["id"].orEmpty(),call.receive())} }
            put("/menu/{productId}") { call.respondHospitality{operations.saveMenuProfile(call.businessId(),call.userId(),call.parameters["productId"].orEmpty(),call.receive())} }
            post("/ingredients") { call.respondHospitality(HttpStatusCode.Created){operations.createIngredient(call.businessId(),call.userId(),call.receive())} }
            put("/recipes/{productId}") { call.respondHospitality{operations.saveRecipe(call.businessId(),call.userId(),call.parameters["productId"].orEmpty(),call.receive())} }
            post("/bar-stock") { call.respondHospitality(HttpStatusCode.Created){operations.recordBarEvent(call.businessId(),call.userId(),call.receive())} }
            post("/shifts/open") { call.respondHospitality(HttpStatusCode.Created){operations.openShift(call.businessId(),call.userId(),call.receive())} }
            post("/shifts/{id}/close") { call.respondHospitality{operations.closeShift(call.businessId(),call.userId(),call.parameters["id"].orEmpty(),call.receive())} }
            post("/suppliers") { call.respondHospitality(HttpStatusCode.Created){operations.createSupplier(call.businessId(),call.userId(),call.receive())} }
            post("/purchase-orders") { call.respondHospitality(HttpStatusCode.Created){operations.createPurchaseOrder(call.businessId(),call.userId(),call.receive())} }
            post("/purchase-orders/{id}/receive") { call.respondHospitality{operations.receivePurchaseOrder(call.businessId(),call.userId(),call.parameters["id"].orEmpty())} }
            post("/approvals") { call.respondHospitality(HttpStatusCode.Created){operations.requestApproval(call.businessId(),call.userId(),call.receive())} }
            post("/approvals/{id}/decision") {
                val request=call.receive<ApprovalDecisionRequest>();call.respondHospitality{operations.decideApproval(call.businessId(),call.userId(),call.parameters["id"].orEmpty(),request.approved)}
            }
            post("/tabs/{orderId}/split") { call.respondHospitality{operations.splitBill(call.businessId(),call.userId(),call.parameters["orderId"].orEmpty(),call.receive())} }
        }
    }
}
private suspend inline fun <reified T> ApplicationCall.respondHospitality(status:HttpStatusCode=HttpStatusCode.OK,block:()->T){ runCatching(block).fold({respond(status,ApiResponse(true,data=it))},{respond(HttpStatusCode.BadRequest,ApiResponse<Unit>(false,message=it.message?:"Hospitality request failed"))}) }

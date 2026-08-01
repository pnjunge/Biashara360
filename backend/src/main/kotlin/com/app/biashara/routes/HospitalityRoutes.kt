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
import org.koin.ktor.ext.inject

fun Route.hospitalityRoutes() {
    val service: HospitalityService by inject()
    val operations: AdvancedHospitalityService by inject()
    route("/hospitality") {
        moduleGuard("SALES")
        intercept(ApplicationCallPipeline.Call) {
            val isToggleRequest = call.request.httpMethod == HttpMethod.Put && call.request.path().endsWith("/hospitality/enabled")
            if (!isToggleRequest && !service.isEnabled(call.businessId())) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Hospitality mode is disabled for this business"))
                finish()
            }
        }
        get { call.respond(ApiResponse(true,data=service.dashboard(call.businessId()))) }
        put("/enabled") {
            if(!call.hasRole("ADMIN")) return@put call.respond(HttpStatusCode.Forbidden,ApiResponse<Unit>(false,message="Admin access required"))
            val request=call.receive<Map<String,Boolean>>(); call.respond(ApiResponse(true,data=service.setEnabled(call.businessId(),request["enabled"]?:false)))
        }
        post("/tables") {
            if(!call.hasRole("ADMIN")) return@post call.respond(HttpStatusCode.Forbidden,ApiResponse<Unit>(false,message="Admin access required"))
            call.respondHospitality(HttpStatusCode.Created) { service.createTable(call.businessId(),call.receive()) }
        }
        put("/tables/{id}") {
            if(!call.hasRole("ADMIN")) return@put call.respond(HttpStatusCode.Forbidden,ApiResponse<Unit>(false,message="Admin access required"))
            call.respondHospitality { service.updateTable(call.businessId(),call.parameters["id"].orEmpty(),call.receive()) }
        }
        post("/orders") {
            val userId=call.principal<JWTPrincipal>()!!.payload.subject
            val request=call.receive<HospitalityOrderRequest>()
            if(request.items.any{it.complimentary||it.discountAmount>0}&&!call.hasRole("ADMIN")) return@post call.respond(HttpStatusCode.Forbidden,ApiResponse<Unit>(false,message="Manager approval is required for discounts or complimentary items"))
            val result=service.createOrder(call.businessId(),userId,request); call.respond(if(result.success) HttpStatusCode.Created else HttpStatusCode.BadRequest,result)
        }
        patch("/tickets/{id}") { call.respondHospitality { service.updateTicket(call.businessId(),call.parameters["id"].orEmpty(),call.receive()) } }
        post("/tabs/{orderId}/transfer") { call.respondHospitality { service.transferTab(call.businessId(),call.parameters["orderId"].orEmpty(),call.receive()) } }
        post("/tabs/{orderId}/close") { call.respondHospitality { service.closeTab(call.businessId(),call.parameters["orderId"].orEmpty(),call.receive()) } }
        route("/operations") {
            fun ApplicationCall.userId()=principal<JWTPrincipal>()!!.payload.subject
            get { call.respond(ApiResponse(true,data=operations.dashboard(call.businessId()))) }
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
                if(!call.hasRole("ADMIN")) return@post call.respond(HttpStatusCode.Forbidden,ApiResponse<Unit>(false,message="Manager access required"))
                val request=call.receive<ApprovalDecisionRequest>();call.respondHospitality{operations.decideApproval(call.businessId(),call.userId(),call.parameters["id"].orEmpty(),request.approved)}
            }
            post("/tabs/{orderId}/split") { call.respondHospitality{operations.splitBill(call.businessId(),call.userId(),call.parameters["orderId"].orEmpty(),call.receive())} }
        }
    }
}
private suspend inline fun <reified T> ApplicationCall.respondHospitality(status:HttpStatusCode=HttpStatusCode.OK,block:()->T){ runCatching(block).fold({respond(status,ApiResponse(true,data=it))},{respond(HttpStatusCode.BadRequest,ApiResponse<Unit>(false,message=it.message?:"Hospitality request failed"))}) }

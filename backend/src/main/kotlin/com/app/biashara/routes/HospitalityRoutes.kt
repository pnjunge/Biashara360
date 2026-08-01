package com.app.biashara.routes

import com.app.biashara.models.*
import com.app.biashara.services.HospitalityService
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
    route("/hospitality") {
        moduleGuard("SALES")
        get { call.respond(ApiResponse(true,data=service.dashboard(call.businessId()))) }
        put("/enabled") {
            if(!call.hasRole("ADMIN")) return@put call.respond(HttpStatusCode.Forbidden,ApiResponse<Unit>(false,message="Admin access required"))
            val request=call.receive<Map<String,Boolean>>(); call.respond(ApiResponse(true,data=service.setEnabled(call.businessId(),request["enabled"]?:false)))
        }
        post("/tables") {
            if(!call.hasRole("ADMIN")) return@post call.respond(HttpStatusCode.Forbidden,ApiResponse<Unit>(false,message="Admin access required"))
            call.respondHospitality(HttpStatusCode.Created) { service.createTable(call.businessId(),call.receive()) }
        }
        post("/orders") {
            val userId=call.principal<JWTPrincipal>()!!.payload.subject
            val result=service.createOrder(call.businessId(),userId,call.receive()); call.respond(if(result.success) HttpStatusCode.Created else HttpStatusCode.BadRequest,result)
        }
        patch("/tickets/{id}") { call.respondHospitality { service.updateTicket(call.businessId(),call.parameters["id"].orEmpty(),call.receive()) } }
        post("/tabs/{orderId}/close") { call.respondHospitality { service.closeTab(call.businessId(),call.parameters["orderId"].orEmpty(),call.receive()) } }
    }
}
private suspend inline fun <reified T> ApplicationCall.respondHospitality(status:HttpStatusCode=HttpStatusCode.OK,block:()->T){ runCatching(block).fold({respond(status,ApiResponse(true,data=it))},{respond(HttpStatusCode.BadRequest,ApiResponse<Unit>(false,message=it.message?:"Hospitality request failed"))}) }

package com.app.biashara.routes
import com.app.biashara.models.*
import com.app.biashara.services.SubscriptionService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.subscriptionRoutes() {
    val service:SubscriptionService by inject()
    route("/subscriptions") {
        get("/bands") { call.respond(ApiResponse(true,data=service.bands())) }
        authenticate("jwt-auth") { post("/checkout") { val result=service.checkout(call.businessId(),call.receive());call.respond(if(result.success) HttpStatusCode.Created else HttpStatusCode.BadRequest,result) } }
    }
}

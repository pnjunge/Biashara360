package com.app.biashara.routes

import com.app.biashara.db.DatabaseFactory
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

/**
 * Comprehensive health check endpoints for monitoring and load balancers.
 */

@Serializable
data class HealthCheckResponse(
    val status: String,  // "healthy", "degraded", "unhealthy"
    val timestamp: String = Instant.now().toString(),
    val version: String = "1.0.0",
    val uptime: Long,
    val checks: Map<String, ComponentHealth>
)

@Serializable
data class ComponentHealth(
    val status: String,  // "up", "down", "degraded"
    val responseTime: Long? = null,  // milliseconds
    val message: String? = null,
    val details: Map<String, String>? = null
)

private val startTime = System.currentTimeMillis()

fun Route.healthRoutes() {
    
    // Basic health check (fast, for load balancers)
    get("/health") {
        call.respond(
            HttpStatusCode.OK,
            mapOf(
                "status" to "healthy",
                "timestamp" to Instant.now().toString()
            )
        )
    }
    
    // Liveness probe (is the app running?)
    get("/health/live") {
        call.respond(
            HttpStatusCode.OK,
            mapOf(
                "status" to "alive",
                "timestamp" to Instant.now().toString()
            )
        )
    }
    
    // Readiness probe (is the app ready to serve traffic?)
    get("/health/ready") {
        val checks = mutableMapOf<String, ComponentHealth>()
        var overallHealthy = true
        
        // Check database connectivity
        val dbHealth = checkDatabase()
        checks["database"] = dbHealth
        if (dbHealth.status != "up") overallHealthy = false
        
        val status = if (overallHealthy) "ready" else "not_ready"
        val httpStatus = if (overallHealthy) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
        
        call.respond(
            httpStatus,
            mapOf(
                "status" to status,
                "timestamp" to Instant.now().toString(),
                "checks" to checks
            )
        )
    }
    
    // Detailed health check (comprehensive status)
    get("/health/detailed") {
        val checks = mutableMapOf<String, ComponentHealth>()
        var degraded = false
        var unhealthy = false
        
        // Database check
        val dbHealth = checkDatabase()
        checks["database"] = dbHealth
        if (dbHealth.status == "down") unhealthy = true
        else if (dbHealth.status == "degraded") degraded = true
        
        // Application metrics
        checks["application"] = ComponentHealth(
            status = "up",
            message = "Application running",
            details = mapOf(
                "uptime" to "${(System.currentTimeMillis() - startTime) / 1000}s",
                "environment" to (System.getenv("ENVIRONMENT") ?: "development")
            )
        )
        
        // External services (optional - can add Mpesa, CyberSource checks here)
        checks["external_services"] = ComponentHealth(
            status = "up",
            message = "External service checks not implemented",
            details = null
        )
        
        val overallStatus = when {
            unhealthy -> "unhealthy"
            degraded -> "degraded"
            else -> "healthy"
        }
        
        val httpStatus = when (overallStatus) {
            "unhealthy" -> HttpStatusCode.ServiceUnavailable
            "degraded" -> HttpStatusCode.OK
            else -> HttpStatusCode.OK
        }
        
        call.respond(
            httpStatus,
            HealthCheckResponse(
                status = overallStatus,
                uptime = System.currentTimeMillis() - startTime,
                checks = checks
            )
        )
    }
}

/**
 * Check database connectivity and response time.
 */
private fun checkDatabase(): ComponentHealth {
    val startTime = System.currentTimeMillis()
    return try {
        transaction {
            // Simple query to verify database connectivity
            exec("SELECT 1") { /* no-op */ }
        }
        val responseTime = System.currentTimeMillis() - startTime
        
        val status = when {
            responseTime > 5000 -> "degraded"  // > 5 seconds is slow
            else -> "up"
        }
        
        ComponentHealth(
            status = status,
            responseTime = responseTime,
            message = if (status == "degraded") "Database responding slowly" else "Database operational",
            details = mapOf(
                "responseTimeMs" to responseTime.toString(),
                "connection" to "active"
            )
        )
    } catch (e: Exception) {
        ComponentHealth(
            status = "down",
            responseTime = System.currentTimeMillis() - startTime,
            message = "Database connection failed: ${e.message}",
            details = mapOf("error" to (e.message ?: "Unknown error"))
        )
    }
}

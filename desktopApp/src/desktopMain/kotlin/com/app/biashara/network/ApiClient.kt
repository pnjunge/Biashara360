package com.app.biashara.network

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Centralized HTTP client for desktop application.
 * Handles authentication, retry logic, and error handling.
 */
object ApiClient {
    
    private const val DEFAULT_TIMEOUT_MS = 30_000L
    private const val DEFAULT_RETRY_ATTEMPTS = 3
    
    /**
     * Base URL for API (configurable via environment or config)
     */
    var baseUrl: String = System.getenv("API_BASE_URL") ?: "http://localhost:8080/v1"
    
    /**
     * Authentication token storage
     */
    var accessToken: String? = null
    var refreshToken: String? = null
    
    /**
     * Configured HTTP client with all plugins
     */
    val client = HttpClient(CIO) {
        // JSON serialization
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
                coerceInputValues = true
                encodeDefaults = true
            })
        }
        
        // Logging (for development)
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
            filter { request ->
                // Don't log auth endpoints to avoid leaking credentials
                !request.url.encodedPath.contains("/auth/")
            }
        }
        
        // Timeout configuration
        install(HttpTimeout) {
            requestTimeoutMillis = DEFAULT_TIMEOUT_MS
            connectTimeoutMillis = 10_000L
            socketTimeoutMillis = DEFAULT_TIMEOUT_MS
        }
        
        // Retry logic with exponential backoff
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = DEFAULT_RETRY_ATTEMPTS)
            retryOnException(maxRetries = DEFAULT_RETRY_ATTEMPTS, retryOnTimeout = true)
            exponentialDelay()
            
            modifyRequest { request ->
                // Add retry attempt to headers for debugging
                request.headers.append("X-Retry-Attempt", retryCount.toString())
            }
        }
        
        // Default request configuration
        defaultRequest {
            url(baseUrl)
            contentType(ContentType.Application.Json)
            
            // Add auth token if available
            accessToken?.let { token ->
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            
            // Add client metadata
            header("X-Client-Type", "Desktop")
            header("X-Client-Version", "1.0.0")
            header("User-Agent", "Biashara360-Desktop/1.0.0")
        }
        
        // Response validation
        expectSuccess = false  // Don't throw on non-2xx responses, handle manually
    }
    
    /**
     * Update authentication tokens
     */
    fun setTokens(access: String, refresh: String) {
        accessToken = access
        refreshToken = refresh
    }
    
    /**
     * Clear authentication tokens (logout)
     */
    fun clearTokens() {
        accessToken = null
        refreshToken = null
    }
    
    /**
     * Check if user is authenticated
     */
    fun isAuthenticated(): Boolean = accessToken != null
    
    /**
     * Update base URL (useful for switching environments)
     */
    fun updateBaseUrl(url: String) {
        baseUrl = url.trimEnd('/')
    }
}

/**
 * Extension for exponential backoff in retry policy
 */
fun HttpRequestRetry.Configuration.exponentialDelay(
    base: Long = 1000,
    maxDelay: Long = 10000
) {
    delayMillis { retry ->
        minOf(base * (1 shl retry), maxDelay)
    }
}

/**
 * Common API response wrapper
 */
@kotlinx.serialization.Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String = "",
    val errors: List<String> = emptyList()
)

/**
 * Paged response wrapper
 */
@kotlinx.serialization.Serializable
data class PagedResponse<T>(
    val data: List<T>,
    val total: Int,
    val page: Int,
    val pageSize: Int,
    val hasMore: Boolean
)

/**
 * Error response from API
 */
@kotlinx.serialization.Serializable
data class ErrorResponse(
    val success: Boolean = false,
    val error: ErrorDetail
)

@kotlinx.serialization.Serializable
data class ErrorDetail(
    val code: String,
    val message: String,
    val details: kotlinx.serialization.json.JsonObject? = null,
    val timestamp: String? = null
)

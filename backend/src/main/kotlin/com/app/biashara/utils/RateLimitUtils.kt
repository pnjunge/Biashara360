package com.app.biashara.utils

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.http.*
import io.ktor.server.response.*
import com.app.biashara.models.ApiResponse
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Simple in-memory rate limiter for authentication endpoints.
 * For production, use Redis or a dedicated rate limiting service.
 *
 * Implements sliding window counter algorithm.
 */
object RateLimitUtils {

    private data class RateLimit(
        val counter: AtomicInteger = AtomicInteger(0),
        var windowStart: Long = System.currentTimeMillis()
    )

    private val limits = ConcurrentHashMap<String, RateLimit>()

    // Configuration
    private const val WINDOW_SIZE_MS = 60_000L // 1 minute
    private const val MAX_ATTEMPTS_AUTH = 5   // 5 attempts per minute
    private const val MAX_ATTEMPTS_GENERAL = 20 // 20 requests per minute

    /**
     * Checks if a request should be rate limited.
     *
     * @param identifier Unique identifier (IP, user ID, etc.)
     * @param endpoint API endpoint being accessed
     * @param maxAttempts Maximum allowed attempts in window
     * @return true if rate limit exceeded, false otherwise
     */
    fun isRateLimited(
        identifier: String,
        endpoint: String,
        maxAttempts: Int = MAX_ATTEMPTS_GENERAL
    ): Boolean {
        val key = "$identifier:$endpoint"
        val now = System.currentTimeMillis()
        
        return limits.compute(key) { _, existing ->
            if (existing == null) {
                RateLimit().also { it.counter.set(1) }
            } else {
                // Check if window has expired
                if (now - existing.windowStart >= WINDOW_SIZE_MS) {
                    // Reset counter for new window
                    RateLimit().also { 
                        it.counter.set(1)
                        it.windowStart = now
                    }
                } else {
                    // Still in same window, increment counter
                    existing.counter.incrementAndGet()
                    existing
                }
            }
        }!!.let { it.counter.get() > maxAttempts }
    }

    /**
     * Clears rate limit for a specific identifier (e.g., after successful login).
     */
    fun clearLimit(identifier: String, endpoint: String) {
        limits.remove("$identifier:$endpoint")
    }

    /**
     * Clears all rate limits (use with caution, usually for testing).
     */
    fun clearAll() {
        limits.clear()
    }
}

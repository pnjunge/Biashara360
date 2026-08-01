package com.app.biashara.cache

import io.ktor.server.plugins.ratelimit.RateLimiter
import kotlin.time.Duration.Companion.milliseconds

class DistributedRateLimiter(
    private val store: RateLimitStore,
    namespace: String,
    requestKey: String,
    private val limit: Int,
    private val windowSeconds: Long
) : RateLimiter {
    private val key = "rate-limit:$namespace:$requestKey"

    override suspend fun tryConsume(tokens: Int): RateLimiter.State {
        var decision = store.consume(key, limit, windowSeconds)
        repeat((tokens - 1).coerceAtLeast(0)) {
            decision = store.consume(key, limit, windowSeconds)
        }
        return if (decision.allowed) {
            RateLimiter.State.Available(decision.remaining, limit, decision.resetAtMillis)
        } else {
            RateLimiter.State.Exhausted((decision.resetAtMillis - System.currentTimeMillis()).coerceAtLeast(1).milliseconds)
        }
    }
}

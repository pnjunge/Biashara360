package com.app.biashara.cache

import io.ktor.server.config.ApplicationConfig
import io.lettuce.core.RedisClient
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.StatefulRedisConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

data class RateLimitDecision(
    val allowed: Boolean,
    val remaining: Int,
    val resetAtMillis: Long
)

interface RateLimitStore : AutoCloseable {
    val configured: Boolean
    suspend fun consume(key: String, limit: Int, windowSeconds: Long): RateLimitDecision
    suspend fun ping(): Boolean
    override fun close() = Unit
}

interface CacheStore {
    suspend fun get(key: String): String?
    suspend fun put(key: String, value: String, ttlSeconds: Long): Boolean
    suspend fun delete(key: String): Boolean
}

class InMemoryRateLimitStore : RateLimitStore {
    private data class Window(var count: Int, val resetAtMillis: Long)
    private val windows = ConcurrentHashMap<String, Window>()
    override val configured: Boolean = false

    override suspend fun consume(key: String, limit: Int, windowSeconds: Long): RateLimitDecision {
        val now = System.currentTimeMillis()
        val windowMillis = windowSeconds * 1_000
        val current = windows.compute(key) { _, existing ->
            if (existing == null || existing.resetAtMillis <= now) Window(1, now + windowMillis)
            else existing.apply { count += 1 }
        }!!
        return RateLimitDecision(current.count <= limit, (limit - current.count).coerceAtLeast(0), current.resetAtMillis)
    }

    override suspend fun ping(): Boolean = true
}

class RedisRateLimitStore(config: ApplicationConfig) : RateLimitStore, CacheStore {
    private val fallback = InMemoryRateLimitStore()
    private val redisUrl = config.propertyOrNull("redis.url")?.getString()?.trim().orEmpty()
    override val configured: Boolean = redisUrl.isNotEmpty()
    private val client: RedisClient? = redisUrl.takeIf(String::isNotEmpty)?.let(RedisClient::create)
    @Volatile
    private var connection: StatefulRedisConnection<String, String>? = null

    override suspend fun consume(key: String, limit: Int, windowSeconds: Long): RateLimitDecision {
        return runCatching {
            withContext(Dispatchers.IO) {
                val activeConnection = connection() ?: error("Redis is not configured")
                val result = activeConnection.sync().eval<List<Any>>(
                    RATE_LIMIT_SCRIPT,
                    ScriptOutputType.MULTI,
                    arrayOf(key),
                    limit.toString(),
                    windowSeconds.toString()
                )
                val count = result[0].toString().toLong().toInt()
                val ttlSeconds = result[1].toString().toLong().coerceAtLeast(1)
                RateLimitDecision(
                    allowed = count <= limit,
                    remaining = (limit - count).coerceAtLeast(0),
                    resetAtMillis = System.currentTimeMillis() + ttlSeconds * 1_000
                )
            }
        }.getOrElse { fallback.consume(key, limit, windowSeconds) }
    }

    override suspend fun ping(): Boolean {
        if (!configured) return true
        return runCatching {
            withContext(Dispatchers.IO) { connection()?.sync()?.ping() == "PONG" }
        }
            .getOrDefault(false)
    }

    override suspend fun get(key: String): String? = runCatching {
        withContext(Dispatchers.IO) { connection()?.sync()?.get(key) }
    }.getOrNull()

    override suspend fun put(key: String, value: String, ttlSeconds: Long): Boolean = runCatching {
        withContext(Dispatchers.IO) {
            connection()?.sync()?.setex(key, ttlSeconds, value) == "OK"
        }
    }.getOrDefault(false)

    override suspend fun delete(key: String): Boolean = runCatching {
        withContext(Dispatchers.IO) { (connection()?.sync()?.del(key) ?: 0) > 0 }
    }.getOrDefault(false)

    private fun connection(): StatefulRedisConnection<String, String>? {
        connection?.takeIf { it.isOpen }?.let { return it }
        return synchronized(this) {
            connection?.takeIf { it.isOpen } ?: runCatching { client?.connect() }
                .getOrNull()
                .also { connection = it }
        }
    }

    override fun close() {
        runCatching { connection?.close() }
        runCatching { client?.shutdown() }
    }

    private companion object {
        // Atomic fixed-window counter. EXPIRE is applied only when the key is first created.
        const val RATE_LIMIT_SCRIPT = """
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[2]) end
            local ttl = redis.call('TTL', KEYS[1])
            return {count, ttl}
        """
    }
}

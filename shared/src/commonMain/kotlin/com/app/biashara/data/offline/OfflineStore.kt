package com.app.biashara.data.offline

import app.cash.sqldelight.db.SqlDriver
import com.app.biashara.db.Biashara360Database
import com.app.biashara.UserSession
import com.app.biashara.domain.model.User
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Serializable
data class QueuedRequest(val id: String, val url: String, val method: String, val body: String,
    val reference: String, val provisional: String, val createdAt: String = kotlinx.datetime.Clock.System.now().toString(), val error: String? = null)
data class OfflineState(val offline: Boolean = false, val pending: Int = 0, val error: String? = null)

class OfflineStore(database: Biashara360Database, driver: SqlDriver) {
    init {
        // Also create the additive table for devices that already have schema version 1.
        driver.execute(null, "CREATE TABLE IF NOT EXISTS NativeOfflineRecord (key TEXT NOT NULL PRIMARY KEY, owner TEXT NOT NULL, category TEXT NOT NULL, payload TEXT NOT NULL)", 0)
    }
    private val queries = database.nativeOfflineQueries
    private val json = Json { ignoreUnknownKeys = true }
    fun put(key: String, owner: String, category: String, value: String) = queries.put(key, owner, category, value)
    fun get(key: String): String? = queries.get(key).executeAsOneOrNull()
    fun list(owner: String, category: String) = queries.list(owner, category).executeAsList()
    fun delete(key: String) = queries.delete(key)
    fun queued(owner: String) = list(owner, "queue").map { json.decodeFromString<QueuedRequest>(it.payload) }
    fun enqueue(owner: String, request: QueuedRequest) = put(request.id, owner, "queue", json.encodeToString(QueuedRequest.serializer(), request))
    fun saveUser(user: User) = put("saved-device-user", "device", "session", json.encodeToString(User.serializer(), user))
    fun savedUser(): User? = get("saved-device-user")?.let { json.decodeFromString<User>(it) }
    fun clearUser() = delete("saved-device-user")
}

object NativeOffline {
    var store: OfflineStore? = null
    private val mutable = MutableStateFlow(OfflineState())
    val state = mutable.asStateFlow()
    fun owner(): String? = UserSession.currentUser.value?.let { "${it.businessId}:${it.id}" }
    fun update(offline: Boolean = mutable.value.offline, error: String? = null) {
        val queued = owner()?.let { store?.queued(it) }.orEmpty()
        mutable.value = OfflineState(offline, queued.size, error ?: queued.firstOrNull { it.error != null }?.error)
    }
}

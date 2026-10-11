package com.app.biashara.data.offline

import com.app.biashara.data.remote.BASE_URL
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*

fun startOfflineReplay(client: HttpClient, store: OfflineStore) {
    CoroutineScope(client.coroutineContext).launch {
        while (isActive) {
            delay(10_000)
            replayOfflineOnce(client, store)
        }
    }
}

suspend fun replayOfflineOnce(client: HttpClient, store: OfflineStore) {
    val json = Json { ignoreUnknownKeys = true }
            val owner = NativeOffline.owner() ?: return
            for (original in store.queued(owner)) {
                val queued = store.queued(owner).firstOrNull { it.id == original.id } ?: continue
                if (NativeOffline.owner() != owner) break
                if (queued.error != null) { NativeOffline.update(error = queued.error); break }
                try {
                    val response = client.request(queued.url) {
                        method = HttpMethod.parse(queued.method)
                        header("X-Offline-Replay", "true")
                        header("X-Client-Reference", queued.reference)
                        contentType(ContentType.Application.Json)
                        setBody(queued.body)
                    }
                    val result = json.parseToJsonElement(response.bodyAsText()).jsonObject
                    if (response.status.value !in 200..299 || result["success"]?.jsonPrimitive?.booleanOrNull != true) {
                        val error = result["message"]?.jsonPrimitive?.content ?: "Server rejected queued action"
                        store.enqueue(owner, queued.copy(error = error))
                        NativeOffline.update(error = error)
                        break
                    }
                    // Resolve dependencies before removing the creation record.
                    if (queued.provisional.isNotBlank()) {
                        val remoteId = result["data"]!!.jsonObject["id"]!!.jsonPrimitive.content
                        store.put("$owner:map:${queued.provisional}", owner, "mapping", remoteId)
                        val ticketResponse = client.get("$BASE_URL/hospitality") { header("X-Offline-Replay", "true") }
                        check(ticketResponse.status.value in 200..299) { "Could not refresh created order; replay will retry" }
                        if (ticketResponse.status.value in 200..299) {
                            val dashboard = json.parseToJsonElement(ticketResponse.bodyAsText()).jsonObject
                            check(dashboard["success"]?.jsonPrimitive?.booleanOrNull == true) { "Could not refresh kitchen dependencies; replay will retry" }
                            val ticket = dashboard["data"]?.jsonObject?.get("tickets")?.jsonArray?.firstOrNull {
                                it.jsonObject["orderId"]?.jsonPrimitive?.content == remoteId && it.jsonObject["station"]?.jsonPrimitive?.content == "KITCHEN"
                            }
                            val needsTicket = store.queued(owner).any { it.url.endsWith("/offline-ticket-${queued.provisional}") }
                            check(!needsTicket || ticket != null) { "Kitchen dependency is not available yet; replay will retry" }
                            ticket?.jsonObject?.get("id")?.jsonPrimitive?.content?.let {
                                store.put("$owner:map:offline-ticket-${queued.provisional}", owner, "mapping", it)
                            }
                            if (dashboard["success"]?.jsonPrimitive?.booleanOrNull == true) store.put("$owner:$BASE_URL/hospitality", owner, "cache", dashboard.toString())
                        }
                        for (dependent in store.queued(owner).filter { it.id != queued.id }) {
                            var url = dependent.url
                            var body = dependent.body
                            for (mapping in store.list(owner, "mapping")) {
                                val localId = mapping.key.substringAfter("$owner:map:")
                                url = url.replace("/$localId/", "/${mapping.payload}/").let { if (it.endsWith("/$localId")) it.dropLast(localId.length) + mapping.payload else it }
                                body = body.replace("\"$localId\"", "\"${mapping.payload}\"")
                            }
                            store.enqueue(owner, dependent.copy(url = url, body = body))
                        }
                    }
                    store.delete(queued.id)
                    NativeOffline.update(offline = false)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) {
                    NativeOffline.update(offline = isOfflineFailure(error), error = if (isOfflineFailure(error)) null else error.message)
                    break
                }
            }

}

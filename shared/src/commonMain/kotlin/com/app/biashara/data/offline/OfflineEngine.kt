package com.app.biashara.data.offline

import com.app.biashara.UserSession
import com.app.biashara.data.remote.*
import com.app.biashara.domain.usecase.generateId
import io.ktor.client.engine.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.util.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.serialization.json.*

@OptIn(InternalAPI::class)
class OfflineEngine(private val network: HttpClientEngine, private val store: OfflineStore) : HttpClientEngine by network {
    override fun install(client: io.ktor.client.HttpClient) = super<HttpClientEngine>.install(client)
    private val lock = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        if (data.headers["X-Offline-Replay"] == "true") return network.execute(data)
        val owner = NativeOffline.owner() ?: return network.execute(data)
        val path = data.url.encodedPath.removePrefix(Url(BASE_URL).encodedPath)
        if (data.url.host != Url(BASE_URL).host) return network.execute(data)
        if (data.method == HttpMethod.Get) {
            val cacheable = path in setOf("/access/me", "/hospitality/status", "/hospitality/duty", "/hospitality", "/hospitality/operations", "/products", "/customers", "/orders", "/expenses", "/payments", "/tax/rates", "/business/profile", "/settings/session-timeouts") || Regex("/orders/[^/]+").matches(path)
            if (!cacheable) return network.execute(data)
            val key = "$owner:${data.url}"
            return try {
                val response = network.execute(data)
                if (response.statusCode.value !in 200..299 || path.startsWith("/auth/")) return response
                val text = (response.body as? ByteReadChannel)?.readRemaining()?.readText() ?: return response
                if (runCatching { json.parseToJsonElement(text).jsonObject["success"]?.jsonPrimitive?.boolean }.getOrNull() == true) {
                    store.put(key, owner, "cache", text)
                }
                NativeOffline.update(offline = false)
                HttpResponseData(response.statusCode, response.requestTime, response.headers, response.version, ByteReadChannel(project(owner, path, text)), response.callContext)
            } catch (error: Exception) {
                if (!isOfflineFailure(error)) throw error
                NativeOffline.update(offline = true)
                val cached = store.get(key) ?: throw error
                response(data, project(owner, path, cached))
            }
        }
        val allowed = (data.method == HttpMethod.Post && path in setOf("/orders", "/hospitality/orders")) ||
            (data.method == HttpMethod.Patch && path.startsWith("/hospitality/tickets/")) ||
            (data.method == HttpMethod.Post && Regex("/hospitality/tabs/[^/]+/close").matches(path))
        if (!allowed) return network.execute(data)
        return lock.withLock {
            val bodyText = when (val body = data.body) {
                is OutgoingContent.ByteArrayContent -> body.bytes().decodeToString()
                else -> return@withLock network.execute(data)
            }
            val body = json.parseToJsonElement(bodyText).jsonObject
            val method = body["paymentMethod"]?.jsonPrimitive?.content ?: "TAB"
            // External payment authorization cannot be fabricated or queued as paid.
            if (method !in setOf("CASH", "COD", "TAB")) return@withLock network.execute(data)
            val reference = body["clientReference"]?.jsonPrimitive?.contentOrNull ?: generateId()
            val prepared = if (path == "/orders") JsonObject(body + ("clientReference" to JsonPrimitive(reference))).toString() else bodyText
            val request = QueuedRequest(generateId(), data.url.toString(), data.method.value, prepared, reference,
                if (path in setOf("/orders", "/hospitality/orders")) reference else "")
            // Persist before transmission: a lost response must not lose or duplicate the transaction.
            store.enqueue(owner, request)
            val headers = Headers.build { appendAll(data.headers); append("X-Client-Reference", reference) }
            val wire = HttpRequestData(data.url, data.method, headers, TextContent(prepared, ContentType.Application.Json), data.executionContext, data.attributes)
            try {
                if (store.queued(owner).first().id != request.id) {
                    return@withLock response(data, acceptOffline(owner, path, request))
                }
                val result = network.execute(wire)
                store.delete(request.id)
                NativeOffline.update(offline = false)
                result
            } catch (error: Exception) {
                if (!isOfflineFailure(error)) { store.delete(request.id); NativeOffline.update(); throw error }
                NativeOffline.update(offline = true)
                try { response(data, acceptOffline(owner, path, request)) }
                catch (validation: Exception) {
                    // Keep ambiguous transmissions for review; never silently discard a sale.
                    store.enqueue(owner, request.copy(error = validation.message))
                    NativeOffline.update(error = validation.message)
                    throw validation
                }
            }
        }
    }

    private suspend fun response(data: HttpRequestData, text: String) = HttpResponseData(
        HttpStatusCode.OK, io.ktor.util.date.GMTDate(), headersOf(HttpHeaders.ContentType, "application/json"),
        HttpProtocolVersion.HTTP_1_1, ByteReadChannel(text), callContext())

    private fun cache(owner: String, path: String): JsonObject? = store.list(owner, "cache").firstOrNull {
        Url(it.key.substringAfter("$owner:")).encodedPath == Url(BASE_URL).encodedPath + path
    }?.let { json.parseToJsonElement(it.payload).jsonObject }

    private fun acceptOffline(owner: String, path: String, request: QueuedRequest): String {
        val permission = when {
            path == "/orders" -> "orders.create"
            path == "/hospitality/orders" -> "hospitality.orders"
            path.startsWith("/hospitality/tickets/") -> "hospitality.kitchen"
            else -> "hospitality.billing"
        }
        check(NativeAccessSession.permits(permission)) { "Cached permission required: $permission" }
        if (path.startsWith("/hospitality/")) {
            val status = cache(owner, "/hospitality/status")?.get("data")?.jsonObject
            check(status?.get("shiftOpen")?.jsonPrimitive?.booleanOrNull == true) { "An open trading day must be cached before working offline" }
        }
        val payload = json.parseToJsonElement(request.body).jsonObject
        if (path in setOf("/orders", "/hospitality/orders")) {
            val items = payload["items"]?.jsonArray ?: error("Add items before checkout")
            check(items.isNotEmpty() && items.all { it.jsonObject["quantity"]!!.jsonPrimitive.int > 0 && it.jsonObject["unitPrice"]!!.jsonPrimitive.double.let { price -> price.isFinite() && price >= 0 } }) { "Invalid order items" }
            if (payload["serviceType"]?.jsonPrimitive?.content == "DINE_IN") check(payload["tableId"]?.jsonPrimitive?.contentOrNull != null || payload["hospitalityTableId"]?.jsonPrimitive?.contentOrNull != null) { "Select a table for dine in" }
        }
        val result = when {
            path in setOf("/orders", "/hospitality/orders") -> localOrder(owner, request)
            path.startsWith("/hospitality/tickets/") -> {
                val id = path.substringAfterLast('/')
                val body = json.parseToJsonElement(request.body).jsonObject
                check(body["status"]?.jsonPrimitive?.content == "SERVED") { "Only Mark Served can be queued offline" }
                val dashboard = project(owner, "/hospitality", cache(owner, "/hospitality")?.toString() ?: error("Load kitchen orders before going offline"))
                json.parseToJsonElement(dashboard).jsonObject["data"]!!.jsonObject["tickets"]!!.jsonArray.first { it.jsonObject["id"]?.jsonPrimitive?.content == id }
            }
            else -> {
                check(payload["paymentMethod"]?.jsonPrimitive?.content == "CASH") { "Offline tab settlement requires Cash" }
                val id = path.split('/')[3]
                val dashboard = json.parseToJsonElement(project(owner, "/hospitality", cache(owner, "/hospitality")?.toString() ?: error("Load tabs before going offline"))).jsonObject["data"]!!.jsonObject
                check(dashboard["tickets"]!!.jsonArray.none { it.jsonObject["orderId"]?.jsonPrimitive?.content == id && it.jsonObject["station"]?.jsonPrimitive?.content == "KITCHEN" && it.jsonObject["status"]?.jsonPrimitive?.content != "SERVED" }) { "Serve all kitchen orders before settling this tab" }
                val tab = cache(owner, "/hospitality")?.get("data")?.jsonObject?.get("openTabs")?.jsonArray?.firstOrNull { it.jsonObject["id"]?.jsonPrimitive?.content == id }?.jsonObject
                    ?: store.queued(owner).firstOrNull { it.provisional == id }?.let { localOrder(owner, it) } ?: error("Tab not cached")
                JsonObject(tab + mapOf("paymentStatus" to JsonPrimitive("PAID"), "tabStatus" to JsonPrimitive("CLOSED")))
            }
        }
        NativeOffline.update()
        return buildJsonObject { put("success", true); put("data", result); put("message", "Saved offline — pending server sync") }.toString()
    }

    private fun localOrder(owner: String, request: QueuedRequest): JsonObject {
        val body = json.parseToJsonElement(request.body).jsonObject
        val products = cache(owner, "/products")?.get("data")?.jsonArray ?: error("Sync products before taking offline sales")
        val items = body["items"]!!.jsonArray.map { line ->
            val item = line.jsonObject
            val product = products.firstOrNull { it.jsonObject["id"] == item["productId"] }?.jsonObject ?: error("Product not cached")
            JsonObject(item + mapOf("productName" to product["name"]!!, "buyingPrice" to (product["buyingPrice"] ?: JsonPrimitive(0)), "id" to JsonPrimitive(generateId())))
        }
        val now = request.createdAt
        return JsonObject(body + mapOf("id" to JsonPrimitive(request.provisional), "orderNumber" to JsonPrimitive("OFFLINE-${request.reference.takeLast(8)}"),
            "businessId" to JsonPrimitive(UserSession.getBusinessId()), "customerId" to (body["customerId"] ?: JsonNull),
            "customerName" to (body["customerName"] ?: JsonPrimitive("Walk-in Guest")), "customerPhone" to (body["customerPhone"] ?: JsonPrimitive("")),
            "deliveryLocation" to (body["deliveryLocation"] ?: JsonPrimitive("")), "createdAt" to JsonPrimitive(now), "updatedAt" to JsonPrimitive(now),
            "paymentStatus" to JsonPrimitive("PENDING"), "deliveryStatus" to JsonPrimitive("PROCESSING"), "items" to JsonArray(items),
            "hospitalityTableId" to (body["tableId"] ?: body["hospitalityTableId"] ?: JsonNull), "tabStatus" to JsonPrimitive("OPEN"),
            "subtotal" to JsonPrimitive(items.sumOf { it["quantity"]!!.jsonPrimitive.int * it["unitPrice"]!!.jsonPrimitive.double })))
    }

    internal fun project(owner: String, path: String, text: String): String {
        if (path == "/orders") {
            val root = json.parseToJsonElement(text).jsonObject
            val page = root["data"]?.jsonObject ?: return text
            if (page["page"]?.jsonPrimitive?.intOrNull != 1) return text
            val orders = page["data"]?.jsonArray.orEmpty().toMutableList()
            for (queued in store.queued(owner).filter { it.error == null && Url(it.url).encodedPath.endsWith("/orders") }) {
                if (store.get("$owner:map:${queued.provisional}") == null && orders.none { it.jsonObject["id"]?.jsonPrimitive?.content == queued.provisional }) orders.add(localOrder(owner, queued))
            }
            return JsonObject(root + ("data" to JsonObject(page + ("data" to JsonArray(orders))))).toString()
        }
        if (path != "/hospitality") return text
        val root = json.parseToJsonElement(text).jsonObject
        val dashboard = root["data"]?.jsonObject ?: return text
        val tabs = dashboard["openTabs"]?.jsonArray?.toMutableList() ?: return text
        val tickets = dashboard["tickets"]?.jsonArray?.toMutableList() ?: return text
        for (request in store.queued(owner).filter { it.error == null }) {
            val queuedPath = Url(request.url).encodedPath.removePrefix(Url(BASE_URL).encodedPath)
            if (queuedPath == "/hospitality/orders" || queuedPath == "/orders" && json.parseToJsonElement(request.body).jsonObject["serviceType"]?.jsonPrimitive?.content in setOf("DINE_IN", "TAKEAWAY", "DELIVERY")) {
                val order = localOrder(owner, request)
                if (tabs.none { it.jsonObject["id"] == order["id"] }) tabs.add(order)
                val profiles = cache(owner, "/hospitality/operations")?.get("data")?.jsonObject?.get("menuProfiles")?.jsonArray.orEmpty()
                val food = order["items"]!!.jsonArray.filter { item ->
                    val profile = profiles.firstOrNull { it.jsonObject["productId"] == item.jsonObject["productId"] }?.jsonObject
                    // Without a cached explicit routing rule, require kitchen service conservatively.
                    profile == null || profile["preparationStation"]?.jsonPrimitive?.contentOrNull == "KITCHEN"
                }
                if (food.isNotEmpty() && tickets.none { it.jsonObject["orderId"] == order["id"] }) tickets.add(buildJsonObject {
                    put("id", "offline-ticket-${request.provisional}"); put("orderId", request.provisional); put("orderNumber", order["orderNumber"]!!)
                    put("station", "KITCHEN"); put("status", "PREPARING"); put("items", JsonArray(food)); put("createdAt", order["createdAt"]!!)
                })
            } else if (queuedPath.endsWith("/close")) {
                val id = queuedPath.split('/')[3]
                tabs.removeAll { it.jsonObject["id"]?.jsonPrimitive?.content == id }
            } else if (queuedPath.startsWith("/hospitality/tickets/")) {
                val index = tickets.indexOfFirst { it.jsonObject["id"]?.jsonPrimitive?.content == queuedPath.substringAfterLast('/') }
                if (index >= 0) tickets[index] = JsonObject(tickets[index].jsonObject + ("status" to JsonPrimitive("SERVED")))
            }
        }
        return JsonObject(root + ("data" to JsonObject(dashboard + mapOf("openTabs" to JsonArray(tabs), "tickets" to JsonArray(tickets))))).toString()
    }
}

fun isOfflineFailure(error: Exception): Boolean = error !is kotlinx.coroutines.CancellationException && error is io.ktor.utils.io.errors.IOException

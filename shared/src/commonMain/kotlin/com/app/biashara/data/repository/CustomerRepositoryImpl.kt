package com.app.biashara.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.app.biashara.data.remote.ApiResponse
import com.app.biashara.data.remote.BASE_URL
import com.app.biashara.db.Biashara360Database
import com.app.biashara.db.CustomerEntity
import com.app.biashara.domain.model.*
import com.app.biashara.domain.repository.CustomerRepository
import com.app.biashara.domain.usecase.normalizeKenyanMobile
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@kotlinx.serialization.Serializable
data class CustomerDto(
    val id: String,
    val businessId: String,
    val name: String,
    val phone: String,
    val email: String?,
    val location: String,
    val notes: String = "",
    val loyaltyPoints: Int = 0,
    val isActive: Boolean = true,
    val createdAt: String,
    val updatedAt: String
)

@kotlinx.serialization.Serializable
private data class CustomerRequestDto(
    val name: String,
    val phone: String,
    val email: String?,
    val location: String,
    val notes: String
)

class CustomerRepositoryImpl(
    private val database: Biashara360Database,
    private val client: HttpClient
) : CustomerRepository {

    private val queries = database.biashara360DatabaseQueries

    override fun getCustomers(businessId: String): Flow<List<Customer>> =
        queries.selectAllCustomers(businessId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { it.map { entity -> entity.toDomain() } }

    override fun getTopCustomers(businessId: String, limit: Int): Flow<List<Customer>> =
        getCustomers(businessId).map { customers -> customers.take(limit) }

    override fun getTopCustomersWithStats(businessId: String, limit: Int): Flow<List<Pair<Customer, CustomerStats>>> {
        return combine(
            getCustomers(businessId),
            queries.selectAllOrders(businessId).asFlow().mapToList(Dispatchers.Default)
        ) { customers, orders ->
            val ordersByCustomer = orders.filter { it.customer_id != null }.groupBy { it.customer_id!! }
            customers.map { customer ->
                val customerOrders = ordersByCustomer[customer.id] ?: emptyList()
                val totalSpent = customerOrders.sumOf { it.subtotal }
                val stats = CustomerStats(
                    customerId = customer.id,
                    totalOrders = customerOrders.size,
                    totalSpent = totalSpent,
                    averageOrderValue = if (customerOrders.isNotEmpty()) totalSpent / customerOrders.size else 0.0,
                    lastOrderDate = customerOrders.maxOfOrNull { it.created_at }?.let {
                        runCatching { Instant.parse(it) }.getOrNull()
                    }
                )
                customer to stats
            }
            .filter { it.second.totalOrders > 0 }
            .sortedByDescending { it.second.totalSpent }
            .take(limit)
        }
    }

    override fun getRepeatCustomers(businessId: String): Flow<List<Customer>> =
        getCustomers(businessId)

    override suspend fun getCustomer(id: String): Customer? =
        queries.selectCustomerById(id).executeAsOneOrNull()?.toDomain()

    override suspend fun getCustomerByPhone(phone: String): Customer? {
        val normalized = phone.normalizeKenyanMobile() ?: return null
        return queries.selectAllCustomers(com.app.biashara.UserSession.getBusinessId())
            .executeAsList()
            .firstOrNull { it.phone == normalized }
            ?.toDomain()
    }

    override suspend fun saveCustomer(customer: Customer): Result<Customer> = runCatching {
        val normalizedPhone = customer.phone.normalizeKenyanMobile()
            ?: throw IllegalArgumentException("Enter a valid Kenyan mobile number")
        val request = CustomerRequestDto(
            name = customer.name.trim(),
            phone = normalizedPhone,
            email = customer.email?.trim()?.ifBlank { null },
            location = customer.location.trim(),
            notes = customer.notes.trim()
        )
        var response: ApiResponse<CustomerDto> = client.put("$BASE_URL/customers/${customer.id}") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
        if (!response.success && response.message.contains("not found", ignoreCase = true)) {
            response = client.post("$BASE_URL/customers") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
        val saved = response.data
            ?: throw Exception(response.message.ifBlank { "Failed to save customer" })
        queries.insertCustomer(
            id = saved.id,
            business_id = saved.businessId,
            name = saved.name,
            phone = saved.phone,
            email = saved.email,
            location = saved.location,
            notes = saved.notes,
            loyalty_points = saved.loyaltyPoints.toLong(),
            is_active = if (saved.isActive) 1L else 0L,
            created_at = saved.createdAt,
            updated_at = saved.updatedAt
        )
        saved.toDomain()
    }

    override suspend fun getCustomerStats(customerId: String): CustomerStats {
        // Get all orders for the customer to calculate stats
        val orders = queries.selectOrdersByCustomer(customerId)
            .executeAsList()

        val orderCount = orders.size.toLong()
        val totalSpent = orders.sumOf { it.subtotal }
        val averageOrder = if (orders.isNotEmpty()) totalSpent / orders.size else 0.0
        val lastOrderDate = orders.maxByOrNull { it.created_at }?.let {
            runCatching { Instant.parse(it.created_at) }.getOrNull()
        }

        return CustomerStats(
            customerId = customerId,
            totalOrders = orderCount.toInt(),
            totalSpent = totalSpent,
            averageOrderValue = averageOrder,
            lastOrderDate = lastOrderDate
        )
    }

    override fun searchCustomers(businessId: String, query: String): Flow<List<Customer>> =
        queries.searchCustomers(businessId, query)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { it.map { entity -> entity.toDomain() } }

    override suspend fun addLoyaltyPoints(customerId: String, points: Int): Result<Unit> =
        runCatching {
            queries.updateLoyaltyPoints(
                points = points.toLong(),
                updatedAt = Clock.System.now().toString(),
                customerId = customerId
            )
        }

    override suspend fun sendMessage(message: CustomerMessage): Result<Unit> =
        Result.success(Unit) // Handled by messaging service layer

    /** Sync customers from API and update local cache **/
    suspend fun syncCustomersFromApi(businessId: String): Result<List<Customer>> = runCatching {
        val response: ApiResponse<List<CustomerDto>> = client.get("$BASE_URL/customers") {
            url { parameters.append("businessId", businessId) }
        }.body()

        if (!response.success || response.data == null) {
            throw Exception(response.message.ifBlank { "Failed to fetch customers" })
        }

        // Update local cache
        response.data.forEach { dto ->
            queries.insertCustomer(
                id = dto.id,
                business_id = dto.businessId,
                name = dto.name,
                phone = dto.phone,
                email = dto.email,
                location = dto.location,
                notes = dto.notes,
                loyalty_points = dto.loyaltyPoints.toLong(),
                is_active = if (dto.isActive) 1L else 0L,
                created_at = dto.createdAt,
                updated_at = dto.updatedAt
            )
        }

        response.data.map { it.toDomain() }
    }

    private fun CustomerDto.toDomain() = Customer(
        id = id,
        businessId = businessId,
        name = name,
        phone = phone,
        email = email,
        location = location,
        notes = notes,
        loyaltyPoints = loyaltyPoints,
        isActive = isActive,
        createdAt = Instant.parse(createdAt),
        updatedAt = Instant.parse(updatedAt)
    )

    private fun CustomerEntity.toDomain() = Customer(
        id = id,
        businessId = business_id,
        name = name,
        phone = phone,
        email = email,
        location = location,
        notes = notes,
        loyaltyPoints = loyalty_points.toInt(),
        isActive = is_active == 1L,
        createdAt = runCatching { Instant.parse(created_at) }
            .getOrDefault(Clock.System.now()),
        updatedAt = runCatching { Instant.parse(updated_at) }
            .getOrDefault(Clock.System.now())
    )
}

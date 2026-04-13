package com.app.biashara.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.app.biashara.db.Biashara360Database
import com.app.biashara.db.CustomerEntity
import com.app.biashara.domain.model.*
import com.app.biashara.domain.repository.CustomerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class CustomerRepositoryImpl(
    private val database: Biashara360Database
) : CustomerRepository {

    private val queries = database.biashara360DatabaseQueries

    override fun getCustomers(businessId: String): Flow<List<Customer>> =
        queries.selectAllCustomers(businessId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { it.map { entity -> entity.toDomain() } }

    override fun getTopCustomers(businessId: String, limit: Int): Flow<List<Customer>> =
        getCustomers(businessId).map { customers -> customers.take(limit) }

    override fun getRepeatCustomers(businessId: String): Flow<List<Customer>> =
        getCustomers(businessId)

    override suspend fun getCustomer(id: String): Customer? =
        queries.selectCustomerById(id).executeAsOneOrNull()?.toDomain()

    override suspend fun getCustomerByPhone(phone: String): Customer? = null

    override suspend fun saveCustomer(customer: Customer): Result<Customer> = runCatching {
        val now = Clock.System.now().toString()
        queries.insertCustomer(
            id = customer.id,
            business_id = customer.businessId,
            name = customer.name,
            phone = customer.phone,
            email = customer.email,
            location = customer.location,
            notes = customer.notes,
            loyalty_points = customer.loyaltyPoints.toLong(),
            is_active = if (customer.isActive) 1L else 0L,
            created_at = customer.createdAt.toString(),
            updated_at = now
        )
        customer
    }

    override suspend fun getCustomerStats(customerId: String): CustomerStats {
        val orderCount = queries.selectOrderCountForCustomer(customerId).executeAsOne().order_count
        val totalSpent = queries.sumOrderSpendForCustomer(customerId).executeAsOne().total_spent ?: 0.0
        val avg = queries.avgOrderValueForCustomer(customerId).executeAsOne().avg_value ?: 0.0
        val lastOrderDate = queries.lastOrderDateForCustomer(customerId)
            .executeAsOneOrNull()?.last_date?.let {
                runCatching { Instant.parse(it) }.getOrNull()
            }
        return CustomerStats(
            customerId = customerId,
            totalOrders = orderCount.toInt(),
            totalSpent = totalSpent,
            averageOrderValue = avg,
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

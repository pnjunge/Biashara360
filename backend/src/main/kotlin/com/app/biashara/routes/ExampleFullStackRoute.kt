@file:Suppress("UNUSED_PARAMETER")
package com.app.biashara.routes

import com.app.biashara.constants.Constants
import com.app.biashara.exceptions.*
import com.app.biashara.models.ApiResponse
import com.app.biashara.models.PagedResponse
import com.app.biashara.utils.PaginationParams
import com.app.biashara.validation.Validator
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

/**
 * EXAMPLE: Complete route demonstrating ALL frameworks working together.
 * 
 * This example shows:
 * 1. Input Validation Framework
 * 2. DTO Layer (Request/Response separation)
 * 3. Constants & Enums (Type-safe business rules)
 * 4. Exception Handling (Custom exceptions)
 * 5. Pagination (Consistent list responses)
 * 6. Proper HTTP status codes
 * 
 * Use this as a template for implementing new features.
 */

// ──── DTOs (What API accepts/returns) ────────────────────────────────────────

@Serializable
data class CreateCustomerRequestDTO(
    val name: String,
    val phone: String,
    val email: String? = null,
    val location: String = "",
    val notes: String = "",
    val loyaltyTier: String = "BRONZE"  // BRONZE, SILVER, GOLD, PLATINUM
)

@Serializable
data class UpdateCustomerRequestDTO(
    val name: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val location: String? = null,
    val notes: String? = null
)

@Serializable
data class CustomerResponseDTO(
    val id: String,
    val businessId: String,
    val name: String,
    val phone: String,
    val email: String?,
    val location: String,
    val notes: String,
    val loyaltyPoints: Int,
    val loyaltyTier: String,
    val totalOrders: Int,
    val totalSpent: Double,
    val isVip: Boolean,  // Computed field
    val lifetimeValue: String,  // Computed field (formatted)
    val createdAt: String
)

// ──── Enums (Type-safe status values) ────────────────────────────────────────

enum class CustomerTier(val value: String, val minSpent: Double) {
    BRONZE("BRONZE", 0.0),
    SILVER("SILVER", 10_000.0),
    GOLD("GOLD", 50_000.0),
    PLATINUM("PLATINUM", 100_000.0);
    
    companion object {
        fun from(value: String): CustomerTier? = entries.find { it.value == value.uppercase() }
        fun isValid(value: String): Boolean = from(value) != null
        fun calculateTier(totalSpent: Double): CustomerTier {
            return when {
                totalSpent >= PLATINUM.minSpent -> PLATINUM
                totalSpent >= GOLD.minSpent -> GOLD
                totalSpent >= SILVER.minSpent -> SILVER
                else -> BRONZE
            }
        }
    }
}

// ──── Example Routes ──────────────────────────────────────────────────────────

fun Route.exampleCustomerRoutes() {
    route("/customers") {
        moduleGuard("CRM")  // Ensure CRM module is enabled
        
        /**
         * LIST: Get all customers with pagination, filtering, and sorting
         * GET /customers?search=john&tier=GOLD&page=1&pageSize=20&sortBy=totalSpent
         */
        get {
            val businessId = call.businessId()
            
            // Extract query parameters
            val searchQuery = call.request.queryParameters["search"]
            val tierFilter = call.request.queryParameters["tier"]
            val sortBy = call.request.queryParameters["sortBy"] ?: "createdAt"
            val sortOrder = call.request.queryParameters["sortOrder"] ?: "desc"
            val page = call.request.queryParameters["page"]?.toIntOrNull()
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull()
            
            // VALIDATION: Validate query parameters
            Validator.validate {
                field("tier", tierFilter) {
                    optional {
                        custom("Invalid customer tier", "INVALID_TIER") {
                            CustomerTier.isValid(it as String)
                        }
                    }
                }
                field("sortBy", sortBy) {
                    oneOf("name", "createdAt", "totalSpent", "loyaltyPoints")
                }
                field("sortOrder", sortOrder) {
                    oneOf("asc", "desc")
                }
                field("page", page) {
                    optional {
                        positive()
                    }
                }
                field("pageSize", pageSize) {
                    optional {
                        positive()
                        max(Constants.Business.MAX_PAGE_SIZE.toDouble())
                    }
                }
            }
            
            // BUSINESS LOGIC: Fetch customers (mock implementation)
            val allCustomers = mockFetchCustomers(businessId, searchQuery, tierFilter)
            
            // PAGINATION: Apply pagination
            if (page != null) {
                val params = PaginationParams.from(page, pageSize)
                val pagedCustomers = allCustomers.drop(params.offset.toInt()).take(params.limit)
                
                val response = PagedResponse(
                    data = pagedCustomers,
                    total = allCustomers.size,
                    page = params.page,
                    pageSize = params.pageSize,
                    hasMore = (params.page * params.pageSize) < allCustomers.size
                )
                call.respond(HttpStatusCode.OK, response)
            } else {
                call.respond(HttpStatusCode.OK, ApiResponse(true, data = allCustomers))
            }
        }
        
        /**
         * CREATE: Add new customer
         * POST /customers
         */
        post {
            val businessId = call.businessId()
            val req = call.receive<CreateCustomerRequestDTO>()
            
            // VALIDATION: Comprehensive input validation
            Validator.validate {
                field("name", req.name) {
                    required()
                    length(2, 255)
                }
                field("phone", req.phone) {
                    required()
                    phone()
                }
                field("email", req.email) {
                    optional {
                        email()
                        maxLength(255)
                    }
                }
                field("location", req.location) {
                    maxLength(500)
                }
                field("notes", req.notes) {
                    maxLength(1000)
                }
                field("loyaltyTier", req.loyaltyTier) {
                    required()
                    custom("Invalid loyalty tier", "INVALID_TIER") {
                        CustomerTier.isValid(it as String)
                    }
                }
            }
            
            // BUSINESS RULE: Check for duplicate phone
            if (mockCustomerExists(businessId, req.phone)) {
                throw DuplicateResourceException(
                    resource = "Customer",
                    field = "phone",
                    value = req.phone
                )
            }
            
            // BUSINESS LOGIC: Create customer (mock)
            val customer = mockCreateCustomer(businessId, req)
            
            // DTO: Return response DTO
            call.respond(HttpStatusCode.Created, ApiResponse(true, data = customer))
        }
        
        route("/{id}") {
            /**
             * GET: Retrieve single customer
             * GET /customers/{id}
             */
            get {
                val businessId = call.businessId()
                val id = call.parameters["id"] ?: throw IllegalArgumentException("Customer ID required")
                
                // VALIDATION: UUID format
                Validator.validate {
                    field("id", id) {
                        uuid()
                    }
                }
                
                // BUSINESS LOGIC: Fetch customer
                val customer = mockFetchCustomerById(businessId, id)
                    ?: throw NotFoundException("Customer", id)
                
                call.respond(HttpStatusCode.OK, ApiResponse(true, data = customer))
            }
            
            /**
             * UPDATE: Modify customer details
             * PUT /customers/{id}
             */
            put {
                val businessId = call.businessId()
                val id = call.parameters["id"] ?: throw IllegalArgumentException("Customer ID required")
                val req = call.receive<UpdateCustomerRequestDTO>()
                
                // VALIDATION
                Validator.validate {
                    field("id", id) {
                        uuid()
                    }
                    field("name", req.name) {
                        optional {
                            length(2, 255)
                        }
                    }
                    field("phone", req.phone) {
                        optional {
                            phone()
                        }
                    }
                    field("email", req.email) {
                        optional {
                            email()
                        }
                    }
                }
                
                // Check exists
                mockFetchCustomerById(businessId, id)
                    ?: throw NotFoundException("Customer", id)
                
                // Update
                val updated = mockUpdateCustomer(businessId, id, req)
                call.respond(HttpStatusCode.OK, ApiResponse(true, data = updated))
            }
            
            /**
             * DELETE: Remove customer (soft delete)
             * DELETE /customers/{id}
             */
            delete {
                val businessId = call.businessId()
                val id = call.parameters["id"] ?: throw IllegalArgumentException("Customer ID required")
                
                // Admin only
                if (!call.hasRole("ADMIN")) {
                    throw ForbiddenException("Admin access required to delete customers")
                }
                
                // VALIDATION
                Validator.validate {
                    field("id", id) {
                        uuid()
                    }
                }
                
                // Check exists
                mockFetchCustomerById(businessId, id)
                    ?: throw NotFoundException("Customer", id)
                
                // BUSINESS RULE: Can't delete customers with active orders
                if (mockHasActiveOrders(businessId, id)) {
                    throw BusinessRuleException(
                        message = "Cannot delete customer with active orders",
                        errorCode = "CUSTOMER_HAS_ORDERS"
                    )
                }
                
                // Delete
                mockDeleteCustomer(businessId, id)
                call.respond(HttpStatusCode.OK, ApiResponse<Unit>(true, message = "Customer deleted successfully"))
            }
            
            /**
             * CUSTOM ACTION: Award loyalty points
             * POST /customers/{id}/award-points
             */
            post("/award-points") {
                val businessId = call.businessId()
                val id = call.parameters["id"] ?: throw IllegalArgumentException("Customer ID required")
                
                @Serializable
                data class AwardPointsRequest(val points: Int, val reason: String)
                
                val req = call.receive<AwardPointsRequest>()
                
                // VALIDATION
                Validator.validate {
                    field("id", id) {
                        uuid()
                    }
                    field("points", req.points) {
                        positive()
                        max(10000.0)
                    }
                    field("reason", req.reason) {
                        required()
                        minLength(5)
                        maxLength(500)
                    }
                }
                
                // Award points
                val customer = mockAwardPoints(businessId, id, req.points)
                call.respond(HttpStatusCode.OK, ApiResponse(true, data = customer, message = "${req.points} points awarded"))
            }
        }
    }
}

// ──── Mock Functions (Replace with actual service calls) ─────────────────────

private fun mockFetchCustomers(businessId: String, search: String?, tier: String?): List<CustomerResponseDTO> {
    // In real implementation, query database with filters
    return listOf(
        CustomerResponseDTO(
            id = "cust_001",
            businessId = businessId,
            name = "John Doe",
            phone = "254712345678",
            email = "john@example.com",
            location = "Nairobi",
            notes = "Regular customer",
            loyaltyPoints = 150,
            loyaltyTier = "SILVER",
            totalOrders = 5,
            totalSpent = 15_000.0,
            isVip = true,
            lifetimeValue = "KES 15,000",
            createdAt = "2026-01-15T10:30:00Z"
        )
    )
}

private fun mockFetchCustomerById(businessId: String, id: String): CustomerResponseDTO? {
    return if (id == "cust_001") {
        mockFetchCustomers(businessId, null, null).first()
    } else null
}

private fun mockCustomerExists(businessId: String, phone: String): Boolean = false

private fun mockCreateCustomer(businessId: String, req: CreateCustomerRequestDTO): CustomerResponseDTO {
    return CustomerResponseDTO(
        id = "cust_${System.currentTimeMillis()}",
        businessId = businessId,
        name = req.name,
        phone = req.phone,
        email = req.email,
        location = req.location,
        notes = req.notes,
        loyaltyPoints = 0,
        loyaltyTier = req.loyaltyTier,
        totalOrders = 0,
        totalSpent = 0.0,
        isVip = false,
        lifetimeValue = "KES 0",
        createdAt = java.time.Instant.now().toString()
    )
}

private fun mockUpdateCustomer(businessId: String, id: String, req: UpdateCustomerRequestDTO): CustomerResponseDTO {
    val existing = mockFetchCustomerById(businessId, id)!!
    return existing.copy(
        name = req.name ?: existing.name,
        phone = req.phone ?: existing.phone,
        email = req.email ?: existing.email,
        location = req.location ?: existing.location,
        notes = req.notes ?: existing.notes
    )
}

private fun mockDeleteCustomer(businessId: String, id: String) {
    // Soft delete in real implementation
}

private fun mockHasActiveOrders(businessId: String, customerId: String): Boolean = false

private fun mockAwardPoints(businessId: String, id: String, points: Int): CustomerResponseDTO {
    val existing = mockFetchCustomerById(businessId, id)!!
    return existing.copy(loyaltyPoints = existing.loyaltyPoints + points)
}

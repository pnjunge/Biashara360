package com.app.biashara.routes

import com.app.biashara.constants.Constants
import com.app.biashara.constants.ProductCategory
import com.app.biashara.constants.StockTransactionType
import com.app.biashara.exceptions.ForbiddenException
import com.app.biashara.exceptions.NotFoundException
import com.app.biashara.models.*
import com.app.biashara.services.ProductService
import com.app.biashara.services.InventoryCategoryService
import com.app.biashara.utils.PaginationParams
import com.app.biashara.validation.Validator
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Product and inventory routes with comprehensive validation.
 */
fun Route.productRoutesValidated() {
    val productService: ProductService by inject()
    val categoryService: InventoryCategoryService by inject()

    route("/products") {
        moduleGuard("INVENTORY")

        route("/categories") {
            get {
                call.respond(ApiResponse(true, data = categoryService.getAll(call.businessId())))
            }
            post {
                if (!call.hasRole("ADMIN")) throw ForbiddenException("Admin access required to manage categories")
                val req = call.receive<CreateInventoryCategoryRequest>()
                val result = categoryService.create(call.businessId(), req.name, req.imageUrl)
                call.respond(if (result.success) HttpStatusCode.Created else HttpStatusCode.BadRequest, result)
            }
            put("/{categoryId}") {
                if (!call.hasRole("ADMIN")) throw ForbiddenException("Admin access required to manage categories")
                val categoryId = call.parameters["categoryId"] ?: throw IllegalArgumentException("Category ID required")
                val req = call.receive<UpdateInventoryCategoryRequest>()
                val result = categoryService.update(categoryId, call.businessId(), req)
                call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, result)
            }
        }
        
        /**
         * List all products with pagination
         * GET /products?q=search&lowStock=true&page=1&pageSize=20
         */
        get {
            val businessId = call.businessId()
            val query = call.request.queryParameters["q"]
            val lowStock = call.request.queryParameters["lowStock"]?.toBoolean() ?: false
            val includeInactive = call.request.queryParameters["includeInactive"]?.toBoolean() ?: false
            
            // Validate pagination parameters
            val page = call.request.queryParameters["page"]?.toIntOrNull()
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull()
            
            if (page != null && page < 1) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(false, message = "Page must be >= 1")
                )
                return@get
            }
            
            if (pageSize != null && (pageSize < 1 || pageSize > Constants.Business.MAX_PAGE_SIZE)) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(false, message = "Page size must be between 1 and ${Constants.Business.MAX_PAGE_SIZE}")
                )
                return@get
            }
            
            val products = productService.getAll(businessId, query, lowStock, includeInactive)
            
            // Apply pagination if requested
            if (page != null) {
                val params = PaginationParams.from(page, pageSize)
                val pagedProducts = products.drop(params.offset.toInt()).take(params.limit)
                call.respond(
                    PagedResponse(
                        data = pagedProducts,
                        total = products.size,
                        page = params.page,
                        pageSize = params.pageSize,
                        hasMore = (params.page * params.pageSize) < products.size
                    )
                )
            } else {
                call.respond(ApiResponse(true, data = products))
            }
        }

        /**
         * Create new product
         * POST /products
         */
        post {
            val businessId = call.businessId()
            val req = call.receive<ProductRequest>()
            
            // Validate product creation request
            Validator.validate {
                field("sku", req.sku) {
                    required()
                    matches(Constants.Patterns.SKU, "SKU can only contain letters, numbers, hyphens, and underscores")
                    maxLength(50)
                }
                field("name", req.name) {
                    required()
                    length(2, Constants.Business.MAX_PRODUCT_NAME_LENGTH)
                }
                field("description", req.description) {
                    maxLength(Constants.Business.MAX_DESCRIPTION_LENGTH)
                }
                field("buyingPrice", req.buyingPrice) {
                    required()
                    validAmount(min = 0.0, max = Constants.Business.MAX_ORDER_AMOUNT)
                }
                field("sellingPrice", req.sellingPrice) {
                    required()
                    validAmount(min = 0.0, max = Constants.Business.MAX_ORDER_AMOUNT)
                    custom("Selling price should be greater than buying price", "INVALID_PROFIT_MARGIN") {
                        (it as Double) >= req.buyingPrice
                    }
                }
                field("currentStock", req.currentStock) {
                    nonNegative()
                    range(
                        Constants.Business.MIN_STOCK_QUANTITY.toDouble(),
                        Constants.Business.MAX_STOCK_QUANTITY.toDouble()
                    )
                }
                field("lowStockThreshold", req.lowStockThreshold) {
                    nonNegative()
                    max(1000.0)
                }
                field("category", req.category) {
                    optional {
                        maxLength(80)
                        matches(Regex("^[A-Za-z0-9][A-Za-z0-9 &/_-]*$"), "Category contains unsupported characters")
                    }
                }
                field("imageUrl", req.imageUrl) {
                    optional {
                        maxLength(500)
                        matches(Regex("^https?://.*"), "Image URL must be a valid HTTP(S) URL")
                    }
                }
                field("barcode", req.barcode) {
                    optional {
                        maxLength(100)
                        matches(Regex("^[A-Za-z0-9-]+$"), "Barcode contains unsupported characters")
                    }
                }
            }
            
            val result = productService.create(businessId, req)
            call.respond(
                if (result.success) HttpStatusCode.Created else HttpStatusCode.BadRequest,
                result
            )
        }

        route("/{id}") {
            /**
             * Get product by ID
             * GET /products/{id}
             */
            get {
                val businessId = call.businessId()
                val id = call.parameters["id"] ?: throw IllegalArgumentException("Product ID required")
                
                Validator.validate {
                    field("id", id) {
                        required()
                    }
                }
                
                val product = productService.getById(id, businessId)
                    ?: throw NotFoundException("Product", id)
                
                call.respond(ApiResponse(true, data = product))
            }

            /**
             * Update product
             * PUT /products/{id}
             */
            put {
                val businessId = call.businessId()
                val id = call.parameters["id"] ?: throw IllegalArgumentException("Product ID required")
                val req = call.receive<ProductRequest>()
                
                Validator.validate {
                    field("id", id) {
                        required()
                    }
                    field("sku", req.sku) {
                        required()
                        matches(Constants.Patterns.SKU, "SKU can only contain letters, numbers, hyphens, and underscores")
                        maxLength(50)
                    }
                    field("name", req.name) {
                        required()
                        length(2, Constants.Business.MAX_PRODUCT_NAME_LENGTH)
                    }
                    field("buyingPrice", req.buyingPrice) {
                        required()
                        positive()
                    }
                    field("sellingPrice", req.sellingPrice) {
                        required()
                        positive()
                        custom("Selling price should be greater than buying price", "INVALID_PROFIT_MARGIN") {
                            (it as Double) >= req.buyingPrice
                        }
                    }
                    field("imageUrl", req.imageUrl) {
                        optional {
                            maxLength(500)
                            matches(Regex("^https?://.*"), "Image URL must be a valid HTTP(S) URL")
                        }
                    }
                }
                
                val result = productService.update(id, businessId, req)
                call.respond(
                    when {
                        result.success -> HttpStatusCode.OK
                        result.message.contains("changed since", ignoreCase = true) -> HttpStatusCode.Conflict
                        else -> HttpStatusCode.BadRequest
                    },
                    result
                )
            }

            /**
             * Toggle product active status (enable/disable)
             * PUT /products/{id}/status
             */
            put("/status") {
                val businessId = call.businessId()
                val id = call.parameters["id"] ?: throw IllegalArgumentException("Product ID required")
                val req = call.receive<UpdateProductStatusRequest>()
                
                Validator.validate {
                    field("id", id) {
                        required()
                    }
                }
                
                val result = productService.toggleStatus(id, businessId, req.isActive)
                call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.NotFound, result)
            }

            /**
             * Delete (soft delete / disable) product
             * DELETE /products/{id}
             */
            delete {
                val businessId = call.businessId()
                val id = call.parameters["id"] ?: throw IllegalArgumentException("Product ID required")
                
                if (!call.hasRole("ADMIN")) {
                    throw ForbiddenException("Admin access required to delete products")
                }
                
                Validator.validate {
                    field("id", id) {
                        required()
                    }
                }
                
                val result = productService.delete(id, businessId)
                call.respond(
                    if (result.success) HttpStatusCode.OK else HttpStatusCode.NotFound,
                    result
                )
            }

            /**
             * Update stock levels
             * POST /products/{id}/stock
             */
            post("/stock") {
                val businessId = call.businessId()
                val id = call.parameters["id"] ?: throw IllegalArgumentException("Product ID required")
                val req = call.receive<StockUpdateRequest>()
                
                // Validate stock update request
                Validator.validate {
                    field("id", id) {
                        required()
                    }
                    field("type", req.type) {
                        required()
                        custom("Invalid stock transaction type", "INVALID_TYPE") {
                            StockTransactionType.isValid(it as String)
                        }
                    }
                    field("quantity", req.quantity) {
                        required()
                        nonNegative()
                        range(0.0, Constants.Business.MAX_STOCK_QUANTITY.toDouble())
                    }
                    field("note", req.note ?: "") {
                        maxLength(500)
                    }
                }
                
                val result = productService.updateStock(id, businessId, req)
                call.respond(
                    if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest,
                    result
                )
            }
        }
    }
}

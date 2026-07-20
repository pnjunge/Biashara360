package com.app.biashara.network

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

/**
 * Base API service with common request handling.
 * All API repositories should extend this class.
 */
abstract class ApiService {
    
    protected val client = ApiClient.client
    
    /**
     * Safe GET request with error handling
     */
    protected suspend inline fun <reified T> get(
        path: String,
        params: Map<String, String> = emptyMap()
    ): Result<T> {
        return try {
            val response: HttpResponse = client.get(path) {
                params.forEach { (key, value) ->
                    parameter(key, value)
                }
            }
            
            handleResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Safe POST request with error handling
     */
    protected suspend inline fun <reified T, reified R> post(
        path: String,
        body: T
    ): Result<R> {
        return try {
            val response: HttpResponse = client.post(path) {
                setBody(body)
            }
            
            handleResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Safe PUT request with error handling
     */
    protected suspend inline fun <reified T, reified R> put(
        path: String,
        body: T
    ): Result<R> {
        return try {
            val response: HttpResponse = client.put(path) {
                setBody(body)
            }
            
            handleResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Safe PATCH request with error handling
     */
    protected suspend inline fun <reified T, reified R> patch(
        path: String,
        body: T
    ): Result<R> {
        return try {
            val response: HttpResponse = client.patch(path) {
                setBody(body)
            }
            
            handleResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Safe DELETE request with error handling
     */
    protected suspend inline fun <reified T> delete(
        path: String
    ): Result<T> {
        return try {
            val response: HttpResponse = client.delete(path)
            handleResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Handle HTTP response and convert to Result
     */
    protected suspend inline fun <reified T> handleResponse(
        response: HttpResponse
    ): Result<T> {
        return when (response.status) {
            HttpStatusCode.OK,
            HttpStatusCode.Created -> {
                try {
                    Result.success(response.body<T>())
                } catch (e: Exception) {
                    Result.failure(Exception("Failed to parse response: ${e.message}"))
                }
            }
            HttpStatusCode.Unauthorized -> {
                // Auto-logout on 401
                ApiClient.clearTokens()
                Result.failure(UnauthorizedException("Session expired. Please login again."))
            }
            HttpStatusCode.Forbidden -> {
                Result.failure(ForbiddenException("Access denied"))
            }
            HttpStatusCode.NotFound -> {
                Result.failure(NotFoundException("Resource not found"))
            }
            HttpStatusCode.BadRequest -> {
                try {
                    val errorResponse = response.body<ErrorResponse>()
                    Result.failure(BadRequestException(errorResponse.error.message))
                } catch (e: Exception) {
                    Result.failure(BadRequestException("Bad request"))
                }
            }
            HttpStatusCode.InternalServerError -> {
                Result.failure(ServerException("Server error occurred"))
            }
            else -> {
                Result.failure(Exception("Unexpected error: ${response.status.value}"))
            }
        }
    }
}

// Custom exceptions for better error handling
class UnauthorizedException(message: String) : Exception(message)
class ForbiddenException(message: String) : Exception(message)
class NotFoundException(message: String) : Exception(message)
class BadRequestException(message: String) : Exception(message)
class ServerException(message: String) : Exception(message)
class NetworkException(message: String) : Exception(message)

/**
 * Extension function to unwrap ApiResponse
 */
suspend inline fun <reified T> HttpResponse.unwrap(): Result<T> {
    return try {
        val apiResponse = body<ApiResponse<T>>()
        if (apiResponse.success && apiResponse.data != null) {
            Result.success(apiResponse.data)
        } else {
            Result.failure(Exception(apiResponse.message.ifEmpty { "Request failed" }))
        }
    } catch (e: Exception) {
        Result.failure(Exception("Failed to parse response: ${e.message}"))
    }
}

package com.app.biashara.services

import com.app.biashara.db.*
import com.app.biashara.models.*
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class SubscriptionService(private val settings:SystemSettingsService, private val orders:OrderService, private val mpesa:MpesaService) {
    private val json=Json{ignoreUnknownKeys=true}
    fun bands():List<SubscriptionBand> = runCatching { json.decodeFromString<List<SubscriptionBand>>(settings.getSetting("subscription_user_bands").orEmpty()) }.getOrElse { defaults }
        .filter { it.minUsers > 0 && it.maxUsers >= it.minUsers && it.monthlyPrice > 0 }.sortedBy { it.minUsers }
    fun configureBands(value:List<SubscriptionBand>):ApiResponse<List<SubscriptionBand>> {
        val normalized=value.sortedBy{it.minUsers}
        if(normalized.isEmpty() || normalized.any{it.id.isBlank()||it.name.isBlank()||it.minUsers<1||it.maxUsers<it.minUsers||it.monthlyPrice<=0} || normalized.zipWithNext().any{it.first.maxUsers>=it.second.minUsers}) return ApiResponse(false,message="Bands must be valid, positive, and non-overlapping")
        settings.saveSetting("subscription_user_bands",json.encodeToString(normalized)); return ApiResponse(true,data=normalized,message="Subscription bands updated")
    }
    suspend fun checkout(businessId:String, req:SubscriptionCheckoutRequest):ApiResponse<OrderResponse> {
        val method=req.paymentMethod.uppercase(); if(method !in setOf("MPESA","CARD")) return ApiResponse(false,message="Choose M-Pesa or card")
        val band=bands().firstOrNull{req.userCount in it.minUsers..it.maxUsers} ?: return ApiResponse(false,message="No subscription band supports this number of users")
        val order=transaction {
            val now=Clock.System.now(); val id=UUID.randomUUID().toString(); val number="B360-SUB-${UUID.randomUUID().toString().take(8).uppercase()}"
            OrdersTable.insert { it[OrdersTable.id]=id;it[orderNumber]=number;it[OrdersTable.businessId]=businessId;it[clientReference]="subscription:${band.id}:${req.userCount}";it[customerName]="Subscription";it[customerPhone]=req.phoneNumber;it[deliveryLocation]="Digital";it[paymentStatus]="PENDING";it[deliveryStatus]="PROCESSING";it[paymentMethod]=method;it[salesChannel]="WEB";it[serviceType]="SUBSCRIPTION";it[baseAmount]=band.monthlyPrice;it[subtotal]=band.monthlyPrice;it[notes]="${band.name} subscription for ${req.userCount} users";it[createdAt]=now;it[updatedAt]=now }
            OrderItemsTable.insert { it[OrderItemsTable.id]=UUID.randomUUID().toString();it[orderId]=id;it[productId]="SUB-${band.id}";it[productName]="${band.name} (${req.userCount} users / month)";it[quantity]=1;it[unitPrice]=band.monthlyPrice;it[buyingPrice]=0.0 }
            orders.getById(id,businessId)!!
        }
        if(method=="MPESA") when(val push=mpesa.initiateSTKPush(req.phoneNumber,order.subtotal,order.orderNumber,"Biashara360 subscription",null)){ is StkPushResult.Success -> orders.recordMpesaCheckoutAttempt(businessId,order.id,push.checkoutRequestId); is StkPushResult.Error -> return ApiResponse(false,message=push.message) }
        return ApiResponse(true,data=order)
    }
    companion object { val defaults=listOf(SubscriptionBand("STARTER","Starter",1,2,500.0),SubscriptionBand("TEAM","Team",3,5,1000.0),SubscriptionBand("GROWTH","Growth",6,10,2000.0)) }
}

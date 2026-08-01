package com.app.biashara.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class BusinessProfile(
    val id: String,
    val name: String,
    val owner: String,
    val phone: String,
    val email: String,
    val type: String,
    val county: String,
    val address: String,
    val kraPin: String,
    val paybillNumber: String,
    val accountNumber: String,
    val subscriptionTier: String,
    val subscriptionEnabled: Boolean = true,
    val hospitalityEnabled: Boolean = false,
    val receiptHeader: String = "Welcome to our store!",
    val receiptFooter: String = "Thank you for shopping with us!",
    val receiptLogo: String? = null,
    val receiptShowTax: Boolean = true,
    val receiptShowCustomer: Boolean = true
)

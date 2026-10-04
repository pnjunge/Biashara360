package com.app.biashara.services

import com.app.biashara.auth.generateId
import com.app.biashara.db.*
import com.app.biashara.models.*
import kotlinx.datetime.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

internal data class PurchasePaymentTotals(val paid:Double,val outstanding:Double,val status:String)
internal fun ingredientPaymentTotals(po:ResultRow):PurchasePaymentTotals {
    val paid=IngredientPurchasePaymentsTable.select { (IngredientPurchasePaymentsTable.purchaseOrderId eq po[PurchaseOrdersTable.id]) and (IngredientPurchasePaymentsTable.businessId eq po[PurchaseOrdersTable.businessId]) }.sumOf { it[IngredientPurchasePaymentsTable.amount] }
    val outstanding=(kotlin.math.round((po[PurchaseOrdersTable.totalCost]-paid)*100)/100).coerceAtLeast(0.0)
    val status=when { po[PurchaseOrdersTable.totalCost]==0.0 -> "NOT_REQUIRED"; outstanding<0.005 -> "PAID"; paid>0 -> "PARTIAL"; !po[PurchaseOrdersTable.paymentsKnown] -> "UNRECORDED"; else -> "UNPAID" }
    return PurchasePaymentTotals(paid,outstanding,status)
}
internal fun recordIngredientPurchaseExpense(po:ResultRow,now:Instant) {
    if(po[PurchaseOrdersTable.totalCost]==0.0)return
    val supplier=SuppliersTable.select { (SuppliersTable.id eq po[PurchaseOrdersTable.supplierId]) and (SuppliersTable.businessId eq po[PurchaseOrdersTable.businessId]) }.first()
    ExpensesTable.insert {
        it[id]=generateId();it[businessId]=po[PurchaseOrdersTable.businessId];it[purchaseOrderId]=po[PurchaseOrdersTable.id]
        it[category]="STOCK_PURCHASE";it[amount]=po[PurchaseOrdersTable.totalCost]
        it[description]="Ingredient purchase: #${po[PurchaseOrdersTable.orderNumber]} - ${supplier[SuppliersTable.name]}"
        it[expenseDate]=now.toLocalDateTime(TimeZone.of("Africa/Nairobi")).date;it[recordedAt]=now
    }
}
/** Supplier outflows never enter the customer-payment ledger. Caller holds the business and PO locks. */
internal fun recordIngredientPayment(businessId:String,userId:String,po:ResultRow,request:IngredientPurchasePaymentRequest):String {
    require(po[PurchaseOrdersTable.status]=="RECEIVED") { "Receive the goods before recording supplier payment" }
    val method=request.method.trim().uppercase();val reference=request.reference.trim();val key=request.clientReference.trim()
    require(method in setOf("CASH","MPESA","CARD","BANK_TRANSFER")) { "Choose a supported payment method" }
    require(key.length in 8..80 && reference.length<=120) { "A valid payment request reference is required" }
    require(!request.paidFromTill || method=="CASH") { "Only cash payments can come from the cash drawer" }
    require(method=="CASH" || reference.isNotBlank()) { "Enter the supplier payment transaction reference" }
    require(request.amount.isFinite() && request.amount>0 && kotlin.math.abs(request.amount*100-kotlin.math.round(request.amount*100))<0.00001) { "Payment must be a positive KES amount with at most two decimal places" }
    val prior=IngredientPurchasePaymentsTable.select { (IngredientPurchasePaymentsTable.businessId eq businessId) and (IngredientPurchasePaymentsTable.clientReference eq key) }.firstOrNull()
    if(prior!=null) {
        require(prior[IngredientPurchasePaymentsTable.purchaseOrderId]==po[PurchaseOrdersTable.id] && prior[IngredientPurchasePaymentsTable.amount]==request.amount && prior[IngredientPurchasePaymentsTable.method]==method && prior[IngredientPurchasePaymentsTable.reference]==reference && prior[IngredientPurchasePaymentsTable.paidFromTill]==request.paidFromTill) { "This payment request reference was already used for different details" }
        return prior[IngredientPurchasePaymentsTable.id]
    }
    require(reference.isBlank() || !IngredientPurchasePaymentsTable.select { (IngredientPurchasePaymentsTable.businessId eq businessId) and (IngredientPurchasePaymentsTable.method eq method) and (IngredientPurchasePaymentsTable.reference eq reference) }.any()) { "This supplier payment transaction reference has already been recorded" }
    if(request.paidFromTill) require(HospitalityShiftsTable.select{(HospitalityShiftsTable.businessId eq businessId) and (HospitalityShiftsTable.status eq "OPEN")}.any()) { "Open the business trading day before recording a cash drawer payment" }
    require(request.amount<=ingredientPaymentTotals(po).outstanding+0.000001) { "Payment exceeds the supplier balance" }
    val id=generateId();val now=Clock.System.now()
    IngredientPurchasePaymentsTable.insert {it[IngredientPurchasePaymentsTable.id]=id;it[IngredientPurchasePaymentsTable.businessId]=businessId;it[purchaseOrderId]=po[PurchaseOrdersTable.id];it[amount]=request.amount;it[IngredientPurchasePaymentsTable.method]=method;it[IngredientPurchasePaymentsTable.reference]=reference;it[clientReference]=key;it[paidFromTill]=request.paidFromTill;it[recordedBy]=userId;it[paidAt]=now}
    PurchaseOrdersTable.update({PurchaseOrdersTable.id eq po[PurchaseOrdersTable.id]}) {it[paymentsKnown]=true}
    AuditEventsTable.insert {it[AuditEventsTable.id]=generateId();it[AuditEventsTable.businessId]=businessId;it[AuditEventsTable.userId]=userId;it[action]="INGREDIENT_SUPPLIER_PAYMENT";it[entityType]="PURCHASE_ORDER";it[entityId]=po[PurchaseOrdersTable.id];it[details]="Payment $id: KES ${request.amount}, $method, reference $reference";it[occurredAt]=now}
    return id
}
internal fun expensePaymentTotals(expense:ResultRow):PurchasePaymentTotals {
    val business=expense[ExpensesTable.businessId];val poId=expense[ExpensesTable.purchaseOrderId]
    if(poId!=null) return ingredientPaymentTotals(PurchaseOrdersTable.select { (PurchaseOrdersTable.id eq poId) and (PurchaseOrdersTable.businessId eq business) }.first())
    val invoice=if(expense[ExpensesTable.category]=="STOCK_PURCHASE") PurchaseInvoicesTable.select { (PurchaseInvoicesTable.id eq expense[ExpensesTable.id]) and (PurchaseInvoicesTable.businessId eq business) }.firstOrNull() else null
    val paid=if(invoice==null || invoice[PurchaseInvoicesTable.paymentStatus]=="PAID")expense[ExpensesTable.amount] else 0.0
    return PurchasePaymentTotals(paid,expense[ExpensesTable.amount]-paid,if(paid==expense[ExpensesTable.amount])"PAID" else "UNPAID")
}
internal fun stockPurchaseCashOut(business:String,start:Instant,end:Instant):Double {
    val zone=TimeZone.of("Africa/Nairobi");val first=start.toLocalDateTime(zone).date;val last=Instant.fromEpochMilliseconds(end.toEpochMilliseconds()-1).toLocalDateTime(zone).date
    val manualAndInvoices=ExpensesTable.select { (ExpensesTable.businessId eq business) and (ExpensesTable.category eq "STOCK_PURCHASE") and ExpensesTable.purchaseOrderId.isNull() and (ExpensesTable.expenseDate greaterEq first) and (ExpensesTable.expenseDate lessEq last) }.sumOf { expensePaymentTotals(it).paid }
    val ingredientPayments=IngredientPurchasePaymentsTable.select { (IngredientPurchasePaymentsTable.businessId eq business) and (IngredientPurchasePaymentsTable.paidAt greaterEq start) and (IngredientPurchasePaymentsTable.paidAt less end) }.sumOf { it[IngredientPurchasePaymentsTable.amount] }
    return manualAndInvoices+ingredientPayments
}

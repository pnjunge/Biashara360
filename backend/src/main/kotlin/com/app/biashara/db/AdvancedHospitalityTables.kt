package com.app.biashara.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object HospitalityReservationsTable : Table("hospitality_reservations") {
    val id=varchar("id",36); val businessId=varchar("business_id",36); val tableId=varchar("table_id",36).nullable()
    val customerName=varchar("customer_name",255); val customerPhone=varchar("customer_phone",20); val guestCount=integer("guest_count")
    val reservedAt=timestamp("reserved_at"); val durationMinutes=integer("duration_minutes"); val status=varchar("status",20); val notes=varchar("notes",500)
    val createdAt=timestamp("created_at"); val updatedAt=timestamp("updated_at"); override val primaryKey=PrimaryKey(id)
}
object HospitalityMenuProfilesTable : Table("hospitality_menu_profiles") {
    val productId=varchar("product_id",36); val businessId=varchar("business_id",36); val preparationStation=varchar("preparation_station",20).nullable()
    val mealPeriods=text("meal_periods"); val sizesJson=text("sizes_json"); val extrasJson=text("extras_json"); val variantsJson=text("variants_json"); val comboJson=text("combo_json")
    val soldOut=bool("sold_out"); val happyHourPrice=double("happy_hour_price").nullable(); val happyHourStart=varchar("happy_hour_start",5).nullable(); val happyHourEnd=varchar("happy_hour_end",5).nullable()
    val ageRestricted=bool("age_restricted"); val minimumAge=integer("minimum_age").nullable(); val updatedAt=timestamp("updated_at"); override val primaryKey=PrimaryKey(productId)
}
object InventoryIngredientsTable : Table("inventory_ingredients") {
    val id=varchar("id",36); val businessId=varchar("business_id",36); val name=varchar("name",160); val unit=varchar("unit",20)
    val quantity=double("quantity"); val reorderLevel=double("reorder_level"); val unitCost=double("unit_cost"); val isActive=bool("is_active")
    val createdAt=timestamp("created_at"); val updatedAt=timestamp("updated_at"); override val primaryKey=PrimaryKey(id)
}
object ProductRecipesTable : Table("product_recipes") {
    val productId=varchar("product_id",36); val ingredientId=varchar("ingredient_id",36); val quantity=double("quantity"); override val primaryKey=PrimaryKey(productId,ingredientId)
}
object BarStockEventsTable : Table("bar_stock_events") {
    val id=varchar("id",36); val businessId=varchar("business_id",36); val productId=varchar("product_id",36).nullable(); val ingredientId=varchar("ingredient_id",36).nullable()
    val eventType=varchar("event_type",30); val quantity=double("quantity"); val unit=varchar("unit",20); val reason=varchar("reason",500); val recordedBy=varchar("recorded_by",36).nullable(); val recordedAt=timestamp("recorded_at"); override val primaryKey=PrimaryKey(id)
}
object HospitalityShiftsTable : Table("hospitality_shifts") {
    val id=varchar("id",36); val businessId=varchar("business_id",36); val openedBy=varchar("opened_by",36); val closedBy=varchar("closed_by",36).nullable()
    val openedAt=timestamp("opened_at"); val closedAt=timestamp("closed_at").nullable(); val openingFloat=double("opening_float"); val expectedCash=double("expected_cash").nullable(); val actualCash=double("actual_cash").nullable()
    val mpesaTotal=double("mpesa_total").nullable(); val cardTotal=double("card_total").nullable(); val mpesaActual=double("mpesa_actual").nullable(); val cardActual=double("card_actual").nullable(); val tipsTotal=double("tips_total"); val expensesTotal=double("expenses_total"); val status=varchar("status",20); val notes=varchar("notes",500); override val primaryKey=PrimaryKey(id)
}
object SuppliersTable : Table("suppliers") {
    val id=varchar("id",36); val businessId=varchar("business_id",36); val name=varchar("name",255); val phone=varchar("phone",20); val email=varchar("email",255).nullable(); val address=varchar("address",500).nullable(); val isActive=bool("is_active"); val createdAt=timestamp("created_at"); val updatedAt=timestamp("updated_at"); override val primaryKey=PrimaryKey(id)
}
object PurchaseOrdersTable : Table("purchase_orders") {
    val id=varchar("id",36); val businessId=varchar("business_id",36); val supplierId=varchar("supplier_id",36); val orderNumber=varchar("order_number",60); val status=varchar("status",20); val orderedAt=timestamp("ordered_at"); val receivedAt=timestamp("received_at").nullable(); val totalCost=double("total_cost"); val notes=varchar("notes",500); val createdBy=varchar("created_by",36).nullable(); override val primaryKey=PrimaryKey(id)
}
object PurchaseOrderItemsTable : Table("purchase_order_items") {
    val id=varchar("id",36); val purchaseOrderId=varchar("purchase_order_id",36); val ingredientId=varchar("ingredient_id",36); val orderedQuantity=double("ordered_quantity"); val receivedQuantity=double("received_quantity"); val unitCost=double("unit_cost"); override val primaryKey=PrimaryKey(id)
}
object ManagerApprovalsTable : Table("manager_approvals") {
    val id=varchar("id",36); val businessId=varchar("business_id",36); val actionType=varchar("action_type",30); val entityType=varchar("entity_type",30); val entityId=varchar("entity_id",36); val requestedBy=varchar("requested_by",36); val approvedBy=varchar("approved_by",36).nullable(); val status=varchar("status",20); val reason=varchar("reason",500); val requestedAt=timestamp("requested_at"); val decidedAt=timestamp("decided_at").nullable(); override val primaryKey=PrimaryKey(id)
}
object AuditEventsTable : Table("audit_events") {
    val id=varchar("id",36); val businessId=varchar("business_id",36); val userId=varchar("user_id",36).nullable(); val action=varchar("action",80); val entityType=varchar("entity_type",40); val entityId=varchar("entity_id",36).nullable(); val details=text("details"); val occurredAt=timestamp("occurred_at"); override val primaryKey=PrimaryKey(id)
}
object OrderSplitPaymentsTable : Table("order_split_payments") {
    val id=varchar("id",36);val businessId=varchar("business_id",36);val orderId=varchar("order_id",36);val amount=double("amount");val method=varchar("method",20);val status=varchar("status",20);val transactionCode=varchar("transaction_code",100).nullable();val createdBy=varchar("created_by",36).nullable();val createdAt=timestamp("created_at");override val primaryKey=PrimaryKey(id)
}

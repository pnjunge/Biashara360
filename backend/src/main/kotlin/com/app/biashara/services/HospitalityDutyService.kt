package com.app.biashara.services

import com.app.biashara.auth.generateId
import com.app.biashara.db.*
import com.app.biashara.models.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

/** Personal staff shifts are independent of the business trading day. */
class HospitalityDutyService(private val access: AccessControlService = AccessControlService()) {
    private val json=Json { ignoreUnknownKeys=true }
    fun dashboard(businessId: String, userId: String, manage: Boolean = false): HospitalityDutyResponse = transaction {
        val activeStaff = HospitalityStaffShiftsTable.select {
            (HospitalityStaffShiftsTable.businessId eq businessId) and (HospitalityStaffShiftsTable.status eq "OPEN")
        }.map { it[HospitalityStaffShiftsTable.userId] }.toSet()
        val staff = UsersTable.select { (UsersTable.businessId eq businessId) and (UsersTable.isActive eq true) }
            .filter { eligible(businessId, it[UsersTable.id]) }.map { HandoverStaffResponse(it[UsersTable.id],it[UsersTable.name],it[UsersTable.id] in activeStaff) }
        val history = HospitalityBillHandoversTable.select { HospitalityBillHandoversTable.businessId eq businessId }
            .orderBy(HospitalityBillHandoversTable.requestedAt,SortOrder.DESC).filter { manage || it[HospitalityBillHandoversTable.fromUserId]==userId || it[HospitalityBillHandoversTable.toUserId]==userId || it[HospitalityBillHandoversTable.requestedBy]==userId }.take(200).map { handoverResponse(it) }
        HospitalityDutyResponse(openShift(businessId,userId)?.let { shiftResponse(it) },staff,history,activeOrders(businessId).filter { owner(it)==userId }.map { it[OrdersTable.id] },reports(businessId,userId,false).filter { it.status=="CLOSED" }.take(10))
    }

    fun startShift(businessId: String, userId: String, request: StaffShiftRequest): StaffShiftResponse = transaction {
        lockBusiness(businessId); require(eligible(businessId,userId)) { "Waiter or cashier access is required" }
        require(openShift(businessId,userId)==null) { "Your staff shift is already open" }
        if(HospitalityShiftsTable.select { (HospitalityShiftsTable.businessId eq businessId) and (HospitalityShiftsTable.status eq "OPEN") }.none()) {
            val dayId=generateId()
            HospitalityShiftsTable.insert {
                it[id]=dayId;it[HospitalityShiftsTable.businessId]=businessId;it[openedBy]=userId;it[openedAt]=Clock.System.now()
                it[openingFloat]=0.0;it[tipsTotal]=0.0;it[expensesTotal]=0.0;it[status]="OPEN";it[notes]="Trading day opened by the first personal staff shift; zero cash float"
            }
            audit(businessId,userId,"TRADING_DAY_STARTED_BY_STAFF","SHIFT",dayId)
        }
        val id=generateId()
        HospitalityStaffShiftsTable.insert {
            it[HospitalityStaffShiftsTable.id]=id; it[HospitalityStaffShiftsTable.businessId]=businessId; it[HospitalityStaffShiftsTable.userId]=userId
            it[openedAt]=Clock.System.now(); it[status]="OPEN"; it[notes]=request.notes.take(500)
        }
        audit(businessId,userId,"STAFF_SHIFT_STARTED","STAFF_SHIFT",id)
        shiftResponse(openShift(businessId,userId)!!)
    }

    fun endShift(businessId: String,userId: String,request: StaffShiftRequest): StaffShiftResponse = transaction {
        lockBusiness(businessId)
        val shift=openShift(businessId,userId) ?: error("Your staff shift is not open")
        require(activeOrders(businessId).none { owner(it)==userId }) { "Settle or hand over your open bills and wait for acceptance before ending your staff shift" }
        freezeShift(shift,Clock.System.now(),request.notes)
        shiftResponse(HospitalityStaffShiftsTable.select { HospitalityStaffShiftsTable.id eq shift[HospitalityStaffShiftsTable.id] }.first())
    }

    fun reports(businessId:String,userId:String,manage:Boolean=false):List<StaffShiftResponse> = transaction {
        HospitalityStaffShiftsTable.select { (HospitalityStaffShiftsTable.businessId eq businessId) and (if(manage) Op.TRUE else HospitalityStaffShiftsTable.userId eq userId) }
            .orderBy(HospitalityStaffShiftsTable.openedAt,SortOrder.DESC).limit(100).map { shiftResponse(it) }
    }

    internal fun closeStaffShiftsForDay(businessId:String,closedAt:Instant) {
        HospitalityStaffShiftsTable.select { (HospitalityStaffShiftsTable.businessId eq businessId) and (HospitalityStaffShiftsTable.status eq "OPEN") }.toList().forEach { freezeShift(it,closedAt,it[HospitalityStaffShiftsTable.notes]) }
    }

    private fun freezeShift(shift:ResultRow,closedAt:Instant,notes:String) {
        val summary=calculateSummary(shift,closedAt)
        HospitalityStaffShiftsTable.update({ HospitalityStaffShiftsTable.id eq shift[HospitalityStaffShiftsTable.id] }) {
            it[status]="CLOSED"; it[HospitalityStaffShiftsTable.closedAt]=closedAt; it[HospitalityStaffShiftsTable.notes]=notes.take(500); it[summaryJson]=json.encodeToString(summary)
        }
        audit(shift[HospitalityStaffShiftsTable.businessId],shift[HospitalityStaffShiftsTable.userId],"STAFF_SHIFT_ENDED","STAFF_SHIFT",shift[HospitalityStaffShiftsTable.id],json.encodeToString(summary))
    }

    fun claimBill(businessId:String,userId:String,orderId:String):String = transaction {
        lockBusiness(businessId); require(eligible(businessId,userId)) { "Waiter or cashier access is required" }
        require(openShift(businessId,userId)!=null) { "Start your staff shift first" }
        val order=activeOrder(businessId,orderId)
        require(owner(order)==null) { "This bill already has a responsible staff member; request a handover" }
        OrdersTable.update({ OrdersTable.id eq orderId }) { it[responsibleUserId]=userId; it[updatedAt]=Clock.System.now() }
        audit(businessId,userId,"BILL_CLAIMED","ORDER",orderId); orderId
    }

    fun requestHandover(businessId:String,userId:String,request:BillHandoverRequest,manage:Boolean=false):List<BillHandoverResponse> = transaction {
        lockBusiness(businessId)
        require(eligible(businessId,userId)) { "Waiter or cashier access is required" }
        require(request.orderIds.isNotEmpty() && request.orderIds.size<=100 && request.orderIds.distinct().size==request.orderIds.size) { "Select distinct open bills (maximum 100)" }
        require(request.toUserId!=userId && eligible(businessId,request.toUserId)) { "Choose another active waiter or cashier in this business" }
        require(openShift(businessId,request.toUserId)!=null) { "The receiving staff member must start their staff shift first" }
        val targets=request.orderIds.map { id ->
            val order=activeOrder(businessId,id)
            val from=owner(order) ?: error("Claim an unassigned bill before handing it over")
            require(from!=request.toUserId) { "The receiving staff member already owns this bill" }
            require(from==userId || manage) { "Only the responsible staff member or a shift manager can hand over this bill" }
            require(!HospitalityBillHandoversTable.select { (HospitalityBillHandoversTable.orderId eq id) and (HospitalityBillHandoversTable.status eq "PENDING") }.any()) { "A handover is already pending for this bill" }
            order
        }
        val ids=targets.map { order ->
            val id=generateId()
            HospitalityBillHandoversTable.insert {
                it[HospitalityBillHandoversTable.id]=id; it[HospitalityBillHandoversTable.businessId]=businessId; it[orderId]=order[OrdersTable.id]
                it[fromUserId]=owner(order)!!; it[toUserId]=request.toUserId; it[requestedBy]=userId
                it[status]="PENDING"; it[notes]=request.notes.take(500); it[balanceAtRequest]=balance(order)
                it[requestedAt]=Clock.System.now()
            }
            audit(businessId,userId,"BILL_HANDOVER_REQUESTED","ORDER",order[OrdersTable.id],"Handover $id to ${request.toUserId}: ${request.notes.take(500)}")
            id
        }
        ids.map { id -> handoverResponse(HospitalityBillHandoversTable.select { HospitalityBillHandoversTable.id eq id }.first()) }
    }

    fun decide(businessId:String,userId:String,id:String,request:BillHandoverDecisionRequest):BillHandoverResponse = transaction {
        lockBusiness(businessId)
        val row=HospitalityBillHandoversTable.select { (HospitalityBillHandoversTable.id eq id) and (HospitalityBillHandoversTable.businessId eq businessId) }.forUpdate().firstOrNull() ?: error("Handover not found")
        require(row[HospitalityBillHandoversTable.status]=="PENDING") { "This handover has already been decided" }
        val action=request.action.trim().uppercase(); require(action in setOf("ACCEPT","REJECT","CANCEL")) { "Choose ACCEPT, REJECT or CANCEL" }
        if(action=="CANCEL") require(userId==row[HospitalityBillHandoversTable.fromUserId] || userId==row[HospitalityBillHandoversTable.requestedBy]) { "Only the sender can cancel this handover" }
        else require(userId==row[HospitalityBillHandoversTable.toUserId]) { "Only the receiving staff member can accept or reject this handover" }
        if(action=="ACCEPT") {
            require(eligible(businessId,userId) && openShift(businessId,userId)!=null) { "Start your staff shift with waiter or cashier access before accepting" }
            val order=activeOrder(businessId,row[HospitalityBillHandoversTable.orderId])
            require(owner(order)==row[HospitalityBillHandoversTable.fromUserId]) { "Bill responsibility has changed; cancel and recreate this handover" }
            OrdersTable.update({ OrdersTable.id eq order[OrdersTable.id] }) { it[responsibleUserId]=userId; it[updatedAt]=Clock.System.now() }
        }
        val status=when(action) { "ACCEPT" -> "ACCEPTED"; "REJECT" -> "REJECTED"; else -> "CANCELLED" }
        HospitalityBillHandoversTable.update({ HospitalityBillHandoversTable.id eq id }) { it[HospitalityBillHandoversTable.status]=status; it[decidedAt]=Clock.System.now() }
        audit(businessId,userId,"BILL_HANDOVER_$status","ORDER",row[HospitalityBillHandoversTable.orderId],"Handover $id")
        handoverResponse(HospitalityBillHandoversTable.select { HospitalityBillHandoversTable.id eq id }.first())
    }

    private fun eligible(businessId:String,userId:String):Boolean {
        if(!UsersTable.select { (UsersTable.id eq userId) and (UsersTable.businessId eq businessId) and (UsersTable.isActive eq true) }.any()) return false
        return access.hasPermission(userId,businessId,"hospitality.view") && (access.hasPermission(userId,businessId,"hospitality.orders") || access.hasPermission(userId,businessId,"hospitality.billing"))
    }
    private fun lockBusiness(id:String) { require(BusinessesTable.select { BusinessesTable.id eq id }.forUpdate().firstOrNull()!=null) { "Business not found" } }
    private fun openShift(b:String,u:String)=HospitalityStaffShiftsTable.select { (HospitalityStaffShiftsTable.businessId eq b) and (HospitalityStaffShiftsTable.userId eq u) and (HospitalityStaffShiftsTable.status eq "OPEN") }.firstOrNull()
    private fun owner(row:ResultRow)=row[OrdersTable.responsibleUserId] ?: row[OrdersTable.serverUserId]
    private fun activeOrders(b:String)=OrdersTable.select { (OrdersTable.businessId eq b) and (OrdersTable.serviceType inList listOf("DINE_IN","TAKEAWAY","DELIVERY")) and (OrdersTable.tabStatus inList HospitalityService.ACTIVE_TAB_STATUSES) and (OrdersTable.paymentStatus notInList listOf("PAID","CANCELLED","REFUNDED")) }.toList()
    private fun activeOrder(b:String,id:String):ResultRow = OrdersTable.select { (OrdersTable.businessId eq b) and (OrdersTable.id eq id) and (OrdersTable.serviceType inList listOf("DINE_IN","TAKEAWAY","DELIVERY")) and (OrdersTable.tabStatus inList HospitalityService.ACTIVE_TAB_STATUSES) and (OrdersTable.paymentStatus notInList listOf("PAID","CANCELLED","REFUNDED")) }.forUpdate().firstOrNull() ?: error("Active unpaid bill not found")
    private fun balance(order:ResultRow):Double {
        val paid=PaymentsTable.select { (PaymentsTable.orderId eq order[OrdersTable.id]) and (PaymentsTable.businessId eq order[OrdersTable.businessId]) and (PaymentsTable.status eq "SUCCESS") }.sumOf { it[PaymentsTable.amount] }
        return (order[OrdersTable.subtotal]-paid).coerceAtLeast(0.0)
    }
    private fun calculateSummary(shift:ResultRow,end:Instant):StaffShiftSummary {
        val business=shift[HospitalityStaffShiftsTable.businessId]; val user=shift[HospitalityStaffShiftsTable.userId]; val start=shift[HospitalityStaffShiftsTable.openedAt]
        val completed=OrdersTable.select {
            (OrdersTable.businessId eq business) and (OrdersTable.serviceType inList listOf("RETAIL","DINE_IN","TAKEAWAY","DELIVERY")) and
                (OrdersTable.paymentStatus eq "PAID") and (OrdersTable.deliveryStatus neq "CANCELLED") and
                (OrdersTable.completedAt greaterEq start) and (OrdersTable.completedAt less end) and
                ((OrdersTable.serverUserId eq user) or (OrdersTable.responsibleUserId eq user) or (OrdersTable.settledByUserId eq user))
        }.orderBy(OrdersTable.completedAt,SortOrder.ASC).toList()
        val payments=PaymentsTable.select {
            (PaymentsTable.businessId eq business) and (PaymentsTable.collectedByUserId eq user) and (PaymentsTable.status eq "SUCCESS") and
                (PaymentsTable.transactionDate greaterEq start) and (PaymentsTable.transactionDate less end) and
                (PaymentsTable.orderId inSubQuery OrdersTable.slice(OrdersTable.id).select { (OrdersTable.businessId eq business) and (OrdersTable.serviceType inList listOf("RETAIL","DINE_IN","TAKEAWAY","DELIVERY")) })
        }.toList()
        val totals=payments.groupBy { it[PaymentsTable.method] }.mapValues { (_,rows)->rows.sumOf { it[PaymentsTable.amount] } }
        val transfers=HospitalityBillHandoversTable.select {
            (HospitalityBillHandoversTable.businessId eq business) and (HospitalityBillHandoversTable.status eq "ACCEPTED") and
                (HospitalityBillHandoversTable.decidedAt greaterEq start) and (HospitalityBillHandoversTable.decidedAt less end)
        }.toList()
        return StaffShiftSummary(
            completedOrderCount=completed.size,completedOrderTotal=completed.sumOf { it[OrdersTable.subtotal] },
            servedOrderCount=completed.count { it[OrdersTable.serverUserId]==user },settledOrderCount=completed.count { it[OrdersTable.settledByUserId]==user },
            cashTotal=totals["CASH"]?:0.0,mpesaTotal=totals["MPESA"]?:0.0,cardTotal=totals["CARD"]?:0.0,
            otherTotal=totals.filterKeys { it !in setOf("CASH","MPESA","CARD") }.values.sum(),collectedTotal=payments.sumOf { it[PaymentsTable.amount] },
            handedOverCount=transfers.count { it[HospitalityBillHandoversTable.fromUserId]==user },receivedBillCount=transfers.count { it[HospitalityBillHandoversTable.toUserId]==user },
            completedOrders=completed.map { order -> StaffCompletedOrder(order[OrdersTable.id],order[OrdersTable.orderNumber],order[OrdersTable.subtotal],order[OrdersTable.completedAt]!!.toString(),listOfNotNull(if(order[OrdersTable.serverUserId]==user)"Served" else null,if(order[OrdersTable.responsibleUserId]==user)"Responsible" else null,if(order[OrdersTable.settledByUserId]==user)"Settled" else null).joinToString(", ")) }
        )
    }
    private fun shiftResponse(r:ResultRow):StaffShiftResponse {
        val summary=r[HospitalityStaffShiftsTable.summaryJson]?.let { json.decodeFromString<StaffShiftSummary>(it) } ?: calculateSummary(r,r[HospitalityStaffShiftsTable.closedAt]?:Clock.System.now())
        val name=UsersTable.select { (UsersTable.id eq r[HospitalityStaffShiftsTable.userId]) and (UsersTable.businessId eq r[HospitalityStaffShiftsTable.businessId]) }.firstOrNull()?.get(UsersTable.name) ?: "Former staff"
        return StaffShiftResponse(r[HospitalityStaffShiftsTable.id],r[HospitalityStaffShiftsTable.userId],r[HospitalityStaffShiftsTable.openedAt].toString(),r[HospitalityStaffShiftsTable.closedAt]?.toString(),r[HospitalityStaffShiftsTable.status],r[HospitalityStaffShiftsTable.notes],name,summary)
    }
    private fun handoverResponse(r:ResultRow):BillHandoverResponse {
        fun name(id:String)=UsersTable.select { (UsersTable.id eq id) and (UsersTable.businessId eq r[HospitalityBillHandoversTable.businessId]) }.firstOrNull()?.get(UsersTable.name) ?: "Former staff"
        val order=OrdersTable.select { OrdersTable.id eq r[HospitalityBillHandoversTable.orderId] }.first()
        return BillHandoverResponse(r[HospitalityBillHandoversTable.id],r[HospitalityBillHandoversTable.orderId],order[OrdersTable.orderNumber],r[HospitalityBillHandoversTable.fromUserId],name(r[HospitalityBillHandoversTable.fromUserId]),r[HospitalityBillHandoversTable.toUserId],name(r[HospitalityBillHandoversTable.toUserId]),r[HospitalityBillHandoversTable.requestedBy],r[HospitalityBillHandoversTable.status],r[HospitalityBillHandoversTable.notes],r[HospitalityBillHandoversTable.balanceAtRequest],r[HospitalityBillHandoversTable.requestedAt].toString(),r[HospitalityBillHandoversTable.decidedAt]?.toString())
    }
    private fun audit(b:String,u:String,action:String,type:String,id:String,details:String="") {
        AuditEventsTable.insert { it[AuditEventsTable.id]=generateId(); it[businessId]=b; it[userId]=u; it[AuditEventsTable.action]=action; it[entityType]=type; it[entityId]=id; it[AuditEventsTable.details]=details; it[occurredAt]=Clock.System.now() }
    }
}

internal fun requireStaffOnDuty(businessId:String,userId:String?) {
    if(userId==null) return
    val shifts=HospitalityStaffShiftsTable.select { (HospitalityStaffShiftsTable.businessId eq businessId) and (HospitalityStaffShiftsTable.userId eq userId) }.toList()
    // Existing installations continue operating until staff first adopt personal shifts.
    require(shifts.isEmpty() || shifts.any { it[HospitalityStaffShiftsTable.status]=="OPEN" }) { "Your staff shift has ended. Start another staff shift before taking or settling bills" }
}

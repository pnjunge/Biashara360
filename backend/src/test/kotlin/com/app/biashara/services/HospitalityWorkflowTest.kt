package com.app.biashara.services

import com.app.biashara.db.*
import com.app.biashara.models.*
import kotlinx.datetime.Clock
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.test.*
import com.app.biashara.routes.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

class HospitalityWorkflowTest {
 private val orders=OrderService()
 private val service=HospitalityService(orders)
 private val ops=AdvancedHospitalityService(orders)
 private lateinit var ingredient:String
 @AfterTest fun cleanup() {org.koin.core.context.stopKoin()}
 @BeforeTest fun setup() {
  org.koin.core.context.stopKoin()
  val postgres=System.getenv("HOTEL_TEST_POSTGRES_URL")
  require(postgres==null || (postgres.startsWith("jdbc:postgresql://127.0.0.1:")&&postgres.endsWith("/hotel_test")))
  val database=if(postgres!=null)Database.connect(postgres,driver="org.postgresql.Driver",user="postgres",password="hotel-test-only") else Database.connect("jdbc:h2:mem:hosp-${UUID.randomUUID()};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",driver="org.h2.Driver")
  org.jetbrains.exposed.sql.transactions.TransactionManager.defaultDatabase=database
  transaction(database) {
   if(postgres!=null)exec("TRUNCATE TABLE businesses CASCADE")
   SchemaUtils.create(IngredientPurchasePaymentsTable,ExpensesTable,PurchaseInvoicesTable,HotelRoomTypes,HotelRooms,HotelReservations,HotelFolioEntries,HospitalityStaffShiftsTable,HospitalityBillHandoversTable,BusinessesTable,BranchesTable,UsersTable,ProductsTable,CustomersTable,OrdersTable,OrderItemsTable,StockMovementsTable,PaymentsTable,HospitalityTablesTable,KitchenTicketsTable,HospitalityReservationsTable,HospitalityMenuProfilesTable,InventoryIngredientsTable,ProductRecipesTable,BarStockEventsTable,HospitalityShiftsTable,SuppliersTable,PurchaseOrdersTable,PurchaseOrderItemsTable,ManagerApprovalsTable,AuditEventsTable,OrderSplitPaymentsTable,AccessRolesTable,UserAccessRolesTable,AccessGroupsTable,AccessGroupRolesTable,UserAccessGroupsTable,PermissionsTable,RolePermissionsTable)
   for(b in listOf("business","other"))BusinessesTable.insert{it[id]=b;it[name]=b;it[storefrontSlug]=b;it[type]="HOSPITALITY";it[hospitalityEnabled]=true;it[ownerEmail]="$b@test.com";it[ownerPhone]="254700000001";it[createdAt]=Clock.System.now();it[updatedAt]=Clock.System.now()}
   for(u in listOf("staff","manager"))UsersTable.insert{it[id]=u;it[businessId]="business";it[name]=u;it[email]="$u@test.com";it[phone]=if(u=="staff")"254700000002" else "254700000003";it[passwordHash]="unused";it[role]="STAFF";it[createdAt]=Clock.System.now();it[updatedAt]=Clock.System.now()}
   for(p in listOf("meal","drink"))ProductsTable.insert{it[id]=p;it[businessId]="business";it[sku]=p;it[name]=p;it[buyingPrice]=10.0;it[sellingPrice]=100.0;it[currentStock]=100;it[category]=if(p=="meal")"Meals" else "Drinks";it[createdAt]=Clock.System.now();it[updatedAt]=Clock.System.now()}
  }
  ops.openShift("business","manager",ShiftOpenRequest())
  ingredient=ops.createIngredient("business","staff",IngredientRequest("Flour","G",10.0,unitCost=2.0)).id
 }
 private fun tab(vararg products:String)=service.createOrder("business","staff",HospitalityOrderRequest(serviceType="TAKEAWAY",items=products.map{OrderItemRequest(productId=it,quantity=1,unitPrice=100.0)})).data!!
 @Test fun `ingredient demand is combined across the whole order`() {
  for(p in listOf("meal","drink"))ops.saveRecipe("business","manager",p,SaveRecipeRequest(listOf(RecipeLine(ingredient,6.0))))
  val r=service.createOrder("business","staff",HospitalityOrderRequest(serviceType="TAKEAWAY",items=listOf("meal","drink").map{OrderItemRequest(it,1,100.0)}))
  assertFalse(r.success)
  assertEquals(10.0,ops.dashboard("business").ingredients.single().quantity)
 }
 @Test fun `routing sends only food to kitchen and supports no preparation profiles`() {
  ops.saveMenuProfile("business","manager","meal",MenuProfileRequest(preparationStation="KITCHEN"))
  ops.saveMenuProfile("business","manager","drink",MenuProfileRequest(preparationStation=null))
  assertFails { ops.saveMenuProfile("business","manager","drink",MenuProfileRequest(preparationStation="KITCHEN")) }
  tab("meal","drink")
  val tickets=service.dashboard("business").tickets
  assertEquals(listOf("KITCHEN"),tickets.map{it.station});assertEquals("meal",tickets.single().items.single().productId)
 }
 @Test fun `cancel closes tab cancels tickets and prevents settlement`() {
  val order=tab("meal")
  assertTrue(orders.cancel(order.id,"business").success)
  assertTrue(service.dashboard("business").openTabs.isEmpty())
  assertEquals("CANCELLED",service.dashboard("business").tickets.single().status)
  assertFails{service.closeTab("business",order.id,CloseHospitalityTabRequest("CASH"))}
 }
 @Test fun `approval applies discount once rejects self decision and foreign targets`() {
  val order=tab("meal")
  assertFails{ops.requestApproval("other","staff",ApprovalRequest("DISCOUNT_ORDER","ORDER",order.id,"discount",amount=20.0))}
  val approval=ops.requestApproval("business","staff",ApprovalRequest("DISCOUNT_ORDER","ORDER",order.id,"Goodwill",amount=20.0))
  assertFails{ops.decideApproval("business","staff",approval.id,true)}
  ops.decideApproval("business","manager",approval.id,true)
  assertEquals(80.0,orders.getById(order.id,"business")!!.subtotal)
  assertFails{ops.decideApproval("business","manager",approval.id,true)}
  assertEquals(80.0,orders.getById(order.id,"business")!!.subtotal)
 }
 @Test fun `invalid stock action rolls back approval decision`() {
  val request=ops.requestApproval("business","staff",ApprovalRequest("STOCK_EVENT","INGREDIENT",ingredient,"Waste",quantity=11.0,eventType="WASTAGE"))
  assertFails{ops.decideApproval("business","manager",request.id,true)}
  assertEquals("PENDING",ops.dashboard("business").approvals.single().status)
  assertEquals(10.0,ops.dashboard("business").ingredients.single().quantity)
 }
 @Test fun `opening a bottle does not deduct contents and excessive wastage fails`() {
  ops.recordBarEvent("business","manager",BarStockEventRequest(ingredientId=ingredient,eventType="OPEN_BOTTLE",quantity=1.0,unit="G"))
  assertEquals(10.0,ops.dashboard("business").ingredients.single().quantity)
  assertFails{ops.recordBarEvent("business","manager",BarStockEventRequest(ingredientId=ingredient,eventType="WASTAGE",quantity=11.0,unit="G"))}
 }
 @Test fun `purchase receipt is not applied twice`() {
  val supplier=ops.createSupplier("business","manager",SupplierRequest("Supplier"))
  val po=ops.createPurchaseOrder("business","manager",PurchaseOrderRequest(supplier.id,items=listOf(PurchaseOrderLineRequest(ingredient,3.0,2.0))))
  val request=ops.requestApproval("business","staff",ApprovalRequest("RECEIVE_PURCHASE_ORDER","PURCHASE_ORDER",po.id,"Delivered"))
  ops.decideApproval("business","manager",request.id,true)
  assertFails{ops.receivePurchaseOrder("business","manager",po.id)}
  assertEquals(13.0,ops.dashboard("business").ingredients.single().quantity)
 }
 @Test fun `financial reports exclude unpaid cancelled and hotel orders and include modifiers`() {
  val unpaid=tab("meal")
  val paid=service.createOrder("business","staff",HospitalityOrderRequest(serviceType="TAKEAWAY",items=listOf(OrderItemRequest("drink",1,100.0,modifiers=listOf(MenuOption("Large",20.0)))))).data!!
  service.closeTab("business",paid.id,CloseHospitalityTabRequest("CASH"))
  val hotel=tab("meal");transaction{OrdersTable.update({OrdersTable.id eq hotel.id}){it[serviceType]="HOTEL";it[paymentStatus]="PAID"}}
  val report=ops.report("business","2000-01-01","2100-01-01")
  assertEquals(120.0,report.byItem.sumOf{it.amount});assertEquals(1,report.byWaiter.sumOf{it.count})
  assertNotNull(unpaid)
 }
 @Test fun `concurrent tabs never overspend ingredients`() {
  ops.saveRecipe("business","manager","meal",SaveRecipeRequest(listOf(RecipeLine(ingredient,6.0))))
  val executor=java.util.concurrent.Executors.newFixedThreadPool(2)
  val start=java.util.concurrent.CountDownLatch(1)
  try {
   val attempts=(1..2).map{executor.submit<Boolean>{start.await();service.createOrder("business","staff",HospitalityOrderRequest(serviceType="TAKEAWAY",items=listOf(OrderItemRequest("meal",1,100.0)))).success}}
   start.countDown();assertEquals(1,attempts.count{it.get(15,java.util.concurrent.TimeUnit.SECONDS)})
   assertEquals(4.0,ops.dashboard("business").ingredients.single().quantity)
  } finally {executor.shutdownNow()}
 }
 @Test fun `complimentary approval produces a zero tab that closes without a fake payment`() {
  val order=tab("meal")
  val request=ops.requestApproval("business","staff",ApprovalRequest("COMPLIMENTARY_ORDER","ORDER",order.id,"Service recovery"))
  ops.decideApproval("business","manager",request.id,true)
  assertEquals(0.0,orders.getById(order.id,"business")!!.subtotal)
  service.closeTab("business",order.id,CloseHospitalityTabRequest("CASH"))
  assertTrue(service.dashboard("business").openTabs.isEmpty())
  assertEquals(0,transaction{PaymentsTable.select{PaymentsTable.orderId eq order.id}.count()}.toInt())
 }
 @Test fun `hospitality permissions separate viewers from management and permit authorized managers`() = testApplication {
  environment {config=io.ktor.server.config.MapApplicationConfig()}
  transaction {
   for(code in listOf("hospitality.view","hospitality.approvals","hospitality.kitchen","hospitality.purchasing","hospitality.purchase_payments"))if(!PermissionsTable.select{PermissionsTable.code eq code}.any())PermissionsTable.insert {it[id]=UUID.randomUUID().toString();it[PermissionsTable.code]=code;it[module]="HOSPITALITY";it[action]="MANAGE";it[name]=code;it[createdAt]=Clock.System.now()}
  }
  val access=AccessControlService()
  val role=access.createRole("business",SaveAccessRoleRequest("Viewer",allowedMenus=listOf("HOSPITALITY"),permissions=listOf("hospitality.view")))
  val managerRole=access.createRole("business",SaveAccessRoleRequest("Approval manager",allowedMenus=listOf("HOSPITALITY_OPS"),permissions=listOf("hospitality.view","hospitality.approvals","hospitality.kitchen","hospitality.purchasing","hospitality.purchase_payments")))
  transaction{UserAccessRolesTable.insert{it[userId]="staff";it[roleId]=role.id};UserAccessRolesTable.insert{it[userId]="manager";it[roleId]=managerRole.id}}
  val algorithm=Algorithm.HMAC256("hospitality-test-secret")
  fun token(user:String)=JWT.create().withSubject(user).withClaim("businessId","business").withClaim("role","STAFF").sign(algorithm)
  application {
   install(Koin){modules(module{single{service};single{ops};single{access}})}
   install(ContentNegotiation){json()}
   install(Authentication){jwt{verifier(JWT.require(algorithm).build());validate{JWTPrincipal(it.payload)}}}
   routing{authenticate{hospitalityRoutes()}}
  }
  assertEquals(HttpStatusCode.OK,client.get("/hospitality"){bearerAuth(token("staff"))}.status)
  assertEquals(HttpStatusCode.BadRequest,client.post("/hospitality/duty/start"){bearerAuth(token("staff"));contentType(ContentType.Application.Json);setBody("{}")}.status)
  for(path in listOf("/hospitality/operations/menu/meal","/hospitality/operations/shifts/open","/hospitality/operations/ingredients","/hospitality/operations/purchase-orders","/hospitality/operations/purchase-orders/missing/payments")) {
   val response=if(path.contains("/menu/"))client.put(path){bearerAuth(token("staff"));contentType(ContentType.Application.Json);setBody("{}")}else client.post(path){bearerAuth(token("staff"));contentType(ContentType.Application.Json);setBody("{}")}
   assertEquals(HttpStatusCode.Forbidden,response.status)
  }
  assertEquals(HttpStatusCode.Forbidden,client.get("/hospitality/operations/report?startDate=2026-01-01&endDate=2026-01-02"){bearerAuth(token("staff"))}.status)
  val order=tab("meal");val request=ops.requestApproval("business","staff",ApprovalRequest("DISCOUNT_ORDER","ORDER",order.id,"Goodwill",amount=10.0))
  assertEquals(HttpStatusCode.Forbidden,client.post("/hospitality/operations/approvals/${request.id}/decision"){bearerAuth(token("staff"));contentType(ContentType.Application.Json);setBody("{\"approved\":true}")}.status)
  assertEquals(HttpStatusCode.OK,client.post("/hospitality/operations/approvals/${request.id}/decision"){bearerAuth(token("manager"));contentType(ContentType.Application.Json);setBody("{\"approved\":true}")}.status)
  assertEquals(90.0,orders.getById(order.id,"business")!!.subtotal)
  val ticket=service.dashboard("business").tickets.single()
  assertEquals(HttpStatusCode.Forbidden,client.patch("/hospitality/tickets/${ticket.id}"){bearerAuth(token("staff"));contentType(ContentType.Application.Json);setBody("{\"status\":\"PREPARING\"}")}.status)
  val cashier=access.createRole("business",SaveAccessRoleRequest("Cashier",allowedMenus=listOf("HOSPITALITY","OPEN_TABS"),permissions=listOf("hospitality.view","hospitality.kitchen")))
  transaction{UserAccessRolesTable.insert{it[userId]="staff";it[roleId]=cashier.id}}
  assertEquals(HttpStatusCode.OK,client.patch("/hospitality/tickets/${ticket.id}"){bearerAuth(token("staff"));contentType(ContentType.Application.Json);setBody("{\"status\":\"PREPARING\"}")}.status)
  assertEquals("PREPARING",service.dashboard("business").tickets.single().status)
  val (_,purchase)=goatPurchase(1.0,100.0)
  val receiverRole=access.createRole("business",SaveAccessRoleRequest("Stock receiver",allowedMenus=listOf("HOSPITALITY_OPS"),permissions=listOf("hospitality.view","hospitality.purchasing")))
  transaction{UserAccessRolesTable.insert{it[userId]="staff";it[roleId]=receiverRole.id}}
  val purchaserDashboard=client.get("/hospitality/operations"){bearerAuth(token("staff"))}.bodyAsText()
  assertTrue(purchaserDashboard.contains("\"tradingDayOpen\":true"));assertTrue(purchaserDashboard.contains("\"shifts\":[]"))
  val receivePath="/hospitality/operations/purchase-orders/${purchase.id}/receive"
  assertEquals(HttpStatusCode.Forbidden,client.post(receivePath){bearerAuth(token("staff"));contentType(ContentType.Application.Json);setBody("{\"payment\":{\"amount\":100,\"method\":\"CASH\",\"clientReference\":\"receiver-payment\"}}")}.status)
  assertEquals("ORDERED",ops.dashboard("business").purchaseOrders.first{it.id==purchase.id}.status)
  assertEquals(HttpStatusCode.OK,client.post(receivePath){bearerAuth(token("staff"));contentType(ContentType.Application.Json);setBody("{}")}.status)
  assertEquals(HttpStatusCode.Forbidden,client.post("/hospitality/operations/purchase-orders/${purchase.id}/payments"){bearerAuth(token("staff"));contentType(ContentType.Application.Json);setBody("{}")}.status)
  val payerRole=access.createRole("business",SaveAccessRoleRequest("Supplier accountant",allowedMenus=listOf("HOSPITALITY_OPS"),permissions=listOf("hospitality.view","hospitality.purchasing","hospitality.purchase_payments")))
  transaction{UserAccessRolesTable.insert{it[userId]="manager";it[roleId]=payerRole.id}}
  assertEquals(HttpStatusCode.OK,client.post("/hospitality/operations/purchase-orders/${purchase.id}/payments"){bearerAuth(token("manager"));contentType(ContentType.Application.Json);setBody("{\"amount\":100,\"method\":\"CASH\",\"clientReference\":\"accountant-payment\"}")}.status)


 }

 @Test fun `opening tab after shift closure returns actionable validation and retry creates one order`() = testApplication {
  authorizeDuty();val duty=HospitalityDutyService()
  duty.startShift("business","staff",StaffShiftRequest());duty.endShift("business","staff",StaffShiftRequest())
  environment {config=io.ktor.server.config.MapApplicationConfig()}
  val algorithm=Algorithm.HMAC256("hospitality-tab-test-secret")
  fun token(user:String)=JWT.create().withSubject(user).withClaim("businessId","business").withClaim("role","ADMIN").sign(algorithm)
  application {
   install(Koin){modules(module{single{service};single{ops};single{AccessControlService()}})}
   install(ContentNegotiation){json()}
   install(Authentication){jwt{verifier(JWT.require(algorithm).build());validate{JWTPrincipal(it.payload)}}}
   routing{authenticate{hospitalityRoutes()}}
  }
  val body="""{"serviceType":"TAKEAWAY","items":[{"productId":"meal","quantity":1,"unitPrice":100}]}"""
  val ended=client.post("/hospitality/orders"){bearerAuth(token("staff"));contentType(ContentType.Application.Json);setBody(body)}
  assertEquals(HttpStatusCode.BadRequest,ended.status);assertTrue(ended.bodyAsText().contains("Your staff shift has ended"))
  assertEquals(0,transaction{OrdersTable.selectAll().count()}.toInt())
  val day=ops.dashboard("business").shifts.single()
  ops.closeShift("business","manager",day.id,ShiftCloseRequest(actualCash=0.0,actualMpesa=0.0,actualCard=0.0))
  val closed=client.post("/hospitality/orders"){bearerAuth(token("manager"));contentType(ContentType.Application.Json);setBody(body)}
  assertEquals(HttpStatusCode.BadRequest,closed.status);assertTrue(closed.bodyAsText().contains("opening a shift"))
  assertEquals(0,transaction{OrdersTable.selectAll().count()}.toInt())
  ops.openShift("business","manager",ShiftOpenRequest());duty.startShift("business","staff",StaffShiftRequest())
  val opened=client.post("/hospitality/orders"){bearerAuth(token("staff"));contentType(ContentType.Application.Json);setBody(body)}
  assertEquals(HttpStatusCode.Created,opened.status);assertTrue(opened.bodyAsText().contains("orderNumber"))
  assertEquals(1,transaction{OrdersTable.selectAll().count()}.toInt());assertEquals(1,service.dashboard("business").tickets.size)
 }

 @Test fun `tables start spaced and merging then unmerging releases the source`() {
  val first=service.createTable("business",CreateHospitalityTableRequest("A"))
  val second=service.createTable("business",CreateHospitalityTableRequest("B"))
  assertEquals(2,service.dashboard("business").tables.map{it.positionX to it.positionY}.distinct().size)
  service.createOrder("business","staff",HospitalityOrderRequest(tableId=first.id,items=listOf(OrderItemRequest("meal",1,100.0))))
  ops.updateTableOperations("business","manager",first.id,TableOperationsRequest(mergeIntoTableId=second.id))
  assertEquals("AVAILABLE",service.dashboard("business").tables.first{it.id==first.id}.status)
  assertEquals("OCCUPIED",service.dashboard("business").tables.first{it.id==second.id}.status)
  ops.updateTableOperations("business","manager",first.id,TableOperationsRequest())
  assertNull(service.dashboard("business").tables.first{it.id==first.id}.mergedIntoTableId)
 }

 @Test fun `cash split settles exactly once and releases the table`() {
  val order=tab("meal")
  val payments=SplitBillRequest(listOf(SplitPaymentLine(40.0,"CASH"),SplitPaymentLine(60.0,"CASH")))
  ops.splitBill("business","manager",order.id,payments)
  assertEquals("PAID",orders.getById(order.id,"business")!!.paymentStatus)
  assertEquals("SPLIT",orders.getById(order.id,"business")!!.paymentMethod)
  assertFails{ops.splitBill("business","manager",order.id,payments)}
  assertEquals(100.0,transaction{PaymentsTable.select{PaymentsTable.orderId eq order.id}.sumOf{it[PaymentsTable.amount]}})
 }
 private fun authorizeDuty() { transaction { UsersTable.update({ UsersTable.businessId eq "business" }) { it[role]="ADMIN" } } }
 private fun stock(product:String)=transaction { ProductsTable.select { ProductsTable.id eq product }.first()[ProductsTable.currentStock] }

 @Test fun `recipe portions sell with zero product stock and cancellation never invents portion stock`() {
  transaction { ProductsTable.update({ProductsTable.id eq "meal"}) {it[currentStock]=0} }
  ops.saveRecipe("business","manager","meal",SaveRecipeRequest(listOf(RecipeLine(ingredient,2.0))))
  val before=ProductService().getById("meal","business")!!
  assertEquals("INGREDIENTS",before.stockMode); assertEquals(5,before.currentStock)
  val order=tab("meal")
  assertEquals(0,stock("meal")); assertEquals(8.0,ops.dashboard("business").ingredients.single().quantity)
  assertEquals(4.0,order.items.single().buyingPrice)
  assertTrue(orders.cancel(order.id,"business").success)
  assertEquals(0,stock("meal")); assertEquals(8.0,ops.dashboard("business").ingredients.single().quantity)
  assertFalse(ProductService().updateStock("meal","business",StockUpdateRequest("STOCK_IN",5)).success)
 }
 @Test fun `POS recipes share bulk stock and retries deduct once`() {
  ops.saveRecipe("business","manager","meal",SaveRecipeRequest(listOf(RecipeLine(ingredient,3.0))))
  val request=CreateOrderRequest(customerName="Guest",items=listOf(OrderItemRequest("meal",2,100.0)),paymentMethod="CASH",clientReference="portion-retry")
  val first=orders.create("business",request,"WEB")
  assertTrue(first.success);assertEquals(first.data!!.id,orders.create("business",request,"WEB").data!!.id)
  assertEquals(4.0,ops.dashboard("business").ingredients.single().quantity);assertEquals(100,stock("meal"))
  assertFalse(orders.create("business",request.copy(clientReference="another"),"WEB").success)
 }
 @Test fun `availability uses the limiting ingredient and empty recipe returns to product stock`() {
  val oil=ops.createIngredient("business","manager",IngredientRequest("Oil","ML",3.0)).id
  ops.saveRecipe("business","manager","meal",SaveRecipeRequest(listOf(RecipeLine(ingredient,2.0),RecipeLine(oil,1.0))))
  assertEquals(3,ProductService().getById("meal","business")!!.currentStock)
  ops.saveRecipe("business","manager","meal",SaveRecipeRequest(emptyList()))
  assertEquals("PRODUCT",ProductService().getById("meal","business")!!.stockMode)
  assertEquals(100,ProductService().getById("meal","business")!!.currentStock)
 }
 @Test fun `bulk receipts convert kilograms and configured bottles with correct unit costs`() {
  val spirit=ops.createIngredient("business","manager",IngredientRequest("Spirit","ML",purchaseUnit="BOTTLE",purchaseUnitSize=750.0)).id
  val supplier=ops.createSupplier("business","manager",SupplierRequest("Bulk supplier")).id
  val po=ops.createPurchaseOrder("business","manager",PurchaseOrderRequest(supplier,items=listOf(PurchaseOrderLineRequest(ingredient,2.0,1000.0,"KG"),PurchaseOrderLineRequest(spirit,2.0,1500.0,"BOTTLE"))))
  assertEquals(5000.0,po.totalCost);assertEquals(2000.0,po.items.first{it.ingredientId==ingredient}.stockQuantity)
  assertEquals(2.0,po.items.first{it.ingredientId==spirit}.stockUnitCost)
  // Changing configuration after ordering must not change the receipt conversion.
  ops.configurePurchaseUnit("business","manager",spirit,IngredientPurchaseUnitRequest("BOTTLE",1000.0))
  ops.receivePurchaseOrder("business","manager",po.id)
  val rows=ops.dashboard("business").ingredients
  assertEquals(2010.0,rows.first{it.id==ingredient}.quantity);assertEquals(2020.0/2010.0,rows.first{it.id==ingredient}.unitCost)
  assertEquals(1500.0,rows.first{it.id==spirit}.quantity);assertEquals(2.0,rows.first{it.id==spirit}.unitCost)
  assertFails{ops.receivePurchaseOrder("business","manager",po.id)}
 }
 @Test fun `incompatible purchase units and foreign ingredients fail before creating purchases`() {
  val supplier=ops.createSupplier("business","manager",SupplierRequest("Bulk supplier")).id
  assertFails { ops.createPurchaseOrder("business","manager",PurchaseOrderRequest(supplier,items=listOf(PurchaseOrderLineRequest(ingredient,1.0,10.0,"L")))) }
  assertFails { ops.createPurchaseOrder("business","manager",PurchaseOrderRequest(supplier,items=listOf(PurchaseOrderLineRequest(ingredient,1.0,10.0,"BOTTLE")))) }
  assertFails { ops.createPurchaseOrder("other","manager",PurchaseOrderRequest(supplier,items=listOf(PurchaseOrderLineRequest(ingredient,1.0,10.0,"G")))) }
  assertTrue(ops.dashboard("business").purchaseOrders.isEmpty())
  assertFails { ops.configurePurchaseUnit("business","manager",ingredient,IngredientPurchaseUnitRequest("PACK",0.0)) }
 }
 @Test fun `concurrent POS portions never overspend shared bulk stock`() {
  ops.saveRecipe("business","manager","meal",SaveRecipeRequest(listOf(RecipeLine(ingredient,6.0))))
  val executor=java.util.concurrent.Executors.newFixedThreadPool(2);val start=java.util.concurrent.CountDownLatch(1)
  try {
   val attempts=(1..2).map{executor.submit<Boolean>{start.await();orders.create("business",CreateOrderRequest(customerName="Guest",items=listOf(OrderItemRequest("meal",1,100.0)),paymentMethod="CASH")).success}}
   start.countDown();assertEquals(1,attempts.count{it.get(15,java.util.concurrent.TimeUnit.SECONDS)})
   assertEquals(4.0,ops.dashboard("business").ingredients.single().quantity);assertEquals(100,stock("meal"))
  } finally {executor.shutdownNow()}
 }
 @Test fun `failed mixed stock order does not consume ingredients or persist an order`() {
  ops.saveRecipe("business","manager","meal",SaveRecipeRequest(listOf(RecipeLine(ingredient,2.0))))
  transaction {ProductsTable.update({ProductsTable.id eq "drink"}){it[currentStock]=0}}
  assertFalse(service.createOrder("business","staff",HospitalityOrderRequest(serviceType="TAKEAWAY",items=listOf(OrderItemRequest("meal",1,100.0),OrderItemRequest("drink",1,100.0)))).success)
  assertEquals(10.0,ops.dashboard("business").ingredients.single().quantity)
  assertTrue(service.dashboard("business").openTabs.isEmpty())
 }
 @Test fun `handover requires receiver acceptance before sender can end shift`() {
  authorizeDuty();val duty=HospitalityDutyService()
  duty.startShift("business","staff",StaffShiftRequest());duty.startShift("business","manager",StaffShiftRequest())
  val first=tab("meal");val second=tab("drink")
  val handovers=duty.requestHandover("business","staff",BillHandoverRequest(listOf(first.id,second.id),"manager","Table still dining"))
  assertEquals("staff",orders.getById(first.id,"business")!!.responsibleUserId)
  assertFails{duty.endShift("business","staff",StaffShiftRequest())}
  assertFails{duty.decide("business","staff",handovers.first().id,BillHandoverDecisionRequest("ACCEPT"))}
  handovers.forEach { duty.decide("business","manager",it.id,BillHandoverDecisionRequest("ACCEPT")) }
  assertEquals("CLOSED",duty.endShift("business","staff",StaffShiftRequest()).status)
  val received=orders.getById(first.id,"business")!!
  assertEquals("manager",received.responsibleUserId);assertEquals("staff",received.serverUserId)
  assertEquals(first.items,received.items);assertEquals(first.subtotal,received.subtotal)
  assertTrue(service.hasOpenShift("business"));assertEquals(2,service.dashboard("business").openTabs.size)
  assertFails{duty.decide("business","manager",handovers.first().id,BillHandoverDecisionRequest("ACCEPT"))}
  assertFails{tab("meal")}
 }
 @Test fun `handover enforces tenant owner recipient duty and duplicate protection`() {
  authorizeDuty();val duty=HospitalityDutyService();val order=tab("meal")
  assertFails{duty.requestHandover("business","staff",BillHandoverRequest(listOf(order.id),"manager"))}
  duty.startShift("business","manager",StaffShiftRequest())
  assertFails{duty.requestHandover("business","manager",BillHandoverRequest(listOf(order.id),"staff"))}
  assertFails{duty.requestHandover("business","staff",BillHandoverRequest(listOf(order.id),"staff"))}
  assertFails{duty.requestHandover("other","staff",BillHandoverRequest(listOf(order.id),"manager"))}
  val request=duty.requestHandover("business","staff",BillHandoverRequest(listOf(order.id),"manager")).single()
  assertFails{duty.requestHandover("business","staff",BillHandoverRequest(listOf(order.id),"manager"))}
  assertFails{duty.decide("other","manager",request.id,BillHandoverDecisionRequest("ACCEPT"))}
  duty.decide("business","manager",request.id,BillHandoverDecisionRequest("REJECT"))
  assertEquals("staff",orders.getById(order.id,"business")!!.responsibleUserId)
  val retry=duty.requestHandover("business","staff",BillHandoverRequest(listOf(order.id),"manager")).single()
  duty.decide("business","staff",retry.id,BillHandoverDecisionRequest("CANCEL"))
  assertEquals("staff",orders.getById(order.id,"business")!!.responsibleUserId)
 }
 @Test fun `paid bill cannot be accepted and unassigned bills must be claimed`() {
  authorizeDuty();val duty=HospitalityDutyService();duty.startShift("business","manager",StaffShiftRequest())
  val order=tab("meal");val request=duty.requestHandover("business","staff",BillHandoverRequest(listOf(order.id),"manager")).single()
  service.closeTab("business",order.id,CloseHospitalityTabRequest("CASH"),"staff")
  assertFails{duty.decide("business","manager",request.id,BillHandoverDecisionRequest("ACCEPT"))}
  val unassigned=service.createOrder("business",null,HospitalityOrderRequest(serviceType="TAKEAWAY",items=listOf(OrderItemRequest("meal",1,100.0)))).data!!
  duty.claimBill("business","manager",unassigned.id)
  assertEquals("manager",orders.getById(unassigned.id,"business")!!.responsibleUserId)
  assertFails{duty.claimBill("business","manager",unassigned.id)}
 }
 @Test fun `handover preserves old payments and attributes remaining cash to the receiving cashier`() {
  authorizeDuty();val duty=HospitalityDutyService();duty.startShift("business","staff",StaffShiftRequest());duty.startShift("business","manager",StaffShiftRequest())
  val order=tab("meal")
  transaction { PaymentsTable.insert { it[id]="deposit";it[businessId]="business";it[orderId]=order.id;it[collectedByUserId]="staff";it[transactionCode]="deposit";it[amount]=20.0;it[payerPhone]="";it[payerName]="Guest";it[method]="CASH";it[status]="SUCCESS";it[channel]="HOSPITALITY_POS";it[reconciled]=true;it[transactionDate]=Clock.System.now() } }
  val request=duty.requestHandover("business","staff",BillHandoverRequest(listOf(order.id),"manager")).single()
  assertEquals(80.0,request.balanceAtRequest)
  duty.decide("business","manager",request.id,BillHandoverDecisionRequest("ACCEPT"))
  duty.endShift("business","staff",StaffShiftRequest())
  assertFails{service.closeTab("business",order.id,CloseHospitalityTabRequest("CASH"),"staff")}
  service.closeTab("business",order.id,CloseHospitalityTabRequest("CASH"),"manager")
  val payments=transaction { PaymentsTable.select { PaymentsTable.orderId eq order.id }.map { it[PaymentsTable.collectedByUserId] to it[PaymentsTable.amount] } }
  assertTrue("staff" to 20.0 in payments);assertTrue("manager" to 80.0 in payments)
  assertEquals(100.0,payments.sumOf{it.second})
 }
 @Test fun `business day close still blocks handed over unpaid bills`() {
  authorizeDuty();val duty=HospitalityDutyService();duty.startShift("business","manager",StaffShiftRequest())
  val order=tab("meal");val request=duty.requestHandover("business","staff",BillHandoverRequest(listOf(order.id),"manager")).single()
  duty.decide("business","manager",request.id,BillHandoverDecisionRequest("ACCEPT"))
  val shift=ops.dashboard("business").shifts.single()
  assertFails{ops.closeShift("business","manager",shift.id,ShiftCloseRequest(0.0,0.0,0.0))}
  assertEquals("OPEN",duty.dashboard("business","manager").shift!!.status)
  service.closeTab("business",order.id,CloseHospitalityTabRequest("CASH"),"manager")
  ops.closeShift("business","manager",shift.id,ShiftCloseRequest(100.0,0.0,0.0))
  assertNull(duty.dashboard("business","manager").shift)
 }
 @Test fun `staff shift tally counts only this users completed orders and freezes on close`() {
  authorizeDuty();val duty=HospitalityDutyService()
  val before=tab("meal");service.closeTab("business",before.id,CloseHospitalityTabRequest("CASH"),"staff")
  duty.startShift("business","staff",StaffShiftRequest());duty.startShift("business","manager",StaffShiftRequest())
  val completed=tab("meal");service.closeTab("business",completed.id,CloseHospitalityTabRequest("CASH"),"staff")
  val others=service.createOrder("business","manager",HospitalityOrderRequest(serviceType="TAKEAWAY",items=listOf(OrderItemRequest("drink",2,100.0)))).data!!
  service.closeTab("business",others.id,CloseHospitalityTabRequest("CASH"),"manager")
  val unpaid=tab("drink");val handover=duty.requestHandover("business","staff",BillHandoverRequest(listOf(unpaid.id),"manager")).single()
  duty.decide("business","manager",handover.id,BillHandoverDecisionRequest("ACCEPT"))
  val preview=duty.dashboard("business","staff").shift!!.summary
  assertEquals(1,preview.completedOrderCount);assertEquals(100.0,preview.completedOrderTotal);assertEquals(100.0,preview.cashTotal)
  assertEquals(listOf(completed.id),preview.completedOrders.map{it.id});assertEquals(1,preview.handedOverCount)
  val ended=duty.endShift("business","staff",StaffShiftRequest("Till handed over"))
  assertEquals(preview,ended.summary)
  service.closeTab("business",unpaid.id,CloseHospitalityTabRequest("CASH"),"manager")
  assertEquals(preview,duty.reports("business","staff").single().summary)
  assertEquals(300.0,duty.dashboard("business","manager").shift!!.summary.cashTotal)
  assertEquals(2,duty.reports("business","manager",true).size)
  assertEquals(listOf("staff"),duty.reports("business","staff",false).map{it.userId})
 }
 @Test fun `staff tally tracks cash mpesa card and excludes unpaid or cancelled orders`() {
  authorizeDuty();val duty=HospitalityDutyService();duty.startShift("business","staff",StaffShiftRequest())
  val cash=tab("meal");service.closeTab("business",cash.id,CloseHospitalityTabRequest("CASH"),"staff")
  for(method in listOf("MPESA","CARD")) {
   val order=tab("drink");service.closeTab("business",order.id,CloseHospitalityTabRequest(method),"staff")
   transaction {
    PaymentsTable.insert {it[id]=UUID.randomUUID().toString();it[businessId]="business";it[orderId]=order.id;it[collectedByUserId]="staff";it[transactionCode]=method;it[amount]=100.0;it[payerPhone]="";it[payerName]="Guest";it[PaymentsTable.method]=method;it[status]="SUCCESS";it[channel]="TEST";it[reconciled]=true;it[transactionDate]=Clock.System.now()}
    OrdersTable.update({OrdersTable.id eq order.id}) {it[paymentStatus]="PAID";it[tabStatus]="CLOSED";it[completedAt]=Clock.System.now()}
   }
  }
  val hotel=tab("meal")
  transaction {
   OrdersTable.update({OrdersTable.id eq hotel.id}) {it[serviceType]="HOTEL";it[paymentStatus]="PAID";it[tabStatus]="CLOSED";it[completedAt]=Clock.System.now()}
   PaymentsTable.insert {it[id]=UUID.randomUUID().toString();it[businessId]="business";it[orderId]=hotel.id;it[collectedByUserId]="staff";it[transactionCode]="hotel";it[amount]=500.0;it[payerPhone]="";it[payerName]="Guest";it[method]="CASH";it[status]="SUCCESS";it[channel]="TEST";it[reconciled]=true;it[transactionDate]=Clock.System.now()}
  }
  val original=transaction { OrdersTable.select{OrdersTable.id eq cash.id}.single()[OrdersTable.completedAt] }
  orders.updatePaymentStatus(cash.id,"business",UpdatePaymentStatusRequest(status="PAID"))
  assertEquals(original,transaction { OrdersTable.select{OrdersTable.id eq cash.id}.single()[OrdersTable.completedAt] })
  val cancelled=tab("meal");orders.cancel(cancelled.id,"business")
  val unpaid=tab("meal")
  val tally=duty.dashboard("business","staff").shift!!.summary
  assertEquals(3,tally.completedOrderCount);assertEquals(300.0,tally.completedOrderTotal)
  assertEquals(100.0,tally.cashTotal);assertEquals(100.0,tally.mpesaTotal);assertEquals(100.0,tally.cardTotal);assertEquals(300.0,tally.collectedTotal)
  assertFalse(tally.completedOrders.any{it.id==cancelled.id||it.id==unpaid.id})
  assertFails{duty.endShift("business","staff",StaffShiftRequest())}
 }
 @Test fun `food preparation is created once for POS and drinks never enter ticket`() {
  val req=CreateOrderRequest(customerName="Guest",customerPhone="",serviceType="TAKEAWAY",serverUserId="staff",items=listOf(OrderItemRequest("meal",1,100.0),OrderItemRequest("drink",1,100.0)),paymentMethod="CASH",clientReference="food-pos")
  assertTrue(orders.create("business",req,"WEB","staff").success)
  assertTrue(orders.create("business",req,"WEB","staff").success)
  val tickets=service.dashboard("business").tickets
  assertEquals(1,tickets.size);assertEquals(listOf("meal"),tickets.single().items.map{it.productId})
  ops.saveMenuProfile("business","manager","meal",MenuProfileRequest(preparationStation=null))
  assertEquals(listOf("meal"),service.dashboard("business").tickets.single().items.map{it.productId})
  assertTrue(service.dashboard("business").tickets.single().items.all{it.preparationStation=="KITCHEN"})
 }
 @Test fun `receiver acceptance races cannot transfer a bill twice`() {
  authorizeDuty();val duty=HospitalityDutyService();duty.startShift("business","manager",StaffShiftRequest())
  val order=tab("meal");val request=duty.requestHandover("business","staff",BillHandoverRequest(listOf(order.id),"manager")).single()
  val executor=java.util.concurrent.Executors.newFixedThreadPool(2);val start=java.util.concurrent.CountDownLatch(1)
  try {
   val attempts=(1..2).map{executor.submit<Boolean>{start.await();runCatching{duty.decide("business","manager",request.id,BillHandoverDecisionRequest("ACCEPT"))}.isSuccess}}
   start.countDown();assertEquals(1,attempts.count{it.get(15,java.util.concurrent.TimeUnit.SECONDS)})
   assertEquals("manager",orders.getById(order.id,"business")!!.responsibleUserId)
  } finally {executor.shutdownNow()}
 }

 @Test fun `cashier can browse checkout products but cannot mutate inventory or purchases`() = testApplication {
  environment {config=io.ktor.server.config.MapApplicationConfig()}
  transaction {
   SchemaUtils.create(InventoryCategoriesTable,PurchaseInvoicesTable)
   for(code in listOf("products.view","products.create","products.update","inventory.view","inventory.adjust","purchases.view","purchases.create","inventory.suppliers"))if(!PermissionsTable.select{PermissionsTable.code eq code}.any())PermissionsTable.insert {it[id]=UUID.randomUUID().toString();it[PermissionsTable.code]=code;it[module]="INVENTORY";it[action]="MANAGE";it[name]=code;it[createdAt]=Clock.System.now()}
  }
  val access=AccessControlService()
  // An inventory menu alone must never authorize mutations.
  val cashier=access.createRole("business",SaveAccessRoleRequest("Cashier",allowedMenus=listOf("POS","INVENTORY","PURCHASES"),permissions=listOf("products.view","inventory.view")))
  val keeper=access.createRole("business",SaveAccessRoleRequest("Storekeeper",allowedMenus=listOf("INVENTORY","PURCHASES"),permissions=listOf("products.view","products.create","products.update","inventory.adjust","purchases.view","purchases.create","inventory.suppliers")))
  transaction {UserAccessRolesTable.insert{it[userId]="staff";it[roleId]=cashier.id};UserAccessRolesTable.insert{it[userId]="manager";it[roleId]=keeper.id}}
  val algorithm=Algorithm.HMAC256("cashier-stock-test")
  fun token(user:String)=JWT.create().withSubject(user).withClaim("businessId","business").withClaim("role","STAFF").sign(algorithm)
  application {
   install(Koin){modules(module{single{access};single{ProductService()};single{InventoryCategoryService()};single{PurchaseInvoiceService()};single{SupplierService()}})}
   install(ContentNegotiation){json()}
   install(Authentication){jwt{verifier(JWT.require(algorithm).build());validate{JWTPrincipal(it.payload)}}}
   routing{authenticate{productRoutesValidated();purchaseRoutes();supplierRoutes()}}
  }
  assertEquals(HttpStatusCode.OK,client.get("/products"){bearerAuth(token("staff"))}.status)
  val before=transaction{ProductsTable.select{ProductsTable.id eq "meal"}.single()[ProductsTable.currentStock]}
  for(path in listOf("/products","/products/meal/stock","/purchases","/suppliers"))assertEquals(HttpStatusCode.Forbidden,client.post(path){bearerAuth(token("staff"));contentType(ContentType.Application.Json);setBody("{}")}.status,path)
  for(path in listOf("/products/meal","/products/meal/status","/suppliers/missing"))assertEquals(HttpStatusCode.Forbidden,client.put(path){bearerAuth(token("staff"));contentType(ContentType.Application.Json);setBody("{}")}.status,path)
  assertEquals(HttpStatusCode.Forbidden,client.get("/purchases"){bearerAuth(token("staff"))}.status)
  assertEquals(HttpStatusCode.Forbidden,client.delete("/suppliers/missing"){bearerAuth(token("staff"))}.status)
  assertEquals(before,transaction{ProductsTable.select{ProductsTable.id eq "meal"}.single()[ProductsTable.currentStock]})
  assertEquals(HttpStatusCode.OK,client.get("/purchases"){bearerAuth(token("manager"))}.status)
  assertEquals(HttpStatusCode.OK,client.get("/suppliers"){bearerAuth(token("manager"))}.status)
  assertEquals(HttpStatusCode.OK,client.post("/products/meal/stock"){bearerAuth(token("manager"));contentType(ContentType.Application.Json);setBody("{\"type\":\"STOCK_IN\",\"quantity\":2}")}.status)
  assertEquals(before+2,transaction{ProductsTable.select{ProductsTable.id eq "meal"}.single()[ProductsTable.currentStock]})
 }

 @Test fun `product measure saves and older updates preserve it`() {
  val catalog=ProductService()
  val request=ProductRequest(sku="bulk-unit",name="Bulk meat",buyingPrice=50.0,sellingPrice=100.0,currentStock=10,category="Meat",baseUnit=" kg ")
  val created=catalog.create("business",request).data!!
  assertEquals("KG",created.baseUnit)
  assertEquals("KG",catalog.getAll("business").first{it.id==created.id}.baseUnit)
  assertEquals("KG",catalog.update(created.id,"business",request.copy(baseUnit=null)).data!!.baseUnit)
  assertEquals("G",catalog.update(created.id,"business",request.copy(baseUnit="G")).data!!.baseUnit)
  assertFalse(catalog.update(created.id,"business",request.copy(baseUnit=" ")).success)
  assertEquals("G",catalog.getById(created.id,"business")!!.baseUnit)
  assertEquals("PCS",catalog.create("business",request.copy(sku="default-unit",baseUnit=null)).data!!.baseUnit)
 }

 private fun goatPurchase(quantity:Double=17.0,cost:Double=500.0):Pair<String,PurchaseOrderResponse> {
  val goat=ops.createIngredient("business","manager",IngredientRequest("Goat","G")).id
  val supplier=ops.createSupplier("business","manager",SupplierRequest("Goat supplier"))
  return goat to ops.createPurchaseOrder("business","manager",PurchaseOrderRequest(supplier.id,items=listOf(PurchaseOrderLineRequest(goat,quantity,cost,"KG"))))
 }
 @Test fun `goat receipt records expenditure while profit counts only sold portions`() {
  val (goat,po)=goatPurchase()
  val paid=ops.receivePurchaseOrder("business","manager",po.id,ReceiveIngredientPurchaseRequest(IngredientPurchasePaymentRequest(8500.0,"CASH",clientReference="goat-full-payment")))
  assertEquals(17000.0,ops.dashboard("business").ingredients.first{it.id==goat}.quantity)
  assertEquals("PAID",paid.paymentStatus);assertEquals(8500.0,paid.paidAmount);assertNotNull(paid.expenseId)
  assertEquals(0,transaction{PaymentsTable.selectAll().count()}.toInt())
  val expense=ExpenseService().getAll("business").single()
  assertEquals(po.id,expense.purchaseOrderId);assertEquals(8500.0,expense.amount);assertEquals(8500.0,expense.paidAmount);assertFalse(expense.affectsProfit);assertTrue(expense.linkedPurchase)
  assertFalse(ExpenseService().delete(expense.id,"business").success)
  assertFalse(ExpenseService().update(expense.id,"business",ExpenseRequest("RENT",1.0,"Changed",expense.expenseDate)).success)
  ops.saveRecipe("business","manager","meal",SaveRecipeRequest(listOf(RecipeLine(goat,1000.0))))
  transaction{ProductsTable.update({ProductsTable.id eq "meal"}){it[sellingPrice]=1000.0}}
  val sale=tab("meal");service.closeTab("business",sale.id,CloseHospitalityTabRequest("CASH"),"staff")
  val date=Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.of("Africa/Nairobi")).date.toString()
  ExpenseService().create("business",ExpenseRequest("RENT",100.0,"Rent",date))
  val summary=ExpenseService().getProfitSummary("business",date,date)
  assertEquals(1000.0,summary.totalRevenue);assertEquals(500.0,summary.totalCostOfGoods);assertEquals(100.0,summary.totalExpenses);assertEquals(400.0,summary.netProfit)
  val cache=object:com.app.biashara.cache.CacheStore {
   override suspend fun get(key:String):String?=null
   override suspend fun put(key:String,value:String,ttlSeconds:Long)=true
   override suspend fun delete(key:String)=true
  }
  assertEquals(400.0,kotlinx.coroutines.runBlocking { DashboardService(ProductService(),orders,cache).getDashboard("business").netProfitMonth })
  assertEquals(8500.0,summary.stockPurchases);assertEquals(8500.0,summary.stockPurchasePayments);assertEquals(8600.0,summary.cashflowOut)
  assertEquals(16000.0,ops.dashboard("business").ingredients.first{it.id==goat}.quantity)
  transaction{InventoryIngredientsTable.update({InventoryIngredientsTable.id eq goat}){it[unitCost]=0.9}}
  assertEquals(500.0,ops.report("business",date,date).foodCost)
 }
 @Test fun `unpaid receipt partial supplier payments and retries do not duplicate stock or costs`() {
  val (goat,po)=goatPurchase()
  val received=ops.receivePurchaseOrder("business","manager",po.id)
  assertEquals("UNPAID",received.paymentStatus);assertEquals(8500.0,received.outstandingAmount)
  val request=IngredientPurchasePaymentRequest(3000.0,"MPESA","TX-GOAT-1","goat-part-payment")
  val partial=ops.payPurchaseOrder("business","manager",po.id,request)
  assertEquals("PARTIAL",partial.paymentStatus);assertEquals(5500.0,partial.outstandingAmount)
  ops.payPurchaseOrder("business","manager",po.id,request)
  assertEquals(1,transaction{IngredientPurchasePaymentsTable.selectAll().count()}.toInt())
  assertFails{ops.payPurchaseOrder("business","manager",po.id,request.copy(amount=1.0))}
  assertFails{ops.payPurchaseOrder("business","manager",po.id,request.copy(clientReference="second-reference"))}
  assertFails{ops.payPurchaseOrder("business","manager",po.id,request.copy(amount=5500.01,reference="TX-GOAT-2",clientReference="new-payment-ref"))}
  assertFails{ops.payPurchaseOrder("other","manager",po.id,request)}
  assertFails{ops.receivePurchaseOrder("business","manager",po.id)}
  val paid=ops.payPurchaseOrder("business","manager",po.id,IngredientPurchasePaymentRequest(5500.0,"BANK_TRANSFER","BANK-GOAT-2","final-goat-payment"))
  assertEquals("PAID",paid.paymentStatus);assertEquals(2,paid.payments.size)
  assertEquals(1,transaction{ExpensesTable.selectAll().count()}.toInt());assertEquals(17000.0,ops.dashboard("business").ingredients.first{it.id==goat}.quantity)
 }
 @Test fun `invalid receipt payment rolls back receipt and accounting together`() {
  val (goat,po)=goatPurchase()
  assertFails{ops.receivePurchaseOrder("business","manager",po.id,ReceiveIngredientPurchaseRequest(IngredientPurchasePaymentRequest(9000.0,"CASH",clientReference="invalid-goat-pay")))}
  assertEquals(0.0,ops.dashboard("business").ingredients.first{it.id==goat}.quantity)
  assertEquals("ORDERED",ops.dashboard("business").purchaseOrders.single().status)
  assertEquals(0,transaction{ExpensesTable.selectAll().count()}.toInt());assertEquals(0,transaction{IngredientPurchasePaymentsTable.selectAll().count()}.toInt())
  assertFails{ops.payPurchaseOrder("business","manager",po.id,IngredientPurchasePaymentRequest(100.0,"CASH",clientReference="before-receipt"))}
 }
 @Test fun `cash drawer supplier payout reduces business closing cash once`() {
  val (_,po)=goatPurchase(1.0,100.0)
  ops.receivePurchaseOrder("business","manager",po.id,ReceiveIngredientPurchaseRequest(IngredientPurchasePaymentRequest(100.0,"CASH",clientReference="drawer-goat-pay",paidFromTill=true)))
  val sale=tab("meal");service.closeTab("business",sale.id,CloseHospitalityTabRequest("CASH"),"staff")
  val shift=ops.dashboard("business").shifts.single()
  val closed=ops.closeShift("business","manager",shift.id,ShiftCloseRequest(actualCash=0.0,actualMpesa=0.0,actualCard=0.0))
  assertEquals(0.0,closed.expectedCash);assertEquals(100.0,closed.expensesTotal);assertEquals(0.0,closed.variance)
  assertEquals(100.0,closed.supplierCashPayments)
  val expense=ExpenseService().getAll("business").single()
  assertEquals(100.0,expense.paidAmount);assertEquals(1,expense.payments.size);assertTrue(expense.payments.single().paidFromTill);assertEquals("CASH",expense.payments.single().method)
  assertEquals(0,ExpenseService().getAll("other-business").size)
 }

 @Test fun `drawer payment without open trading day fails without recording payment`() {
  val (_,po)=goatPurchase(1.0,100.0);ops.receivePurchaseOrder("business","manager",po.id)
  val day=ops.dashboard("business").shifts.single()
  ops.closeShift("business","manager",day.id,ShiftCloseRequest(actualCash=0.0,actualMpesa=0.0,actualCard=0.0))
  val error=assertFails { ops.payPurchaseOrder("business","manager",po.id,IngredientPurchasePaymentRequest(100.0,"CASH",clientReference="closed-drawer-pay",paidFromTill=true)) }
  assertTrue(error.message.orEmpty().contains("Open the business trading day"))
  assertEquals(0,transaction{IngredientPurchasePaymentsTable.selectAll().count()}.toInt())
  assertEquals("UNPAID",ExpenseService().getAll("business").single().paymentStatus)
  ops.openShift("business","manager",ShiftOpenRequest())
  val paid=ops.payPurchaseOrder("business","manager",po.id,IngredientPurchasePaymentRequest(100.0,"CASH",clientReference="closed-drawer-pay",paidFromTill=true))
  assertTrue(paid.payments.single().paidFromTill);assertEquals(100.0,ops.dashboard("business").shifts.first{it.status=="OPEN"}.supplierCashPayments)
 }

 @Test fun `simultaneous supplier payments cannot exceed the purchase balance`() {
  val (_,po)=goatPurchase();ops.receivePurchaseOrder("business","manager",po.id)
  val executor=java.util.concurrent.Executors.newFixedThreadPool(2);val start=java.util.concurrent.CountDownLatch(1)
  try {
   val attempts=(1..2).map{index->executor.submit<Boolean>{start.await();runCatching{ops.payPurchaseOrder("business","manager",po.id,IngredientPurchasePaymentRequest(6000.0,"CASH",clientReference="parallel-payment-$index"))}.isSuccess}}
   start.countDown();assertEquals(1,attempts.count{it.get(15,java.util.concurrent.TimeUnit.SECONDS)})
   val result=ops.dashboard("business").purchaseOrders.single();assertEquals(6000.0,result.paidAmount);assertEquals(2500.0,result.outstandingAmount);assertEquals(1,result.payments.size)
  }finally{executor.shutdownNow()}
 }

 @Test fun `product stock purchases stay in spending and outside operating profit`() {
  val date=Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.of("Africa/Nairobi")).date.toString()
  val invoices=PurchaseInvoiceService()
  assertTrue(invoices.create("business",CreatePurchaseInvoiceRequest(invoiceNumber="PAID-STOCK",supplierName="Supplier",invoiceDate=date,items=listOf(PurchaseLineItem("meal","Meal",quantity=2,unitCost=50.0)))).success)
  assertTrue(invoices.create("business",CreatePurchaseInvoiceRequest(invoiceNumber="UNPAID-STOCK",supplierName="Supplier",invoiceDate=date,paymentStatus="PENDING",items=listOf(PurchaseLineItem("meal","Meal",quantity=2,unitCost=50.0)))).success)
  val summary=ExpenseService().getProfitSummary("business",date,date)
  assertEquals(0.0,summary.totalExpenses);assertEquals(0.0,summary.netProfit);assertEquals(200.0,summary.stockPurchases);assertEquals(100.0,summary.stockPurchasePayments);assertEquals(100.0,summary.cashflowOut)
 }

 @Test fun `ingredient receipts average the cost of remaining stock without repricing past sales`() {
  val (goat,po)=goatPurchase(2.0,500.0);ops.receivePurchaseOrder("business","manager",po.id)
  ops.saveRecipe("business","manager","meal",SaveRecipeRequest(listOf(RecipeLine(goat,1000.0))))
  val first=tab("meal");service.closeTab("business",first.id,CloseHospitalityTabRequest("CASH"))
  val second=ops.createPurchaseOrder("business","manager",PurchaseOrderRequest(po.supplierId,items=listOf(PurchaseOrderLineRequest(goat,1.0,1000.0,"KG"))))
  ops.receivePurchaseOrder("business","manager",second.id)
  val ingredient=ops.dashboard("business").ingredients.first{it.id==goat}
  assertEquals(2000.0,ingredient.quantity);assertEquals(0.75,ingredient.unitCost)
  assertEquals(500.0,orders.getById(first.id,"business")!!.items.single().buyingPrice)
  assertEquals(750.0,tab("meal").items.single().buyingPrice)
 }

 @Test fun `ordinary expense edits persist and foreign edits cannot change profit`() {
  val date=Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.of("Africa/Nairobi")).date.toString()
  val expenses=ExpenseService();val entry=expenses.create("business",ExpenseRequest("RENT",100.0,"Rent",date)).data!!
  assertTrue(expenses.update(entry.id,"business",ExpenseRequest("RENT",250.0,"Revised rent",date)).success)
  assertEquals(250.0,expenses.getProfitSummary("business",date,date).totalExpenses)
  assertFalse(expenses.update(entry.id,"other",ExpenseRequest("RENT",1.0,"Wrong tenant",date)).success)
  assertEquals(250.0,expenses.getAll("business").single().amount)
 }

 @Test fun `fractional purchase values settle in cents without leaving an unpayable balance`() {
  val (_,po)=goatPurchase(0.333,100.01)
  assertEquals(33.3,po.totalCost)
  val result=ops.receivePurchaseOrder("business","manager",po.id,ReceiveIngredientPurchaseRequest(IngredientPurchasePaymentRequest(33.3,"CASH",clientReference="fractional-purchase")))
  assertEquals("PAID",result.paymentStatus);assertEquals(0.0,result.outstandingAmount)
 }

}

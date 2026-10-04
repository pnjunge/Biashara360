package com.app.biashara.services

import com.app.biashara.db.*
import com.app.biashara.models.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.test.*
import com.app.biashara.routes.hospitalityRoutes
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.request.*
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
   SchemaUtils.create(BusinessesTable,BranchesTable,UsersTable,ProductsTable,CustomersTable,OrdersTable,OrderItemsTable,StockMovementsTable,PaymentsTable,HospitalityTablesTable,KitchenTicketsTable,HospitalityReservationsTable,HospitalityMenuProfilesTable,InventoryIngredientsTable,ProductRecipesTable,BarStockEventsTable,HospitalityShiftsTable,SuppliersTable,PurchaseOrdersTable,PurchaseOrderItemsTable,ManagerApprovalsTable,AuditEventsTable,OrderSplitPaymentsTable,AccessRolesTable,UserAccessRolesTable,AccessGroupsTable,AccessGroupRolesTable,UserAccessGroupsTable,PermissionsTable,RolePermissionsTable)
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
 @Test fun `routing obeys kitchen bar and no preparation profiles`() {
  ops.saveMenuProfile("business","manager","meal",MenuProfileRequest(preparationStation=null))
  ops.saveMenuProfile("business","manager","drink",MenuProfileRequest(preparationStation="BAR"))
  tab("meal","drink")
  val tickets=service.dashboard("business").tickets
  assertEquals(listOf("BAR"),tickets.map{it.station});assertEquals("drink",tickets.single().items.single().productId)
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
   for(code in listOf("hospitality.view","hospitality.approvals","hospitality.kitchen"))if(!PermissionsTable.select{PermissionsTable.code eq code}.any())PermissionsTable.insert {it[id]=UUID.randomUUID().toString();it[PermissionsTable.code]=code;it[module]="HOSPITALITY";it[action]="MANAGE";it[name]=code;it[createdAt]=Clock.System.now()}
  }
  val access=AccessControlService()
  val role=access.createRole("business",SaveAccessRoleRequest("Viewer",allowedMenus=listOf("HOSPITALITY"),permissions=listOf("hospitality.view")))
  val managerRole=access.createRole("business",SaveAccessRoleRequest("Approval manager",allowedMenus=listOf("HOSPITALITY_OPS"),permissions=listOf("hospitality.view","hospitality.approvals","hospitality.kitchen")))
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
  for(path in listOf("/hospitality/operations/menu/meal","/hospitality/operations/shifts/open","/hospitality/operations/ingredients","/hospitality/operations/purchase-orders")) {
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

}

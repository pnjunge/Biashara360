package com.app.biashara.services

import com.app.biashara.db.*
import com.app.biashara.models.*
import kotlinx.datetime.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.*
import com.app.biashara.routes.hotelRoutes
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

class HotelServiceTest {
    private lateinit var database: Database
    private val service = HotelService()
    private lateinit var typeId: String
    private lateinit var roomId: String
    private val today get() = Clock.System.now().toLocalDateTime(TimeZone.of("Africa/Nairobi")).date
    private fun date(days: Int) = today.plus(days, DateTimeUnit.DAY).toString()
    private fun key() = UUID.randomUUID().toString()
    @BeforeTest fun setup() {
        org.koin.core.context.stopKoin()
        val postgres = System.getenv("HOTEL_TEST_POSTGRES_URL")
        require(postgres == null || (postgres.startsWith("jdbc:postgresql://127.0.0.1:") && postgres.endsWith("/hotel_test"))) { "PostgreSQL hotel tests require an isolated loopback hotel_test database" }
        database = if (postgres != null) Database.connect(postgres, driver="org.postgresql.Driver", user="postgres", password="hotel-test-only") else Database.connect("jdbc:h2:mem:hotel-${key()};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=15000", driver="org.h2.Driver")
        TransactionManager.defaultDatabase = database
        transaction(database) {
            if (postgres != null) exec("TRUNCATE TABLE businesses CASCADE")
            SchemaUtils.create(BusinessesTable, BranchesTable, UsersTable, OrdersTable, PaymentsTable, HotelRoomTypes, HotelRooms, HotelReservations, HotelFolioEntries, HotelRoomBlocks, HotelPaymentRequests, CyberSourceTransactionsTable, AccessRolesTable, UserAccessRolesTable, AccessGroupsTable, AccessGroupRolesTable, UserAccessGroupsTable, PermissionsTable, RolePermissionsTable)
            for (b in listOf("business", "other")) BusinessesTable.insert {
                it[id]=b; it[name]=b; it[storefrontSlug]=b; it[type]="HOTEL"; it[hotelEnabled]=true
                it[ownerEmail]="$b@example.com"; it[ownerPhone]="254700000001"; it[createdAt]=Clock.System.now(); it[updatedAt]=Clock.System.now()
            }
            UsersTable.insert { it[id]="actor"; it[businessId]="business"; it[name]="Clerk"; it[email]="clerk@example.com"; it[phone]="254700000002"; it[passwordHash]="unused"; it[role]="STAFF"; it[createdAt]=Clock.System.now(); it[updatedAt]=Clock.System.now() }
        }
        typeId = service.saveType("business","actor",null,HotelTypeRequest("Double",capacity=2,nightlyRateCents=10_000)).id
        roomId = service.saveRoom("business","actor",null,HotelRoomRequest(typeId,"101")).id
    }
    @AfterTest fun cleanup() { org.koin.core.context.stopKoin() }
    private fun booking(room: String = roomId, start: Int=0, end: Int=2, guests: Int=1) = HotelReservationRequest(room,"Jane Guest",guestPhone="254700000001",guests=guests,arrival=date(start),departure=date(end))
    @Test fun `overlaps and capacity are rejected while departure day may be rebooked`() {
        service.saveReservation("business","actor",null,booking())
        assertFailsWith<IllegalArgumentException> { service.saveReservation("business","actor",null,booking(start=1,end=3)) }
        service.saveReservation("business","actor",null,booking(start=2,end=3))
        assertFailsWith<IllegalArgumentException> { service.saveReservation("business","actor",null,booking(start=3,end=4,guests=3)) }
        assertFailsWith<IllegalArgumentException> { service.saveReservation("business","actor",null,booking(start=2,end=2)) }
        assertEquals(2,service.dashboard("business","actor",date(0),date(5)).reservations.size)
    }
    @Test fun `concurrent booking requests never double book a room`() {
        val start=CountDownLatch(1); val executor=Executors.newFixedThreadPool(2)
        try {
            val futures=(1..2).map { executor.submit<Boolean> { start.await(); runCatching { service.saveReservation("business","actor",null,booking()) }.isSuccess } }
            start.countDown()
            assertEquals(1,futures.count { it.get(20,TimeUnit.SECONDS) })
            assertEquals(1,service.dashboard("business","actor",date(0),date(3)).reservations.size)
        } finally { executor.shutdownNow() }
    }
    @Test fun `tenant scope is enforced on every referenced resource`() {
        val r=service.saveReservation("business","actor",null,booking())
        assertFails { service.saveRoom("other","actor",null,HotelRoomRequest(typeId,"201")) }
        assertFails { service.saveReservation("other","actor",null,booking()) }
        assertFails { service.folio("other","actor",r.id) }
        assertFails { service.housekeeping("other","actor",roomId,HotelHousekeepingRequest("CLEAN")) }
        assertFails { service.exportCalendar("other",roomId) }
    }
    @Test fun `deposits are idempotent and checkout requires full settlement and clean checkin`() {
        val r=service.saveReservation("business","actor",null,booking())
        service.housekeeping("business","actor",roomId,HotelHousekeepingRequest("DIRTY"))
        assertFailsWith<IllegalArgumentException> { service.transition("business","actor",r.id,"CHECK_IN") }
        service.housekeeping("business","actor",roomId,HotelHousekeepingRequest("INSPECTED"))
        service.transition("business","actor",r.id,"CHECK_IN")
        val request=HotelPaymentRequest(5_000,"CASH",key())
        val p=service.payment("business","actor",r.id,request)
        assertEquals(p,service.payment("business","actor",r.id,request))
        assertFailsWith<IllegalArgumentException> { service.payment("business","actor",r.id,request.copy(amountCents=4_000)) }
        assertEquals(15_000L,service.folio("business","actor",r.id).balanceCents)
        assertFailsWith<IllegalArgumentException> { service.transition("business","actor",r.id,"CHECK_OUT") }
        service.payment("business","actor",r.id,HotelPaymentRequest(15_000,"CASH",key()))
        assertEquals("CHECKED_OUT",service.transition("business","actor",r.id,"CHECK_OUT").reservation.status)
        assertEquals("DIRTY",service.dashboard("business","actor",date(0),date(3)).rooms.single().housekeepingStatus)
        assertFailsWith<IllegalArgumentException> { service.postEntry("business","actor",r.id,HotelFolioRequest("CHARGE","Late charge",100,requestId=key())) }
    }
    @Test fun `cancelled deposits become credit and refund is limited to actual credit`() {
        val r=service.saveReservation("business","actor",null,booking())
        service.payment("business","actor",r.id,HotelPaymentRequest(5_000,"CASH",key()))
        val cancelled=service.transition("business","actor",r.id,"CANCEL")
        assertEquals(-5_000L,cancelled.balanceCents)
        assertFailsWith<IllegalArgumentException> { service.postEntry("business","actor",r.id,HotelFolioRequest("CASH_REFUND","Refund cash",5_001,requestId=key())) }
        val request=HotelFolioRequest("CASH_REFUND","Refund cash",5_000,requestId=key())
        assertEquals(0L,service.postEntry("business","actor",r.id,request).balanceCents)
        assertEquals(0L,service.postEntry("business","actor",r.id,request).balanceCents)
    }
    @Test fun `electronic requests never credit unverified payments`() {
        val r=service.saveReservation("business","actor",null,booking())
        assertFailsWith<IllegalArgumentException> { service.payment("business","actor",r.id,HotelPaymentRequest(1_050,"MPESA",key())) }
        val p=service.payment("business","actor",r.id,HotelPaymentRequest(20_000,"CARD",key()))
        assertEquals(20_000L,service.folio("business","actor",r.id).balanceCents)
        transaction { OrdersTable.update({ OrdersTable.id eq p.orderId }) { it[paymentStatus]="PAID" } }
        assertEquals(20_000L,service.folio("business","actor",r.id).balanceCents)
        transaction { PaymentsTable.insert {
            it[id]=key(); it[businessId]="business"; it[orderId]=p.orderId; it[transactionCode]="card-test"; it[amount]=200.0
            it[payerName]="Jane"; it[payerPhone]="254700000001"; it[method]="CARD"; it[status]="SUCCESS"; it[channel]="CARD"; it[transactionDate]=Clock.System.now()
        } }
        assertEquals(0L,service.folio("business","actor",r.id).balanceCents)
        transaction { PaymentsTable.insert {
            it[id]=key(); it[businessId]="business"; it[orderId]=p.orderId; it[transactionCode]="duplicate-callback"; it[amount]=200.0
            it[payerName]="Jane"; it[payerPhone]="254700000001"; it[method]="CARD"; it[status]="SUCCESS"; it[channel]="CARD"; it[transactionDate]=Clock.System.now()
        } }
        assertEquals(0L,service.folio("business","actor",r.id).balanceCents)
        assertEquals(1,service.folio("business","actor",r.id).entries.size)
    }
    @Test fun `rate snapshots survive changes and moving rooms dirties the old room`() {
        val r=service.saveReservation("business","actor",null,booking())
        service.saveType("business","actor",typeId,HotelTypeRequest("Double",capacity=2,nightlyRateCents=50_000))
        assertEquals(20_000L,service.folio("business","actor",r.id).roomChargeCents)
        val second=service.saveRoom("business","actor",null,HotelRoomRequest(typeId,"102"))
        service.transition("business","actor",r.id,"CHECK_IN")
        service.saveReservation("business","actor",r.id,booking(second.id,end=3))
        assertEquals(30_000L,service.folio("business","actor",r.id).roomChargeCents)
        assertEquals("DIRTY",service.dashboard("business","actor",date(0),date(4)).rooms.first{it.id==roomId}.housekeepingStatus)
    }
    private fun calendar(start: Int, end: Int) = "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nBEGIN:VEVENT\r\nUID:external-1\r\nDTSTART;VALUE=DATE:${date(start).replace("-","")}\r\nDTEND;VALUE=DATE:${date(end).replace("-","")}\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n"
    @Test fun `calendar import atomically replaces channel blocks and refuses conflicts or unsupported events`() {
        service.importCalendar("business","actor",roomId,HotelCalendarImportRequest("Airbnb",calendar(0,2)))
        assertFailsWith<IllegalArgumentException> { service.saveReservation("business","actor",null,booking()) }
        service.importCalendar("business","actor",roomId,HotelCalendarImportRequest("Airbnb",calendar(2,3)))
        val r=service.saveReservation("business","actor",null,booking())
        assertFailsWith<IllegalArgumentException> { service.importCalendar("business","actor",roomId,HotelCalendarImportRequest("Airbnb",calendar(0,2))) }
        assertEquals(date(2),service.dashboard("business","actor",date(0),date(4)).blocks.single().arrival)
        assertFailsWith<IllegalArgumentException> { HotelCalendar.parse(calendar(3,4).replace("END:VEVENT","RRULE:FREQ=DAILY\r\nEND:VEVENT")) }
        assertFailsWith<IllegalArgumentException> { HotelCalendar.parse("not a calendar") }
        val exported=service.exportCalendar("business",roomId)
        assertFalse(exported.contains(r.guestName)); assertEquals(2,HotelCalendar.parse(exported).size)
    }
    @Test fun `room blocks and disabled rooms prevent bookings without disturbing existing stays`() {
        service.block("business","actor",HotelBlockRequest(roomId,date(0),date(2),"Maintenance"))
        assertFailsWith<IllegalArgumentException> { service.saveReservation("business","actor",null,booking()) }
        val r=service.saveReservation("business","actor",null,booking(start=2,end=4))
        assertFailsWith<IllegalArgumentException> { service.block("business","actor",HotelBlockRequest(roomId,date(3),date(5),"Painting")) }
        assertFailsWith<IllegalArgumentException> { service.saveRoom("business","actor",roomId,HotelRoomRequest(typeId,"101",isActive=false)) }
        assertEquals("CONFIRMED",service.folio("business","actor",r.id).reservation.status)
    }
    @Test fun `hotel routes enforce view frontdesk and housekeeping permissions`() = testApplication {
        environment { config = io.ktor.server.config.MapApplicationConfig() }
        transaction {
            for (code in listOf("hotel.view","hotel.housekeeping")) if (!PermissionsTable.select { PermissionsTable.code eq code }.any()) PermissionsTable.insert {
                it[id]=key(); it[PermissionsTable.code]=code; it[module]="HOTEL"; it[action]="VIEW"; it[name]=code; it[createdAt]=Clock.System.now()
            }
        }
        val access=AccessControlService()
        val algorithm=Algorithm.HMAC256("hotel-test-secret")
        val token=JWT.create().withSubject("actor").withClaim("businessId","business").withClaim("role","STAFF").sign(algorithm)
        application {
            install(Koin) { modules(module { single { service }; single { access } }) }
            install(ContentNegotiation) { json() }
            install(Authentication) { jwt { verifier(JWT.require(algorithm).build()); validate { JWTPrincipal(it.payload) } } }
            routing { authenticate { hotelRoutes() } }
        }
        assertEquals(HttpStatusCode.Forbidden,client.get("/hotel") { bearerAuth(token) }.status)
        val role=access.createRole("business",SaveAccessRoleRequest("Housekeeper",allowedMenus=listOf("HOTEL"),permissions=listOf("hotel.view","hotel.housekeeping")))
        transaction { UserAccessRolesTable.insert { it[userId]="actor";it[roleId]=role.id } }
        assertEquals(HttpStatusCode.OK,client.get("/hotel") { bearerAuth(token) }.status)
        val stay=service.saveReservation("business","actor",null,booking())
        assertEquals(HttpStatusCode.Forbidden,client.post("/hotel/reservations/${stay.id}/check-in") { bearerAuth(token) }.status)
        assertEquals(HttpStatusCode.Forbidden,client.get("/hotel/reservations/${stay.id}/folio") { bearerAuth(token) }.status)
        assertEquals(HttpStatusCode.OK,client.put("/hotel/rooms/$roomId/housekeeping") { bearerAuth(token);contentType(ContentType.Application.Json);setBody("""{"status":"DIRTY"}""") }.status)
        assertEquals(HttpStatusCode.Forbidden,client.put("/hotel/enabled") { bearerAuth(token);contentType(ContentType.Application.Json);setBody("""{"enabled":false}""") }.status)
    }

    @Test fun `cancelled discounted stays never refund more than the money received`() {
        val r=service.saveReservation("business","actor",null,booking())
        service.postEntry("business","actor",r.id,HotelFolioRequest("CREDIT","Discount",5_000,requestId=key()))
        service.payment("business","actor",r.id,HotelPaymentRequest(15_000,"CASH",key()))
        assertEquals(-15_000L,service.transition("business","actor",r.id,"CANCEL").balanceCents)
        service.postEntry("business","actor",r.id,HotelFolioRequest("CASH_REFUND","Cash returned",15_000,requestId=key()))
        assertEquals(0L,service.folio("business","actor",r.id).balanceCents)
        assertEquals(0.0,ReportService().paymentReport("business",date(0),date(1)).totalAmount)
    }
    @Test fun `occupancy excludes maintenance blocks and reports collected funds separately`() {
        val second=service.saveRoom("business","actor",null,HotelRoomRequest(typeId,"102"))
        service.block("business","actor",HotelBlockRequest(second.id,date(0),date(2),"Maintenance"))
        val r=service.saveReservation("business","actor",null,booking())
        service.payment("business","actor",r.id,HotelPaymentRequest(5_000,"CASH",key()))
        val report=service.report("business","actor",date(0),date(2))
        assertEquals(2,report.roomNightsAvailable);assertEquals(2,report.roomNightsBooked)
        assertEquals(100.0,report.occupancyPercent);assertEquals(20_000L,report.accommodationRevenueCents)
        assertEquals(5_000L,report.collectedCents);assertEquals(15_000L,report.outstandingBalanceCents)
    }

}

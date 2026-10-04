package com.app.biashara.services

import com.app.biashara.db.*
import com.app.biashara.models.*
import com.app.biashara.routes.reportRoutes
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
import io.mockk.mockk
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.test.*

class AccessControlServiceTest {
    private val service = AccessControlService()

    @BeforeTest
    fun setup() {
        val db = Database.connect("jdbc:h2:mem:access-${UUID.randomUUID()};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        TransactionManager.defaultDatabase = db
        transaction(db) {
            SchemaUtils.create(BusinessesTable, BranchesTable, UsersTable, AccessRolesTable, AccessGroupsTable,
                AccessGroupRolesTable, UserAccessGroupsTable, UserAccessRolesTable, PermissionsTable, RolePermissionsTable)
            for (business in listOf("business", "other")) {
                BusinessesTable.insert {
                    it[id] = business; it[name] = business; it[storefrontSlug] = business; it[type] = "RETAIL"
                    it[ownerPhone] = "123"; it[ownerEmail] = "$business@example.com"
                    it[createdAt] = Clock.System.now(); it[updatedAt] = Clock.System.now()
                }
            }
            UsersTable.insert {
                it[id] = "staff"; it[businessId] = "business"; it[name] = "Staff"
                it[email] = "staff@example.com"; it[phone] = "123"; it[passwordHash] = "unused"
                it[role] = "STAFF"; it[createdAt] = Clock.System.now(); it[updatedAt] = Clock.System.now()
            }
            PermissionsTable.insert {
                it[id] = "financial"; it[code] = "reports.financial"; it[module] = "REPORTS"
                it[action] = "VIEW"; it[name] = "Financial Reports"; it[createdAt] = Clock.System.now()
            }
        }
    }

    private fun reportingRole(business: String = "business") = service.createRole(business,
        SaveAccessRoleRequest(name = "Financial reader", allowedMenus = listOf("REPORTS"), permissions = listOf("reports.financial")))

    @Test
    fun `any named group inherits active role permissions and menus with revocation`() {
        val role = reportingRole()
        val group = service.createGroup("business", SaveAccessGroupRequest(name = "Cashiers", allowedMenus = listOf("POS"), roleIds = listOf(role.id)))
        service.assignUsers("business", group.id, AssignGroupUsersRequest(listOf("staff")))
        assertTrue(service.hasPermission("staff", "business", "reports.financial"))
        assertEquals(setOf("POS", "REPORTS"), service.myMenus("business", "staff", "STAFF").enabledMenus.toSet())
        assertEquals(listOf("reports.financial"), service.myMenus("business", "staff", "STAFF").permissions)
        service.toggleRoleStatus("business", role.id, false)
        assertFalse(service.hasPermission("staff", "business", "reports.financial"))
        assertEquals(listOf("POS"), service.myMenus("business", "staff", "STAFF").enabledMenus)
        service.toggleRoleStatus("business", role.id, true)
        service.toggleGroupStatus("business", group.id, false)
        assertFalse(service.hasPermission("staff", "business", "reports.financial"))
        assertTrue(service.myMenus("business", "staff", "STAFF").enabledMenus.isEmpty())
        service.toggleGroupStatus("business", group.id, true)
        service.updateGroup("business", group.id, SaveAccessGroupRequest(name = "Cashiers"))
        service.config("business") // Reading configuration must not restore cleared rights.
        assertFalse(service.hasPermission("staff", "business", "reports.financial"))
        assertTrue(service.myMenus("business", "staff", "STAFF").enabledMenus.isEmpty())
    }

    @Test
    fun `cross tenant and unknown role assignments are rejected atomically`() {
        val foreign = reportingRole("other")
        assertFailsWith<IllegalArgumentException> {
            service.createGroup("business", SaveAccessGroupRequest(name = "Bad group", roleIds = listOf(foreign.id)))
        }
        val group = service.createGroup("business", SaveAccessGroupRequest(name = "Local group", allowedMenus = listOf("POS")))
        assertFailsWith<IllegalArgumentException> {
            service.updateGroup("business", group.id, SaveAccessGroupRequest(name = "Changed", roleIds = listOf("missing")))
        }
        assertEquals("Local group", service.config("business").groups.single().name)
        assertFalse(service.hasPermission("staff", "other", "reports.financial"))
    }

    @Test
    fun `direct roles remain effective and business menu restrictions are respected`() {
        val role = reportingRole()
        transaction { UserAccessRolesTable.insert { it[userId] = "staff"; it[roleId] = role.id } }
        assertTrue(service.hasPermission("staff", "business", "reports.financial"))
        service.updateMenus("business", UpdateMenusRequest(listOf("POS")))
        assertTrue(service.myMenus("business", "staff", "STAFF").enabledMenus.isEmpty())
    }
    @Test
    fun `staff reporting route honors assigned financial right and business module gate`() = testApplication {
        environment { config = io.ktor.server.config.MapApplicationConfig() }
        val role = reportingRole()
        val group = service.createGroup("business", SaveAccessGroupRequest(name = "Any group", allowedMenus = listOf("REPORTS")))
        service.assignUsers("business", group.id, AssignGroupUsersRequest(listOf("staff")))
        val algorithm = Algorithm.HMAC256("test-secret")
        val token = JWT.create().withSubject("staff").withClaim("businessId", "business")
            .withClaim("role", "STAFF").sign(algorithm)
        application {
            install(Koin) { modules(module {
                single { service }
                single { mockk<ExpenseService>() }
                single { mockk<ReportService>() }
            }) }
            install(ContentNegotiation) { json() }
            install(Authentication) { jwt {
                verifier(JWT.require(algorithm).build())
                validate { JWTPrincipal(it.payload) }
            } }
            routing { authenticate { reportRoutes() } }
        }
        suspend fun status() = client.get("/reports/profit-summary") { bearerAuth(token) }.status
        assertEquals(HttpStatusCode.Forbidden, status())
        service.updateGroup("business", group.id, SaveAccessGroupRequest(name = "Any group", roleIds = listOf(role.id)))
        // Missing dates now reach validation, proving the STAFF user passed authorization.
        assertEquals(HttpStatusCode.BadRequest, status())
        transaction { BusinessesTable.update({ BusinessesTable.id eq "business" }) { it[enabledModules] = "SALES" } }
        assertEquals(HttpStatusCode.Forbidden, status())
    }

}

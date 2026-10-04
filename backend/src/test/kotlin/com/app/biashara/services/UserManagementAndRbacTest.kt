package com.app.biashara.services

import com.app.biashara.auth.PasswordUtils
import com.app.biashara.auth.JwtUtils
import com.app.biashara.db.*
import com.app.biashara.models.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import java.util.UUID
import kotlin.test.*

class UserManagementAndRbacTest {
    private val testConfig = io.ktor.server.config.MapApplicationConfig(
        "jwt.secret" to "rbac-test-only-signing-secret",
        "jwt.issuer" to "rbac-tests",
        "jwt.audience" to "rbac-test-users"
    )
    private val testHttpClient = io.ktor.client.HttpClient(io.ktor.client.engine.mock.MockEngine {
        respond("{}", io.ktor.http.HttpStatusCode.OK, io.ktor.http.headersOf(io.ktor.http.HttpHeaders.ContentType, "application/json"))
    })
    private val auditLogService = AuditLogService()
    private val emailService = EmailService(testConfig)
    private val smsService = SmsService(testConfig, testHttpClient)
    private val whatsappOtpService = WhatsAppOtpService(testConfig, testHttpClient)
    private val authService = AuthService(smsService, emailService, whatsappOtpService, auditLogService)
    private val userManagementService = UserManagementService(authService, auditLogService)
    private val accessControlService = AccessControlService(auditLogService)

    private val testBusinessId = UUID.randomUUID().toString()
    private val adminUserId = UUID.randomUUID().toString()
    private val branch1Id = UUID.randomUUID().toString()
    private val branch2Id = UUID.randomUUID().toString()

    @BeforeTest
    fun setup() {
        JwtUtils.init(testConfig)
        val db = Database.connect(
            "jdbc:h2:mem:rbac-${UUID.randomUUID()};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
            driver = "org.h2.Driver"
        )
        TransactionManager.defaultDatabase = db

        transaction(db) {
            SchemaUtils.create(
                BusinessesTable,
                BranchesTable,
                UsersTable,
                AccessRolesTable,
                AccessGroupsTable,
                AccessGroupRolesTable,
                UserAccessGroupsTable,
                UserAccessRolesTable,
                PermissionsTable,
                RolePermissionsTable,
                UserBranchesTable,
                RefreshTokensTable,
                AuditLogsTable
            )

            // Seed business
            BusinessesTable.insert {
                it[id] = testBusinessId
                it[name] = "Biashara Nairobi"
                it[storefrontSlug] = "biashara-nairobi"
                it[type] = "RETAIL"
                it[ownerPhone] = "+254700000001"
                it[ownerEmail] = "admin@biashara.co.ke"
                it[createdAt] = Clock.System.now()
                it[updatedAt] = Clock.System.now()
            }

            // Seed branches
            BranchesTable.insert {
                it[id] = branch1Id
                it[businessId] = testBusinessId
                it[name] = "Head Office Branch"
                it[code] = "HQ"
                it[isHeadOffice] = true
                it[isActive] = true
                it[createdAt] = Clock.System.now()
                it[updatedAt] = Clock.System.now()
            }
            BranchesTable.insert {
                it[id] = branch2Id
                it[businessId] = testBusinessId
                it[name] = "Westlands Outlet"
                it[code] = "WST"
                it[isHeadOffice] = false
                it[isActive] = true
                it[createdAt] = Clock.System.now()
                it[updatedAt] = Clock.System.now()
            }

            // Seed admin user
            UsersTable.insert {
                it[id] = adminUserId
                it[businessId] = testBusinessId
                it[name] = "System Administrator"
                it[email] = "admin@biashara.co.ke"
                it[phone] = "+254700000001"
                it[passwordHash] = PasswordUtils.hash("AdminPass123!")
                it[role] = "ADMIN"
                it[branchId] = branch1Id
                it[status] = "ACTIVE"
                it[createdAt] = Clock.System.now()
                it[updatedAt] = Clock.System.now()
            }

            // Seed sample granular permissions
            val samplePermissions = listOf(
                Triple("users.view", "View Users", "users"),
                Triple("users.create", "Create Users", "users"),
                Triple("users.edit", "Edit Users", "users"),
                Triple("orders.view", "View Orders", "orders"),
                Triple("orders.create", "Create Orders", "orders"),
                Triple("orders.refund", "Refund Orders", "orders"),
                Triple("inventory.view", "View Inventory", "inventory"),
                Triple("inventory.adjust", "Adjust Inventory", "inventory"),
                Triple("payments.view", "View Payments", "payments"),
                Triple("payments.process", "Process Payments", "payments"),
                Triple("reports.view", "View Reports", "reports")
            )
            for ((code, name, module) in samplePermissions) {
                PermissionsTable.insert {
                    it[id] = UUID.randomUUID().toString()
                    it[PermissionsTable.code] = code
                    it[PermissionsTable.name] = name
                    it[PermissionsTable.module] = module
                    it[action] = code.substringAfterLast('.').uppercase()
                    it[createdAt] = Clock.System.now()
                }
            }
        }
    }

    @AfterTest
    fun tearDown() {
        testHttpClient.close()
    }

    @Test
    fun `test user creation with active status, branch and audit trail`() {
        val inviteReq = InviteUserRequest(
            name = "Grace Wanjiku",
            email = "grace@biashara.co.ke",
            phone = "+254711223344",
            role = "STAFF",
            password = "SecretPassword123!",
            branchId = branch1Id
        )

        val result = userManagementService.inviteUser(testBusinessId, inviteReq, adminUserId)
        assertTrue(result.success)
        assertNotNull(result.data)

        val createdUser = result.data!!
        assertEquals("Grace Wanjiku", createdUser.name)
        assertEquals("grace@biashara.co.ke", createdUser.email)
        assertEquals("STAFF", createdUser.role)
        assertEquals(branch1Id, createdUser.branchId)
        assertEquals(listOf(branch1Id), createdUser.assignedBranchIds)
        assertEquals("ACTIVE", createdUser.status)
        assertTrue(createdUser.isActive)

        // Verify audit log was recorded
        val activity = userManagementService.getUserActivity(userId = createdUser.id, businessId = testBusinessId)
        assertTrue(activity.isNotEmpty())
        assertEquals("CREATE_USER", activity.first().action)
    }

    @Test
    fun `test edit user profile and primary branch`() {
        val inviteReq = InviteUserRequest(
            name = "John Doe",
            email = "john@biashara.co.ke",
            phone = "+254722334455",
            role = "STAFF",
            password = "SecretPassword123!",
            branchId = branch1Id
        )
        val created = userManagementService.inviteUser(testBusinessId, inviteReq, adminUserId).data!!

        val editReq = EditUserRequest(
            name = "Johnathan Doe",
            email = "johnathan@biashara.co.ke",
            phone = "+254799887766",
            branchId = branch2Id
        )
        val editResult = userManagementService.editUser(created.id, testBusinessId, adminUserId, editReq)
        assertTrue(editResult.success)
        val updated = editResult.data!!
        assertEquals("Johnathan Doe", updated.name)
        assertEquals("johnathan@biashara.co.ke", updated.email)
        assertEquals("254799887766", updated.phone)
        assertEquals(branch2Id, updated.branchId)

        // Verify audit log
        val activity = userManagementService.getUserActivity(userId = created.id, businessId = testBusinessId)
        assertTrue(activity.any { it.action == "USER_UPDATE" })
    }

    @Test
    fun `test multi branch assignment for staff`() {
        val inviteReq = InviteUserRequest(
            name = "Sarah Cashier",
            email = "sarah@biashara.co.ke",
            phone = "+254733445566",
            role = "STAFF",
            password = "SecretPassword123!",
            branchId = branch1Id
        )
        val created = userManagementService.inviteUser(testBusinessId, inviteReq, adminUserId).data!!

        // Assign to both branch 1 and branch 2
        val assignReq = AssignUserBranchesRequest(
            branchIds = listOf(branch1Id, branch2Id),
            primaryBranchId = branch2Id
        )
        val assignResult = userManagementService.assignUserBranches(created.id, testBusinessId, adminUserId, assignReq)
        assertTrue(assignResult.success)
        val userWithBranches = assignResult.data!!
        assertEquals(branch2Id, userWithBranches.branchId)
        assertEquals(2, userWithBranches.assignedBranchIds?.size)
        assertTrue(userWithBranches.assignedBranchIds!!.contains(branch1Id))
        assertTrue(userWithBranches.assignedBranchIds!!.contains(branch2Id))
    }

    @Test
    fun `test staff PIN assignment, lockout on 5 failed attempts, and unlockAccount`() {
        val inviteReq = InviteUserRequest(
            name = "Peter Cashier",
            email = "peter@biashara.co.ke",
            phone = "+254744556677",
            role = "STAFF",
            password = "SecretPassword123!",
            branchId = branch1Id
        )
        val created = userManagementService.inviteUser(testBusinessId, inviteReq, adminUserId).data!!

        // 1. Assign staff PIN "123456"
        val setPinResult = userManagementService.setStaffPin(
            created.id, testBusinessId, adminUserId, AdminSetStaffPinRequest(pin = "123456")
        )
        assertTrue(setPinResult.success)
        assertTrue(setPinResult.data!!.hasPinSet == true)

        // 2. Simulate 5 failed PIN attempts
        for (i in 1..4) {
            val failedLogin = authService.loginWithPin(PinLoginRequest(pin = "000000", email = created.email))
            assertFalse(failedLogin.success)
            assertEquals("Invalid credentials", failedLogin.message)
        }

        // 5th failed attempt should lock account
        val fifthAttempt = authService.loginWithPin(PinLoginRequest(pin = "000000", email = created.email))
        assertFalse(fifthAttempt.success)
        assertEquals("Invalid credentials", fifthAttempt.message)

        // Verify status in DB
        val lockedUser = userManagementService.listUsers(testBusinessId).first { it.id == created.id }
        assertEquals("LOCKED", lockedUser.status)
        assertTrue(lockedUser.isPinLocked == true)
        val blockedLogin = authService.loginWithPin(PinLoginRequest(pin = "123456", email = created.email))
        assertFalse(blockedLogin.success)
        assertEquals("PIN login is temporarily locked. Use your password or try again later.", blockedLogin.message)

        // 3. Unlock account via admin unlockAccount
        val unlockResult = userManagementService.unlockAccount(created.id, testBusinessId, adminUserId)
        assertTrue(unlockResult.success)
        val unlockedUser = unlockResult.data!!
        assertEquals("ACTIVE", unlockedUser.status)
        assertFalse(unlockedUser.isPinLocked == true)

        // Now correct PIN sign-in succeeds
        val successLogin = authService.loginWithPin(PinLoginRequest(pin = "123456", email = created.email))
        assertTrue(successLogin.success)
    }

    @Test
    fun `test admin reset password overrides user credentials`() {
        val inviteReq = InviteUserRequest(
            name = "Mary Manager",
            email = "mary@biashara.co.ke",
            phone = "+254755667788",
            role = "MANAGER",
            password = "OriginalPassword123!",
            branchId = branch1Id
        )
        val created = userManagementService.inviteUser(testBusinessId, inviteReq, adminUserId).data!!

        // Admin resets password
        val resetResult = userManagementService.adminResetPassword(
            created.id, testBusinessId, adminUserId, AdminResetPasswordRequest(newPassword = "NewSecretPass456!")
        )
        assertTrue(resetResult.success)

        // Old password fails
        val oldLogin = authService.login(LoginRequest(email = "mary@biashara.co.ke", password = "OriginalPassword123!"))
        assertFalse(oldLogin.success)

        // New password succeeds
        val newLogin = authService.login(LoginRequest(email = "mary@biashara.co.ke", password = "NewSecretPass456!"))
        assertTrue(newLogin.success)
    }

    @Test
    fun `test granular permissions matrix role creation and permission checking`() {
        // Create custom "Head Cashier" role with specific permissions
        val createRoleReq = SaveAccessRoleRequest(
            name = "Head Cashier",
            description = "Cashier with order refund and POS authorization",
            allowedMenus = listOf("POS", "ORDERS", "PAYMENTS"),
            permissions = listOf("orders.view", "orders.create", "orders.refund", "payments.process"),
            isActive = true
        )
        val role = accessControlService.createRole(testBusinessId, createRoleReq)
        assertEquals("Head Cashier", role.name)
        assertEquals(4, role.permissions?.size)
        assertTrue(role.permissions!!.contains("orders.refund"))

        // Assign role to a staff member
        val inviteReq = InviteUserRequest(
            name = "Alice Cashier",
            email = "alice@biashara.co.ke",
            phone = "+254766778899",
            role = "STAFF",
            password = "SecretPassword123!",
            roleIds = listOf(role.id)
        )
        val created = userManagementService.inviteUser(testBusinessId, inviteReq, adminUserId).data!!

        // Verify permission evaluation
        val canRefund = accessControlService.hasPermission(created.id, testBusinessId, "orders.refund")
        assertTrue(canRefund)

        val canAdjustInventory = accessControlService.hasPermission(created.id, testBusinessId, "inventory.adjust")
        assertFalse(canAdjustInventory)
    }

    @Test
    fun `test user active status toggle disable and enable`() {
        val inviteReq = InviteUserRequest(
            name = "David Staff",
            email = "david@biashara.co.ke",
            phone = "+254777889900",
            role = "STAFF",
            password = "SecretPassword123!",
            branchId = branch1Id
        )
        val created = userManagementService.inviteUser(testBusinessId, inviteReq, adminUserId).data!!

        // Deactivate user
        val deactivateRes = userManagementService.setActiveStatus(created.id, testBusinessId, adminUserId, UpdateUserStatusRequest(isActive = false))
        assertTrue(deactivateRes.success)
        assertEquals("DISABLED", deactivateRes.data!!.status)
        assertFalse(deactivateRes.data!!.isActive)

        // Login fails when disabled
        val loginAttempt = authService.login(LoginRequest(email = "david@biashara.co.ke", password = "SecretPassword123!"))
        assertFalse(loginAttempt.success)
        assertEquals("Account is deactivated", loginAttempt.message)

        // Reactivate user
        val activateRes = userManagementService.setActiveStatus(created.id, testBusinessId, adminUserId, UpdateUserStatusRequest(isActive = true))
        assertTrue(activateRes.success)
        assertEquals("ACTIVE", activateRes.data!!.status)
        assertTrue(activateRes.data!!.isActive)

        // Login succeeds again
        val loginSuccess = authService.login(LoginRequest(email = "david@biashara.co.ke", password = "SecretPassword123!"))
        assertTrue(loginSuccess.success)
    }
}

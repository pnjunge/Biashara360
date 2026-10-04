package com.app.biashara.services

import com.app.biashara.db.*
import com.app.biashara.models.UpdateInventoryCategoryRequest
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.UUID
import javax.imageio.ImageIO
import kotlin.test.*

class CategoryImageUploadTest {
    private fun image(width: Int = 32, format: String = "png"): String {
        val output = ByteArrayOutputStream()
        ImageIO.write(BufferedImage(width, 32, BufferedImage.TYPE_INT_RGB), format, output)
        return "data:image/$format;base64," + Base64.getEncoder().encodeToString(output.toByteArray())
    }

    @Test
    fun `accepts thumbnails and existing URLs but rejects invalid images and oversized payloads`() {
        for (value in listOf(image(), image(format = "jpeg"), "https://example.com/category.png")) {
            assertEquals(value, CategoryImage.validate(value))
        }
        for (value in listOf(image(513), "data:image/svg+xml;base64,PHN2Zy8+", "data:image/png;base64,not-base64",
            "data:image/png;base64," + "A".repeat(400_000), "https://", "file:///tmp/image.png",
            image().replace("image/png", "image/jpeg"), "data:image/png;base64,aGVsbG8=")) {
            assertNull(CategoryImage.validate(value))
        }
    }

    @Test
    fun `category upload persists on create and edit and status updates preserve it`() {
        val db = Database.connect("jdbc:h2:mem:category-${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        TransactionManager.defaultDatabase = db
        transaction(db) {
            SchemaUtils.create(BusinessesTable, ProductsTable, InventoryCategoriesTable, com.app.biashara.db.ProductRecipesTable, com.app.biashara.db.InventoryIngredientsTable)
            for (business in listOf("business", "other")) BusinessesTable.insert {
                it[id] = business; it[name] = business; it[storefrontSlug] = business; it[type] = "RETAIL"
                it[ownerPhone] = "123"; it[ownerEmail] = "$business@example.com"
                it[createdAt] = Clock.System.now(); it[updatedAt] = Clock.System.now()
            }
        }
        val service = InventoryCategoryService()
        val uploaded = image(512, "jpeg")
        assertTrue(uploaded.length > 500) // Exceeds the old VARCHAR limit.
        val created = service.create("business", "Beverages", uploaded)
        assertTrue(created.success, created.message)
        val id = assertNotNull(created.data).id
        assertEquals(uploaded, service.getAll("business").single().imageUrl)
        val replacement = image(format = "jpeg")
        assertTrue(service.update(id, "business", UpdateInventoryCategoryRequest(imageUrl = replacement)).success)
        service.update(id, "business", UpdateInventoryCategoryRequest(isActive = false))
        assertEquals(replacement, service.getAll("business").single().imageUrl)
        assertFalse(service.update(id, "other", UpdateInventoryCategoryRequest(imageUrl = uploaded)).success)
        assertFalse(service.update(id, "business", UpdateInventoryCategoryRequest(imageUrl = "data:image/png;base64,bad")).success)
        assertEquals(replacement, service.getAll("business").single().imageUrl)
        assertTrue(service.update(id, "business", UpdateInventoryCategoryRequest(imageUrl = "")).success)
        assertNull(service.getAll("business").single().imageUrl)
    }
}

package com.app.biashara.routes

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProductRouteRegistrationTest {
    @Test
    fun `application registers product routes that include inventory categories`() {
        val applicationSource = Files.readString(
            Path.of("src/main/kotlin/com/app/biashara/Application.kt")
        )
        val productRouteSource = Files.readString(
            Path.of("src/main/kotlin/com/app/biashara/routes/ProductRoutes.kt")
        )

        assertTrue(applicationSource.contains("productRoutesValidated()"))
        assertFalse(applicationSource.contains("\n                productRoutes()"))
        assertTrue(productRouteSource.contains("route(\"/categories\")"))
    }
}

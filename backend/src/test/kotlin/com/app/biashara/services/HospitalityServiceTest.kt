package com.app.biashara.services

import kotlin.test.Test
import kotlin.test.assertEquals

class HospitalityServiceTest {
    @Test
    fun `active tabs include tabs awaiting an M-Pesa payment`() {
        assertEquals(
            listOf("OPEN", "AWAITING_PAYMENT"),
            HospitalityService.ACTIVE_TAB_STATUSES,
        )
    }

    @Test
    fun `retail products do not create preparation tickets`() {
        assertEquals(null, hospitalityStationFor("Clothing"))
        assertEquals(null, hospitalityStationFor("Electronics"))
    }

    @Test
    fun `food and drink categories use their preparation stations`() {
        assertEquals("KITCHEN", hospitalityStationFor("Food & Beverage"))
        assertEquals("BAR", hospitalityStationFor("Beer & Wine"))
    }
    @Test
    fun `mixed order sends only food to kitchen and preserves full customer tab`() {
        data class Item(val name: String, val category: String, val quantity: Int)
        val food = Item("Chicken and chips", "Meals", 2)
        val order = listOf(food, Item("Tusker", "Beer & Wine", 3), Item("Water", "Drinks", 1))

        assertEquals(listOf(food), kitchenItems(order) { it.category })
        assertEquals(3, order.size)
        assertEquals(6, order.sumOf { it.quantity })
    }

    @Test
    fun `drinks and retail only order needs no kitchen ticket`() {
        val categories = listOf("Beer & Wine", "Spirits", "Cocktails", "Juice", "Soda", "Water", "Clothing")
        assertEquals(emptyList(), kitchenItems(categories) { it })
    }

    @Test
    fun `food selection handles category case and surrounding whitespace`() {
        assertEquals(listOf("  FOOD  ", "Snacks", "Bakery"),
            kitchenItems(listOf("  FOOD  ", "Snacks", "Bakery", " DRINKS ")) { it })
    }
}

package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.PantryIntelligenceService
import com.example.model.ExpiryStatus
import com.example.model.PantryCategory
import com.example.model.PantryItem
import com.example.model.StorageLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Smart Pantry", appName)
    }

    @Test
    fun `test pantry item expiry calculations`() {
        val now = System.currentTimeMillis()
        val oneDay = TimeUnit.DAYS.toMillis(1)

        val freshItem = PantryItem(
            name = "Apples",
            quantity = 5.0,
            unit = "pcs",
            category = PantryCategory.PRODUCE.name,
            scanDate = now,
            expiryDate = now + (oneDay * 7),
            storageLocation = StorageLocation.FRIDGE.name
        )
        assertEquals(ExpiryStatus.FRESH, freshItem.getExpiryStatus())
        assertTrue(freshItem.daysUntilExpiry() >= 6)

        val expiringSoonItem = PantryItem(
            name = "Milk",
            quantity = 1.0,
            unit = "carton",
            category = PantryCategory.DAIRY.name,
            scanDate = now - (oneDay * 5),
            expiryDate = now + (oneDay * 1),
            storageLocation = StorageLocation.FRIDGE.name
        )
        assertEquals(ExpiryStatus.EXPIRING_SOON, expiringSoonItem.getExpiryStatus())

        val expiredItem = PantryItem(
            name = "Bread",
            quantity = 1.0,
            unit = "loaf",
            category = PantryCategory.BAKERY.name,
            scanDate = now - (oneDay * 10),
            expiryDate = now - (oneDay * 2),
            storageLocation = StorageLocation.PANTRY.name
        )
        assertEquals(ExpiryStatus.EXPIRED, expiredItem.getExpiryStatus())
    }

    @Test
    fun `test zero waste recipes match expiring items`() {
        val now = System.currentTimeMillis()
        val oneDay = TimeUnit.DAYS.toMillis(1)

        val expiringItems = listOf(
            PantryItem(
                name = "Fresh Salmon Fillets",
                quantity = 2.0,
                unit = "pcs",
                category = PantryCategory.MEAT_SEAFOOD.name,
                scanDate = now,
                expiryDate = now + oneDay,
                storageLocation = StorageLocation.FRIDGE.name
            ),
            PantryItem(
                name = "Fresh Baby Spinach",
                quantity = 200.0,
                unit = "g",
                category = PantryCategory.PRODUCE.name,
                scanDate = now,
                expiryDate = now + (oneDay * 2),
                storageLocation = StorageLocation.FRIDGE.name
            )
        )

        val recipes = PantryIntelligenceService.getCuratedFallbackRecipes(expiringItems)
        assertTrue(recipes.isNotEmpty())
        val salmonRecipe = recipes.firstOrNull { it.title.contains("Salmon", ignoreCase = true) }
        assertNotNull(salmonRecipe)
        assertTrue(salmonRecipe!!.matchedExpiringIngredients.any { it.contains("Salmon") })
    }

    @Test
    fun `test receipt fallback parsing`() {
        val sample = PantryIntelligenceService.generateFallbackScannedReceipt()
        assertTrue(sample.items.isNotEmpty())
        assertTrue(sample.totalAmount > 0.0)
    }
}


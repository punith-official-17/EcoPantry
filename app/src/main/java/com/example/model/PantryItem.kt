package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class PantryCategory(val displayName: String, val iconName: String, val defaultShelfLifeDays: Int) {
    PRODUCE("Produce", "eco", 6),
    DAIRY("Dairy", "egg_alt", 10),
    PANTRY("Pantry", "kitchen", 180),
    MEAT_SEAFOOD("Meat & Seafood", "set_meal", 4),
    BAKERY("Bakery", "bakery_dining", 5),
    BEVERAGES("Beverages", "local_cafe", 30),
    FROZEN("Frozen", "ac_unit", 120),
    SNACKS_CONDIMENTS("Snacks & Condiments", "restaurant", 90);

    companion object {
        fun fromString(value: String): PantryCategory {
            return entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) ||
                it.displayName.equals(value, ignoreCase = true)
            } ?: PANTRY
        }
    }
}

enum class StorageLocation(val label: String) {
    FRIDGE("Fridge"),
    PANTRY("Pantry"),
    FREEZER("Freezer");

    companion object {
        fun fromString(value: String): StorageLocation {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.label.equals(value, ignoreCase = true) } ?: PANTRY
        }
    }
}

enum class ExpiryStatus {
    EXPIRED,
    EXPIRING_TODAY,
    EXPIRING_SOON, // 1 - 3 days
    FRESH          // 4+ days
}

@Entity(tableName = "pantry_items")
data class PantryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "pcs",
    val category: String = PantryCategory.PANTRY.name,
    val scanDate: Long = System.currentTimeMillis(),
    val expiryDate: Long = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7),
    val storageLocation: String = StorageLocation.FRIDGE.name,
    val isConsumed: Boolean = false,
    val isDiscarded: Boolean = false,
    val estimatedPrice: Double = 2.99,
    val notes: String = ""
) {
    fun getCategoryEnum(): PantryCategory = PantryCategory.fromString(category)
    fun getStorageEnum(): StorageLocation = StorageLocation.fromString(storageLocation)

    fun daysUntilExpiry(): Int {
        val now = System.currentTimeMillis()
        val diff = expiryDate - now
        return (diff / (1000 * 60 * 60 * 24)).toInt()
    }

    fun getExpiryStatus(): ExpiryStatus {
        val days = daysUntilExpiry()
        return when {
            days < 0 -> ExpiryStatus.EXPIRED
            days == 0 -> ExpiryStatus.EXPIRING_TODAY
            days in 1..3 -> ExpiryStatus.EXPIRING_SOON
            else -> ExpiryStatus.FRESH
        }
    }

    fun formattedExpiryDate(): String {
        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return formatter.format(Date(expiryDate))
    }

    fun formattedScanDate(): String {
        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return formatter.format(Date(scanDate))
    }
}

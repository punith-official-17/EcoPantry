package com.example.model

data class ScannedItemCandidate(
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "pcs",
    val category: PantryCategory = PantryCategory.PANTRY,
    val estimatedShelfLifeDays: Int = 7,
    val estimatedPrice: Double = 2.99,
    val storageLocation: StorageLocation = StorageLocation.FRIDGE,
    var isSelected: Boolean = true
) {
    fun toPantryItem(): PantryItem {
        val now = System.currentTimeMillis()
        val expiry = now + (estimatedShelfLifeDays.toLong() * 24 * 60 * 60 * 1000)
        return PantryItem(
            name = name.trim().replaceFirstChar { it.uppercase() },
            quantity = quantity,
            unit = unit,
            category = category.name,
            scanDate = now,
            expiryDate = expiry,
            storageLocation = storageLocation.name,
            estimatedPrice = estimatedPrice
        )
    }
}

data class ReceiptScanResult(
    val storeName: String? = null,
    val totalAmount: Double? = null,
    val rawText: String = "",
    val items: List<ScannedItemCandidate> = emptyList()
)

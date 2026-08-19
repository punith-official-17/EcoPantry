package com.example.data

import com.example.model.PantryItem
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class PantryRepository(private val pantryDao: PantryDao) {

    val allActiveItems: Flow<List<PantryItem>> = pantryDao.getAllActiveItems()

    fun getItemsExpiringWithinDays(days: Int): Flow<List<PantryItem>> {
        val threshold = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days.toLong())
        return pantryDao.getItemsExpiringSoon(threshold)
    }

    fun getItemsByCategory(category: String): Flow<List<PantryItem>> {
        return pantryDao.getItemsByCategory(category)
    }

    val consumedHistory: Flow<List<PantryItem>> = pantryDao.getConsumedItems()
    val discardedHistory: Flow<List<PantryItem>> = pantryDao.getDiscardedItems()

    suspend fun addItem(item: PantryItem): Long = pantryDao.insertItem(item)

    suspend fun addItems(items: List<PantryItem>): List<Long> = pantryDao.insertAll(items)

    suspend fun updateItem(item: PantryItem) = pantryDao.updateItem(item)

    suspend fun deleteItem(item: PantryItem) = pantryDao.deleteItem(item)

    suspend fun deleteById(id: Long) = pantryDao.deleteById(id)

    suspend fun markConsumed(id: Long) = pantryDao.markConsumed(id)

    suspend fun markDiscarded(id: Long) = pantryDao.markDiscarded(id)

    suspend fun updateQuantity(id: Long, quantity: Double) = pantryDao.updateQuantity(id, quantity)
}

package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.PantryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PantryDao {

    @Query("SELECT * FROM pantry_items WHERE isConsumed = 0 AND isDiscarded = 0 ORDER BY expiryDate ASC")
    fun getAllActiveItems(): Flow<List<PantryItem>>

    @Query("SELECT * FROM pantry_items WHERE isConsumed = 0 AND isDiscarded = 0 AND expiryDate <= :thresholdTime ORDER BY expiryDate ASC")
    fun getItemsExpiringSoon(thresholdTime: Long): Flow<List<PantryItem>>

    @Query("SELECT * FROM pantry_items WHERE isConsumed = 0 AND isDiscarded = 0 AND category = :category ORDER BY expiryDate ASC")
    fun getItemsByCategory(category: String): Flow<List<PantryItem>>

    @Query("SELECT * FROM pantry_items ORDER BY scanDate DESC")
    fun getAllHistory(): Flow<List<PantryItem>>

    @Query("SELECT * FROM pantry_items WHERE isConsumed = 1 ORDER BY scanDate DESC")
    fun getConsumedItems(): Flow<List<PantryItem>>

    @Query("SELECT * FROM pantry_items WHERE isDiscarded = 1 ORDER BY scanDate DESC")
    fun getDiscardedItems(): Flow<List<PantryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: PantryItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PantryItem>): List<Long>

    @Update
    suspend fun updateItem(item: PantryItem)

    @Delete
    suspend fun deleteItem(item: PantryItem)

    @Query("DELETE FROM pantry_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE pantry_items SET isConsumed = 1 WHERE id = :id")
    suspend fun markConsumed(id: Long)

    @Query("UPDATE pantry_items SET isDiscarded = 1 WHERE id = :id")
    suspend fun markDiscarded(id: Long)

    @Query("UPDATE pantry_items SET quantity = :quantity WHERE id = :id")
    suspend fun updateQuantity(id: Long, quantity: Double)
}

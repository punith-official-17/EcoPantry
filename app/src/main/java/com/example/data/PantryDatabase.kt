package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.PantryCategory
import com.example.model.PantryItem
import com.example.model.StorageLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@Database(entities = [PantryItem::class], version = 1, exportSchema = false)
abstract class PantryDatabase : RoomDatabase() {

    abstract fun pantryDao(): PantryDao

    companion object {
        @Volatile
        private var INSTANCE: PantryDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): PantryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PantryDatabase::class.java,
                    "smart_pantry_db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(PantryDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class PantryDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.pantryDao())
                    }
                }
            }

            private suspend fun populateInitialData(dao: PantryDao) {
                val now = System.currentTimeMillis()
                val oneDay = TimeUnit.DAYS.toMillis(1)

                val initialItems = listOf(
                    PantryItem(
                        name = "Organic Whole Milk",
                        quantity = 1.0,
                        unit = "carton",
                        category = PantryCategory.DAIRY.name,
                        scanDate = now - oneDay * 5,
                        expiryDate = now + (oneDay * 1), // Expiring tomorrow!
                        storageLocation = StorageLocation.FRIDGE.name,
                        estimatedPrice = 4.29,
                        notes = "Opened 2 days ago"
                    ),
                    PantryItem(
                        name = "Fresh Baby Spinach",
                        quantity = 200.0,
                        unit = "g",
                        category = PantryCategory.PRODUCE.name,
                        scanDate = now - oneDay * 4,
                        expiryDate = now + (oneDay * 2), // Expiring in 2 days!
                        storageLocation = StorageLocation.FRIDGE.name,
                        estimatedPrice = 2.99,
                        notes = "Great for salads or quick sauté"
                    ),
                    PantryItem(
                        name = "Greek Yogurt (Plain)",
                        quantity = 500.0,
                        unit = "g",
                        category = PantryCategory.DAIRY.name,
                        scanDate = now - oneDay * 7,
                        expiryDate = now + (oneDay * 3), // Expiring in 3 days!
                        storageLocation = StorageLocation.FRIDGE.name,
                        estimatedPrice = 3.99,
                        notes = "For breakfast smoothies or dips"
                    ),
                    PantryItem(
                        name = "Fresh Salmon Fillets",
                        quantity = 2.0,
                        unit = "pcs",
                        category = PantryCategory.MEAT_SEAFOOD.name,
                        scanDate = now - oneDay * 1,
                        expiryDate = now + (oneDay * 1), // Expiring tomorrow!
                        storageLocation = StorageLocation.FRIDGE.name,
                        estimatedPrice = 9.50,
                        notes = "Cook thoroughly"
                    ),
                    PantryItem(
                        name = "Sourdough Bread",
                        quantity = 1.0,
                        unit = "loaf",
                        category = PantryCategory.BAKERY.name,
                        scanDate = now - oneDay * 2,
                        expiryDate = now + (oneDay * 2),
                        storageLocation = StorageLocation.PANTRY.name,
                        estimatedPrice = 4.50,
                        notes = "Artisan rustic loaf"
                    ),
                    PantryItem(
                        name = "Ripe Avocados",
                        quantity = 3.0,
                        unit = "pcs",
                        category = PantryCategory.PRODUCE.name,
                        scanDate = now - oneDay * 3,
                        expiryDate = now + (oneDay * 4),
                        storageLocation = StorageLocation.FRIDGE.name,
                        estimatedPrice = 3.99,
                        notes = "Soft and ready to eat"
                    ),
                    PantryItem(
                        name = "Extra Virgin Olive Oil",
                        quantity = 750.0,
                        unit = "ml",
                        category = PantryCategory.PANTRY.name,
                        scanDate = now - oneDay * 20,
                        expiryDate = now + (oneDay * 180),
                        storageLocation = StorageLocation.PANTRY.name,
                        estimatedPrice = 11.99,
                        notes = "Cold pressed"
                    ),
                    PantryItem(
                        name = "Jasmine Rice",
                        quantity = 2.0,
                        unit = "kg",
                        category = PantryCategory.PANTRY.name,
                        scanDate = now - oneDay * 15,
                        expiryDate = now + (oneDay * 300),
                        storageLocation = StorageLocation.PANTRY.name,
                        estimatedPrice = 5.49,
                        notes = "Sealed container"
                    ),
                    PantryItem(
                        name = "Free Range Eggs",
                        quantity = 12.0,
                        unit = "pcs",
                        category = PantryCategory.DAIRY.name,
                        scanDate = now - oneDay * 6,
                        expiryDate = now + (oneDay * 8),
                        storageLocation = StorageLocation.FRIDGE.name,
                        estimatedPrice = 4.79,
                        notes = "Grade A large"
                    ),
                    PantryItem(
                        name = "Frozen Blueberries",
                        quantity = 400.0,
                        unit = "g",
                        category = PantryCategory.FROZEN.name,
                        scanDate = now - oneDay * 10,
                        expiryDate = now + (oneDay * 90),
                        storageLocation = StorageLocation.FREEZER.name,
                        estimatedPrice = 3.49,
                        notes = "Wild antioxidant rich"
                    )
                )

                dao.insertAll(initialItems)
            }
        }
    }
}

package com.nenykely.front_kotlin.data

import android.content.Context
import androidx.room.*
import com.google.gson.Gson
import com.nenykely.front_kotlin.data.models.Delivery

@Entity(tableName = "deliveries")
data class DeliveryEntity(
    @PrimaryKey val id: Int,
    val status: String,
    val deliveryJson: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface DeliveryDao {
    @Query("SELECT * FROM deliveries ORDER BY updatedAt DESC")
    suspend fun getAllDeliveries(): List<DeliveryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeliveries(deliveries: List<DeliveryEntity>)

    @Query("DELETE FROM deliveries")
    suspend fun clearAll()
}

@Database(entities = [DeliveryEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deliveryDao(): DeliveryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "delivery_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

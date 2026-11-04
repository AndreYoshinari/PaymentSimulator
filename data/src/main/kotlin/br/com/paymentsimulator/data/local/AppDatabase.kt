package br.com.paymentsimulator.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ProductEntity::class, PaymentEntity::class, PaymentItemEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao

    abstract fun paymentDao(): PaymentDao
}

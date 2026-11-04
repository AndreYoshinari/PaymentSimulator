package br.com.paymentsimulator.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PaymentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(payment: PaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertItems(items: List<PaymentItemEntity>)

    @Query("DELETE FROM payment_items WHERE paymentId = :paymentId")
    abstract suspend fun deleteItems(paymentId: String)

    @Query("SELECT id FROM payments WHERE idempotencyKey = :key LIMIT 1")
    abstract suspend fun findIdByIdempotencyKey(key: String): String?

    @Transaction
    open suspend fun upsert(payment: PaymentEntity, items: List<PaymentItemEntity>) {
        val owner = findIdByIdempotencyKey(payment.idempotencyKey)
        if (owner != null && owner != payment.id) {
            return
        }
        deleteItems(payment.id)
        insert(payment)
        if (items.isNotEmpty()) {
            insertItems(items)
        }
    }

    @Transaction
    @Query("SELECT * FROM payments WHERE id = :id")
    abstract suspend fun findById(id: String): PaymentWithItems?

    @Transaction
    @Query("SELECT * FROM payments WHERE idempotencyKey = :key")
    abstract suspend fun findByIdempotencyKey(key: String): PaymentWithItems?

    @Transaction
    @Query("SELECT * FROM payments ORDER BY createdAtEpochMillis ASC")
    abstract fun observeAll(): Flow<List<PaymentWithItems>>

    @Transaction
    @Query("SELECT * FROM payments WHERE id = :id")
    abstract fun observeById(id: String): Flow<PaymentWithItems?>
}

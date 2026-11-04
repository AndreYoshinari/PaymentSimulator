package br.com.paymentsimulator.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Fictional payment history. The amount is stored in cents.
 * Card numbers, CVV and credentials are never stored.
 */
@Entity(
    tableName = "payments",
    indices = [Index(value = ["idempotencyKey"], unique = true)],
)
data class PaymentEntity(
    @PrimaryKey val id: String,
    val idempotencyKey: String,
    val amountInCents: Long,
    val currency: String,
    val method: String,
    val status: String,
    val createdAtEpochMillis: Long,
)

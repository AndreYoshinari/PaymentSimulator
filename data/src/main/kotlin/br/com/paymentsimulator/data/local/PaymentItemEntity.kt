package br.com.paymentsimulator.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payment_items",
    foreignKeys = [
        ForeignKey(
            entity = PaymentEntity::class,
            parentColumns = ["id"],
            childColumns = ["paymentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("paymentId")],
)
data class PaymentItemEntity(
    @PrimaryKey(autoGenerate = true) val lineId: Long = 0,
    val paymentId: String,
    val productId: String,
    val productName: String,
    val priceInCents: Long,
    val currency: String,
    val quantity: Int,
)

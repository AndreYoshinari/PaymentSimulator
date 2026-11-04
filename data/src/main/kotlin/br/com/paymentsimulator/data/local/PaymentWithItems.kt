package br.com.paymentsimulator.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class PaymentWithItems(
    @Embedded val payment: PaymentEntity,
    @Relation(parentColumn = "id", entityColumn = "paymentId")
    val items: List<PaymentItemEntity>,
)

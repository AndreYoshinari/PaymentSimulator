package br.com.paymentsimulator.domain.model

import java.time.Instant

fun Payment.toReceipt(): PaymentReceipt {
    return PaymentReceipt(
        paymentId = id,
        idempotencyKey = idempotencyKey,
        amount = amount,
        method = method,
        status = status,
        createdAt = createdAt,
    )
}

data class PaymentReceipt(
    val paymentId: PaymentId,
    val idempotencyKey: IdempotencyKey,
    val amount: Money,
    val method: PaymentMethod,
    val status: PaymentStatus,
    val createdAt: Instant,
)

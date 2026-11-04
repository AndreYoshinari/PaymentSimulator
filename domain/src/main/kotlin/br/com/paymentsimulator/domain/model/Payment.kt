package br.com.paymentsimulator.domain.model

import java.time.Instant
import java.util.UUID

@JvmInline
value class PaymentId(val value: UUID)

data class Payment(
    val id: PaymentId,
    val idempotencyKey: IdempotencyKey,
    val cart: Cart,
    val method: PaymentMethod,
    val status: PaymentStatus,
    val createdAt: Instant,
) {
    val amount: Money
        get() = cart.total

    init {
        require(cart.items.isNotEmpty()) { "Payment cart must not be empty." }
        require(amount.amountInCents > 0L) { "Payment amount must be positive." }
    }
}

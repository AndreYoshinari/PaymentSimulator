package br.com.paymentsimulator.domain.model

import java.time.Instant

data class PaymentRequest(
    val cart: Cart,
    val method: PaymentMethod,
) {
    fun start(
        id: PaymentId,
        idempotencyKey: IdempotencyKey,
        createdAt: Instant,
    ): PaymentResult {
        cart.validateForPayment()?.let { return PaymentResult.Failure(it) }
        // A payment enters the flow as pending. Authorization is a later step.
        return PaymentResult.Success(
            Payment(
                id = id,
                idempotencyKey = idempotencyKey,
                cart = cart,
                method = method,
                status = PaymentStatus.PENDING,
                createdAt = createdAt,
            ),
        )
    }
}

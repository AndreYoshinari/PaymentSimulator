package br.com.paymentsimulator.data.remote

data class PaymentRequestDto(
    val idempotencyKey: String,
    val amountInCents: Long,
    val currency: String,
    val method: String,
)

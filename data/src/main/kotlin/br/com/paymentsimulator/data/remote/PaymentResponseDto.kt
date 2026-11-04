package br.com.paymentsimulator.data.remote

data class PaymentResponseDto(
    val idempotencyKey: String,
    val amountInCents: Long,
    val currency: String,
    val method: String,
    val status: String,
)

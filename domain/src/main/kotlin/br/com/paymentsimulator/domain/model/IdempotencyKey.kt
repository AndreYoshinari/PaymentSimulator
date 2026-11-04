package br.com.paymentsimulator.domain.model

@JvmInline
value class IdempotencyKey(val value: String) {
    init {
        require(value.isNotBlank()) { "Idempotency key must not be blank." }
    }
}

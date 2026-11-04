package br.com.paymentsimulator.domain.model

enum class Currency {
    BRL,
}

data class Money(
    val amountInCents: Long,
    val currency: Currency = Currency.BRL,
) {
    init {
        require(amountInCents >= 0L) { "amountInCents must be zero or positive." }
    }

    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "Cannot combine amounts of different currencies." }
        return Money(Math.addExact(amountInCents, other.amountInCents), currency)
    }

    operator fun times(quantity: Int): Money {
        require(quantity >= 0) { "quantity must be zero or positive." }
        return Money(Math.multiplyExact(amountInCents, quantity.toLong()), currency)
    }
}

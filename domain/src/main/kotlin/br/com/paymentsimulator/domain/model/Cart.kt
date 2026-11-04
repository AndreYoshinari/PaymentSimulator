package br.com.paymentsimulator.domain.model

data class Cart(
    val items: List<CartItem> = emptyList(),
) {
    init {
        require(items.distinctBy { it.product.id }.size == items.size) {
            "Cart cannot contain the same product more than once."
        }
    }

    val total: Money
        get() = items.fold(Money(0)) { acc, item -> acc + item.subtotal }

    val itemCount: Int
        get() = items.sumOf { it.quantity }

    fun validateForPayment(): PaymentError? = when {
        items.isEmpty() -> PaymentError.EmptyCart
        total.amountInCents == 0L -> PaymentError.NonPositiveAmount
        else -> null
    }

    fun without(productId: ProductId): Cart {
        return copy(items = items.filterNot { it.product.id == productId })
    }

    fun add(product: Product, quantity: Int = 1): Cart {
        require(quantity > 0) { "Cart item quantity must be greater than zero." }
        val existing = items.firstOrNull { it.product.id == product.id }
        if (existing == null) {
            return copy(items = items + CartItem(product, quantity))
        }
        val merged = existing.copy(quantity = Math.addExact(existing.quantity, quantity))
        return copy(
            items = items.map { item ->
                if (item.product.id == product.id) merged else item
            },
        )
    }
}

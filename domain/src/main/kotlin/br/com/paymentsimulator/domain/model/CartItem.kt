package br.com.paymentsimulator.domain.model

data class CartItem(
    val product: Product,
    val quantity: Int,
) {
    init {
        require(quantity > 0) { "Cart item quantity must be greater than zero." }
    }

    val subtotal: Money
        get() = product.price * quantity
}

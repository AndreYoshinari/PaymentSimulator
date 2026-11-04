package br.com.paymentsimulator.presentation

import br.com.paymentsimulator.domain.model.PaymentError

enum class CartUserMessage {
    InvalidQuantity,
    ProductNotFound,
    ProductNotInCart,
    EmptyCart,
    NonPositiveAmount,
    UpdateFailed,
    ProductsUnavailable,
}

fun PaymentError.toUserMessage(): CartUserMessage {
    return when (this) {
        PaymentError.InvalidQuantity -> CartUserMessage.InvalidQuantity
        PaymentError.ProductNotFound -> CartUserMessage.ProductNotFound
        PaymentError.ProductNotInCart -> CartUserMessage.ProductNotInCart
        PaymentError.EmptyCart -> CartUserMessage.EmptyCart
        PaymentError.NonPositiveAmount -> CartUserMessage.NonPositiveAmount
        else -> CartUserMessage.UpdateFailed
    }
}

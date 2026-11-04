package br.com.paymentsimulator.presentation.cart

import br.com.paymentsimulator.presentation.CartUserMessage

data class CartLineUi(
    val id: String,
    val name: String,
    val quantity: Int,
    val subtotal: String,
)

data class CartContent(
    val lines: List<CartLineUi>,
    val subtotal: String,
    val total: String,
    val canCheckout: Boolean,
    val message: CartUserMessage? = null,
)

sealed interface CartUiState {
    data object Loading : CartUiState

    data class Success(val content: CartContent) : CartUiState

    data object Error : CartUiState
}

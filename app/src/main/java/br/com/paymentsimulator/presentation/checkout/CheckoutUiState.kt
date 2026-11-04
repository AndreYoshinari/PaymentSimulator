package br.com.paymentsimulator.presentation.checkout

import br.com.paymentsimulator.presentation.CartUserMessage

data class CheckoutLineUi(
    val id: String,
    val name: String,
    val quantity: Int,
    val subtotal: String,
)

data class CheckoutContent(
    val lines: List<CheckoutLineUi>,
    val total: String,
    val canConfirm: Boolean,
    val message: CartUserMessage? = null,
)

sealed interface CheckoutUiState {
    data object Loading : CheckoutUiState

    data class Success(val content: CheckoutContent) : CheckoutUiState

    data object Error : CheckoutUiState
}

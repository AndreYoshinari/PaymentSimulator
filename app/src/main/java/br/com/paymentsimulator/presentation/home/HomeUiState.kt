package br.com.paymentsimulator.presentation.home

import br.com.paymentsimulator.presentation.CartUserMessage

data class HomeProductUi(
    val id: String,
    val name: String,
    val description: String,
    val price: String,
    val quantityInCart: Int,
)

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Success(
        val products: List<HomeProductUi>,
        val itemCount: Int,
        val message: CartUserMessage? = null,
    ) : HomeUiState

    data object Error : HomeUiState
}

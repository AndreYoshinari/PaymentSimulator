package br.com.paymentsimulator.presentation.paymentmethod

import br.com.paymentsimulator.presentation.CartUserMessage

enum class PaymentChoice {
    PIX,
    CREDIT_CARD,
}

enum class CardScenarioUi {
    APPROVED,
    DECLINED,
    TIMEOUT,
    UNAVAILABLE,
    DUPLICATE_RESPONSE,
    UNKNOWN_THEN_APPROVED,
}

data class PaymentMethodContent(
    val total: String,
    val choice: PaymentChoice?,
    val scenario: CardScenarioUi,
    val canConfirm: Boolean,
    val confirming: Boolean,
    val message: CartUserMessage? = null,
)

sealed interface PaymentMethodUiState {
    data object Loading : PaymentMethodUiState

    data class Success(val content: PaymentMethodContent) : PaymentMethodUiState

    data object Error : PaymentMethodUiState
}

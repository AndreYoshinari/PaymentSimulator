package br.com.paymentsimulator.presentation.paymentresult

import br.com.paymentsimulator.presentation.payment.PaymentOutcome

data class PaymentResultContent(
    val outcome: PaymentOutcome,
    val paymentId: String,
    val checking: Boolean,
    val canCheckAgain: Boolean,
)

sealed interface PaymentResultUiState {
    data object Loading : PaymentResultUiState

    data class Ready(val content: PaymentResultContent) : PaymentResultUiState

    data object NotFound : PaymentResultUiState
}

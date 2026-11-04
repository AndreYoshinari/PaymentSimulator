package br.com.paymentsimulator.presentation.receipt

import br.com.paymentsimulator.domain.model.PaymentMethod
import br.com.paymentsimulator.domain.model.PaymentStatus

data class ReceiptContent(
    val id: String,
    val createdAt: String,
    val amount: String,
    val method: PaymentMethod,
    val status: PaymentStatus,
    val confirmationCode: String,
)

sealed interface ReceiptUiState {
    data object Loading : ReceiptUiState

    data class Ready(val content: ReceiptContent) : ReceiptUiState

    data object NotFound : ReceiptUiState

    data object Error : ReceiptUiState
}

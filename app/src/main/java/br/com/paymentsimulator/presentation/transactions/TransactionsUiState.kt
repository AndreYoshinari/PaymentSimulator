package br.com.paymentsimulator.presentation.transactions

import br.com.paymentsimulator.domain.model.PaymentMethod
import br.com.paymentsimulator.domain.model.PaymentStatus

data class TransactionDetailsUi(
    val id: String,
    val amount: String,
    val method: PaymentMethod,
    val status: PaymentStatus,
    val createdAt: String,
)

data class TransactionsContent(
    val filter: PaymentStatus?,
    val transactions: List<TransactionDetailsUi>,
    val selected: TransactionDetailsUi?,
    val hasAnyPayment: Boolean,
)

sealed interface TransactionsUiState {
    data object Loading : TransactionsUiState

    data class Success(val content: TransactionsContent) : TransactionsUiState

    data object Error : TransactionsUiState
}

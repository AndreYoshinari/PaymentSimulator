package br.com.paymentsimulator.presentation.processing

sealed interface ProcessingUiState {
    data object Loading : ProcessingUiState

    data class Processing(val paymentId: String) : ProcessingUiState

    data class Resolved(val paymentId: String) : ProcessingUiState

    data object NotFound : ProcessingUiState
}

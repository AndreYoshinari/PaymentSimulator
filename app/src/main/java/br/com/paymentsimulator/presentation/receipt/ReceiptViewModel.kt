package br.com.paymentsimulator.presentation.receipt

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.paymentsimulator.domain.model.PaymentReceipt
import br.com.paymentsimulator.domain.usecase.ObservePaymentReceiptUseCase
import br.com.paymentsimulator.presentation.processing.paymentId
import br.com.paymentsimulator.ui.format.format
import br.com.paymentsimulator.ui.format.formatDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted

@OptIn(ExperimentalCoroutinesApi::class)
class ReceiptViewModel(
    savedStateHandle: SavedStateHandle,
    observeReceipt: ObservePaymentReceiptUseCase,
) : ViewModel() {
    private val paymentId = savedStateHandle.paymentId()
    private val attempts = MutableStateFlow(0)

    val state: StateFlow<ReceiptUiState> = savedStateHandle.paymentId().let { id ->
        if (id == null) {
            MutableStateFlow(ReceiptUiState.NotFound)
        } else {
            attempts.flatMapLatest {
                observeReceipt(id)
                    .map { receipt -> receipt.toState() }
                    .catch { emit(ReceiptUiState.Error) }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ReceiptUiState.Loading,
            )
        }
    }

    fun retry() {
        if (paymentId != null) {
            attempts.value += 1
        }
    }
}

internal fun fictionalConfirmationCode(paymentId: String): String {
    val compact = paymentId.replace("-", "").take(8).uppercase()
    return "SIM-$compact"
}

private fun PaymentReceipt?.toState(): ReceiptUiState {
    if (this == null) {
        return ReceiptUiState.NotFound
    }
    val rawId = paymentId.value.toString()
    return ReceiptUiState.Ready(
        ReceiptContent(
            id = rawId,
            createdAt = createdAt.formatDateTime(),
            amount = amount.format(),
            method = method,
            status = status,
            confirmationCode = fictionalConfirmationCode(rawId),
        ),
    )
}

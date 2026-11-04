package br.com.paymentsimulator.presentation.processing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.paymentsimulator.domain.model.DomainResult
import br.com.paymentsimulator.domain.model.PaymentError
import br.com.paymentsimulator.domain.model.PaymentId
import br.com.paymentsimulator.domain.usecase.GetPaymentStatusUseCase
import br.com.paymentsimulator.presentation.payment.PaymentOutcome
import br.com.paymentsimulator.presentation.payment.toPaymentOutcome
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ProcessingViewModel(
    savedStateHandle: SavedStateHandle,
    private val getPaymentStatus: GetPaymentStatusUseCase,
) : ViewModel() {
    private val paymentId = savedStateHandle.paymentId()
    private val resolvedEvents = MutableSharedFlow<String>(replay = 1)
    private var announced = false

    private val current = MutableStateFlow<ProcessingUiState>(ProcessingUiState.Loading)

    val resolved = resolvedEvents.asSharedFlow()
    val state: StateFlow<ProcessingUiState> = current

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val id = paymentId
        if (id == null) {
            current.value = ProcessingUiState.NotFound
            return
        }
        val rawId = id.value.toString()
        when (val result = getPaymentStatus(id)) {
            is DomainResult.Failure -> {
                if (result.error == PaymentError.PaymentNotFound) {
                    current.value = ProcessingUiState.NotFound
                } else {
                    current.value = ProcessingUiState.Resolved(rawId)
                    announce(rawId)
                }
            }
            is DomainResult.Success -> {
                if (result.toPaymentOutcome() == PaymentOutcome.InProgress) {
                    current.value = ProcessingUiState.Processing(rawId)
                } else {
                    current.value = ProcessingUiState.Resolved(rawId)
                    announce(rawId)
                }
            }
        }
    }

    private fun announce(paymentId: String) {
        if (announced) {
            return
        }
        announced = true
        resolvedEvents.tryEmit(paymentId)
    }
}

internal fun SavedStateHandle.paymentId(): PaymentId? {
    val raw = get<String>("paymentId") ?: return null
    return try {
        PaymentId(UUID.fromString(raw))
    } catch (_: IllegalArgumentException) {
        null
    }
}

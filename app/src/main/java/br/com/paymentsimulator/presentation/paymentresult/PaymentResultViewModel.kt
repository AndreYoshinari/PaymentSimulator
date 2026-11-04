package br.com.paymentsimulator.presentation.paymentresult

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.paymentsimulator.domain.model.DomainResult
import br.com.paymentsimulator.domain.model.PaymentError
import br.com.paymentsimulator.domain.usecase.GetPaymentStatusUseCase
import br.com.paymentsimulator.presentation.payment.canCheckAgain
import br.com.paymentsimulator.presentation.payment.toPaymentOutcome
import br.com.paymentsimulator.presentation.processing.paymentId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PaymentResultViewModel(
    savedStateHandle: SavedStateHandle,
    private val getPaymentStatus: GetPaymentStatusUseCase,
) : ViewModel() {
    private val paymentId = savedStateHandle.paymentId()
    private var checking = false

    private val current = MutableStateFlow<PaymentResultUiState>(PaymentResultUiState.Loading)

    val state: StateFlow<PaymentResultUiState> = current

    init {
        viewModelScope.launch { load() }
    }

    fun checkAgain() {
        val shown = current.value as? PaymentResultUiState.Ready ?: return
        if (!shown.content.canCheckAgain || checking) {
            return
        }
        checking = true
        current.value = PaymentResultUiState.Ready(shown.content.copy(checking = true))
        viewModelScope.launch {
            try {
                load()
            } finally {
                checking = false
            }
        }
    }

    private suspend fun load() {
        val id = paymentId
        if (id == null) {
            current.value = PaymentResultUiState.NotFound
            return
        }
        val rawId = id.value.toString()
        val outcome = when (val result = getPaymentStatus(id)) {
            is DomainResult.Failure -> {
                if (result.error == PaymentError.PaymentNotFound) {
                    current.value = PaymentResultUiState.NotFound
                    return
                }
                result.toPaymentOutcome()
            }
            is DomainResult.Success -> result.toPaymentOutcome()
        } ?: return
        current.value = PaymentResultUiState.Ready(
            PaymentResultContent(
                outcome = outcome,
                paymentId = rawId,
                checking = false,
                canCheckAgain = outcome.canCheckAgain(),
            ),
        )
    }
}

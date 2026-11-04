package br.com.paymentsimulator.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.paymentsimulator.domain.model.Payment
import br.com.paymentsimulator.domain.model.PaymentStatus
import br.com.paymentsimulator.domain.usecase.ObservePaymentHistoryUseCase
import br.com.paymentsimulator.ui.format.format
import br.com.paymentsimulator.ui.format.formatDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModel(
    observeHistory: ObservePaymentHistoryUseCase,
) : ViewModel() {
    private val attempts = MutableStateFlow(0)
    private val filter = MutableStateFlow<PaymentStatus?>(null)
    private val selectedId = MutableStateFlow<String?>(null)

    val state: StateFlow<TransactionsUiState> = attempts.flatMapLatest {
        combine(observeHistory(), filter, selectedId) { payments, selectedFilter, selected ->
            payments.toSuccess(selectedFilter, selected) as TransactionsUiState
        }.catch {
            emit(TransactionsUiState.Error)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionsUiState.Loading,
    )

    fun retry() {
        attempts.value += 1
    }

    fun selectFilter(status: PaymentStatus?) {
        filter.value = status
    }

    fun select(id: String) {
        selectedId.value = id
    }

    private fun List<Payment>.toSuccess(
        selectedFilter: PaymentStatus?,
        selected: String?,
    ): TransactionsUiState.Success {
        val newestFirst = sortedByDescending { it.createdAt }
        val visible = if (selectedFilter == null) {
            newestFirst
        } else {
            newestFirst.filter { it.status == selectedFilter }
        }
        val items = visible.map { it.toDetails() }
        return TransactionsUiState.Success(
            TransactionsContent(
                filter = selectedFilter,
                transactions = items,
                selected = items.firstOrNull { it.id == selected },
                hasAnyPayment = isNotEmpty(),
            ),
        )
    }
}

private fun Payment.toDetails(): TransactionDetailsUi {
    return TransactionDetailsUi(
        id = id.value.toString(),
        amount = amount.format(),
        method = method,
        status = status,
        createdAt = createdAt.formatDateTime(),
    )
}

package br.com.paymentsimulator.presentation.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.paymentsimulator.domain.model.Cart
import br.com.paymentsimulator.domain.repository.CartRepository
import br.com.paymentsimulator.domain.usecase.CalculateCartTotalUseCase
import br.com.paymentsimulator.presentation.CartUserMessage
import br.com.paymentsimulator.presentation.toUserMessage
import br.com.paymentsimulator.ui.format.format
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class CheckoutViewModel(
    cartRepository: CartRepository,
    private val calculateCartTotal: CalculateCartTotalUseCase,
) : ViewModel() {
    private val cart = cartRepository.observe()
        .stateIn(viewModelScope, SharingStarted.Eagerly, Cart())
    private val message = MutableStateFlow<CartUserMessage?>(null)
    private val confirmedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val confirmed = confirmedEvents.asSharedFlow()

    val state: StateFlow<CheckoutUiState> = combine(cart, message) { current, actionMessage ->
        current.toSuccess(actionMessage) as CheckoutUiState
    }.catch {
        emit(CheckoutUiState.Error)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CheckoutUiState.Loading,
    )

    fun confirm() {
        val current = cart.value
        val error = current.validateForPayment()
        if (error != null) {
            message.value = error.toUserMessage()
            return
        }
        message.value = null
        confirmedEvents.tryEmit(Unit)
    }

    private fun Cart.toSuccess(actionMessage: CartUserMessage?): CheckoutUiState.Success {
        return CheckoutUiState.Success(
            CheckoutContent(
                lines = items.map { item ->
                    CheckoutLineUi(
                        id = item.product.id.value.toString(),
                        name = item.product.name,
                        quantity = item.quantity,
                        subtotal = item.subtotal.format(),
                    )
                },
                total = calculateCartTotal(this).format(),
                canConfirm = validateForPayment() == null,
                message = actionMessage,
            ),
        )
    }
}

package br.com.paymentsimulator.presentation.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.paymentsimulator.domain.model.Cart
import br.com.paymentsimulator.domain.model.DomainResult
import br.com.paymentsimulator.domain.model.PaymentError
import br.com.paymentsimulator.domain.repository.CartRepository
import br.com.paymentsimulator.domain.usecase.AddToCartUseCase
import br.com.paymentsimulator.domain.usecase.CalculateCartTotalUseCase
import br.com.paymentsimulator.domain.usecase.ChangeCartQuantityUseCase
import br.com.paymentsimulator.domain.usecase.RemoveFromCartUseCase
import br.com.paymentsimulator.presentation.CartUserMessage
import br.com.paymentsimulator.presentation.home.toProductId
import br.com.paymentsimulator.presentation.toUserMessage
import br.com.paymentsimulator.ui.format.format
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CartViewModel(
    private val cartRepository: CartRepository,
    private val addToCart: AddToCartUseCase,
    private val changeQuantity: ChangeCartQuantityUseCase,
    private val removeFromCart: RemoveFromCartUseCase,
    private val calculateCartTotal: CalculateCartTotalUseCase,
) : ViewModel() {
    private val message = MutableStateFlow<CartUserMessage?>(null)

    val state: StateFlow<CartUiState> = combine(
        cartRepository.observe(),
        message,
    ) { cart, actionMessage ->
        cart.toSuccess(actionMessage) as CartUiState
    }.catch {
        emit(CartUiState.Error)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CartUiState.Loading,
    )

    fun increase(productId: String) {
        val id = productId.toProductId() ?: return showInvalidId()
        viewModelScope.launch {
            apply { cart -> addToCart(cart, id, quantity = 1) }
        }
    }

    fun decrease(productId: String) {
        val id = productId.toProductId() ?: return showInvalidId()
        viewModelScope.launch {
            apply { cart ->
                val quantity = cart.items.firstOrNull { it.product.id == id }?.quantity
                    ?: return@apply DomainResult.Failure(PaymentError.ProductNotInCart)
                changeQuantity(cart, id, quantity - 1)
            }
        }
    }

    fun remove(productId: String) {
        val id = productId.toProductId() ?: return showInvalidId()
        viewModelScope.launch {
            apply { cart -> removeFromCart(cart, id) }
        }
    }

    private suspend fun apply(block: suspend (Cart) -> DomainResult<Cart>) {
        when (val result = cartRepository.modify(block)) {
            is DomainResult.Success -> message.value = null
            is DomainResult.Failure -> message.value = result.error.toUserMessage()
        }
    }

    private fun showInvalidId() {
        message.value = CartUserMessage.UpdateFailed
    }

    private fun Cart.toSuccess(actionMessage: CartUserMessage?): CartUiState.Success {
        val total = calculateCartTotal(this)
        return CartUiState.Success(
            CartContent(
                lines = items.map { item ->
                    CartLineUi(
                        id = item.product.id.value.toString(),
                        name = item.product.name,
                        quantity = item.quantity,
                        subtotal = item.subtotal.format(),
                    )
                },
                subtotal = this.total.format(),
                total = total.format(),
                canCheckout = validateForPayment() == null,
                message = actionMessage,
            ),
        )
    }
}

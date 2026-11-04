package br.com.paymentsimulator.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.paymentsimulator.domain.model.DomainResult
import br.com.paymentsimulator.domain.model.Product
import br.com.paymentsimulator.domain.model.ProductId
import br.com.paymentsimulator.domain.repository.CartRepository
import br.com.paymentsimulator.domain.repository.ProductRepository
import br.com.paymentsimulator.domain.usecase.AddToCartUseCase
import br.com.paymentsimulator.presentation.CartUserMessage
import br.com.paymentsimulator.presentation.toUserMessage
import br.com.paymentsimulator.ui.format.format
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class HomeViewModel(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository,
    private val addToCart: AddToCartUseCase,
) : ViewModel() {
    private val catalog = MutableStateFlow<CatalogState>(CatalogState.Loading)
    private val message = MutableStateFlow<CartUserMessage?>(null)

    val state: StateFlow<HomeUiState> = combine(
        catalog,
        cartRepository.observe(),
        message,
    ) { catalogState, cart, actionMessage ->
        when (catalogState) {
            CatalogState.Loading -> HomeUiState.Loading
            CatalogState.Failed -> HomeUiState.Error
            is CatalogState.Ready -> HomeUiState.Success(
                products = catalogState.products.map { product ->
                    product.toUi(quantityInCart = cart.items
                        .firstOrNull { it.product.id == product.id }
                        ?.quantity ?: 0)
                },
                itemCount = cart.itemCount,
                message = actionMessage,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState.Loading,
    )

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            catalog.value = CatalogState.Loading
            catalog.value = try {
                CatalogState.Ready(productRepository.getAll())
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                CatalogState.Failed
            }
        }
    }

    fun add(productId: String) {
        val id = productId.toProductId() ?: run {
            message.value = CartUserMessage.UpdateFailed
            return
        }
        viewModelScope.launch {
            when (val result = cartRepository.modify { cart -> addToCart(cart, id, quantity = 1) }) {
                is DomainResult.Success -> message.value = null
                is DomainResult.Failure -> message.value = result.error.toUserMessage()
            }
        }
    }

    private fun Product.toUi(quantityInCart: Int): HomeProductUi {
        return HomeProductUi(
            id = id.value.toString(),
            name = name,
            description = description,
            price = price.format(),
            quantityInCart = quantityInCart,
        )
    }

    private sealed interface CatalogState {
        data object Loading : CatalogState

        data class Ready(val products: List<Product>) : CatalogState

        data object Failed : CatalogState
    }
}

internal fun String.toProductId(): ProductId? {
    return runCatching { ProductId(UUID.fromString(this)) }.getOrNull()
}

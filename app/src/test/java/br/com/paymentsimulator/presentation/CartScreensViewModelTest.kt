package br.com.paymentsimulator.presentation

import app.cash.turbine.test
import br.com.paymentsimulator.data.repository.InMemoryCartRepository
import br.com.paymentsimulator.domain.model.Money
import br.com.paymentsimulator.domain.model.Product
import br.com.paymentsimulator.domain.model.ProductId
import br.com.paymentsimulator.domain.repository.ProductRepository
import br.com.paymentsimulator.domain.usecase.AddToCartUseCase
import br.com.paymentsimulator.domain.usecase.CalculateCartTotalUseCase
import br.com.paymentsimulator.domain.usecase.ChangeCartQuantityUseCase
import br.com.paymentsimulator.domain.usecase.RemoveFromCartUseCase
import br.com.paymentsimulator.presentation.cart.CartUiState
import br.com.paymentsimulator.presentation.cart.CartViewModel
import br.com.paymentsimulator.presentation.home.HomeUiState
import br.com.paymentsimulator.presentation.home.HomeViewModel
import br.com.paymentsimulator.ui.format.format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class CartScreensViewModelTest {
    private val coffee = Product(
        id = ProductId(UUID.fromString("11111111-1111-1111-1111-111111111111")),
        name = "Café",
        description = "Café coado",
        price = Money(500),
    )
    private val tea = Product(
        id = ProductId(UUID.fromString("22222222-2222-2222-2222-222222222222")),
        name = "Chá",
        description = "Chá preto",
        price = Money(300),
    )
    private val catalog = FakeProductRepository(listOf(coffee, tea))
    private val cartRepository = InMemoryCartRepository()
    private val addToCart = AddToCartUseCase(catalog)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun formatsMoneyFromCents() {
        assertEquals("R$ 5,00", Money(500).format())
        assertEquals("R$ 1.500,00", Money(150_000).format())
    }

    @Test
    fun homeShowsProductsAndAddsThemToTheSharedCart() = kotlinx.coroutines.test.runTest {
        val viewModel = HomeViewModel(catalog, cartRepository, addToCart)

        viewModel.state.test {
            val ready = awaitSuccess()
            assertEquals(listOf("Café", "Chá"), ready.products.map { it.name })
            assertEquals(listOf("Café coado", "Chá preto"), ready.products.map { it.description })
            assertEquals("R$ 5,00", ready.products.first().price)
            assertEquals(0, ready.itemCount)

            viewModel.add(coffee.id.value.toString())
            val updated = awaitSuccess()
            assertEquals(1, updated.itemCount)
            assertEquals(1, updated.products.first().quantityInCart)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun homeShowsAnErrorWhenTheCatalogFails() = kotlinx.coroutines.test.runTest {
        val viewModel = HomeViewModel(FailingProductRepository(), cartRepository, addToCart)

        viewModel.state.test {
            assertEquals(HomeUiState.Error, awaitItemAfterLoading())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun cartUpdatesQuantitiesSubtotalAndTotal() = kotlinx.coroutines.test.runTest {
        val viewModel = CartViewModel(
            cartRepository = cartRepository,
            addToCart = addToCart,
            changeQuantity = ChangeCartQuantityUseCase(),
            removeFromCart = RemoveFromCartUseCase(),
            calculateCartTotal = CalculateCartTotalUseCase(),
        )

        viewModel.state.test {
            val empty = awaitCart()
            assertTrue(empty.lines.isEmpty())
            assertEquals("R$ 0,00", empty.subtotal)
            assertEquals("R$ 0,00", empty.total)
            assertFalse(empty.canCheckout)

            viewModel.increase(coffee.id.value.toString())
            viewModel.increase(coffee.id.value.toString())
            viewModel.increase(tea.id.value.toString())
            val filled = awaitCart { it.lines.size == 2 && it.lines.first().quantity == 2 }
            assertEquals("R$ 10,00", filled.lines.first().subtotal)
            assertEquals("R$ 3,00", filled.lines.last().subtotal)
            assertEquals("R$ 13,00", filled.subtotal)
            assertEquals("R$ 13,00", filled.total)
            assertTrue(filled.canCheckout)

            viewModel.decrease(coffee.id.value.toString())
            val decreased = awaitCart { it.lines.first().quantity == 1 }
            assertEquals("R$ 5,00", decreased.lines.first().subtotal)
            assertEquals("R$ 8,00", decreased.total)

            viewModel.decrease(coffee.id.value.toString())
            val rejected = awaitCart { it.message == CartUserMessage.InvalidQuantity }
            assertEquals(1, rejected.lines.first().quantity)

            viewModel.remove(coffee.id.value.toString())
            val removed = awaitCart { it.lines.size == 1 && it.message == null }
            assertEquals(tea.name, removed.lines.single().name)
            assertEquals("R$ 3,00", removed.total)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<HomeUiState>.awaitSuccess(): HomeUiState.Success {
        val item = awaitItemAfterLoading()
        assertTrue(item is HomeUiState.Success)
        return item as HomeUiState.Success
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<HomeUiState>.awaitItemAfterLoading(): HomeUiState {
        val first = awaitItem()
        return if (first is HomeUiState.Loading) awaitItem() else first
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<CartUiState>.awaitCart(
        match: (br.com.paymentsimulator.presentation.cart.CartContent) -> Boolean = { true },
    ): br.com.paymentsimulator.presentation.cart.CartContent {
        while (true) {
            val item = awaitItem()
            if (item is CartUiState.Success && match(item.content)) {
                return item.content
            }
        }
    }
}

private class FakeProductRepository(
    private val products: List<Product>,
) : ProductRepository {
    override suspend fun getAll(): List<Product> = products

    override suspend fun findById(id: ProductId): Product? = products.find { it.id == id }
}

private class FailingProductRepository : ProductRepository {
    override suspend fun getAll(): List<Product> = error("catalog unavailable")

    override suspend fun findById(id: ProductId): Product? = null
}

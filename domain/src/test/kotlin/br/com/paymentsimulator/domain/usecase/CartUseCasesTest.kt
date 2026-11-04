package br.com.paymentsimulator.domain.usecase

import br.com.paymentsimulator.domain.model.Cart
import br.com.paymentsimulator.domain.model.CartItem
import br.com.paymentsimulator.domain.model.DomainResult
import br.com.paymentsimulator.domain.model.PaymentError
import br.com.paymentsimulator.domain.model.product
import br.com.paymentsimulator.domain.usecase.ChangeCartQuantityUseCase
import br.com.paymentsimulator.domain.repository.InMemoryProductRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CartUseCasesTest {
    private val coffee = product(name = "Coffee", priceInCents = 500)
    private val tea = product(
        name = "Tea",
        priceInCents = 300,
        id = "22222222-2222-2222-2222-222222222222",
    )
    private val catalog = InMemoryProductRepository(listOf(coffee, tea))
    private val addToCart = AddToCartUseCase(catalog)
    private val removeFromCart = RemoveFromCartUseCase()
    private val changeQuantity = ChangeCartQuantityUseCase()
    private val calculateTotal = CalculateCartTotalUseCase()

    @Test
    fun addIncludesTheCatalogPrice() {
        runBlocking {
            val result = addToCart(Cart(), coffee.id, quantity = 2)

            assertTrue(result is DomainResult.Success)
            val cart = (result as DomainResult.Success).value
            assertEquals(1, cart.items.size)
            assertEquals(2, cart.items.single().quantity)
            assertEquals(1_000L, calculateTotal(cart).amountInCents)
        }
    }

    @Test
    fun addMergesTheSameProduct() {
        runBlocking {
            val once = (addToCart(Cart(), coffee.id, quantity = 1) as DomainResult.Success).value
            val twice = addToCart(once, coffee.id, quantity = 3)

            assertTrue(twice is DomainResult.Success)
            val cart = (twice as DomainResult.Success).value
            assertEquals(1, cart.items.size)
            assertEquals(4, cart.items.single().quantity)
            assertEquals(2_000L, calculateTotal(cart).amountInCents)
        }
    }

    @Test
    fun addRejectsUnknownProductAndNonPositiveQuantity() {
        runBlocking {
            val missing = product(id = "33333333-3333-3333-3333-333333333333")

            assertEquals(
                DomainResult.Failure(PaymentError.ProductNotFound),
                addToCart(Cart(), missing.id, quantity = 1),
            )
            assertEquals(
                DomainResult.Failure(PaymentError.InvalidQuantity),
                addToCart(Cart(), coffee.id, quantity = 0),
            )
        }
    }

    @Test
    fun removeDropsOnlyTheSelectedProduct() {
        val cart = Cart(listOf(CartItem(coffee, 1), CartItem(tea, 2)))

        val result = removeFromCart(cart, coffee.id)

        assertTrue(result is DomainResult.Success)
        val updated = (result as DomainResult.Success).value
        assertEquals(listOf(tea.id), updated.items.map { it.product.id })
        assertEquals(600L, calculateTotal(updated).amountInCents)
    }

    @Test
    fun removeRejectsAProductThatIsNotInTheCart() {
        val cart = Cart(listOf(CartItem(coffee, 1)))

        assertEquals(
            DomainResult.Failure(PaymentError.ProductNotInCart),
            removeFromCart(cart, tea.id),
        )
        assertEquals(1, cart.items.size)
    }

    @Test
    fun totalOfAnEmptyCartIsZero() {
        assertEquals(0L, calculateTotal(Cart()).amountInCents)
    }

    @Test
    fun changeQuantityUpdatesTheLineAndTheTotal() {
        val cart = Cart(listOf(CartItem(coffee, 2), CartItem(tea, 1)))

        val result = changeQuantity(cart, coffee.id, quantity = 3)

        assertTrue(result is DomainResult.Success)
        val updated = (result as DomainResult.Success).value
        assertEquals(3, updated.items.first { it.product.id == coffee.id }.quantity)
        assertEquals(1_800L, calculateTotal(updated).amountInCents)
        assertEquals(4, updated.itemCount)
    }

    @Test
    fun changeQuantityRejectsZeroAndMissingProducts() {
        val cart = Cart(listOf(CartItem(coffee, 2)))

        assertEquals(
            DomainResult.Failure(PaymentError.InvalidQuantity),
            changeQuantity(cart, coffee.id, quantity = 0),
        )
        assertEquals(
            DomainResult.Failure(PaymentError.ProductNotInCart),
            changeQuantity(cart, tea.id, quantity = 1),
        )
        assertEquals(2, cart.items.single().quantity)
    }
}

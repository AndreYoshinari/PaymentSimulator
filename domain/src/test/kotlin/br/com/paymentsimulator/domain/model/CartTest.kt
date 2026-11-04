package br.com.paymentsimulator.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CartTest {
    @Test
    fun emptyCartTotalsZero() {
        assertEquals(0L, Cart().total.amountInCents)
        assertEquals(0, Cart().itemCount)
        assertEquals(Currency.BRL, Cart().total.currency)
    }

    @Test
    fun itemSubtotalIsPriceTimesQuantity() {
        val item = CartItem(product(priceInCents = 500), quantity = 4)

        assertEquals(2_000L, item.subtotal.amountInCents)
    }

    @Test
    fun rejectsQuantityBelowOne() {
        assertThrows(IllegalArgumentException::class.java) {
            CartItem(product(), quantity = 0)
        }
    }

    @Test
    fun totalsEveryLine() {
        val cart = Cart(
            listOf(
                CartItem(product(priceInCents = 500), quantity = 2),
                CartItem(
                    product(
                        name = "Tea",
                        priceInCents = 300,
                        id = "22222222-2222-2222-2222-222222222222",
                    ),
                    quantity = 1,
                ),
            ),
        )

        assertEquals(1_300L, cart.total.amountInCents)
        assertEquals(3, cart.itemCount)
    }

    @Test
    fun rejectsDuplicateProduct() {
        val coffee = product()

        assertThrows(IllegalArgumentException::class.java) {
            Cart(listOf(CartItem(coffee, 1), CartItem(coffee, 2)))
        }
    }

    @Test
    fun allowsSameNameWhenIdsDiffer() {
        val cart = Cart(
            listOf(
                CartItem(product(name = "Coffee"), 1),
                CartItem(
                    product(
                        name = "Coffee",
                        priceInCents = 300,
                        id = "22222222-2222-2222-2222-222222222222",
                    ),
                    1,
                ),
            ),
        )

        assertEquals(2, cart.items.size)
    }

    @Test
    fun addMergesQuantityAndKeepsTheOriginalPrice() {
        val coffee = product(priceInCents = 500)
        val cart = Cart()
            .add(coffee, quantity = 1)
            .add(coffee.copy(price = Money(999)), quantity = 2)

        assertEquals(1, cart.items.size)
        assertEquals(3, cart.items.single().quantity)
        assertEquals(500L, cart.items.single().product.price.amountInCents)
        assertEquals(1_500L, cart.total.amountInCents)
    }

    @Test
    fun addRejectsNonPositiveQuantity() {
        assertThrows(IllegalArgumentException::class.java) {
            Cart().add(product(), quantity = 0)
        }
        val cart = Cart(listOf(CartItem(product(), quantity = 2)))
        assertThrows(IllegalArgumentException::class.java) {
            cart.add(product(), quantity = -1)
        }
    }

    @Test
    fun addRejectsQuantityOverflow() {
        val coffee = product()
        val cart = Cart(listOf(CartItem(coffee, Int.MAX_VALUE)))

        assertThrows(ArithmeticException::class.java) {
            cart.add(coffee, quantity = 1)
        }
    }

    @Test
    fun addAppendsADifferentProduct() {
        val cart = Cart()
            .add(product())
            .add(product(name = "Tea", id = "22222222-2222-2222-2222-222222222222"))

        assertEquals(2, cart.items.size)
        assertTrue(cart.items.map { it.product.name }.containsAll(listOf("Coffee", "Tea")))
    }
}

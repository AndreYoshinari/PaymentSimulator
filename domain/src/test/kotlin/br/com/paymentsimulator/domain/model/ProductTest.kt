package br.com.paymentsimulator.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.UUID

class ProductTest {
    @Test
    fun rejectsBlankName() {
        assertThrows(IllegalArgumentException::class.java) {
            product(name = "   ")
        }
    }

    @Test
    fun allowsZeroPrice() {
        val product = product(priceInCents = 0)

        assertEquals(0L, product.price.amountInCents)
    }

    @Test
    fun keepsTheGivenId() {
        val id = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")

        val product = product(id = id.toString())

        assertEquals(id, product.id.value)
    }
}

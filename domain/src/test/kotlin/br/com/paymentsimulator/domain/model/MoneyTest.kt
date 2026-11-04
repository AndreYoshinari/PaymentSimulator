package br.com.paymentsimulator.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MoneyTest {
    @Test
    fun rejectsNegativeAmount() {
        assertThrows(IllegalArgumentException::class.java) {
            Money(-1)
        }
    }

    @Test
    fun addsAmountsInCents() {
        val total = Money(1_250) + Money(750)

        assertEquals(2_000L, total.amountInCents)
        assertEquals(Currency.BRL, total.currency)
    }

    @Test
    fun multipliesAmountByQuantity() {
        val subtotal = Money(1_250) * 3

        assertEquals(3_750L, subtotal.amountInCents)
    }

    @Test
    fun rejectsNegativeQuantity() {
        assertThrows(IllegalArgumentException::class.java) {
            Money(1_250) * -1
        }
    }

    @Test
    fun rejectsAdditionOverflow() {
        assertThrows(ArithmeticException::class.java) {
            Money(Long.MAX_VALUE) + Money(1)
        }
    }

    @Test
    fun rejectsMultiplicationOverflow() {
        assertThrows(ArithmeticException::class.java) {
            Money(Long.MAX_VALUE) * 2
        }
    }
}

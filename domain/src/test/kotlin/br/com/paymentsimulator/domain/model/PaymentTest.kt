package br.com.paymentsimulator.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.UUID

class PaymentTest {
    private val createdAt: Instant = Instant.parse("2026-01-15T12:00:00Z")
    private val paymentId: PaymentId = PaymentId(
        UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"),
    )

    @Test
    fun methodsArePixAndCreditCard() {
        assertEquals(
            setOf(PaymentMethod.PIX, PaymentMethod.CREDIT_CARD),
            PaymentMethod.entries.toSet(),
        )
    }

    @Test
    fun statusesCoverThePaymentFlow() {
        assertEquals(
            setOf(
                PaymentStatus.PENDING,
                PaymentStatus.PROCESSING,
                PaymentStatus.APPROVED,
                PaymentStatus.DECLINED,
                PaymentStatus.FAILED,
                PaymentStatus.UNKNOWN,
            ),
            PaymentStatus.entries.toSet(),
        )
    }

    @Test
    fun startCreatesPendingPaymentWithTheGivenIdAndInstant() {
        val cart = Cart(listOf(CartItem(product(priceInCents = 1_500), quantity = 2)))
        val request = PaymentRequest(cart, PaymentMethod.PIX)

        val result = request.start(paymentId, IdempotencyKey("payment-1"), createdAt)

        assertTrue(result is PaymentResult.Success)
        val payment = (result as PaymentResult.Success).payment
        assertEquals(paymentId, payment.id)
        assertEquals(IdempotencyKey("payment-1"), payment.idempotencyKey)
        assertEquals(createdAt, payment.createdAt)
        assertEquals(PaymentMethod.PIX, payment.method)
        assertEquals(PaymentStatus.PENDING, payment.status)
        assertEquals(3_000L, payment.amount.amountInCents)
        assertEquals(cart.total, payment.amount)
    }

    @Test
    fun startAcceptsCreditCardWithoutCardData() {
        val request = PaymentRequest(
            cart = Cart(listOf(CartItem(product(), quantity = 1))),
            method = PaymentMethod.CREDIT_CARD,
        )

        val result = request.start(paymentId, IdempotencyKey("payment-1"), createdAt)

        assertTrue(result is PaymentResult.Success)
        assertEquals(
            PaymentMethod.CREDIT_CARD,
            (result as PaymentResult.Success).payment.method,
        )
    }

    @Test
    fun startRejectsEmptyCart() {
        val result = PaymentRequest(Cart(), PaymentMethod.PIX)
            .start(paymentId, IdempotencyKey("payment-1"), createdAt)

        assertTrue(result is PaymentResult.Failure)
        assertEquals(PaymentError.EmptyCart, (result as PaymentResult.Failure).error)
    }

    @Test
    fun startRejectsZeroAmount() {
        val cart = Cart(listOf(CartItem(product(priceInCents = 0), quantity = 2)))

        val result = PaymentRequest(cart, PaymentMethod.CREDIT_CARD)
            .start(paymentId, IdempotencyKey("payment-1"), createdAt)

        assertTrue(result is PaymentResult.Failure)
        assertEquals(PaymentError.NonPositiveAmount, (result as PaymentResult.Failure).error)
    }

    @Test
    fun paymentRejectsEmptyCart() {
        assertThrows(IllegalArgumentException::class.java) {
            Payment(
                id = paymentId,
                idempotencyKey = IdempotencyKey("payment-1"),
                cart = Cart(),
                method = PaymentMethod.PIX,
                status = PaymentStatus.PENDING,
                createdAt = createdAt,
            )
        }
    }

    @Test
    fun paymentRejectsZeroAmount() {
        val cart = Cart(listOf(CartItem(product(priceInCents = 0), quantity = 1)))

        assertThrows(IllegalArgumentException::class.java) {
            Payment(
                id = paymentId,
                idempotencyKey = IdempotencyKey("payment-1"),
                cart = cart,
                method = PaymentMethod.PIX,
                status = PaymentStatus.APPROVED,
                createdAt = createdAt,
            )
        }
    }

    @Test
    fun paymentCanRepresentEveryStatus() {
        val payment = startedPayment()

        PaymentStatus.entries.forEach { status ->
            assertEquals(status, payment.copy(status = status).status)
        }
    }

    @Test
    fun paymentIdsWithTheSameUuidAreEqual() {
        val uuid = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd")

        assertEquals(PaymentId(uuid), PaymentId(uuid))
    }

    private fun startedPayment(): Payment {
        val result = PaymentRequest(
            cart = Cart(listOf(CartItem(product(), quantity = 1))),
            method = PaymentMethod.PIX,
        ).start(paymentId, IdempotencyKey("payment-1"), createdAt)
        return (result as PaymentResult.Success).payment
    }
}

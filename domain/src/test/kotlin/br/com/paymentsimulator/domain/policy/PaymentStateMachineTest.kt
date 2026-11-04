package br.com.paymentsimulator.domain.policy

import br.com.paymentsimulator.domain.model.Cart
import br.com.paymentsimulator.domain.model.CartItem
import br.com.paymentsimulator.domain.model.IdempotencyKey
import br.com.paymentsimulator.domain.model.Payment
import br.com.paymentsimulator.domain.model.PaymentError
import br.com.paymentsimulator.domain.model.PaymentId
import br.com.paymentsimulator.domain.model.PaymentMethod
import br.com.paymentsimulator.domain.model.PaymentResult
import br.com.paymentsimulator.domain.model.PaymentStatus
import br.com.paymentsimulator.domain.model.product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.UUID

class PaymentStateMachineTest {
    private val allowedTransitions = setOf(
        PaymentStatus.PENDING to PaymentStatus.PROCESSING,
        PaymentStatus.PROCESSING to PaymentStatus.APPROVED,
        PaymentStatus.PROCESSING to PaymentStatus.DECLINED,
        PaymentStatus.PROCESSING to PaymentStatus.FAILED,
        PaymentStatus.PROCESSING to PaymentStatus.UNKNOWN,
        PaymentStatus.UNKNOWN to PaymentStatus.APPROVED,
        PaymentStatus.UNKNOWN to PaymentStatus.DECLINED,
    )

    @Test
    fun allowsOnlyTheDefinedTransitions() {
        eachTransition { from, to ->
            assertEquals(
                "$from -> $to",
                from to to in allowedTransitions,
                PaymentStateMachine.canTransition(from, to),
            )
        }
    }

    @Test
    fun appliesValidTransitionsAndRejectsTheOthers() {
        eachTransition { from, to ->
            val payment = payment(from)
            val result = PaymentStateMachine.transition(payment, to)

            if (from to to in allowedTransitions) {
                assertTrue("$from -> $to", result is PaymentResult.Success)
                val updated = (result as PaymentResult.Success).payment
                assertEquals(to, updated.status)
                assertEquals(payment.id, updated.id)
                assertEquals(payment.cart, updated.cart)
                assertEquals(payment.method, updated.method)
                assertEquals(payment.createdAt, updated.createdAt)
                assertEquals(payment.idempotencyKey, updated.idempotencyKey)
            } else {
                assertTrue("$from -> $to", result is PaymentResult.Failure)
                assertEquals(
                    PaymentError.InvalidStatusTransition(from, to),
                    (result as PaymentResult.Failure).error,
                )
            }
            assertEquals(from, payment.status)
        }
    }

    private fun eachTransition(assertTransition: (PaymentStatus, PaymentStatus) -> Unit) {
        for (from in PaymentStatus.entries) {
            for (to in PaymentStatus.entries) {
                assertTransition(from, to)
            }
        }
    }

    private fun payment(status: PaymentStatus): Payment {
        return Payment(
            id = PaymentId(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc")),
            idempotencyKey = IdempotencyKey("payment-1"),
            cart = Cart(listOf(CartItem(product(), quantity = 1))),
            method = PaymentMethod.PIX,
            status = status,
            createdAt = Instant.parse("2026-01-15T12:00:00Z"),
        )
    }
}

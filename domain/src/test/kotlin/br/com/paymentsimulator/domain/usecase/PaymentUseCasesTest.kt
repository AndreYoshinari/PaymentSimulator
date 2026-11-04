package br.com.paymentsimulator.domain.usecase

import br.com.paymentsimulator.domain.model.Cart
import br.com.paymentsimulator.domain.model.CartItem
import br.com.paymentsimulator.domain.model.DomainResult
import br.com.paymentsimulator.domain.model.IdempotencyKey
import br.com.paymentsimulator.domain.model.Payment
import br.com.paymentsimulator.domain.model.PaymentError
import br.com.paymentsimulator.domain.model.PaymentId
import br.com.paymentsimulator.domain.model.PaymentMethod
import br.com.paymentsimulator.domain.model.PaymentStatus
import br.com.paymentsimulator.domain.model.product
import br.com.paymentsimulator.domain.repository.InMemoryPaymentRepository
import br.com.paymentsimulator.domain.repository.PaymentIdGenerator
import br.com.paymentsimulator.domain.repository.TimeProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.UUID

class PaymentUseCasesTest {
    private val createdAt: Instant = Instant.parse("2026-01-15T12:00:00Z")
    private val paymentId: PaymentId = PaymentId(
        UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"),
    )
    private val otherId: PaymentId = PaymentId(
        UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"),
    )
    private val key: IdempotencyKey = IdempotencyKey("operation-1")
    private val repository = InMemoryPaymentRepository()
    private val createPayment = CreatePaymentUseCase(
        paymentRepository = repository,
        paymentIds = PaymentIdGenerator { paymentId },
        clock = TimeProvider { createdAt },
    )
    private val processPayment = ProcessPaymentUseCase(repository)
    private val retryPayment = RetryPaymentUseCase(repository)
    private val getStatus = GetPaymentStatusUseCase(repository)
    private val getReceipt = GetPaymentReceiptUseCase(repository)

    @Test
    fun createRejectsEmptyCartAndZeroAmount() {
        runBlocking {
            assertEquals(
                DomainResult.Failure(PaymentError.EmptyCart),
                createPayment(Cart(), PaymentMethod.PIX, key),
            )
            assertEquals(
                DomainResult.Failure(PaymentError.NonPositiveAmount),
                createPayment(cart(cents = 0), PaymentMethod.CREDIT_CARD, key),
            )
            assertTrue(repository.payments.isEmpty())
        }
    }

    @Test
    fun createStoresAPendingPayment() {
        runBlocking {
            val result = createPayment(cart(cents = 1_500, quantity = 2), PaymentMethod.PIX, key)

            assertTrue(result is DomainResult.Success)
            val payment = (result as DomainResult.Success).value
            assertEquals(paymentId, payment.id)
            assertEquals(key, payment.idempotencyKey)
            assertEquals(PaymentStatus.PENDING, payment.status)
            assertEquals(3_000L, payment.amount.amountInCents)
            assertEquals(createdAt, payment.createdAt)
            assertEquals(payment, repository.findById(paymentId))
        }
    }

    @Test
    fun repeatingTheSameOperationKeepsTheOriginalIdAndKey() {
        runBlocking {
            var generated = 0
            val create = CreatePaymentUseCase(
                paymentRepository = repository,
                paymentIds = PaymentIdGenerator {
                    generated += 1
                    if (generated == 1) paymentId else otherId
                },
                clock = TimeProvider { createdAt },
            )
            val first = create(cart(), PaymentMethod.PIX, key) as DomainResult.Success
            val second = create(cart(cents = 50), PaymentMethod.CREDIT_CARD, key) as DomainResult.Success

            assertEquals(first.value, second.value)
            assertEquals(paymentId, second.value.id)
            assertEquals(key, second.value.idempotencyKey)
            assertEquals(PaymentMethod.PIX, second.value.method)
            assertEquals(1, generated)
            assertEquals(1, repository.payments.size)
        }
    }

    @Test
    fun processMovesPendingToProcessingAndKeepsTheSameIdentity() {
        runBlocking {
            val created = (createPayment(cart(), PaymentMethod.CREDIT_CARD, key) as DomainResult.Success).value

            val first = processPayment(created.id)
            val second = processPayment(created.id)

            assertTrue(first is DomainResult.Success)
            val processing = (first as DomainResult.Success).value
            assertEquals(PaymentStatus.PROCESSING, processing.status)
            assertEquals(created.id, processing.id)
            assertEquals(created.idempotencyKey, processing.idempotencyKey)
            assertEquals(processing, (second as DomainResult.Success).value)
            assertEquals(2, repository.saveCount)
        }
    }

    @Test
    fun processLeavesDeclinedAndFailedPaymentsUnchanged() {
        runBlocking {
            repository.save(stored(PaymentStatus.DECLINED))
            assertEquals(
                DomainResult.Failure(
                    PaymentError.InvalidStatusTransition(
                        from = PaymentStatus.DECLINED,
                        to = PaymentStatus.PROCESSING,
                    ),
                ),
                processPayment(paymentId),
            )
            assertEquals(PaymentStatus.DECLINED, getStatus(paymentId).let { (it as DomainResult.Success).value })

            repository.save(stored(PaymentStatus.FAILED))
            assertEquals(
                DomainResult.Failure(
                    PaymentError.InvalidStatusTransition(
                        from = PaymentStatus.FAILED,
                        to = PaymentStatus.PROCESSING,
                    ),
                ),
                processPayment(paymentId),
            )
            assertEquals(PaymentStatus.FAILED, repository.findById(paymentId)?.status)
            assertEquals(paymentId, repository.findById(paymentId)?.id)
            assertEquals(2, repository.saveCount)
        }
    }

    @Test
    fun processRejectsAnApprovedPayment() {
        runBlocking {
            repository.save(stored(PaymentStatus.APPROVED))

            val result = processPayment(paymentId)

            assertEquals(DomainResult.Failure(PaymentError.AlreadyApproved), result)
            assertEquals(PaymentStatus.APPROVED, repository.findById(paymentId)?.status)
            assertEquals(1, repository.saveCount)
        }
    }

    @Test
    fun processDoesNotGuessAnUnknownOutcome() {
        runBlocking {
            repository.save(stored(PaymentStatus.UNKNOWN))

            val result = processPayment(paymentId)

            assertEquals(
                DomainResult.Failure(
                    PaymentError.InvalidStatusTransition(
                        from = PaymentStatus.UNKNOWN,
                        to = PaymentStatus.PROCESSING,
                    ),
                ),
                result,
            )
            assertEquals(PaymentStatus.UNKNOWN, repository.findById(paymentId)?.status)
        }
    }

    @Test
    fun retryOfPendingReturnsTheSamePaymentAndRetryOfDeclinedChangesNothing() {
        runBlocking {
            val pending = stored(PaymentStatus.PENDING)
            repository.save(pending)
            val retried = retryPayment(key)

            assertEquals(DomainResult.Success(pending), retried)
            assertEquals(PaymentStatus.PENDING, repository.findById(paymentId)?.status)
            assertEquals(1, repository.saveCount)

            repository.save(stored(PaymentStatus.DECLINED))
            assertEquals(
                DomainResult.Failure(
                    PaymentError.InvalidStatusTransition(
                        from = PaymentStatus.DECLINED,
                        to = PaymentStatus.PROCESSING,
                    ),
                ),
                retryPayment(key),
            )
            assertEquals(PaymentStatus.DECLINED, repository.findById(paymentId)?.status)
            assertEquals(key, repository.findById(paymentId)?.idempotencyKey)
            assertEquals(1, repository.payments.size)
        }
    }

    @Test
    fun differentIdempotencyKeysCreateSeparatePayments() {
        runBlocking {
            var generated = 0
            val create = CreatePaymentUseCase(
                paymentRepository = repository,
                paymentIds = PaymentIdGenerator {
                    generated += 1
                    if (generated == 1) paymentId else otherId
                },
                clock = TimeProvider { createdAt },
            )
            val secondKey = IdempotencyKey("operation-2")

            val first = create(cart(cents = 1_000), PaymentMethod.PIX, key) as DomainResult.Success
            val second = create(cart(cents = 2_000), PaymentMethod.CREDIT_CARD, secondKey) as DomainResult.Success

            assertEquals(paymentId, first.value.id)
            assertEquals(otherId, second.value.id)
            assertEquals(1_000L, first.value.amount.amountInCents)
            assertEquals(2_000L, second.value.amount.amountInCents)
            assertEquals(PaymentMethod.CREDIT_CARD, second.value.method)
            assertEquals(2, generated)
            assertEquals(2, repository.payments.size)
        }
    }

    @Test
    fun retryOfUnknownIsRejectedAndKeepsTheOriginalPayment() {
        runBlocking {
            val original = stored(PaymentStatus.UNKNOWN)
            repository.save(original)

            val result = retryPayment(key)

            assertEquals(DomainResult.Failure(PaymentError.BlindRetryNotAllowed), result)
            assertEquals(original, repository.findByIdempotencyKey(key))
            assertEquals(1, repository.payments.size)
            assertEquals(1, repository.saveCount)
        }
    }

    @Test
    fun retryOfAnOpenOperationReturnsTheSamePayment() {
        runBlocking {
            val original = stored(PaymentStatus.PROCESSING)
            repository.save(original)

            val result = retryPayment(key)

            assertEquals(DomainResult.Success(original), result)
            assertEquals(original.id, (result as DomainResult.Success).value.id)
            assertEquals(original.idempotencyKey, result.value.idempotencyKey)
        }
    }

    @Test
    fun retryOfApprovedOrFailedDoesNotOpenAnotherAttempt() {
        runBlocking {
            repository.save(stored(PaymentStatus.APPROVED))
            assertEquals(
                DomainResult.Failure(PaymentError.AlreadyApproved),
                retryPayment(key),
            )

            repository.save(stored(PaymentStatus.FAILED))
            assertEquals(
                DomainResult.Failure(
                    PaymentError.InvalidStatusTransition(
                        from = PaymentStatus.FAILED,
                        to = PaymentStatus.PROCESSING,
                    ),
                ),
                retryPayment(key),
            )
            assertEquals(paymentId, repository.findById(paymentId)?.id)
            assertEquals(key, repository.findById(paymentId)?.idempotencyKey)
            assertEquals(PaymentStatus.FAILED, repository.findById(paymentId)?.status)
        }
    }

    @Test
    fun statusAndReceiptExposeTheStoredPayment() {
        runBlocking {
            val payment = (createPayment(cart(cents = 2_500), PaymentMethod.PIX, key) as DomainResult.Success).value
            processPayment(payment.id)

            assertEquals(DomainResult.Success(PaymentStatus.PROCESSING), getStatus(payment.id))
            val receipt = getReceipt(payment.id)
            assertTrue(receipt is DomainResult.Success)
            val value = (receipt as DomainResult.Success).value
            assertEquals(payment.id, value.paymentId)
            assertEquals(payment.idempotencyKey, value.idempotencyKey)
            assertEquals(2_500L, value.amount.amountInCents)
            assertEquals(PaymentMethod.PIX, value.method)
            assertEquals(PaymentStatus.PROCESSING, value.status)
            assertEquals(createdAt, value.createdAt)
        }
    }

    @Test
    fun missingPaymentIsReported() {
        runBlocking {
            assertEquals(
                DomainResult.Failure(PaymentError.PaymentNotFound),
                processPayment(paymentId),
            )
            assertEquals(
                DomainResult.Failure(PaymentError.PaymentNotFound),
                retryPayment(key),
            )
            assertEquals(
                DomainResult.Failure(PaymentError.PaymentNotFound),
                getStatus(paymentId),
            )
            assertEquals(
                DomainResult.Failure(PaymentError.PaymentNotFound),
                getReceipt(paymentId),
            )
        }
    }

    private fun cart(cents: Long = 1_000, quantity: Int = 1): Cart {
        return Cart(listOf(CartItem(product(priceInCents = cents), quantity = quantity)))
    }

    private fun stored(status: PaymentStatus): Payment {
        return Payment(
            id = paymentId,
            idempotencyKey = key,
            cart = cart(),
            method = PaymentMethod.PIX,
            status = status,
            createdAt = createdAt,
        )
    }
}

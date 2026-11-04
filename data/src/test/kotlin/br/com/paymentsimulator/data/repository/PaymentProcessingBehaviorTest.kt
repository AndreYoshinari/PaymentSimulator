package br.com.paymentsimulator.data.repository

import app.cash.turbine.test
import br.com.paymentsimulator.data.local.InMemoryPaymentLocalDataSource
import br.com.paymentsimulator.data.remote.PaymentRemoteDataSource
import br.com.paymentsimulator.data.remote.RemotePaymentSnapshot
import br.com.paymentsimulator.domain.model.DomainResult
import br.com.paymentsimulator.domain.model.PaymentError
import br.com.paymentsimulator.domain.model.PaymentMethod
import br.com.paymentsimulator.domain.model.PaymentStatus
import br.com.paymentsimulator.domain.repository.PaymentIdGenerator
import br.com.paymentsimulator.domain.repository.TimeProvider
import br.com.paymentsimulator.domain.usecase.CreatePaymentUseCase
import br.com.paymentsimulator.domain.usecase.GetPaymentReceiptUseCase
import br.com.paymentsimulator.domain.usecase.GetPaymentStatusUseCase
import br.com.paymentsimulator.domain.usecase.ProcessPaymentUseCase
import br.com.paymentsimulator.domain.usecase.RetryPaymentUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PaymentProcessingBehaviorTest {
    @Test
    fun timeoutStaysUnknownAndTheSameKeyDoesNotOpenAnotherCharge() = runBlocking {
        val payment = samplePayment()
        val remote = mockk<PaymentRemoteDataSource>()
        val repository = PaymentRepositoryImpl(InMemoryPaymentLocalDataSource(), remote)
        coEvery { remote.submit(any()) } returns DomainResult.Failure(PaymentError.PaymentTimedOut)
        coEvery { remote.getStatus(payment.idempotencyKey) } returns DomainResult.Success(
            snapshot(payment, PaymentStatus.UNKNOWN),
        )
        val generated = mutableListOf<String>()
        val create = CreatePaymentUseCase(
            paymentRepository = repository,
            paymentIds = PaymentIdGenerator {
                generated += payment.id.value.toString()
                payment.id
            },
            clock = TimeProvider { payment.createdAt },
        )
        val process = ProcessPaymentUseCase(repository)
        val status = GetPaymentStatusUseCase(repository)
        val retry = RetryPaymentUseCase(repository)
        val receipt = GetPaymentReceiptUseCase(repository)

        val created = create(payment.cart, payment.method, payment.idempotencyKey) as DomainResult.Success
        val processed = process(created.value.id) as DomainResult.Success

        assertEquals(PaymentStatus.PROCESSING, processed.value.status)
        assertEquals(created.value.id, processed.value.id)
        assertEquals(DomainResult.Success(PaymentStatus.UNKNOWN), status(created.value.id))
        assertNotEquals(PaymentStatus.DECLINED, repository.findById(created.value.id)?.status)

        val replay = create(
            payment.cart,
            PaymentMethod.CREDIT_CARD,
            payment.idempotencyKey,
        ) as DomainResult.Success
        assertEquals(created.value.id, replay.value.id)
        assertEquals(payment.idempotencyKey, replay.value.idempotencyKey)
        assertEquals(PaymentMethod.PIX, replay.value.method)
        assertEquals(PaymentStatus.UNKNOWN, replay.value.status)
        assertEquals(listOf(payment.id.value.toString()), generated)

        assertEquals(
            DomainResult.Failure(PaymentError.BlindRetryNotAllowed),
            retry(payment.idempotencyKey),
        )
        val storedReceipt = (receipt(created.value.id) as DomainResult.Success).value
        assertEquals(PaymentStatus.UNKNOWN, storedReceipt.status)
        assertEquals(payment.amount, storedReceipt.amount)
        assertEquals(payment.createdAt, storedReceipt.createdAt)

        repository.observeAll().test {
            val history = awaitItem()
            assertEquals(created.value.id, history.single().id)
            assertEquals(PaymentStatus.UNKNOWN, history.single().status)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 1) { remote.submit(any()) }
    }

    @Test
    fun aDeclinedChargeCanBeQueriedAndCannotBeRetried() = runBlocking {
        val payment = samplePayment(method = PaymentMethod.CREDIT_CARD)
        val remote = mockk<PaymentRemoteDataSource>()
        val repository = PaymentRepositoryImpl(InMemoryPaymentLocalDataSource(), remote)
        coEvery { remote.submit(any()) } returns DomainResult.Success(
            snapshot(payment, PaymentStatus.DECLINED),
        )
        val process = ProcessPaymentUseCase(repository)
        val status = GetPaymentStatusUseCase(repository)
        val retry = RetryPaymentUseCase(repository)

        repository.save(payment.copy(status = PaymentStatus.PROCESSING))

        assertEquals(DomainResult.Success(PaymentStatus.DECLINED), status(payment.id))
        assertEquals(
            DomainResult.Failure(
                PaymentError.InvalidStatusTransition(
                    from = PaymentStatus.DECLINED,
                    to = PaymentStatus.PROCESSING,
                ),
            ),
            retry(payment.idempotencyKey),
        )
        assertEquals(
            DomainResult.Failure(
                PaymentError.InvalidStatusTransition(
                    from = PaymentStatus.DECLINED,
                    to = PaymentStatus.PROCESSING,
                ),
            ),
            process(payment.id),
        )
        val stored = repository.findById(payment.id)
        assertEquals(PaymentStatus.DECLINED, stored?.status)
        assertEquals(payment.id, stored?.id)
        assertEquals(payment.amount.amountInCents, stored?.amount?.amountInCents)
        coVerify(exactly = 1) { remote.submit(any()) }
    }

    private fun snapshot(
        payment: br.com.paymentsimulator.domain.model.Payment,
        status: PaymentStatus,
    ): RemotePaymentSnapshot {
        return RemotePaymentSnapshot(
            idempotencyKey = payment.idempotencyKey,
            amount = payment.amount,
            method = payment.method,
            status = status,
        )
    }
}

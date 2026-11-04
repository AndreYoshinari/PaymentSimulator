package br.com.paymentsimulator.data.repository

import br.com.paymentsimulator.data.local.InMemoryPaymentLocalDataSource
import br.com.paymentsimulator.data.remote.FakePaymentApi
import br.com.paymentsimulator.data.remote.FakePaymentRemoteDataSource
import br.com.paymentsimulator.data.remote.FakePaymentScenario
import br.com.paymentsimulator.data.remote.RemotePaymentSnapshot
import br.com.paymentsimulator.data.remote.ScriptedPaymentRemoteDataSource
import br.com.paymentsimulator.domain.model.DomainResult
import br.com.paymentsimulator.domain.model.Money
import br.com.paymentsimulator.domain.model.PaymentError
import br.com.paymentsimulator.domain.model.PaymentMethod
import br.com.paymentsimulator.domain.model.PaymentStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PaymentRepositoryImplTest {
    @Test
    fun pendingPaymentIsStoredWithoutCallingTheApi() {
        runBlocking {
            val remote = ScriptedPaymentRemoteDataSource()
            val repository = repository(remote)
            val payment = samplePayment(status = PaymentStatus.PENDING)

            repository.save(payment)

            assertEquals(0, remote.submitCalls)
            assertEquals(payment, repository.findById(payment.id))
            assertEquals(payment, repository.findByIdempotencyKey(payment.idempotencyKey))
            assertEquals(listOf(payment), repository.observeAll().first())
        }
    }

    @Test
    fun approvedScenarioStoresOneApprovedCharge() {
        runBlocking {
            val api = FakePaymentApi().apply { scenario = FakePaymentScenario.APPROVED }
            val repository = repository(api)
            val payment = samplePayment(status = PaymentStatus.PROCESSING)

            repository.save(payment)
            val stored = repository.findById(payment.id)

            assertEquals(PaymentStatus.APPROVED, stored?.status)
            assertEquals(payment.id, stored?.id)
            assertEquals(payment.idempotencyKey, stored?.idempotencyKey)
            assertEquals(payment.amount, stored?.amount)
            assertEquals(1, api.chargeCount())
        }
    }

    @Test
    fun declinedScenarioStoresADeclinedCharge() {
        runBlocking {
            val api = FakePaymentApi().apply { scenario = FakePaymentScenario.DECLINED }
            val repository = repository(api)

            repository.save(samplePayment(status = PaymentStatus.PROCESSING))

            assertEquals(PaymentStatus.DECLINED, repository.findById(samplePayment().id)?.status)
            assertEquals(1, api.chargeCount())
        }
    }

    @Test
    fun timeoutBecomesUnknownAndDoesNotDecline() {
        runBlocking {
            val api = FakePaymentApi().apply { scenario = FakePaymentScenario.TIMEOUT }
            val repository = repository(api)
            val payment = samplePayment(status = PaymentStatus.PROCESSING)

            repository.save(payment)
            val stored = repository.findById(payment.id)

            assertEquals(PaymentStatus.UNKNOWN, stored?.status)
            assertFalse(stored?.status == PaymentStatus.DECLINED)
            assertFalse(api.settleUnknown(payment.idempotencyKey))
            assertEquals(PaymentStatus.UNKNOWN, repository.findById(payment.id)?.status)
            assertEquals(1, api.chargeCount())
        }
    }

    @Test
    fun unavailableApiKeepsTheProcessingPaymentAndCreatesNoCharge() {
        runBlocking {
            val api = FakePaymentApi().apply { scenario = FakePaymentScenario.UNAVAILABLE }
            val repository = repository(api)
            val payment = samplePayment(status = PaymentStatus.PROCESSING)

            repository.save(payment)

            assertEquals(PaymentStatus.PROCESSING, repository.findById(payment.id)?.status)
            assertEquals(
                DomainResult.Failure(PaymentError.ServiceUnavailable),
                repository.findForStatus(payment.id),
            )
            assertEquals(PaymentStatus.PROCESSING, repository.findById(payment.id)?.status)
            assertEquals(0, api.chargeCount())
        }
    }

    @Test
    fun repeatedSubmitDoesNotOpenAnotherCharge() {
        runBlocking {
            val api = FakePaymentApi().apply { scenario = FakePaymentScenario.DUPLICATE_RESPONSE }
            val repository = repository(api)
            val payment = samplePayment(status = PaymentStatus.PROCESSING)

            repository.save(payment)
            api.scenario = FakePaymentScenario.DECLINED
            repository.save(payment)

            assertEquals(PaymentStatus.APPROVED, repository.findById(payment.id)?.status)
            assertEquals(1, api.chargeCount())
        }
    }

    @Test
    fun unknownChargeCanBeApprovedOnTheNextStatusRead() {
        runBlocking {
            val api = FakePaymentApi().apply { scenario = FakePaymentScenario.UNKNOWN_THEN_APPROVED }
            val repository = repository(api)
            val payment = samplePayment(
                status = PaymentStatus.PROCESSING,
                method = PaymentMethod.CREDIT_CARD,
            )

            repository.save(payment)
            assertEquals(PaymentStatus.UNKNOWN, repository.findById(payment.id)?.status)
            assertEquals(true, api.settleUnknown(payment.idempotencyKey))

            val settled = repository.findById(payment.id)
            assertEquals(PaymentStatus.APPROVED, settled?.status)
            assertEquals(payment.id, settled?.id)
            assertEquals(payment.idempotencyKey, settled?.idempotencyKey)
            assertEquals(1, api.chargeCount())
        }
    }

    @Test
    fun idempotencyConflictLeavesTheStoredPaymentUntouched() {
        runBlocking {
            val remote = ScriptedPaymentRemoteDataSource().apply {
                submitResult = DomainResult.Failure(PaymentError.IdempotencyConflict)
                statusResult = DomainResult.Failure(PaymentError.IdempotencyConflict)
            }
            val repository = PaymentRepositoryImpl(InMemoryPaymentLocalDataSource(), remote)
            val payment = samplePayment(status = PaymentStatus.PROCESSING)

            repository.save(payment)

            assertEquals(PaymentStatus.PROCESSING, repository.findById(payment.id)?.status)
            assertEquals(1, remote.submitCalls)
        }
    }

    @Test
    fun concurrentProcessingOpensASingleChargeWithTheOriginalAmount() {
        runBlocking {
            val api = FakePaymentApi()
            val repository = repository(api)
            val payment = samplePayment(status = PaymentStatus.PROCESSING, cents = 500, quantity = 2)

            coroutineScope {
                List(12) {
                    async(Dispatchers.Default) { repository.save(payment) }
                }.awaitAll()
            }

            val stored = repository.findById(payment.id)
            assertEquals(1, api.chargeCount())
            assertEquals(PaymentStatus.APPROVED, stored?.status)
            assertEquals(1_000L, stored?.amount?.amountInCents)
            assertEquals(payment.id, stored?.id)
            assertEquals(payment.idempotencyKey, stored?.idempotencyKey)
        }
    }

    @Test
    fun aMismatchedRemoteAmountDoesNotApproveThePayment() {
        runBlocking {
            val remote = ScriptedPaymentRemoteDataSource()
            val repository = repository(remote)
            val payment = samplePayment(status = PaymentStatus.PROCESSING, cents = 1_500)
            remote.submitResult = DomainResult.Success(
                RemotePaymentSnapshot(
                    idempotencyKey = payment.idempotencyKey,
                    amount = Money(1),
                    method = payment.method,
                    status = PaymentStatus.APPROVED,
                ),
            )

            repository.save(payment)

            val stored = repository.findById(payment.id)
            assertEquals(PaymentStatus.PROCESSING, stored?.status)
            assertEquals(1_500L, stored?.amount?.amountInCents)
            assertEquals(1, remote.submitCalls)
        }
    }

    @Test
    fun anotherPendingPaymentWithTheSameKeyDoesNotReplaceTheOriginal() {
        runBlocking {
            val remote = ScriptedPaymentRemoteDataSource()
            val repository = repository(remote)
            val original = samplePayment(status = PaymentStatus.PENDING, cents = 500)
            val other = samplePayment(
                status = PaymentStatus.PENDING,
                cents = 900,
                id = "dddddddd-dddd-dddd-dddd-dddddddddddd",
            )

            repository.save(original)
            repository.save(other)

            assertEquals(original, repository.findById(original.id))
            assertEquals(null, repository.findById(other.id))
            assertEquals(0, remote.submitCalls)
        }
    }

    @Test
    fun observingHistoryReadsRoomWithoutCallingTheRemoteApi() = runBlocking {
        val remote = ScriptedPaymentRemoteDataSource()
        val repository = repository(remote)
        val payment = samplePayment(status = PaymentStatus.PENDING)

        repository.save(payment)

        assertEquals(listOf(payment), repository.observeAll().first())
        assertEquals(payment, repository.observeById(payment.id).first())
        assertEquals(0, remote.submitCalls)
        assertEquals(0, remote.statusCalls)
    }

    private fun repository(api: FakePaymentApi): PaymentRepositoryImpl {
        return PaymentRepositoryImpl(
            local = InMemoryPaymentLocalDataSource(),
            remote = FakePaymentRemoteDataSource(api),
        )
    }

    private fun repository(remote: ScriptedPaymentRemoteDataSource): PaymentRepositoryImpl {
        return PaymentRepositoryImpl(
            local = InMemoryPaymentLocalDataSource(),
            remote = remote,
        )
    }
}

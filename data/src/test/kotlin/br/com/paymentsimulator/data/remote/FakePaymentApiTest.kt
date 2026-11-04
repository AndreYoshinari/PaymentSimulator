package br.com.paymentsimulator.data.remote

import br.com.paymentsimulator.domain.api.ApiPaymentStatus
import br.com.paymentsimulator.domain.api.PaymentApiRequest
import br.com.paymentsimulator.domain.api.PaymentApiResult
import br.com.paymentsimulator.domain.model.IdempotencyKey
import br.com.paymentsimulator.domain.model.Money
import br.com.paymentsimulator.domain.model.PaymentMethod
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakePaymentApiTest {
    private val api = FakePaymentApi()

    @Test
    fun approvedScenarioOpensOneCharge() {
        runBlocking {
            api.scenario = FakePaymentScenario.APPROVED
            val request = request()

            val result = api.submit(request)

            assertTrue(result is PaymentApiResult.Approved)
            assertEquals(ApiPaymentStatus.APPROVED, (result as PaymentApiResult.Approved).charge.status)
            assertEquals(result, api.getStatus(request.idempotencyKey))
            assertEquals(1, api.chargeCount())
        }
    }

    @Test
    fun declinedScenarioIsARefusal() {
        runBlocking {
            api.scenario = FakePaymentScenario.DECLINED
            val request = request()

            val result = api.submit(request)

            assertTrue(result is PaymentApiResult.Declined)
            assertEquals(ApiPaymentStatus.DECLINED, (result as PaymentApiResult.Declined).charge.status)
            assertEquals(result, api.getStatus(request.idempotencyKey))
        }
    }

    @Test
    fun timeoutKeepsAnUnknownChargeThatCanBeQueried() {
        runBlocking {
            api.scenario = FakePaymentScenario.TIMEOUT
            val request = request()

            val first = api.submit(request)
            val second = api.submit(request)
            val status = api.getStatus(request.idempotencyKey)

            assertTrue(first is PaymentApiResult.TimedOut)
            assertEquals(first, second)
            assertTrue(status is PaymentApiResult.Unknown)
            assertEquals(
                ApiPaymentStatus.UNKNOWN,
                (status as PaymentApiResult.Unknown).charge.status,
            )
            assertEquals(request.amount, status.charge.amount)
            assertFalse(api.settleUnknown(request.idempotencyKey))
            assertEquals(1, api.chargeCount())
        }
    }

    @Test
    fun unavailableScenarioCreatesNoCharge() {
        runBlocking {
            api.scenario = FakePaymentScenario.UNAVAILABLE
            val request = request()

            assertEquals(PaymentApiResult.Unavailable, api.submit(request))
            assertEquals(PaymentApiResult.Unavailable, api.submit(request))
            assertEquals(PaymentApiResult.Unavailable, api.getStatus(request.idempotencyKey))
            assertEquals(0, api.chargeCount())
        }
    }

    @Test
    fun duplicateScenarioReplaysTheOriginalResponse() {
        runBlocking {
            api.scenario = FakePaymentScenario.DUPLICATE_RESPONSE
            val request = request()

            val original = api.submit(request)
            val replay = api.submit(request)

            assertTrue(original is PaymentApiResult.Approved)
            assertEquals(PaymentApiResult.Duplicate(original), replay)
            assertEquals(1, api.chargeCount())
        }
    }

    @Test
    fun unknownChargeCanLaterBeApprovedWithoutASecondCharge() {
        runBlocking {
            api.scenario = FakePaymentScenario.UNKNOWN_THEN_APPROVED
            val request = request(cents = 4_200, method = PaymentMethod.CREDIT_CARD)

            val submitted = api.submit(request)
            val before = api.getStatus(request.idempotencyKey)
            assertTrue(api.settleUnknown(request.idempotencyKey))
            val after = api.getStatus(request.idempotencyKey)
            val replay = api.submit(request)

            assertTrue(submitted is PaymentApiResult.Unknown)
            assertEquals(submitted, before)
            assertTrue(after is PaymentApiResult.Approved)
            assertEquals(request.idempotencyKey, (after as PaymentApiResult.Approved).charge.idempotencyKey)
            assertEquals(request.amount, after.charge.amount)
            assertEquals(submitted, replay)
            assertEquals(1, api.chargeCount())
        }
    }

    @Test
    fun sameKeyAndSameContentReturnsTheOriginalResult() {
        runBlocking {
            api.scenario = FakePaymentScenario.APPROVED
            val request = request()
            val original = api.submit(request)

            api.scenario = FakePaymentScenario.DECLINED
            val replay = api.submit(request)

            assertEquals(original, replay)
            assertTrue(replay is PaymentApiResult.Approved)
            assertEquals(1, api.chargeCount())
        }
    }

    @Test
    fun sameKeyAndDifferentContentReturnsConflict() {
        runBlocking {
            api.scenario = FakePaymentScenario.APPROVED
            val originalRequest = request(cents = 1_500)
            val original = api.submit(originalRequest)

            val differentAmount = api.submit(request(cents = 9_900))
            val differentMethod = api.submit(request(method = PaymentMethod.CREDIT_CARD))
            val status = api.getStatus(originalRequest.idempotencyKey)

            assertEquals(PaymentApiResult.Conflict(originalRequest.idempotencyKey), differentAmount)
            assertEquals(PaymentApiResult.Conflict(originalRequest.idempotencyKey), differentMethod)
            assertEquals(original, status)
            assertEquals(1_500L, (status as PaymentApiResult.Approved).charge.amount.amountInCents)
            assertEquals(1, api.chargeCount())
        }
    }

    @Test
    fun differentKeysOpenSeparateCharges() {
        runBlocking {
            api.scenario = FakePaymentScenario.APPROVED

            api.submit(request(key = "key-1"))
            api.submit(request(key = "key-2"))

            assertEquals(2, api.chargeCount())
        }
    }

    private fun request(
        key: String = "key-1",
        cents: Long = 1_500,
        method: PaymentMethod = PaymentMethod.PIX,
    ): PaymentApiRequest {
        return PaymentApiRequest(
            idempotencyKey = IdempotencyKey(key),
            amount = Money(cents),
            method = method,
        )
    }
}

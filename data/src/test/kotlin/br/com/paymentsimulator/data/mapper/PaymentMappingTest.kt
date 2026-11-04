package br.com.paymentsimulator.data.mapper

import br.com.paymentsimulator.data.local.PaymentWithItems
import br.com.paymentsimulator.data.remote.PaymentRequestDto
import br.com.paymentsimulator.data.remote.PaymentResponseDto
import br.com.paymentsimulator.data.repository.samplePayment
import br.com.paymentsimulator.domain.api.ApiCharge
import br.com.paymentsimulator.domain.api.ApiPaymentStatus
import br.com.paymentsimulator.domain.api.PaymentApiResult
import br.com.paymentsimulator.domain.model.DomainResult
import br.com.paymentsimulator.domain.model.IdempotencyKey
import br.com.paymentsimulator.domain.model.Money
import br.com.paymentsimulator.domain.model.PaymentError
import br.com.paymentsimulator.domain.model.PaymentMethod
import br.com.paymentsimulator.domain.model.PaymentStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentMappingTest {
    @Test
    fun paymentRoundTripsThroughRoomEntities() {
        val payment = samplePayment(quantity = 2)
        val entity = payment.toEntity()
        val restored = PaymentWithItems(
            payment = entity,
            items = payment.toItemEntities(),
        ).toDomain()

        assertEquals(3_000L, entity.amountInCents)
        assertEquals("BRL", entity.currency)
        assertEquals(payment, restored)
    }

    @Test
    fun requestDtoRoundTripsToTheApiModel() {
        val payment = samplePayment(method = PaymentMethod.CREDIT_CARD)
        val apiRequest = (payment.toRequestDto().toApiRequest() as DomainResult.Success).value

        assertEquals(payment.idempotencyKey, apiRequest.idempotencyKey)
        assertEquals(payment.amount, apiRequest.amount)
        assertEquals(PaymentMethod.CREDIT_CARD, apiRequest.method)
    }

    @Test
    fun invalidRemotePayloadBecomesADomainError() {
        val dto = PaymentRequestDto(
            idempotencyKey = "operation-1",
            amountInCents = 1_000,
            currency = "USD",
            method = "PIX",
        )

        assertEquals(
            DomainResult.Failure(PaymentError.InvalidRemotePayload),
            dto.toApiRequest(),
        )
        assertEquals(
            DomainResult.Failure(PaymentError.InvalidRemotePayload),
            PaymentResponseDto(
                idempotencyKey = "",
                amountInCents = 1_000,
                currency = "BRL",
                method = "PIX",
                status = "APPROVED",
            ).toSnapshot(),
        )
    }

    @Test
    fun apiResultsConvertWithoutTreatingTimeoutAsADecline() {
        val charge = ApiCharge(
            idempotencyKey = IdempotencyKey("operation-1"),
            amount = Money(1_500),
            method = PaymentMethod.PIX,
            status = ApiPaymentStatus.APPROVED,
        )
        val approved = PaymentApiResult.Approved(charge)

        assertEquals(
            DomainResult.Failure(PaymentError.PaymentTimedOut),
            PaymentApiResult.TimedOut(charge.idempotencyKey).toDomainSnapshot(),
        )
        assertEquals(
            DomainResult.Failure(PaymentError.ServiceUnavailable),
            PaymentApiResult.Unavailable.toDomainSnapshot(),
        )
        assertEquals(
            DomainResult.Failure(PaymentError.IdempotencyConflict),
            PaymentApiResult.Conflict(charge.idempotencyKey).toDomainSnapshot(),
        )
        assertEquals(approved.toDomainSnapshot(), PaymentApiResult.Duplicate(approved).toDomainSnapshot())
        val snapshot = (approved.toDomainSnapshot() as DomainResult.Success).value
        assertEquals(PaymentStatus.APPROVED, snapshot.status)
    }
}

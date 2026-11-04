package br.com.paymentsimulator.data.remote

import br.com.paymentsimulator.domain.api.ApiCharge
import br.com.paymentsimulator.domain.api.ApiPaymentStatus
import br.com.paymentsimulator.domain.api.PaymentApi
import br.com.paymentsimulator.domain.api.PaymentApiRequest
import br.com.paymentsimulator.domain.api.PaymentApiResult
import br.com.paymentsimulator.domain.model.IdempotencyKey
import br.com.paymentsimulator.domain.simulation.PaymentSimulation
import br.com.paymentsimulator.domain.simulation.PaymentSimulationScenario
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class FakePaymentApi : PaymentApi, PaymentSimulation {
    var scenario: FakePaymentScenario = FakePaymentScenario.APPROVED

    private val lock = ReentrantLock()
    private val operations = linkedMapOf<IdempotencyKey, Operation>()

    fun chargeCount(): Int = lock.withLock { operations.size }

    override fun use(scenario: PaymentSimulationScenario) {
        val selected = when (scenario) {
            PaymentSimulationScenario.APPROVED -> FakePaymentScenario.APPROVED
            PaymentSimulationScenario.DECLINED -> FakePaymentScenario.DECLINED
            PaymentSimulationScenario.TIMEOUT -> FakePaymentScenario.TIMEOUT
            PaymentSimulationScenario.UNAVAILABLE -> FakePaymentScenario.UNAVAILABLE
            PaymentSimulationScenario.DUPLICATE_RESPONSE -> FakePaymentScenario.DUPLICATE_RESPONSE
            PaymentSimulationScenario.UNKNOWN_THEN_APPROVED -> FakePaymentScenario.UNKNOWN_THEN_APPROVED
        }
        lock.withLock { this.scenario = selected }
    }

    fun settleUnknown(idempotencyKey: IdempotencyKey): Boolean = lock.withLock {
        val current = operations[idempotencyKey] ?: return false
        if (!current.canSettle || current.charge.status != ApiPaymentStatus.UNKNOWN) {
            return false
        }
        operations[idempotencyKey] = current.copy(
            charge = current.charge.copy(status = ApiPaymentStatus.APPROVED),
        )
        return true
    }

    override suspend fun submit(request: PaymentApiRequest): PaymentApiResult = lock.withLock {
        operations[request.idempotencyKey]?.let { existing ->
            if (existing.request != request) {
                return PaymentApiResult.Conflict(request.idempotencyKey)
            }
            return if (scenario == FakePaymentScenario.DUPLICATE_RESPONSE) {
                PaymentApiResult.Duplicate(existing.submitResult)
            } else {
                existing.submitResult
            }
        }
        if (scenario == FakePaymentScenario.UNAVAILABLE) {
            return PaymentApiResult.Unavailable
        }
        val operation = open(request)
        operations[request.idempotencyKey] = operation
        return operation.submitResult
    }

    override suspend fun getStatus(idempotencyKey: IdempotencyKey): PaymentApiResult = lock.withLock {
        if (scenario == FakePaymentScenario.UNAVAILABLE) {
            return PaymentApiResult.Unavailable
        }
        val operation = operations[idempotencyKey] ?: return PaymentApiResult.NotFound
        return when (operation.charge.status) {
            ApiPaymentStatus.APPROVED -> PaymentApiResult.Approved(operation.charge)
            ApiPaymentStatus.DECLINED -> PaymentApiResult.Declined(operation.charge)
            ApiPaymentStatus.UNKNOWN -> PaymentApiResult.Unknown(operation.charge)
        }
    }

    private fun open(request: PaymentApiRequest): Operation {
        val status = when (scenario) {
            FakePaymentScenario.APPROVED,
            FakePaymentScenario.DUPLICATE_RESPONSE,
            -> ApiPaymentStatus.APPROVED
            FakePaymentScenario.DECLINED -> ApiPaymentStatus.DECLINED
            FakePaymentScenario.TIMEOUT,
            FakePaymentScenario.UNKNOWN_THEN_APPROVED,
            -> ApiPaymentStatus.UNKNOWN
            FakePaymentScenario.UNAVAILABLE -> error("Unavailable does not open a charge.")
        }
        val charge = ApiCharge(
            idempotencyKey = request.idempotencyKey,
            amount = request.amount,
            method = request.method,
            status = status,
        )
        val submitResult = when (scenario) {
            FakePaymentScenario.APPROVED,
            FakePaymentScenario.DUPLICATE_RESPONSE,
            -> PaymentApiResult.Approved(charge)
            FakePaymentScenario.DECLINED -> PaymentApiResult.Declined(charge)
            FakePaymentScenario.TIMEOUT -> PaymentApiResult.TimedOut(request.idempotencyKey)
            FakePaymentScenario.UNKNOWN_THEN_APPROVED -> PaymentApiResult.Unknown(charge)
            FakePaymentScenario.UNAVAILABLE -> error("Unavailable does not open a charge.")
        }
        return Operation(
            request = request,
            submitResult = submitResult,
            charge = charge,
            canSettle = scenario == FakePaymentScenario.UNKNOWN_THEN_APPROVED,
        )
    }

    private data class Operation(
        val request: PaymentApiRequest,
        val submitResult: PaymentApiResult,
        val charge: ApiCharge,
        val canSettle: Boolean,
    )
}

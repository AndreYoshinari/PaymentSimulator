package br.com.paymentsimulator.domain.api

import br.com.paymentsimulator.domain.model.IdempotencyKey

sealed interface PaymentApiResult {
    data class Approved(val charge: ApiCharge) : PaymentApiResult

    data class Declined(val charge: ApiCharge) : PaymentApiResult

    data class Unknown(val charge: ApiCharge) : PaymentApiResult

    data class TimedOut(val idempotencyKey: IdempotencyKey) : PaymentApiResult

    data object Unavailable : PaymentApiResult

    data class Conflict(val idempotencyKey: IdempotencyKey) : PaymentApiResult

    data class Duplicate(val original: PaymentApiResult) : PaymentApiResult

    data object NotFound : PaymentApiResult
}

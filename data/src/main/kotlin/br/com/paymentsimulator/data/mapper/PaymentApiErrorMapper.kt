package br.com.paymentsimulator.data.mapper

import br.com.paymentsimulator.data.remote.RemotePaymentSnapshot
import br.com.paymentsimulator.domain.api.PaymentApiResult
import br.com.paymentsimulator.domain.model.DomainResult
import br.com.paymentsimulator.domain.model.PaymentError

fun PaymentApiResult.toDomainSnapshot(): DomainResult<RemotePaymentSnapshot> = when (this) {
    is PaymentApiResult.Approved -> charge.toResponseDto().toSnapshot()
    is PaymentApiResult.Declined -> charge.toResponseDto().toSnapshot()
    is PaymentApiResult.Unknown -> charge.toResponseDto().toSnapshot()
    is PaymentApiResult.Duplicate -> original.toDomainSnapshot()
    is PaymentApiResult.TimedOut -> DomainResult.Failure(PaymentError.PaymentTimedOut)
    PaymentApiResult.Unavailable -> DomainResult.Failure(PaymentError.ServiceUnavailable)
    is PaymentApiResult.Conflict -> DomainResult.Failure(PaymentError.IdempotencyConflict)
    PaymentApiResult.NotFound -> DomainResult.Failure(PaymentError.PaymentNotFound)
}

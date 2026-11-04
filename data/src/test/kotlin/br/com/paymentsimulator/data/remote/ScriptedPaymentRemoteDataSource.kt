package br.com.paymentsimulator.data.remote

import br.com.paymentsimulator.domain.model.DomainResult
import br.com.paymentsimulator.domain.model.IdempotencyKey
import br.com.paymentsimulator.domain.model.Payment
import br.com.paymentsimulator.domain.model.PaymentError

class ScriptedPaymentRemoteDataSource : PaymentRemoteDataSource {
    var submitResult: DomainResult<RemotePaymentSnapshot> =
        DomainResult.Failure(PaymentError.ServiceUnavailable)
    var statusResult: DomainResult<RemotePaymentSnapshot> =
        DomainResult.Failure(PaymentError.PaymentNotFound)
    var submitCalls: Int = 0
    var statusCalls: Int = 0

    override suspend fun submit(payment: Payment): DomainResult<RemotePaymentSnapshot> {
        submitCalls += 1
        return submitResult
    }

    override suspend fun getStatus(idempotencyKey: IdempotencyKey): DomainResult<RemotePaymentSnapshot> {
        statusCalls += 1
        return statusResult
    }
}

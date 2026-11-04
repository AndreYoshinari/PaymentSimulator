package br.com.paymentsimulator.data.remote

import br.com.paymentsimulator.domain.model.DomainResult
import br.com.paymentsimulator.domain.model.IdempotencyKey
import br.com.paymentsimulator.domain.model.Payment

interface PaymentRemoteDataSource {
    suspend fun submit(payment: Payment): DomainResult<RemotePaymentSnapshot>

    suspend fun getStatus(idempotencyKey: IdempotencyKey): DomainResult<RemotePaymentSnapshot>
}

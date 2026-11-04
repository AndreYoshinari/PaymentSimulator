package br.com.paymentsimulator.data.remote

import br.com.paymentsimulator.data.mapper.toApiRequest
import br.com.paymentsimulator.data.mapper.toDomainSnapshot
import br.com.paymentsimulator.data.mapper.toRequestDto
import br.com.paymentsimulator.domain.api.PaymentApi
import br.com.paymentsimulator.domain.model.DomainResult
import br.com.paymentsimulator.domain.model.IdempotencyKey
import br.com.paymentsimulator.domain.model.Payment

class FakePaymentRemoteDataSource(
    private val api: PaymentApi,
) : PaymentRemoteDataSource {
    override suspend fun submit(payment: Payment): DomainResult<RemotePaymentSnapshot> {
        val request = when (val mapped = payment.toRequestDto().toApiRequest()) {
            is DomainResult.Failure -> return mapped
            is DomainResult.Success -> mapped.value
        }
        return api.submit(request).toDomainSnapshot()
    }

    override suspend fun getStatus(idempotencyKey: IdempotencyKey): DomainResult<RemotePaymentSnapshot> {
        return api.getStatus(idempotencyKey).toDomainSnapshot()
    }
}

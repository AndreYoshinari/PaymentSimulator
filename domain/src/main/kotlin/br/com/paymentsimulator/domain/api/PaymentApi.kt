package br.com.paymentsimulator.domain.api

import br.com.paymentsimulator.domain.model.IdempotencyKey

interface PaymentApi {
    suspend fun submit(request: PaymentApiRequest): PaymentApiResult

    suspend fun getStatus(idempotencyKey: IdempotencyKey): PaymentApiResult
}

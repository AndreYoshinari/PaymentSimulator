package br.com.paymentsimulator.data.local

import br.com.paymentsimulator.domain.model.IdempotencyKey
import br.com.paymentsimulator.domain.model.Payment
import br.com.paymentsimulator.domain.model.PaymentId
import kotlinx.coroutines.flow.Flow

interface PaymentLocalDataSource {
    suspend fun upsert(payment: Payment)

    suspend fun findById(id: PaymentId): Payment?

    suspend fun findByIdempotencyKey(key: IdempotencyKey): Payment?

    fun observeAll(): Flow<List<Payment>>

    fun observeById(id: PaymentId): Flow<Payment?>
}

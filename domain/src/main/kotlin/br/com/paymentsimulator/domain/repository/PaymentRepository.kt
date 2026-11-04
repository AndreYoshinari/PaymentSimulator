package br.com.paymentsimulator.domain.repository

import br.com.paymentsimulator.domain.model.DomainResult
import br.com.paymentsimulator.domain.model.IdempotencyKey
import br.com.paymentsimulator.domain.model.Payment
import br.com.paymentsimulator.domain.model.PaymentId
import kotlinx.coroutines.flow.Flow

interface PaymentRepository {
    suspend fun save(payment: Payment)

    suspend fun findById(id: PaymentId): Payment?

    suspend fun findByIdempotencyKey(key: IdempotencyKey): Payment?

    /**
     * Reads the stored payment and reports a remote failure.
     * A timeout stays unknown. This read does not open another charge.
     */
    suspend fun findForStatus(id: PaymentId): DomainResult<Payment>

    fun observeAll(): Flow<List<Payment>>

    fun observeById(id: PaymentId): Flow<Payment?>
}

package br.com.paymentsimulator.domain.repository

import br.com.paymentsimulator.domain.model.DomainResult
import br.com.paymentsimulator.domain.model.IdempotencyKey
import br.com.paymentsimulator.domain.model.Payment
import br.com.paymentsimulator.domain.model.PaymentError
import br.com.paymentsimulator.domain.model.PaymentId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class InMemoryPaymentRepository : PaymentRepository {
    val payments = linkedMapOf<PaymentId, Payment>()
    var saveCount: Int = 0

    override suspend fun save(payment: Payment) {
        saveCount += 1
        payments[payment.id] = payment
    }

    override suspend fun findById(id: PaymentId): Payment? = payments[id]

    override suspend fun findByIdempotencyKey(key: IdempotencyKey): Payment? {
        return payments.values.find { it.idempotencyKey == key }
    }

    override suspend fun findForStatus(id: PaymentId): DomainResult<Payment> {
        val payment = payments[id] ?: return DomainResult.Failure(PaymentError.PaymentNotFound)
        return DomainResult.Success(payment)
    }

    override fun observeAll(): Flow<List<Payment>> = flowOf(payments.values.toList())

    override fun observeById(id: PaymentId): Flow<Payment?> = flowOf(payments[id])
}

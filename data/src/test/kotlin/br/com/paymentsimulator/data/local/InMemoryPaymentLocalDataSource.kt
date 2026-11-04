package br.com.paymentsimulator.data.local

import br.com.paymentsimulator.domain.model.IdempotencyKey
import br.com.paymentsimulator.domain.model.Payment
import br.com.paymentsimulator.domain.model.PaymentId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class InMemoryPaymentLocalDataSource : PaymentLocalDataSource {
    private val payments = linkedMapOf<PaymentId, Payment>()
    private val state = MutableStateFlow<List<Payment>>(emptyList())

    override suspend fun upsert(payment: Payment) {
        payments[payment.id] = payment
        state.value = payments.values.toList()
    }

    override suspend fun findById(id: PaymentId): Payment? = payments[id]

    override suspend fun findByIdempotencyKey(key: IdempotencyKey): Payment? {
        return payments.values.find { it.idempotencyKey == key }
    }

    override fun observeAll(): Flow<List<Payment>> = state

    override fun observeById(id: PaymentId): Flow<Payment?> = state.map { payments[id] }
}

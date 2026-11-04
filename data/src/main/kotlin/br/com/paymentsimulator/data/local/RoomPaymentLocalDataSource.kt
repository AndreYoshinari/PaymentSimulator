package br.com.paymentsimulator.data.local

import br.com.paymentsimulator.data.mapper.toDomain
import br.com.paymentsimulator.data.mapper.toEntity
import br.com.paymentsimulator.data.mapper.toItemEntities
import br.com.paymentsimulator.domain.model.IdempotencyKey
import br.com.paymentsimulator.domain.model.Payment
import br.com.paymentsimulator.domain.model.PaymentId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomPaymentLocalDataSource(
    private val dao: PaymentDao,
) : PaymentLocalDataSource {
    override suspend fun upsert(payment: Payment) {
        dao.upsert(payment.toEntity(), payment.toItemEntities())
    }

    override suspend fun findById(id: PaymentId): Payment? {
        return dao.findById(id.value.toString())?.toDomain()
    }

    override suspend fun findByIdempotencyKey(key: IdempotencyKey): Payment? {
        return dao.findByIdempotencyKey(key.value)?.toDomain()
    }

    override fun observeAll(): Flow<List<Payment>> {
        return dao.observeAll().map { rows -> rows.map { it.toDomain() } }
    }

    override fun observeById(id: PaymentId): Flow<Payment?> {
        return dao.observeById(id.value.toString()).map { row -> row?.toDomain() }
    }
}

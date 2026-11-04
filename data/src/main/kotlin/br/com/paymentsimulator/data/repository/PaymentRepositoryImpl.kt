package br.com.paymentsimulator.data.repository

import br.com.paymentsimulator.data.local.PaymentLocalDataSource
import br.com.paymentsimulator.data.remote.PaymentRemoteDataSource
import br.com.paymentsimulator.data.remote.RemotePaymentSnapshot
import br.com.paymentsimulator.domain.model.DomainResult
import br.com.paymentsimulator.domain.model.IdempotencyKey
import br.com.paymentsimulator.domain.model.Payment
import br.com.paymentsimulator.domain.model.PaymentError
import br.com.paymentsimulator.domain.model.PaymentId
import br.com.paymentsimulator.domain.model.PaymentResult
import br.com.paymentsimulator.domain.model.PaymentStatus
import br.com.paymentsimulator.domain.policy.PaymentStateMachine
import br.com.paymentsimulator.domain.repository.PaymentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PaymentRepositoryImpl(
    private val local: PaymentLocalDataSource,
    private val remote: PaymentRemoteDataSource,
) : PaymentRepository {
    private val mutex = Mutex()

    override suspend fun save(payment: Payment) {
        mutex.withLock {
            val existing = local.findById(payment.id)
            val sameKey = local.findByIdempotencyKey(payment.idempotencyKey)
            if (sameKey != null && sameKey.id != payment.id) {
                return
            }
            // A payment that already left PENDING was submitted. Do not open another charge.
            if (payment.status == PaymentStatus.PROCESSING &&
                existing != null &&
                existing.status != PaymentStatus.PENDING
            ) {
                return
            }
            local.upsert(payment)
            if (payment.status != PaymentStatus.PROCESSING) {
                return
            }
            when (val submitted = remote.submit(payment)) {
                is DomainResult.Success -> {
                    if (submitted.value.matches(payment)) {
                        applyStatus(payment, submitted.value.status)
                    }
                }
                is DomainResult.Failure -> applyRemoteFailure(payment, submitted.error)
            }
        }
    }

    override suspend fun findById(id: PaymentId): Payment? {
        return when (val result = findForStatus(id)) {
            is DomainResult.Success -> result.value
            is DomainResult.Failure -> if (result.error == PaymentError.PaymentNotFound) {
                null
            } else {
                local.findById(id)
            }
        }
    }

    override suspend fun findByIdempotencyKey(key: IdempotencyKey): Payment? {
        return mutex.withLock {
            val payment = local.findByIdempotencyKey(key) ?: return null
            when (val result = read(payment)) {
                is DomainResult.Success -> result.value
                is DomainResult.Failure -> local.findById(payment.id)
            }
        }
    }

    override suspend fun findForStatus(id: PaymentId): DomainResult<Payment> {
        return mutex.withLock {
            val payment = local.findById(id) ?: return DomainResult.Failure(PaymentError.PaymentNotFound)
            read(payment)
        }
    }

    override fun observeAll(): Flow<List<Payment>> = local.observeAll()

    override fun observeById(id: PaymentId): Flow<Payment?> = local.observeById(id)

    private suspend fun read(payment: Payment): DomainResult<Payment> {
        if (payment.status != PaymentStatus.PROCESSING && payment.status != PaymentStatus.UNKNOWN) {
            return DomainResult.Success(payment)
        }
        return when (val remoteStatus = remote.getStatus(payment.idempotencyKey)) {
            is DomainResult.Success -> {
                if (!remoteStatus.value.matches(payment)) {
                    return DomainResult.Failure(PaymentError.InvalidRemotePayload)
                }
                DomainResult.Success(applyStatus(payment, remoteStatus.value.status))
            }
            is DomainResult.Failure -> {
                // The gateway has no charge. Keep the stored payment and do not decline it.
                if (remoteStatus.error == PaymentError.PaymentNotFound) {
                    return DomainResult.Success(payment)
                }
                val current = applyRemoteFailure(payment, remoteStatus.error)
                if (remoteStatus.error == PaymentError.PaymentTimedOut) {
                    DomainResult.Success(current)
                } else {
                    DomainResult.Failure(remoteStatus.error)
                }
            }
        }
    }

    private suspend fun applyStatus(payment: Payment, target: PaymentStatus): Payment {
        val current = local.findById(payment.id) ?: payment
        if (current.status == target) {
            return current
        }
        return when (val transition = PaymentStateMachine.transition(current, target)) {
            is PaymentResult.Failure -> current
            is PaymentResult.Success -> {
                local.upsert(transition.payment)
                transition.payment
            }
        }
    }

    private suspend fun applyRemoteFailure(payment: Payment, error: PaymentError): Payment {
        // A timeout means the outcome is unknown. It is not a decline.
        if (error == PaymentError.PaymentTimedOut) {
            return applyStatus(payment, PaymentStatus.UNKNOWN)
        }
        return payment
    }
}

private fun RemotePaymentSnapshot.matches(payment: Payment): Boolean {
    return idempotencyKey == payment.idempotencyKey &&
        amount == payment.amount &&
        method == payment.method
}

package br.com.paymentsimulator.domain.usecase

import br.com.paymentsimulator.domain.model.Cart
import br.com.paymentsimulator.domain.model.DomainResult
import br.com.paymentsimulator.domain.model.IdempotencyKey
import br.com.paymentsimulator.domain.model.Payment
import br.com.paymentsimulator.domain.model.PaymentMethod
import br.com.paymentsimulator.domain.model.PaymentRequest
import br.com.paymentsimulator.domain.model.PaymentResult
import br.com.paymentsimulator.domain.repository.PaymentIdGenerator
import br.com.paymentsimulator.domain.repository.PaymentRepository
import br.com.paymentsimulator.domain.repository.TimeProvider

class CreatePaymentUseCase(
    private val paymentRepository: PaymentRepository,
    private val paymentIds: PaymentIdGenerator,
    private val clock: TimeProvider,
) {
    suspend operator fun invoke(
        cart: Cart,
        method: PaymentMethod,
        idempotencyKey: IdempotencyKey,
    ): DomainResult<Payment> {
        paymentRepository.findByIdempotencyKey(idempotencyKey)?.let { existing ->
            return DomainResult.Success(existing)
        }
        val started = PaymentRequest(cart, method).start(
            id = paymentIds.next(),
            idempotencyKey = idempotencyKey,
            createdAt = clock.now(),
        )
        return when (started) {
            is PaymentResult.Failure -> DomainResult.Failure(started.error)
            is PaymentResult.Success -> {
                paymentRepository.save(started.payment)
                val stored = paymentRepository.findByIdempotencyKey(idempotencyKey) ?: started.payment
                DomainResult.Success(stored)
            }
        }
    }
}

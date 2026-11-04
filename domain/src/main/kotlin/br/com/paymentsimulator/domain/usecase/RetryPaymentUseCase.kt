package br.com.paymentsimulator.domain.usecase

import br.com.paymentsimulator.domain.model.DomainResult
import br.com.paymentsimulator.domain.model.IdempotencyKey
import br.com.paymentsimulator.domain.model.Payment
import br.com.paymentsimulator.domain.model.PaymentError
import br.com.paymentsimulator.domain.model.PaymentStatus
import br.com.paymentsimulator.domain.repository.PaymentRepository

class RetryPaymentUseCase(
    private val paymentRepository: PaymentRepository,
) {
    suspend operator fun invoke(idempotencyKey: IdempotencyKey): DomainResult<Payment> {
        val payment = paymentRepository.findByIdempotencyKey(idempotencyKey)
            ?: return DomainResult.Failure(PaymentError.PaymentNotFound)
        return when (payment.status) {
            PaymentStatus.UNKNOWN -> DomainResult.Failure(PaymentError.BlindRetryNotAllowed)
            PaymentStatus.APPROVED -> DomainResult.Failure(PaymentError.AlreadyApproved)
            PaymentStatus.PENDING,
            PaymentStatus.PROCESSING,
            -> DomainResult.Success(payment)
            PaymentStatus.DECLINED,
            PaymentStatus.FAILED,
            -> DomainResult.Failure(
                PaymentError.InvalidStatusTransition(
                    from = payment.status,
                    to = PaymentStatus.PROCESSING,
                ),
            )
        }
    }
}

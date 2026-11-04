package br.com.paymentsimulator.domain.usecase

import br.com.paymentsimulator.domain.model.DomainResult
import br.com.paymentsimulator.domain.model.Payment
import br.com.paymentsimulator.domain.model.PaymentError
import br.com.paymentsimulator.domain.model.PaymentId
import br.com.paymentsimulator.domain.model.PaymentResult
import br.com.paymentsimulator.domain.model.PaymentStatus
import br.com.paymentsimulator.domain.policy.PaymentStateMachine
import br.com.paymentsimulator.domain.repository.PaymentRepository

class ProcessPaymentUseCase(
    private val paymentRepository: PaymentRepository,
) {
    suspend operator fun invoke(paymentId: PaymentId): DomainResult<Payment> {
        val payment = paymentRepository.findById(paymentId)
            ?: return DomainResult.Failure(PaymentError.PaymentNotFound)
        payment.cart.validateForPayment()?.let { return DomainResult.Failure(it) }
        if (payment.status == PaymentStatus.APPROVED) {
            return DomainResult.Failure(PaymentError.AlreadyApproved)
        }
        if (payment.status == PaymentStatus.PROCESSING) {
            return DomainResult.Success(payment)
        }
        val transition = PaymentStateMachine.transition(payment, PaymentStatus.PROCESSING)
        return when (transition) {
            is PaymentResult.Failure -> DomainResult.Failure(transition.error)
            is PaymentResult.Success -> {
                paymentRepository.save(transition.payment)
                DomainResult.Success(transition.payment)
            }
        }
    }
}

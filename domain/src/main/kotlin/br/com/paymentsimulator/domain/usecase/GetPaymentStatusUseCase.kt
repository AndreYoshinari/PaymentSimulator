package br.com.paymentsimulator.domain.usecase

import br.com.paymentsimulator.domain.model.DomainResult
import br.com.paymentsimulator.domain.model.PaymentId
import br.com.paymentsimulator.domain.model.PaymentStatus
import br.com.paymentsimulator.domain.repository.PaymentRepository

class GetPaymentStatusUseCase(
    private val paymentRepository: PaymentRepository,
) {
    suspend operator fun invoke(paymentId: PaymentId): DomainResult<PaymentStatus> {
        return when (val payment = paymentRepository.findForStatus(paymentId)) {
            is DomainResult.Failure -> payment
            is DomainResult.Success -> DomainResult.Success(payment.value.status)
        }
    }
}

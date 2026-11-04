package br.com.paymentsimulator.domain.usecase

import br.com.paymentsimulator.domain.model.DomainResult
import br.com.paymentsimulator.domain.model.PaymentError
import br.com.paymentsimulator.domain.model.PaymentId
import br.com.paymentsimulator.domain.model.PaymentReceipt
import br.com.paymentsimulator.domain.model.toReceipt
import br.com.paymentsimulator.domain.repository.PaymentRepository

class GetPaymentReceiptUseCase(
    private val paymentRepository: PaymentRepository,
) {
    suspend operator fun invoke(paymentId: PaymentId): DomainResult<PaymentReceipt> {
        val payment = paymentRepository.findById(paymentId)
            ?: return DomainResult.Failure(PaymentError.PaymentNotFound)
        return DomainResult.Success(payment.toReceipt())
    }
}

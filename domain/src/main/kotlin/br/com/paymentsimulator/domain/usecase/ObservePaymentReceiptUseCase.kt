package br.com.paymentsimulator.domain.usecase

import br.com.paymentsimulator.domain.model.PaymentId
import br.com.paymentsimulator.domain.model.PaymentReceipt
import br.com.paymentsimulator.domain.model.toReceipt
import br.com.paymentsimulator.domain.repository.PaymentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObservePaymentReceiptUseCase(
    private val paymentRepository: PaymentRepository,
) {
    operator fun invoke(paymentId: PaymentId): Flow<PaymentReceipt?> {
        return paymentRepository.observeById(paymentId).map { payment -> payment?.toReceipt() }
    }
}

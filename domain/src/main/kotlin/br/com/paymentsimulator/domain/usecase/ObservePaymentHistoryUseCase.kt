package br.com.paymentsimulator.domain.usecase

import br.com.paymentsimulator.domain.model.Payment
import br.com.paymentsimulator.domain.repository.PaymentRepository
import kotlinx.coroutines.flow.Flow

class ObservePaymentHistoryUseCase(
    private val paymentRepository: PaymentRepository,
) {
    operator fun invoke(): Flow<List<Payment>> = paymentRepository.observeAll()
}

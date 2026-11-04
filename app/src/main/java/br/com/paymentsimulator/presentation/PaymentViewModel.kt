package br.com.paymentsimulator.presentation

import androidx.lifecycle.ViewModel
import br.com.paymentsimulator.domain.model.Cart
import br.com.paymentsimulator.domain.model.DomainResult
import br.com.paymentsimulator.domain.model.IdempotencyKey
import br.com.paymentsimulator.domain.model.Payment
import br.com.paymentsimulator.domain.model.PaymentId
import br.com.paymentsimulator.domain.model.PaymentMethod
import br.com.paymentsimulator.domain.model.PaymentReceipt
import br.com.paymentsimulator.domain.model.PaymentStatus
import br.com.paymentsimulator.domain.usecase.CreatePaymentUseCase
import br.com.paymentsimulator.domain.usecase.GetPaymentReceiptUseCase
import br.com.paymentsimulator.domain.usecase.GetPaymentStatusUseCase
import br.com.paymentsimulator.domain.usecase.ProcessPaymentUseCase
import br.com.paymentsimulator.domain.usecase.RetryPaymentUseCase

class PaymentViewModel(
    private val createPayment: CreatePaymentUseCase,
    private val processPayment: ProcessPaymentUseCase,
    private val getPaymentStatus: GetPaymentStatusUseCase,
    private val retryPayment: RetryPaymentUseCase,
    private val getPaymentReceipt: GetPaymentReceiptUseCase,
) : ViewModel() {
    suspend fun create(
        cart: Cart,
        method: PaymentMethod,
        idempotencyKey: IdempotencyKey,
    ): DomainResult<Payment> {
        return createPayment(cart, method, idempotencyKey)
    }

    suspend fun process(paymentId: PaymentId): DomainResult<Payment> {
        return processPayment(paymentId)
    }

    suspend fun status(paymentId: PaymentId): DomainResult<PaymentStatus> {
        return getPaymentStatus(paymentId)
    }

    suspend fun retry(idempotencyKey: IdempotencyKey): DomainResult<Payment> {
        return retryPayment(idempotencyKey)
    }

    suspend fun receipt(paymentId: PaymentId): DomainResult<PaymentReceipt> {
        return getPaymentReceipt(paymentId)
    }
}

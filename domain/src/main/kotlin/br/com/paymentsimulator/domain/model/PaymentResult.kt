package br.com.paymentsimulator.domain.model

sealed interface PaymentResult {
    data class Success(val payment: Payment) : PaymentResult

    data class Failure(val error: PaymentError) : PaymentResult
}

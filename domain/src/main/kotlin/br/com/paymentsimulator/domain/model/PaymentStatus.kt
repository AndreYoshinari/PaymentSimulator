package br.com.paymentsimulator.domain.model

enum class PaymentStatus {
    PENDING,
    PROCESSING,
    APPROVED,
    DECLINED,
    FAILED,
    UNKNOWN,
}

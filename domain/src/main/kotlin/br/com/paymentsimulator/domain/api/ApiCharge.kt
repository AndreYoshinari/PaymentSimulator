package br.com.paymentsimulator.domain.api

import br.com.paymentsimulator.domain.model.IdempotencyKey
import br.com.paymentsimulator.domain.model.Money
import br.com.paymentsimulator.domain.model.PaymentMethod

enum class ApiPaymentStatus {
    APPROVED,
    DECLINED,
    UNKNOWN,
}

data class ApiCharge(
    val idempotencyKey: IdempotencyKey,
    val amount: Money,
    val method: PaymentMethod,
    val status: ApiPaymentStatus,
)

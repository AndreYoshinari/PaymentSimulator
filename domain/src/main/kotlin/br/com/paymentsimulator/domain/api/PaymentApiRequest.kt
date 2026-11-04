package br.com.paymentsimulator.domain.api

import br.com.paymentsimulator.domain.model.IdempotencyKey
import br.com.paymentsimulator.domain.model.Money
import br.com.paymentsimulator.domain.model.PaymentMethod

data class PaymentApiRequest(
    val idempotencyKey: IdempotencyKey,
    val amount: Money,
    val method: PaymentMethod,
)

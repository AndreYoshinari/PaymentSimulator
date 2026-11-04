package br.com.paymentsimulator.data.remote

import br.com.paymentsimulator.domain.model.IdempotencyKey
import br.com.paymentsimulator.domain.model.Money
import br.com.paymentsimulator.domain.model.PaymentMethod
import br.com.paymentsimulator.domain.model.PaymentStatus

data class RemotePaymentSnapshot(
    val idempotencyKey: IdempotencyKey,
    val amount: Money,
    val method: PaymentMethod,
    val status: PaymentStatus,
)

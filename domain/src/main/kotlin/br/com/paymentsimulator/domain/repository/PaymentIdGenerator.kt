package br.com.paymentsimulator.domain.repository

import br.com.paymentsimulator.domain.model.PaymentId

fun interface PaymentIdGenerator {
    fun next(): PaymentId
}

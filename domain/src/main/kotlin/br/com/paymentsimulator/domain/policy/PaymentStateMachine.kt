package br.com.paymentsimulator.domain.policy

import br.com.paymentsimulator.domain.model.Payment
import br.com.paymentsimulator.domain.model.PaymentError
import br.com.paymentsimulator.domain.model.PaymentResult
import br.com.paymentsimulator.domain.model.PaymentStatus

object PaymentStateMachine {
    private val allowedTransitions: Map<PaymentStatus, Set<PaymentStatus>> = mapOf(
        PaymentStatus.PENDING to setOf(PaymentStatus.PROCESSING),
        PaymentStatus.PROCESSING to setOf(
            PaymentStatus.APPROVED,
            PaymentStatus.DECLINED,
            PaymentStatus.FAILED,
            PaymentStatus.UNKNOWN,
        ),
        PaymentStatus.UNKNOWN to setOf(
            PaymentStatus.APPROVED,
            PaymentStatus.DECLINED,
        ),
        PaymentStatus.APPROVED to emptySet(),
        PaymentStatus.DECLINED to emptySet(),
        PaymentStatus.FAILED to emptySet(),
    )

    fun canTransition(from: PaymentStatus, to: PaymentStatus): Boolean {
        return allowedTransitions.getValue(from).contains(to)
    }

    fun transition(payment: Payment, to: PaymentStatus): PaymentResult {
        if (!canTransition(payment.status, to)) {
            return PaymentResult.Failure(
                PaymentError.InvalidStatusTransition(from = payment.status, to = to),
            )
        }
        return PaymentResult.Success(payment.copy(status = to))
    }
}

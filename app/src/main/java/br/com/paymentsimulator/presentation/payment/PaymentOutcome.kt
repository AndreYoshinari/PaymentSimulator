package br.com.paymentsimulator.presentation.payment

import br.com.paymentsimulator.domain.model.DomainResult
import br.com.paymentsimulator.domain.model.PaymentError
import br.com.paymentsimulator.domain.model.PaymentStatus

sealed interface PaymentOutcome {
    data object Approved : PaymentOutcome

    data object Declined : PaymentOutcome

    data object CommunicationFailure : PaymentOutcome

    data object Unknown : PaymentOutcome

    data object Failed : PaymentOutcome

    data object InProgress : PaymentOutcome
}

fun DomainResult<PaymentStatus>.toPaymentOutcome(): PaymentOutcome? {
    return when (this) {
        is DomainResult.Success -> when (value) {
            PaymentStatus.APPROVED -> PaymentOutcome.Approved
            PaymentStatus.DECLINED -> PaymentOutcome.Declined
            PaymentStatus.FAILED -> PaymentOutcome.Failed
            // A timeout is stored as unknown. It must not be presented as a decline.
            PaymentStatus.UNKNOWN -> PaymentOutcome.Unknown
            PaymentStatus.PENDING,
            PaymentStatus.PROCESSING,
            -> PaymentOutcome.InProgress
        }
        is DomainResult.Failure -> when (error) {
            PaymentError.PaymentNotFound -> null
            PaymentError.PaymentTimedOut -> PaymentOutcome.Unknown
            else -> PaymentOutcome.CommunicationFailure
        }
    }
}

fun PaymentOutcome.canCheckAgain(): Boolean {
    return this == PaymentOutcome.Unknown ||
        this == PaymentOutcome.CommunicationFailure ||
        this == PaymentOutcome.InProgress
}

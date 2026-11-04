package br.com.paymentsimulator.domain.model

sealed interface PaymentError {
    data object EmptyCart : PaymentError

    data object NonPositiveAmount : PaymentError

    data object InvalidQuantity : PaymentError

    data object ProductNotFound : PaymentError

    data object ProductNotInCart : PaymentError

    data object PaymentNotFound : PaymentError

    data object AlreadyApproved : PaymentError

    data object BlindRetryNotAllowed : PaymentError

    data object PaymentTimedOut : PaymentError

    data object ServiceUnavailable : PaymentError

    data object IdempotencyConflict : PaymentError

    data object InvalidRemotePayload : PaymentError

    data class InvalidStatusTransition(
        val from: PaymentStatus,
        val to: PaymentStatus,
    ) : PaymentError
}

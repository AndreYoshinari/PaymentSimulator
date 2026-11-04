package br.com.paymentsimulator.data.mapper

import br.com.paymentsimulator.data.remote.PaymentRequestDto
import br.com.paymentsimulator.data.remote.PaymentResponseDto
import br.com.paymentsimulator.data.remote.RemotePaymentSnapshot
import br.com.paymentsimulator.domain.api.ApiCharge
import br.com.paymentsimulator.domain.api.ApiPaymentStatus
import br.com.paymentsimulator.domain.api.PaymentApiRequest
import br.com.paymentsimulator.domain.model.Currency
import br.com.paymentsimulator.domain.model.DomainResult
import br.com.paymentsimulator.domain.model.IdempotencyKey
import br.com.paymentsimulator.domain.model.Money
import br.com.paymentsimulator.domain.model.Payment
import br.com.paymentsimulator.domain.model.PaymentError
import br.com.paymentsimulator.domain.model.PaymentMethod
import br.com.paymentsimulator.domain.model.PaymentStatus

fun Payment.toRequestDto(): PaymentRequestDto {
    return PaymentRequestDto(
        idempotencyKey = idempotencyKey.value,
        amountInCents = amount.amountInCents,
        currency = amount.currency.name,
        method = method.name,
    )
}

fun PaymentRequestDto.toApiRequest(): DomainResult<PaymentApiRequest> {
    val key = idempotencyKey.takeIf { it.isNotBlank() }
        ?: return DomainResult.Failure(PaymentError.InvalidRemotePayload)
    val currency = currency.toOptionalCurrency()
        ?: return DomainResult.Failure(PaymentError.InvalidRemotePayload)
    val paymentMethod = method.toOptionalEnum<PaymentMethod>()
        ?: return DomainResult.Failure(PaymentError.InvalidRemotePayload)
    if (amountInCents < 0L) {
        return DomainResult.Failure(PaymentError.InvalidRemotePayload)
    }
    return DomainResult.Success(
        PaymentApiRequest(
            idempotencyKey = IdempotencyKey(key),
            amount = Money(amountInCents, currency),
            method = paymentMethod,
        ),
    )
}

fun ApiCharge.toResponseDto(): PaymentResponseDto {
    return PaymentResponseDto(
        idempotencyKey = idempotencyKey.value,
        amountInCents = amount.amountInCents,
        currency = amount.currency.name,
        method = method.name,
        status = status.name,
    )
}

fun PaymentResponseDto.toSnapshot(): DomainResult<RemotePaymentSnapshot> {
    val key = idempotencyKey.takeIf { it.isNotBlank() }
        ?: return DomainResult.Failure(PaymentError.InvalidRemotePayload)
    val currency = currency.toOptionalCurrency()
        ?: return DomainResult.Failure(PaymentError.InvalidRemotePayload)
    val paymentMethod = method.toOptionalEnum<PaymentMethod>()
        ?: return DomainResult.Failure(PaymentError.InvalidRemotePayload)
    val paymentStatus = status.toOptionalEnum<ApiPaymentStatus>()?.toDomain()
        ?: return DomainResult.Failure(PaymentError.InvalidRemotePayload)
    if (amountInCents < 0L) {
        return DomainResult.Failure(PaymentError.InvalidRemotePayload)
    }
    return DomainResult.Success(
        RemotePaymentSnapshot(
            idempotencyKey = IdempotencyKey(key),
            amount = Money(amountInCents, currency),
            method = paymentMethod,
            status = paymentStatus,
        ),
    )
}

private fun ApiPaymentStatus.toDomain(): PaymentStatus = when (this) {
    ApiPaymentStatus.APPROVED -> PaymentStatus.APPROVED
    ApiPaymentStatus.DECLINED -> PaymentStatus.DECLINED
    ApiPaymentStatus.UNKNOWN -> PaymentStatus.UNKNOWN
}

private fun String.toOptionalCurrency(): Currency? {
    return Currency.entries.firstOrNull { it.name == this }
}

private inline fun <reified T : Enum<T>> String.toOptionalEnum(): T? {
    return enumValues<T>().firstOrNull { it.name == this }
}

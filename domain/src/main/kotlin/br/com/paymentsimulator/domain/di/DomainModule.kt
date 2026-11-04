package br.com.paymentsimulator.domain.di

import br.com.paymentsimulator.domain.model.PaymentId
import br.com.paymentsimulator.domain.repository.PaymentIdGenerator
import br.com.paymentsimulator.domain.repository.TimeProvider
import br.com.paymentsimulator.domain.usecase.AddToCartUseCase
import br.com.paymentsimulator.domain.usecase.CalculateCartTotalUseCase
import br.com.paymentsimulator.domain.usecase.ChangeCartQuantityUseCase
import br.com.paymentsimulator.domain.usecase.CreatePaymentUseCase
import br.com.paymentsimulator.domain.usecase.GetPaymentReceiptUseCase
import br.com.paymentsimulator.domain.usecase.ObservePaymentHistoryUseCase
import br.com.paymentsimulator.domain.usecase.ObservePaymentReceiptUseCase
import br.com.paymentsimulator.domain.usecase.GetPaymentStatusUseCase
import br.com.paymentsimulator.domain.usecase.ProcessPaymentUseCase
import br.com.paymentsimulator.domain.usecase.RemoveFromCartUseCase
import br.com.paymentsimulator.domain.usecase.RetryPaymentUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import java.time.Instant
import java.util.UUID

val domainModule = module {
    single<PaymentIdGenerator> {
        PaymentIdGenerator { PaymentId(UUID.randomUUID()) }
    }
    single<TimeProvider> {
        TimeProvider { Instant.now() }
    }
    factoryOf(::AddToCartUseCase)
    factoryOf(::RemoveFromCartUseCase)
    factoryOf(::ChangeCartQuantityUseCase)
    factoryOf(::CalculateCartTotalUseCase)
    factoryOf(::CreatePaymentUseCase)
    factoryOf(::ProcessPaymentUseCase)
    factoryOf(::GetPaymentStatusUseCase)
    factoryOf(::RetryPaymentUseCase)
    factoryOf(::GetPaymentReceiptUseCase)
    factoryOf(::ObservePaymentHistoryUseCase)
    factoryOf(::ObservePaymentReceiptUseCase)
}

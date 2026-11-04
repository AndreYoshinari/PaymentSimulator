package br.com.paymentsimulator.data.di

import br.com.paymentsimulator.data.remote.FakePaymentApi
import br.com.paymentsimulator.domain.api.PaymentApi
import br.com.paymentsimulator.domain.simulation.PaymentSimulation
import org.koin.dsl.module

/**
 * Release still uses the fictional API. A Retrofit client replaces this binding,
 * without changing use cases.
 */
val paymentApiModule = module {
    single { FakePaymentApi() }
    single<PaymentApi> { get<FakePaymentApi>() }
    single<PaymentSimulation> { get<FakePaymentApi>() }
}

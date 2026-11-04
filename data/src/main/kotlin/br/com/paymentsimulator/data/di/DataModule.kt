package br.com.paymentsimulator.data.di

import br.com.paymentsimulator.data.local.AppDatabase
import br.com.paymentsimulator.data.local.PaymentDao
import br.com.paymentsimulator.data.local.PaymentLocalDataSource
import br.com.paymentsimulator.data.local.ProductDao
import br.com.paymentsimulator.data.local.ProductLocalDataSource
import br.com.paymentsimulator.data.local.RoomPaymentLocalDataSource
import br.com.paymentsimulator.data.local.RoomProductLocalDataSource
import br.com.paymentsimulator.data.local.createAppDatabase
import br.com.paymentsimulator.data.remote.FakePaymentRemoteDataSource
import br.com.paymentsimulator.data.remote.PaymentRemoteDataSource
import br.com.paymentsimulator.data.repository.InMemoryCartRepository
import br.com.paymentsimulator.data.repository.PaymentRepositoryImpl
import br.com.paymentsimulator.data.repository.ProductRepositoryImpl
import br.com.paymentsimulator.domain.repository.CartRepository
import br.com.paymentsimulator.domain.repository.PaymentRepository
import br.com.paymentsimulator.domain.repository.ProductRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Persistence and the remote port. [paymentApiModule] chooses the API for the build.
 * Use cases stay on the domain repository interfaces.
 */
val dataModule = module {
    includes(paymentApiModule)
    single { createAppDatabase(androidContext()) }
    single<ProductDao> { get<AppDatabase>().productDao() }
    single<PaymentDao> { get<AppDatabase>().paymentDao() }
    single<ProductLocalDataSource> { RoomProductLocalDataSource(get()) }
    single<PaymentLocalDataSource> { RoomPaymentLocalDataSource(get()) }
    single<PaymentRemoteDataSource> { FakePaymentRemoteDataSource(get()) }
    single<ProductRepository> { ProductRepositoryImpl(get()) }
    single<PaymentRepository> { PaymentRepositoryImpl(get(), get()) }
    single<CartRepository> { InMemoryCartRepository() }
}

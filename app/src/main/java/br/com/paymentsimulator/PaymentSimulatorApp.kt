package br.com.paymentsimulator

import android.app.Application
import br.com.paymentsimulator.data.di.dataModule
import br.com.paymentsimulator.domain.di.domainModule
import br.com.paymentsimulator.presentation.di.presentationModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class PaymentSimulatorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@PaymentSimulatorApp)
            modules(dataModule, domainModule, presentationModule)
        }
    }
}

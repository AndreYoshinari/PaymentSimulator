package br.com.paymentsimulator.di

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import br.com.paymentsimulator.data.di.dataModule
import br.com.paymentsimulator.domain.di.domainModule
import br.com.paymentsimulator.presentation.di.presentationModule
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify

@OptIn(KoinExperimentalAPI::class)
class KoinGraphTest {
    @Test
    fun modulesResolveWithoutCycles() {
        module {
            includes(dataModule, domainModule, presentationModule)
        }.verify(
            extraTypes = listOf(Context::class, SavedStateHandle::class),
        )
    }
}

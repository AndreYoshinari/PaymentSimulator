package br.com.paymentsimulator.presentation.di

import br.com.paymentsimulator.presentation.PaymentViewModel
import br.com.paymentsimulator.presentation.cart.CartViewModel
import br.com.paymentsimulator.presentation.checkout.CheckoutViewModel
import br.com.paymentsimulator.presentation.home.HomeViewModel
import br.com.paymentsimulator.presentation.paymentmethod.PaymentMethodViewModel
import br.com.paymentsimulator.presentation.paymentresult.PaymentResultViewModel
import br.com.paymentsimulator.presentation.processing.ProcessingViewModel
import br.com.paymentsimulator.presentation.receipt.ReceiptViewModel
import br.com.paymentsimulator.presentation.transactions.TransactionsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val presentationModule = module {
    viewModelOf(::HomeViewModel)
    viewModelOf(::CartViewModel)
    viewModelOf(::CheckoutViewModel)
    viewModelOf(::PaymentMethodViewModel)
    viewModelOf(::ProcessingViewModel)
    viewModelOf(::PaymentResultViewModel)
    viewModelOf(::ReceiptViewModel)
    viewModelOf(::TransactionsViewModel)
    viewModelOf(::PaymentViewModel)
}

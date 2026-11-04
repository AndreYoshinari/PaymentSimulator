package br.com.paymentsimulator.domain.usecase

import br.com.paymentsimulator.domain.model.Cart
import br.com.paymentsimulator.domain.model.Money

class CalculateCartTotalUseCase {
    operator fun invoke(cart: Cart): Money = cart.total
}

package br.com.paymentsimulator.domain.usecase

import br.com.paymentsimulator.domain.model.Cart
import br.com.paymentsimulator.domain.model.DomainResult
import br.com.paymentsimulator.domain.model.PaymentError
import br.com.paymentsimulator.domain.model.ProductId

class RemoveFromCartUseCase {
    operator fun invoke(cart: Cart, productId: ProductId): DomainResult<Cart> {
        if (cart.items.none { it.product.id == productId }) {
            return DomainResult.Failure(PaymentError.ProductNotInCart)
        }
        return DomainResult.Success(cart.without(productId))
    }
}

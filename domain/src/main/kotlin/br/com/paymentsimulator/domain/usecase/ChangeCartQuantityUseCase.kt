package br.com.paymentsimulator.domain.usecase

import br.com.paymentsimulator.domain.model.Cart
import br.com.paymentsimulator.domain.model.DomainResult
import br.com.paymentsimulator.domain.model.PaymentError
import br.com.paymentsimulator.domain.model.ProductId

class ChangeCartQuantityUseCase {
    operator fun invoke(
        cart: Cart,
        productId: ProductId,
        quantity: Int,
    ): DomainResult<Cart> {
        if (quantity <= 0) {
            return DomainResult.Failure(PaymentError.InvalidQuantity)
        }
        if (cart.items.none { it.product.id == productId }) {
            return DomainResult.Failure(PaymentError.ProductNotInCart)
        }
        return DomainResult.Success(
            cart.copy(
                items = cart.items.map { item ->
                    if (item.product.id == productId) item.copy(quantity = quantity) else item
                },
            ),
        )
    }
}

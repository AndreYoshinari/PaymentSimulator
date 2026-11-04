package br.com.paymentsimulator.domain.usecase

import br.com.paymentsimulator.domain.model.Cart
import br.com.paymentsimulator.domain.model.DomainResult
import br.com.paymentsimulator.domain.model.PaymentError
import br.com.paymentsimulator.domain.model.ProductId
import br.com.paymentsimulator.domain.repository.ProductRepository

class AddToCartUseCase(
    private val productRepository: ProductRepository,
) {
    suspend operator fun invoke(
        cart: Cart,
        productId: ProductId,
        quantity: Int,
    ): DomainResult<Cart> {
        if (quantity <= 0) {
            return DomainResult.Failure(PaymentError.InvalidQuantity)
        }
        val product = productRepository.findById(productId)
            ?: return DomainResult.Failure(PaymentError.ProductNotFound)
        return DomainResult.Success(cart.add(product, quantity))
    }
}

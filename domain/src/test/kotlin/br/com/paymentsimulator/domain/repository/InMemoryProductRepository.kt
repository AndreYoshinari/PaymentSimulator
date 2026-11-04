package br.com.paymentsimulator.domain.repository

import br.com.paymentsimulator.domain.model.Product
import br.com.paymentsimulator.domain.model.ProductId

class InMemoryProductRepository(
    private val products: List<Product>,
) : ProductRepository {
    override suspend fun getAll(): List<Product> = products

    override suspend fun findById(id: ProductId): Product? {
        return products.find { it.id == id }
    }
}

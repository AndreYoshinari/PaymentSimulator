package br.com.paymentsimulator.data.local

import br.com.paymentsimulator.domain.model.Product
import br.com.paymentsimulator.domain.model.ProductId

class InMemoryProductLocalDataSource(
    products: List<Product> = emptyList(),
) : ProductLocalDataSource {
    private val stored = products.associateBy { it.id }.toMutableMap()

    override suspend fun getAll(): List<Product> = stored.values.toList()

    override suspend fun findById(id: ProductId): Product? = stored[id]
}

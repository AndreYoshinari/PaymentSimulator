package br.com.paymentsimulator.data.repository

import br.com.paymentsimulator.data.local.ProductLocalDataSource
import br.com.paymentsimulator.domain.model.Product
import br.com.paymentsimulator.domain.model.ProductId
import br.com.paymentsimulator.domain.repository.ProductRepository

class ProductRepositoryImpl(
    private val local: ProductLocalDataSource,
) : ProductRepository {
    override suspend fun getAll(): List<Product> = local.getAll()

    override suspend fun findById(id: ProductId): Product? = local.findById(id)
}

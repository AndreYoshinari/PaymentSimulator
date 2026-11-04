package br.com.paymentsimulator.data.local

import br.com.paymentsimulator.domain.model.Product
import br.com.paymentsimulator.domain.model.ProductId

interface ProductLocalDataSource {
    suspend fun getAll(): List<Product>

    suspend fun findById(id: ProductId): Product?
}

package br.com.paymentsimulator.domain.repository

import br.com.paymentsimulator.domain.model.Product
import br.com.paymentsimulator.domain.model.ProductId

interface ProductRepository {
    suspend fun getAll(): List<Product>

    suspend fun findById(id: ProductId): Product?
}

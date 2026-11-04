package br.com.paymentsimulator.data.local

import br.com.paymentsimulator.data.mapper.toDomain
import br.com.paymentsimulator.data.mapper.toEntity
import br.com.paymentsimulator.domain.model.Product
import br.com.paymentsimulator.domain.model.ProductId

class RoomProductLocalDataSource(
    private val dao: ProductDao,
    private val catalog: List<Product> = ProductCatalog.samples,
) : ProductLocalDataSource {
    override suspend fun getAll(): List<Product> {
        ensureSeeded()
        return dao.getAll().map { it.toDomain() }
    }

    override suspend fun findById(id: ProductId): Product? {
        ensureSeeded()
        return dao.findById(id.value.toString())?.toDomain()
    }

    private suspend fun ensureSeeded() {
        if (dao.count() == 0) {
            dao.insertAll(catalog.map { it.toEntity() })
        }
    }
}

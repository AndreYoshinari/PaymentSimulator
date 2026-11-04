package br.com.paymentsimulator.data.repository

import br.com.paymentsimulator.data.local.InMemoryProductLocalDataSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductRepositoryImplTest {
    private val coffee = sampleProduct()
    private val tea = sampleProduct(
        cents = 300,
        name = "Tea",
        id = "22222222-2222-2222-2222-222222222222",
    )
    private val repository = ProductRepositoryImpl(
        InMemoryProductLocalDataSource(listOf(coffee, tea)),
    )

    @Test
    fun getAllReturnsTheStoredCatalog() {
        runBlocking {
            assertEquals(listOf(coffee, tea), repository.getAll())
        }
    }

    @Test
    fun findByIdReturnsTheProductOrNull() {
        runBlocking {
            assertEquals(coffee, repository.findById(coffee.id))
            assertNull(repository.findById(sampleProduct(id = "33333333-3333-3333-3333-333333333333").id))
        }
    }
}

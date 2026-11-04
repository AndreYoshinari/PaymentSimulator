package br.com.paymentsimulator.domain.model

import java.util.UUID

@JvmInline
value class ProductId(val value: UUID)

data class Product(
    val id: ProductId,
    val name: String,
    val price: Money,
    val description: String = "",
) {
    init {
        require(name.isNotBlank()) { "Product name must not be blank." }
    }
}

package br.com.paymentsimulator.data.mapper

import br.com.paymentsimulator.data.local.ProductEntity
import br.com.paymentsimulator.domain.model.Currency
import br.com.paymentsimulator.domain.model.Money
import br.com.paymentsimulator.domain.model.Product
import br.com.paymentsimulator.domain.model.ProductId
import java.util.UUID

fun Product.toEntity(): ProductEntity {
    return ProductEntity(
        id = id.value.toString(),
        name = name,
        description = description,
        priceInCents = price.amountInCents,
        currency = price.currency.name,
    )
}

fun ProductEntity.toDomain(): Product {
    return Product(
        id = ProductId(UUID.fromString(id)),
        name = name,
        description = description,
        price = Money(
            amountInCents = priceInCents,
            currency = currency.toCurrency(),
        ),
    )
}

internal fun String.toCurrency(): Currency {
    return Currency.entries.firstOrNull { it.name == this }
        ?: throw IllegalArgumentException("Unknown currency: $this")
}

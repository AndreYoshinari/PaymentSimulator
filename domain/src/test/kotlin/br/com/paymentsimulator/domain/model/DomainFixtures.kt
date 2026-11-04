package br.com.paymentsimulator.domain.model

import java.util.UUID

fun product(
    name: String = "Coffee",
    priceInCents: Long = 1_250,
    id: String = "11111111-1111-1111-1111-111111111111",
): Product = Product(
    id = ProductId(UUID.fromString(id)),
    name = name,
    price = Money(priceInCents),
)

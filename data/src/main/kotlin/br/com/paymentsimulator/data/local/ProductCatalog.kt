package br.com.paymentsimulator.data.local

import br.com.paymentsimulator.domain.model.Money
import br.com.paymentsimulator.domain.model.Product
import br.com.paymentsimulator.domain.model.ProductId
import java.util.UUID

object ProductCatalog {
    val samples: List<Product> = listOf(
        Product(
            id = ProductId(UUID.fromString("11111111-1111-1111-1111-111111111111")),
            name = "Café",
            description = "Café coado, item fictício do cardápio.",
            price = Money(500),
        ),
        Product(
            id = ProductId(UUID.fromString("22222222-2222-2222-2222-222222222222")),
            name = "Chá",
            description = "Chá preto, sem cobrança real.",
            price = Money(300),
        ),
        Product(
            id = ProductId(UUID.fromString("33333333-3333-3333-3333-333333333333")),
            name = "Água",
            description = "Água sem gás, apenas para simular o pedido.",
            price = Money(200),
        ),
    )
}

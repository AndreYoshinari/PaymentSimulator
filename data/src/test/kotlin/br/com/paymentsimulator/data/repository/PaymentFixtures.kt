package br.com.paymentsimulator.data.repository

import br.com.paymentsimulator.domain.model.Cart
import br.com.paymentsimulator.domain.model.CartItem
import br.com.paymentsimulator.domain.model.IdempotencyKey
import br.com.paymentsimulator.domain.model.Money
import br.com.paymentsimulator.domain.model.Payment
import br.com.paymentsimulator.domain.model.PaymentId
import br.com.paymentsimulator.domain.model.PaymentMethod
import br.com.paymentsimulator.domain.model.PaymentStatus
import br.com.paymentsimulator.domain.model.Product
import br.com.paymentsimulator.domain.model.ProductId
import java.time.Instant
import java.util.UUID

fun sampleProduct(
    cents: Long = 1_500,
    name: String = "Coffee",
    id: String = "11111111-1111-1111-1111-111111111111",
): Product {
    return Product(
        id = ProductId(UUID.fromString(id)),
        name = name,
        price = Money(cents),
    )
}

fun samplePayment(
    status: PaymentStatus = PaymentStatus.PENDING,
    cents: Long = 1_500,
    key: String = "operation-1",
    method: PaymentMethod = PaymentMethod.PIX,
    id: String = "cccccccc-cccc-cccc-cccc-cccccccccccc",
    createdAt: Instant = Instant.parse("2026-01-15T12:00:00Z"),
    quantity: Int = 1,
): Payment {
    return Payment(
        id = PaymentId(UUID.fromString(id)),
        idempotencyKey = IdempotencyKey(key),
        cart = Cart(listOf(CartItem(sampleProduct(cents = cents), quantity = quantity))),
        method = method,
        status = status,
        createdAt = createdAt,
    )
}

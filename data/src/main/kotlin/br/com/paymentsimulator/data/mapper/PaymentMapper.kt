package br.com.paymentsimulator.data.mapper

import br.com.paymentsimulator.data.local.PaymentEntity
import br.com.paymentsimulator.data.local.PaymentItemEntity
import br.com.paymentsimulator.data.local.PaymentWithItems
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

fun Payment.toEntity(): PaymentEntity {
    return PaymentEntity(
        id = id.value.toString(),
        idempotencyKey = idempotencyKey.value,
        amountInCents = amount.amountInCents,
        currency = amount.currency.name,
        method = method.name,
        status = status.name,
        createdAtEpochMillis = createdAt.toEpochMilli(),
    )
}

fun Payment.toItemEntities(): List<PaymentItemEntity> {
    return cart.items.map { item ->
        PaymentItemEntity(
            paymentId = id.value.toString(),
            productId = item.product.id.value.toString(),
            productName = item.product.name,
            priceInCents = item.product.price.amountInCents,
            currency = item.product.price.currency.name,
            quantity = item.quantity,
        )
    }
}

fun PaymentWithItems.toDomain(): Payment {
    val restored = Payment(
        id = PaymentId(UUID.fromString(payment.id)),
        idempotencyKey = IdempotencyKey(payment.idempotencyKey),
        cart = Cart(items.map { it.toCartItem() }),
        method = payment.method.toEnum<PaymentMethod>(),
        status = payment.status.toEnum<PaymentStatus>(),
        createdAt = Instant.ofEpochMilli(payment.createdAtEpochMillis),
    )
    require(restored.amount.amountInCents == payment.amountInCents) {
        "Stored amount does not match the payment items."
    }
    require(restored.amount.currency.name == payment.currency) {
        "Stored currency does not match the payment items."
    }
    return restored
}

private fun PaymentItemEntity.toCartItem(): CartItem {
    return CartItem(
        product = Product(
            id = ProductId(UUID.fromString(productId)),
            name = productName,
            price = Money(priceInCents, currency.toCurrency()),
        ),
        quantity = quantity,
    )
}

private inline fun <reified T : Enum<T>> String.toEnum(): T {
    return enumValues<T>().firstOrNull { it.name == this }
        ?: throw IllegalArgumentException("Unknown ${T::class.simpleName}: $this")
}

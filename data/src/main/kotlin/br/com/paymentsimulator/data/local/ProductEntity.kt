package br.com.paymentsimulator.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val priceInCents: Long,
    val currency: String,
)

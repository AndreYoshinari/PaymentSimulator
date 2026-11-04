package br.com.paymentsimulator.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProductDao {
    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Int

    @Query("SELECT * FROM products ORDER BY name")
    suspend fun getAll(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun findById(id: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)
}

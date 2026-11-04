package br.com.paymentsimulator.domain.repository

import br.com.paymentsimulator.domain.model.Cart
import br.com.paymentsimulator.domain.model.DomainResult
import kotlinx.coroutines.flow.Flow

interface CartRepository {
    fun observe(): Flow<Cart>

    suspend fun modify(block: suspend (Cart) -> DomainResult<Cart>): DomainResult<Cart>
}

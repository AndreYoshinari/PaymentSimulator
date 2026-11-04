package br.com.paymentsimulator.data.repository

import br.com.paymentsimulator.domain.model.Cart
import br.com.paymentsimulator.domain.model.DomainResult
import br.com.paymentsimulator.domain.repository.CartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryCartRepository : CartRepository {
    private val mutex = Mutex()
    private val cart = MutableStateFlow(Cart())

    override fun observe(): Flow<Cart> = cart.asStateFlow()

    override suspend fun modify(block: suspend (Cart) -> DomainResult<Cart>): DomainResult<Cart> {
        return mutex.withLock {
            when (val result = block(cart.value)) {
                is DomainResult.Success -> {
                    cart.value = result.value
                    result
                }
                is DomainResult.Failure -> result
            }
        }
    }
}

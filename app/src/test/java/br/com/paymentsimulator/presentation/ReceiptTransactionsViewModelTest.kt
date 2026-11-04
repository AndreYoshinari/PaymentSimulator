package br.com.paymentsimulator.presentation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import br.com.paymentsimulator.domain.model.Cart
import br.com.paymentsimulator.domain.model.CartItem
import br.com.paymentsimulator.domain.model.DomainResult
import br.com.paymentsimulator.domain.model.IdempotencyKey
import br.com.paymentsimulator.domain.model.Money
import br.com.paymentsimulator.domain.model.Payment
import br.com.paymentsimulator.domain.model.PaymentId
import br.com.paymentsimulator.domain.model.PaymentMethod
import br.com.paymentsimulator.domain.model.PaymentStatus
import br.com.paymentsimulator.domain.model.Product
import br.com.paymentsimulator.domain.model.ProductId
import br.com.paymentsimulator.domain.repository.PaymentRepository
import br.com.paymentsimulator.domain.usecase.ObservePaymentHistoryUseCase
import br.com.paymentsimulator.domain.usecase.ObservePaymentReceiptUseCase
import br.com.paymentsimulator.presentation.receipt.ReceiptUiState
import br.com.paymentsimulator.presentation.receipt.ReceiptViewModel
import br.com.paymentsimulator.presentation.receipt.fictionalConfirmationCode
import br.com.paymentsimulator.presentation.transactions.TransactionsUiState
import br.com.paymentsimulator.presentation.transactions.TransactionsViewModel
import br.com.paymentsimulator.ui.format.formatDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ReceiptTransactionsViewModelTest {
    private val olderId = PaymentId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
    private val newerId = PaymentId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"))
    private val payments = HistoryPayments()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun historyListsNewestFirstAndFiltersByStatus() = runTest {
        payments.history.value = listOf(
            payment(olderId, PaymentStatus.DECLINED, "2026-01-15T12:00:00Z"),
            payment(newerId, PaymentStatus.APPROVED, "2026-01-16T12:00:00Z"),
        )
        val viewModel = TransactionsViewModel(ObservePaymentHistoryUseCase(payments))

        viewModel.state.test {
            val shown = awaitSuccess()
            assertEquals(newerId.value.toString(), shown.transactions.first().id)
            assertEquals("R$ 5,00", shown.transactions.first().amount)
            assertEquals(PaymentStatus.APPROVED, shown.transactions.first().status)
            viewModel.select(olderId.value.toString())
            val selected = awaitSuccess()
            assertEquals(olderId.value.toString(), selected.selected?.id)
            viewModel.selectFilter(PaymentStatus.DECLINED)
            val filtered = awaitSuccess()
            assertEquals(olderId.value.toString(), filtered.transactions.single().id)
            assertEquals(PaymentMethod.PIX, filtered.selected?.method)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emptyHistoryAndFlowFailureAreVisible() = runTest {
        val empty = TransactionsViewModel(ObservePaymentHistoryUseCase(payments))
        empty.state.test {
            val shown = awaitSuccess()
            assertTrue(shown.transactions.isEmpty())
            assertEquals(false, shown.hasAnyPayment)
            cancelAndIgnoreRemainingEvents()
        }

        val failing = TransactionsViewModel(ObservePaymentHistoryUseCase(FailingPayments()))
        failing.state.test {
            while (awaitItem() !is TransactionsUiState.Error) {
                Unit
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun receiptShowsTheStoredFieldsAndAFictionalConfirmation() = runTest {
        val stored = payment(olderId, PaymentStatus.APPROVED, "2026-01-15T12:00:00Z")
        payments.byId.value = stored
        val viewModel = ReceiptViewModel(
            savedStateHandle = SavedStateHandle(mapOf("paymentId" to olderId.value.toString())),
            observeReceipt = ObservePaymentReceiptUseCase(payments),
        )

        viewModel.state.test {
            val ready = awaitReceipt()
            assertEquals(olderId.value.toString(), ready.content.id)
            assertEquals(stored.createdAt.formatDateTime(), ready.content.createdAt)
            assertEquals("R$ 5,00", ready.content.amount)
            assertEquals(PaymentMethod.PIX, ready.content.method)
            assertEquals(PaymentStatus.APPROVED, ready.content.status)
            assertEquals(fictionalConfirmationCode(olderId.value.toString()), ready.content.confirmationCode)
            assertTrue(ready.content.confirmationCode.startsWith("SIM-"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun aFilterWithoutMatchesKeepsTheHistoryAndReceiptRetryRecovers() = runTest {
        payments.history.value = listOf(
            payment(olderId, PaymentStatus.APPROVED, "2026-01-15T12:00:00Z"),
        )
        val transactions = TransactionsViewModel(ObservePaymentHistoryUseCase(payments))
        transactions.state.test {
            awaitSuccess()
            transactions.selectFilter(PaymentStatus.DECLINED)
            val filtered = awaitSuccess()
            assertEquals(true, filtered.hasAnyPayment)
            assertTrue(filtered.transactions.isEmpty())
            assertNull(filtered.selected)
            cancelAndIgnoreRemainingEvents()
        }

        val stored = payment(olderId, PaymentStatus.APPROVED, "2026-01-15T12:00:00Z")
        val recovering = RecoveringReceipts(stored)
        val receipt = ReceiptViewModel(
            savedStateHandle = SavedStateHandle(mapOf("paymentId" to olderId.value.toString())),
            observeReceipt = ObservePaymentReceiptUseCase(recovering),
        )
        receipt.state.test {
            while (awaitItem() !is ReceiptUiState.Error) {
                Unit
            }
            receipt.retry()
            val ready = awaitReceipt()
            assertEquals(PaymentStatus.APPROVED, ready.content.status)
            assertEquals("R$ 5,00", ready.content.amount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun missingReceiptIsNotFound() = runTest {
        val viewModel = ReceiptViewModel(
            savedStateHandle = SavedStateHandle(mapOf("paymentId" to "not-a-uuid")),
            observeReceipt = ObservePaymentReceiptUseCase(payments),
        )

        assertEquals(ReceiptUiState.NotFound, viewModel.state.value)
        assertNull(payments.byId.value)
    }

    private fun payment(id: PaymentId, status: PaymentStatus, createdAt: String): Payment {
        val product = Product(
            id = ProductId(UUID.fromString("11111111-1111-1111-1111-111111111111")),
            name = "Café",
            price = Money(500),
        )
        return Payment(
            id = id,
            idempotencyKey = IdempotencyKey(id.value.toString()),
            cart = Cart(listOf(CartItem(product, quantity = 1))),
            method = PaymentMethod.PIX,
            status = status,
            createdAt = Instant.parse(createdAt),
        )
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<ReceiptUiState>.awaitReceipt(): ReceiptUiState.Ready {
        while (true) {
            val item = awaitItem()
            if (item is ReceiptUiState.Ready) {
                return item
            }
        }
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<TransactionsUiState>.awaitSuccess():
        br.com.paymentsimulator.presentation.transactions.TransactionsContent {
        while (true) {
            val item = awaitItem()
            if (item is TransactionsUiState.Success) {
                return item.content
            }
        }
    }
}

private class HistoryPayments : PaymentRepository {
    val history = MutableStateFlow<List<Payment>>(emptyList())
    val byId = MutableStateFlow<Payment?>(null)

    override suspend fun save(payment: Payment) = Unit

    override suspend fun findById(id: PaymentId): Payment? = null

    override suspend fun findByIdempotencyKey(key: IdempotencyKey): Payment? = null

    override suspend fun findForStatus(id: PaymentId): DomainResult<Payment> {
        error("History observation must not query the remote status.")
    }

    override fun observeAll(): Flow<List<Payment>> = history

    override fun observeById(id: PaymentId): Flow<Payment?> {
        return byId.map { payment -> payment?.takeIf { it.id == id } }
    }
}

private class RecoveringReceipts(
    private val payment: Payment,
) : PaymentRepository {
    private var reads = 0

    override suspend fun save(payment: Payment) = Unit

    override suspend fun findById(id: PaymentId): Payment? = null

    override suspend fun findByIdempotencyKey(key: IdempotencyKey): Payment? = null

    override suspend fun findForStatus(id: PaymentId): DomainResult<Payment> {
        error("Receipt observation must not query the remote status.")
    }

    override fun observeAll(): Flow<List<Payment>> = flow { error("database") }

    override fun observeById(id: PaymentId): Flow<Payment?> = flow {
        reads += 1
        if (reads == 1) {
            error("database")
        }
        emit(payment.takeIf { it.id == id })
    }
}

private class FailingPayments : PaymentRepository {
    override suspend fun save(payment: Payment) = Unit

    override suspend fun findById(id: PaymentId): Payment? = null

    override suspend fun findByIdempotencyKey(key: IdempotencyKey): Payment? = null

    override suspend fun findForStatus(id: PaymentId): DomainResult<Payment> {
        error("History observation must not query the remote status.")
    }

    override fun observeAll(): Flow<List<Payment>> = flow { error("database") }

    override fun observeById(id: PaymentId): Flow<Payment?> = flow { error("database") }
}

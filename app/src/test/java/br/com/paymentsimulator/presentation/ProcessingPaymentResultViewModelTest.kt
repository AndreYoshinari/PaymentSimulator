package br.com.paymentsimulator.presentation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import br.com.paymentsimulator.domain.model.Cart
import br.com.paymentsimulator.domain.model.CartItem
import br.com.paymentsimulator.domain.model.DomainResult
import br.com.paymentsimulator.domain.model.IdempotencyKey
import br.com.paymentsimulator.domain.model.Money
import br.com.paymentsimulator.domain.model.Payment
import br.com.paymentsimulator.domain.model.PaymentError
import br.com.paymentsimulator.domain.model.PaymentId
import br.com.paymentsimulator.domain.model.PaymentMethod
import br.com.paymentsimulator.domain.model.PaymentStatus
import br.com.paymentsimulator.domain.model.Product
import br.com.paymentsimulator.domain.model.ProductId
import br.com.paymentsimulator.domain.repository.PaymentRepository
import br.com.paymentsimulator.domain.usecase.GetPaymentStatusUseCase
import br.com.paymentsimulator.presentation.payment.PaymentOutcome
import br.com.paymentsimulator.presentation.paymentresult.PaymentResultUiState
import br.com.paymentsimulator.presentation.paymentresult.PaymentResultViewModel
import br.com.paymentsimulator.presentation.processing.ProcessingUiState
import br.com.paymentsimulator.presentation.processing.ProcessingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ProcessingPaymentResultViewModelTest {
    private val paymentId = PaymentId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
    private val payments = ScriptedPayments()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun processingStaysVisibleWhileThePaymentIsStillProcessing() = runTest {
        payments.results += DomainResult.Success(payment(PaymentStatus.PROCESSING))
        val viewModel = processingViewModel()

        viewModel.resolved.test {
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(viewModel.state.value is ProcessingUiState.Processing)
        assertEquals(0, payments.saves)
        assertEquals(1, payments.statusReads)
    }

    @Test
    fun resolvedPaymentOpensTheResultWithoutAnotherCharge() = runTest {
        payments.results += DomainResult.Success(payment(PaymentStatus.APPROVED))
        val viewModel = processingViewModel()

        viewModel.resolved.test {
            assertEquals(paymentId.value.toString(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(0, payments.saves)
    }

    @Test
    fun timeoutIsUnknownAndCanBeQueriedAgainWithoutANewCharge() = runTest {
        payments.results += DomainResult.Success(payment(PaymentStatus.UNKNOWN))
        payments.results += DomainResult.Success(payment(PaymentStatus.APPROVED))
        val viewModel = resultViewModel()

        val first = viewModel.state.value as PaymentResultUiState.Ready
        assertEquals(PaymentOutcome.Unknown, first.content.outcome)
        assertNotEquals(PaymentOutcome.Declined, first.content.outcome)
        assertTrue(first.content.canCheckAgain)

        viewModel.checkAgain()

        val second = viewModel.state.value as PaymentResultUiState.Ready
        assertEquals(PaymentOutcome.Approved, second.content.outcome)
        assertEquals(2, payments.statusReads)
        assertEquals(0, payments.saves)
    }

    @Test
    fun communicationFailureIsNotADecline() = runTest {
        payments.results += DomainResult.Failure(PaymentError.ServiceUnavailable)
        val viewModel = resultViewModel()

        val content = (viewModel.state.value as PaymentResultUiState.Ready).content
        assertEquals(PaymentOutcome.CommunicationFailure, content.outcome)
        assertNotEquals(PaymentOutcome.Declined, content.outcome)
        assertEquals(0, payments.saves)
    }

    @Test
    fun failedAndTimedOutResultsAreNotDeclines() = runTest {
        payments.results += DomainResult.Success(payment(PaymentStatus.FAILED))
        val failed = resultViewModel()
        val failedContent = (failed.state.value as PaymentResultUiState.Ready).content
        assertEquals(PaymentOutcome.Failed, failedContent.outcome)
        assertNotEquals(PaymentOutcome.Declined, failedContent.outcome)
        assertEquals(false, failedContent.canCheckAgain)

        payments.results.clear()
        payments.results += DomainResult.Failure(PaymentError.PaymentTimedOut)
        val timedOut = resultViewModel()
        val timedOutContent = (timedOut.state.value as PaymentResultUiState.Ready).content
        assertEquals(PaymentOutcome.Unknown, timedOutContent.outcome)
        assertNotEquals(PaymentOutcome.Declined, timedOutContent.outcome)
        assertEquals(0, payments.saves)
    }

    @Test
    fun anInvalidProcessingIdDoesNotQueryStatus() = runTest {
        val viewModel = ProcessingViewModel(
            savedStateHandle = SavedStateHandle(mapOf("paymentId" to "not-a-uuid")),
            getPaymentStatus = GetPaymentStatusUseCase(payments),
        )

        assertTrue(viewModel.state.value is ProcessingUiState.NotFound)
        viewModel.resolved.test {
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(0, payments.statusReads)
        assertEquals(0, payments.saves)
    }

    @Test
    fun declinedPaymentStaysDeclinedWhenCheckedAgain() = runTest {
        payments.results += DomainResult.Success(payment(PaymentStatus.DECLINED))
        val viewModel = resultViewModel()

        viewModel.checkAgain()

        val content = (viewModel.state.value as PaymentResultUiState.Ready).content
        assertEquals(PaymentOutcome.Declined, content.outcome)
        assertEquals(1, payments.statusReads)
        assertEquals(0, payments.saves)
    }

    private fun processingViewModel(): ProcessingViewModel {
        return ProcessingViewModel(
            savedStateHandle = SavedStateHandle(mapOf("paymentId" to paymentId.value.toString())),
            getPaymentStatus = GetPaymentStatusUseCase(payments),
        )
    }

    private fun resultViewModel(): PaymentResultViewModel {
        return PaymentResultViewModel(
            savedStateHandle = SavedStateHandle(mapOf("paymentId" to paymentId.value.toString())),
            getPaymentStatus = GetPaymentStatusUseCase(payments),
        )
    }

    private fun payment(status: PaymentStatus): Payment {
        val product = Product(
            id = ProductId(UUID.fromString("11111111-1111-1111-1111-111111111111")),
            name = "Café",
            price = Money(500),
        )
        return Payment(
            id = paymentId,
            idempotencyKey = IdempotencyKey("operation-1"),
            cart = Cart(listOf(CartItem(product, quantity = 1))),
            method = PaymentMethod.PIX,
            status = status,
            createdAt = Instant.parse("2026-01-15T12:00:00Z"),
        )
    }
}

private class ScriptedPayments : PaymentRepository {
    val results = ArrayDeque<DomainResult<Payment>>()
    var saves = 0
    var statusReads = 0

    override suspend fun save(payment: Payment) {
        saves += 1
    }

    override suspend fun findById(id: PaymentId): Payment? = null

    override suspend fun findByIdempotencyKey(key: IdempotencyKey): Payment? = null

    override suspend fun findForStatus(id: PaymentId): DomainResult<Payment> {
        statusReads += 1
        return results.removeFirst()
    }

    override fun observeAll(): Flow<List<Payment>> = flowOf(emptyList())

    override fun observeById(id: PaymentId): Flow<Payment?> = flowOf(null)
}

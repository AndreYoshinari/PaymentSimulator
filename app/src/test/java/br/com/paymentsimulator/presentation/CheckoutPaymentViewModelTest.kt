package br.com.paymentsimulator.presentation

import app.cash.turbine.test
import br.com.paymentsimulator.data.repository.InMemoryCartRepository
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
import br.com.paymentsimulator.domain.repository.PaymentIdGenerator
import br.com.paymentsimulator.domain.repository.PaymentRepository
import br.com.paymentsimulator.domain.repository.TimeProvider
import br.com.paymentsimulator.domain.simulation.PaymentSimulation
import br.com.paymentsimulator.domain.simulation.PaymentSimulationScenario
import br.com.paymentsimulator.domain.usecase.CalculateCartTotalUseCase
import br.com.paymentsimulator.domain.usecase.CreatePaymentUseCase
import br.com.paymentsimulator.domain.usecase.ProcessPaymentUseCase
import br.com.paymentsimulator.presentation.checkout.CheckoutUiState
import br.com.paymentsimulator.presentation.checkout.CheckoutViewModel
import br.com.paymentsimulator.presentation.paymentmethod.CardScenarioUi
import br.com.paymentsimulator.presentation.paymentmethod.PaymentMethodViewModel
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutPaymentViewModelTest {
    private val coffee = Product(
        id = ProductId(UUID.fromString("11111111-1111-1111-1111-111111111111")),
        name = "Café",
        description = "Café coado",
        price = Money(500),
    )
    private val payments = MemoryPayments()
    private val simulation = RecordingSimulation()
    private val paymentId = PaymentId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun checkoutShowsQuantityAndTotalFromTheDomain() = runTest {
        val cartRepository = cartWith(CartItem(coffee, quantity = 2))
        val viewModel = CheckoutViewModel(cartRepository, CalculateCartTotalUseCase())

        viewModel.state.test {
            val content = awaitCheckout()
            assertEquals(2, content.lines.single().quantity)
            assertEquals("R$ 10,00", content.lines.single().subtotal)
            assertEquals("R$ 10,00", content.total)
            assertTrue(content.canConfirm)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun checkoutConfirmationRequiresAPayableCart() = runTest {
        val viewModel = CheckoutViewModel(InMemoryCartRepository(), CalculateCartTotalUseCase())

        viewModel.confirmed.test {
            viewModel.confirm()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        viewModel.state.test {
            val content = awaitCheckout()
            assertFalse(content.canConfirm)
            assertEquals(CartUserMessage.EmptyCart, content.message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun cardConfirmationUsesTheSelectedScenarioAndExistingUseCases() = runTest {
        val cartRepository = cartWith(CartItem(coffee, quantity = 2))
        val viewModel = paymentViewModel(cartRepository)

        viewModel.selectCard()
        viewModel.selectScenario(CardScenarioUi.DECLINED)
        viewModel.started.test {
            viewModel.confirm()
            assertEquals(paymentId.value.toString(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(PaymentSimulationScenario.DECLINED, simulation.scenario)
        assertEquals(listOf(PaymentStatus.PENDING, PaymentStatus.PROCESSING), payments.saved.map { it.status })
        assertEquals(PaymentMethod.CREDIT_CARD, payments.saved.last().method)
        assertEquals(1_000L, payments.saved.last().amount.amountInCents)
    }

    @Test
    fun confirmingTwiceKeepsTheSamePayment() = runTest {
        val cartRepository = cartWith(CartItem(coffee, quantity = 1))
        val viewModel = paymentViewModel(cartRepository)

        viewModel.selectCard()
        viewModel.started.test {
            viewModel.confirm()
            val first = awaitItem()
            viewModel.confirm()
            assertEquals(first, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(listOf(PaymentStatus.PENDING, PaymentStatus.PROCESSING), payments.saved.map { it.status })
        assertEquals(1, payments.saved.map { it.id }.distinct().size)
        assertEquals(paymentId.value.toString(), payments.saved.last().id.value.toString())
    }

    @Test
    fun pixConfirmationUsesTheApprovedScenario() = runTest {
        val cartRepository = cartWith(CartItem(coffee, quantity = 1))
        val viewModel = paymentViewModel(cartRepository)

        viewModel.selectPix()
        viewModel.started.test {
            viewModel.confirm()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(PaymentSimulationScenario.APPROVED, simulation.scenario)
        assertEquals(PaymentMethod.PIX, payments.saved.last().method)
    }

    private fun paymentViewModel(cartRepository: InMemoryCartRepository): PaymentMethodViewModel {
        return PaymentMethodViewModel(
            cartRepository = cartRepository,
            calculateCartTotal = CalculateCartTotalUseCase(),
            simulation = simulation,
            createPayment = CreatePaymentUseCase(
                paymentRepository = payments,
                paymentIds = PaymentIdGenerator { paymentId },
                clock = TimeProvider { Instant.parse("2026-01-15T12:00:00Z") },
            ),
            processPayment = ProcessPaymentUseCase(payments),
        )
    }

    private suspend fun cartWith(item: CartItem): InMemoryCartRepository {
        val repository = InMemoryCartRepository()
        repository.modify { DomainResult.Success(Cart(listOf(item))) }
        return repository
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<CheckoutUiState>.awaitCheckout():
        br.com.paymentsimulator.presentation.checkout.CheckoutContent {
        while (true) {
            val item = awaitItem()
            if (item is CheckoutUiState.Success && item.content.lines.isNotEmpty() ||
                item is CheckoutUiState.Success && item.content.message != null
            ) {
                return (item as CheckoutUiState.Success).content
            }
            if (item is CheckoutUiState.Success && !item.content.canConfirm && item.content.lines.isEmpty()) {
                return item.content
            }
        }
    }
}

private class RecordingSimulation : PaymentSimulation {
    var scenario: PaymentSimulationScenario? = null

    override fun use(scenario: PaymentSimulationScenario) {
        this.scenario = scenario
    }
}

private class MemoryPayments : PaymentRepository {
    val saved = mutableListOf<Payment>()
    private val payments = linkedMapOf<PaymentId, Payment>()

    override suspend fun save(payment: Payment) {
        saved += payment
        payments[payment.id] = payment
    }

    override suspend fun findById(id: PaymentId): Payment? = payments[id]

    override suspend fun findByIdempotencyKey(key: IdempotencyKey): Payment? {
        return payments.values.find { it.idempotencyKey == key }
    }

    override suspend fun findForStatus(id: PaymentId): DomainResult<Payment> {
        val payment = payments[id] ?: return DomainResult.Failure(br.com.paymentsimulator.domain.model.PaymentError.PaymentNotFound)
        return DomainResult.Success(payment)
    }

    override fun observeAll(): Flow<List<Payment>> = flowOf(payments.values.toList())

    override fun observeById(id: PaymentId): Flow<Payment?> = flowOf(payments[id])
}

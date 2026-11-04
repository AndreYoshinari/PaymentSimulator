package br.com.paymentsimulator.presentation.paymentmethod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.paymentsimulator.domain.model.Cart
import br.com.paymentsimulator.domain.model.DomainResult
import br.com.paymentsimulator.domain.model.IdempotencyKey
import br.com.paymentsimulator.domain.model.PaymentId
import br.com.paymentsimulator.domain.model.PaymentMethod
import br.com.paymentsimulator.domain.repository.CartRepository
import br.com.paymentsimulator.domain.simulation.PaymentSimulation
import br.com.paymentsimulator.domain.simulation.PaymentSimulationScenario
import br.com.paymentsimulator.domain.usecase.CalculateCartTotalUseCase
import br.com.paymentsimulator.domain.usecase.CreatePaymentUseCase
import br.com.paymentsimulator.domain.usecase.ProcessPaymentUseCase
import br.com.paymentsimulator.presentation.CartUserMessage
import br.com.paymentsimulator.presentation.toUserMessage
import br.com.paymentsimulator.ui.format.format
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class PaymentMethodViewModel(
    private val cartRepository: CartRepository,
    private val calculateCartTotal: CalculateCartTotalUseCase,
    private val simulation: PaymentSimulation,
    private val createPayment: CreatePaymentUseCase,
    private val processPayment: ProcessPaymentUseCase,
) : ViewModel() {
    private val cart = cartRepository.observe()
        .stateIn(viewModelScope, SharingStarted.Eagerly, Cart())
    private val choice = MutableStateFlow<PaymentChoice?>(null)
    private val scenario = MutableStateFlow(CardScenarioUi.APPROVED)
    private val message = MutableStateFlow<CartUserMessage?>(null)
    private val confirming = MutableStateFlow(false)
    private val startedEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    private var attemptKey: IdempotencyKey? = null
    private var attemptPaymentId: PaymentId? = null
    private var attemptSignature: String? = null

    val started = startedEvents.asSharedFlow()

    val state: StateFlow<PaymentMethodUiState> = combine(
        cart,
        choice,
        scenario,
        message,
        confirming,
    ) { current, selected, selectedScenario, actionMessage, isConfirming ->
        current.toSuccess(selected, selectedScenario, actionMessage, isConfirming) as PaymentMethodUiState
    }.catch {
        emit(PaymentMethodUiState.Error)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PaymentMethodUiState.Loading,
    )

    fun selectPix() {
        choice.value = PaymentChoice.PIX
    }

    fun selectCard() {
        choice.value = PaymentChoice.CREDIT_CARD
    }

    fun selectScenario(value: CardScenarioUi) {
        scenario.value = value
    }

    fun confirm() {
        if (confirming.value) {
            return
        }
        val selected = choice.value
        val current = cart.value
        val validation = current.validateForPayment()
        if (selected == null || validation != null) {
            message.value = validation?.toUserMessage() ?: CartUserMessage.UpdateFailed
            return
        }
        confirming.value = true
        message.value = null
        viewModelScope.launch {
            try {
                val scenarioValue = selected.toScenario(scenario.value)
                simulation.use(scenarioValue)
                val signature = "${selected.name}:${scenarioValue.name}:${current.itemCount}:${current.total.amountInCents}"
                val key = keyFor(signature)
                val created = attemptPaymentId?.takeIf { attemptSignature == signature }?.let { id ->
                    DomainResult.Success(id)
                } ?: createPayment(
                    cart = current,
                    method = selected.toDomain(),
                    idempotencyKey = key,
                ).map { it.id }
                when (created) {
                    is DomainResult.Failure -> message.value = created.error.toUserMessage()
                    is DomainResult.Success -> {
                        attemptPaymentId = created.value
                        attemptSignature = signature
                        when (val processed = processPayment(created.value)) {
                            is DomainResult.Failure -> message.value = processed.error.toUserMessage()
                            is DomainResult.Success -> startedEvents.emit(processed.value.id.value.toString())
                        }
                    }
                }
            } finally {
                confirming.value = false
            }
        }
    }

    private fun keyFor(signature: String): IdempotencyKey {
        if (attemptSignature != signature || attemptKey == null) {
            attemptSignature = signature
            attemptPaymentId = null
            attemptKey = IdempotencyKey(UUID.randomUUID().toString())
        }
        return attemptKey!!
    }

    private fun Cart.toSuccess(
        selected: PaymentChoice?,
        selectedScenario: CardScenarioUi,
        actionMessage: CartUserMessage?,
        isConfirming: Boolean,
    ): PaymentMethodUiState.Success {
        return PaymentMethodUiState.Success(
            PaymentMethodContent(
                total = calculateCartTotal(this).format(),
                choice = selected,
                scenario = selectedScenario,
                canConfirm = selected != null && validateForPayment() == null,
                confirming = isConfirming,
                message = actionMessage,
            ),
        )
    }
}

private fun PaymentChoice.toDomain(): PaymentMethod {
    return when (this) {
        PaymentChoice.PIX -> PaymentMethod.PIX
        PaymentChoice.CREDIT_CARD -> PaymentMethod.CREDIT_CARD
    }
}

private fun PaymentChoice.toScenario(cardScenario: CardScenarioUi): PaymentSimulationScenario {
    if (this == PaymentChoice.PIX) {
        return PaymentSimulationScenario.APPROVED
    }
    return when (cardScenario) {
        CardScenarioUi.APPROVED -> PaymentSimulationScenario.APPROVED
        CardScenarioUi.DECLINED -> PaymentSimulationScenario.DECLINED
        CardScenarioUi.TIMEOUT -> PaymentSimulationScenario.TIMEOUT
        CardScenarioUi.UNAVAILABLE -> PaymentSimulationScenario.UNAVAILABLE
        CardScenarioUi.DUPLICATE_RESPONSE -> PaymentSimulationScenario.DUPLICATE_RESPONSE
        CardScenarioUi.UNKNOWN_THEN_APPROVED -> PaymentSimulationScenario.UNKNOWN_THEN_APPROVED
    }
}

private fun <T, R> DomainResult<T>.map(transform: (T) -> R): DomainResult<R> {
    return when (this) {
        is DomainResult.Success -> DomainResult.Success(transform(value))
        is DomainResult.Failure -> this
    }
}

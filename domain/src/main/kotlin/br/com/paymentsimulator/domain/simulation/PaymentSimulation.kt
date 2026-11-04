package br.com.paymentsimulator.domain.simulation

enum class PaymentSimulationScenario {
    APPROVED,
    DECLINED,
    TIMEOUT,
    UNAVAILABLE,
    DUPLICATE_RESPONSE,
    UNKNOWN_THEN_APPROVED,
}

fun interface PaymentSimulation {
    fun use(scenario: PaymentSimulationScenario)
}

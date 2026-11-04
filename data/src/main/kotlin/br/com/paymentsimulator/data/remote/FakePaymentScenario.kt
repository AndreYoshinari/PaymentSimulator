package br.com.paymentsimulator.data.remote

enum class FakePaymentScenario {
    APPROVED,
    DECLINED,
    TIMEOUT,
    UNAVAILABLE,
    DUPLICATE_RESPONSE,
    UNKNOWN_THEN_APPROVED,
}

package br.com.paymentsimulator.navigation

enum class AppDestination(val route: String) {
    Home("home"),
    Cart("cart"),
    Checkout("checkout"),
    PaymentMethod("payment_method"),
    Processing("processing/{paymentId}"),
    PaymentResult("payment_result/{paymentId}"),
    Receipt("receipt/{paymentId}"),
    Transactions("transactions"),
    ;

    companion object {
        val sections = setOf(Home, Cart, Transactions)

        fun fromRoute(route: String?): AppDestination {
            if (route == null) {
                return Home
            }
            return entries.firstOrNull { it.matches(route) } ?: Home
        }
    }

    fun filled(argument: String): String = route.substringBefore("{") + argument

    private fun matches(actual: String): Boolean {
        val prefix = route.substringBefore("/{")
        return actual == route || (prefix != route && (actual == prefix || actual.startsWith("$prefix/")))
    }
}

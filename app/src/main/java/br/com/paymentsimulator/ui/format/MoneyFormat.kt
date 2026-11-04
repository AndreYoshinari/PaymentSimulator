package br.com.paymentsimulator.ui.format

import br.com.paymentsimulator.domain.model.Money

fun Money.format(): String {
    val negative = amountInCents < 0
    val absolute = kotlin.math.abs(amountInCents)
    val reais = absolute / 100
    val cents = (absolute % 100).toString().padStart(2, '0')
    val grouped = reais.toString()
        .reversed()
        .chunked(3)
        .joinToString(".")
        .reversed()
    val prefix = if (negative) "-R$ " else "R$ "
    return "$prefix$grouped,$cents"
}

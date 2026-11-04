package br.com.paymentsimulator.ui.format

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateTimeFormatter = DateTimeFormatter.ofPattern(
    "dd/MM/yyyy HH:mm",
    Locale.forLanguageTag("pt-BR"),
)

fun Instant.formatDateTime(): String {
    return atZone(ZoneId.systemDefault()).format(dateTimeFormatter)
}

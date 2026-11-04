package br.com.paymentsimulator.domain.repository

import java.time.Instant

fun interface TimeProvider {
    fun now(): Instant
}

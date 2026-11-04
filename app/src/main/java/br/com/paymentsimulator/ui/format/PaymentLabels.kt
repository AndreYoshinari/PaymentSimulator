package br.com.paymentsimulator.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import br.com.paymentsimulator.R
import br.com.paymentsimulator.domain.model.PaymentMethod
import br.com.paymentsimulator.domain.model.PaymentStatus

@Composable
fun PaymentMethod.label(): String {
    return when (this) {
        PaymentMethod.PIX -> stringResource(R.string.payment_method_pix)
        PaymentMethod.CREDIT_CARD -> stringResource(R.string.payment_method_card)
    }
}

@Composable
fun PaymentStatus.label(): String {
    return when (this) {
        PaymentStatus.PENDING -> stringResource(R.string.status_pending)
        PaymentStatus.PROCESSING -> stringResource(R.string.status_processing)
        PaymentStatus.APPROVED -> stringResource(R.string.status_approved)
        PaymentStatus.DECLINED -> stringResource(R.string.status_declined)
        PaymentStatus.FAILED -> stringResource(R.string.status_failed)
        PaymentStatus.UNKNOWN -> stringResource(R.string.status_unknown)
    }
}

package br.com.paymentsimulator.ui.paymentresult

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.paymentsimulator.R
import br.com.paymentsimulator.presentation.payment.PaymentOutcome
import br.com.paymentsimulator.presentation.paymentresult.PaymentResultUiState
import br.com.paymentsimulator.presentation.paymentresult.PaymentResultViewModel
import br.com.paymentsimulator.ui.component.PaymentActionGroup
import br.com.paymentsimulator.ui.component.PaymentButton
import br.com.paymentsimulator.ui.component.PaymentButtonStyle
import br.com.paymentsimulator.ui.component.PaymentCard
import br.com.paymentsimulator.ui.component.PaymentDetailRow
import br.com.paymentsimulator.ui.component.PaymentLoadingIndicator
import br.com.paymentsimulator.ui.component.PaymentScreenContent
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PaymentResultRoute(
    onOpenReceipt: (String) -> Unit,
    onBackHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<PaymentResultViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    PaymentResultScreen(
        state = state,
        onCheckAgain = viewModel::checkAgain,
        onOpenReceipt = onOpenReceipt,
        onBackHome = onBackHome,
        modifier = modifier,
    )
}

@Composable
fun PaymentResultScreen(
    state: PaymentResultUiState,
    onCheckAgain: () -> Unit,
    onOpenReceipt: (String) -> Unit,
    onBackHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        PaymentResultUiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            PaymentLoadingIndicator()
        }
        PaymentResultUiState.NotFound -> PaymentScreenContent(modifier = modifier) {
            Text(
                text = stringResource(R.string.payment_result_not_found),
                style = MaterialTheme.typography.bodyLarge,
            )
            PaymentButton(
                text = stringResource(R.string.payment_result_home),
                onClick = onBackHome,
            )
        }
        is PaymentResultUiState.Ready -> PaymentResultContent(
            state = state,
            onCheckAgain = onCheckAgain,
            onOpenReceipt = onOpenReceipt,
            onBackHome = onBackHome,
            modifier = modifier,
        )
    }
}

@Composable
private fun PaymentResultContent(
    state: PaymentResultUiState.Ready,
    onCheckAgain: () -> Unit,
    onOpenReceipt: (String) -> Unit,
    onBackHome: () -> Unit,
    modifier: Modifier,
) {
    val content = state.content
    PaymentScreenContent(modifier = modifier) {
        PaymentCard(
            title = content.outcome.title(),
            supporting = content.outcome.body(),
        ) {
            PaymentDetailRow(
                label = stringResource(R.string.receipt_id),
                value = content.paymentId,
            )
        }
        if (content.canCheckAgain) {
            PaymentActionGroup(
                primary = {
                    PaymentButton(
                        text = stringResource(R.string.payment_result_check_again),
                        onClick = onCheckAgain,
                        loading = content.checking,
                    )
                },
                secondary = {
                    PaymentButton(
                        text = stringResource(R.string.payment_result_home),
                        onClick = onBackHome,
                        style = PaymentButtonStyle.Secondary,
                    )
                },
            )
        } else if (content.outcome == PaymentOutcome.Approved) {
            PaymentActionGroup(
                primary = {
                    PaymentButton(
                        text = stringResource(R.string.payment_result_receipt),
                        onClick = { onOpenReceipt(content.paymentId) },
                    )
                },
                secondary = {
                    PaymentButton(
                        text = stringResource(R.string.payment_result_home),
                        onClick = onBackHome,
                        style = PaymentButtonStyle.Secondary,
                    )
                },
            )
        } else {
            PaymentButton(
                text = stringResource(R.string.payment_result_home),
                onClick = onBackHome,
            )
        }
    }
}

@Composable
private fun PaymentOutcome.title(): String {
    return when (this) {
        PaymentOutcome.Approved -> stringResource(R.string.payment_result_approved_title)
        PaymentOutcome.Declined -> stringResource(R.string.payment_result_declined_title)
        PaymentOutcome.CommunicationFailure -> stringResource(R.string.payment_result_unavailable_title)
        PaymentOutcome.Unknown -> stringResource(R.string.payment_result_unknown_title)
        PaymentOutcome.Failed -> stringResource(R.string.payment_result_failed_title)
        PaymentOutcome.InProgress -> stringResource(R.string.processing_title)
    }
}

@Composable
private fun PaymentOutcome.body(): String {
    return when (this) {
        PaymentOutcome.Approved -> stringResource(R.string.payment_result_approved_body)
        PaymentOutcome.Declined -> stringResource(R.string.payment_result_declined_body)
        PaymentOutcome.CommunicationFailure -> stringResource(R.string.payment_result_unavailable_body)
        PaymentOutcome.Unknown -> stringResource(R.string.payment_result_unknown_body)
        PaymentOutcome.Failed -> stringResource(R.string.payment_result_failed_body)
        PaymentOutcome.InProgress -> stringResource(R.string.processing_body)
    }
}

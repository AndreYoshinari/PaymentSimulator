package br.com.paymentsimulator.ui.receipt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.paymentsimulator.R
import br.com.paymentsimulator.presentation.receipt.ReceiptUiState
import br.com.paymentsimulator.presentation.receipt.ReceiptViewModel
import br.com.paymentsimulator.ui.component.PaymentActionGroup
import br.com.paymentsimulator.ui.component.PaymentButton
import br.com.paymentsimulator.ui.component.PaymentButtonStyle
import br.com.paymentsimulator.ui.component.PaymentCard
import br.com.paymentsimulator.ui.component.PaymentDetailRow
import br.com.paymentsimulator.ui.component.PaymentLoadingIndicator
import br.com.paymentsimulator.ui.component.PaymentScreenContent
import br.com.paymentsimulator.ui.format.label
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ReceiptRoute(
    onOpenTransactions: () -> Unit,
    onBackHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<ReceiptViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    ReceiptScreen(
        state = state,
        onRetry = viewModel::retry,
        onOpenTransactions = onOpenTransactions,
        onBackHome = onBackHome,
        modifier = modifier,
    )
}

@Composable
fun ReceiptScreen(
    state: ReceiptUiState,
    onRetry: () -> Unit,
    onOpenTransactions: () -> Unit,
    onBackHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        ReceiptUiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            PaymentLoadingIndicator()
        }
        ReceiptUiState.NotFound -> ReceiptMessage(
            message = stringResource(R.string.receipt_not_found),
            onOpenTransactions = onOpenTransactions,
            onBackHome = onBackHome,
            modifier = modifier,
        )
        ReceiptUiState.Error -> ReceiptMessage(
            message = stringResource(R.string.receipt_load_error),
            onRetry = onRetry,
            onOpenTransactions = onOpenTransactions,
            onBackHome = onBackHome,
            modifier = modifier,
        )
        is ReceiptUiState.Ready -> ReceiptContent(
            state = state,
            onOpenTransactions = onOpenTransactions,
            onBackHome = onBackHome,
            modifier = modifier,
        )
    }
}

@Composable
private fun ReceiptContent(
    state: ReceiptUiState.Ready,
    onOpenTransactions: () -> Unit,
    onBackHome: () -> Unit,
    modifier: Modifier,
) {
    val content = state.content
    PaymentScreenContent(modifier = modifier) {
        PaymentCard(
            title = stringResource(R.string.receipt_card_title),
            supporting = stringResource(R.string.receipt_body),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PaymentDetailRow(
                    label = stringResource(R.string.receipt_id),
                    value = content.id,
                )
                PaymentDetailRow(
                    label = stringResource(R.string.receipt_date),
                    value = content.createdAt,
                )
                PaymentDetailRow(
                    label = stringResource(R.string.receipt_amount),
                    value = content.amount,
                    emphasizeValue = true,
                )
                PaymentDetailRow(
                    label = stringResource(R.string.receipt_method),
                    value = content.method.label(),
                )
                PaymentDetailRow(
                    label = stringResource(R.string.receipt_status),
                    value = content.status.label(),
                )
                PaymentDetailRow(
                    label = stringResource(R.string.receipt_confirmation_label),
                    value = content.confirmationCode,
                )
            }
        }
        Text(
            text = stringResource(R.string.receipt_confirmation_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ReceiptActions(
            onOpenTransactions = onOpenTransactions,
            onBackHome = onBackHome,
        )
    }
}

@Composable
private fun ReceiptMessage(
    message: String,
    onOpenTransactions: () -> Unit,
    onBackHome: () -> Unit,
    modifier: Modifier,
    onRetry: (() -> Unit)? = null,
) {
    PaymentScreenContent(modifier = modifier) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge)
        if (onRetry != null) {
            PaymentButton(
                text = stringResource(R.string.home_retry),
                onClick = onRetry,
            )
        }
        ReceiptActions(
            onOpenTransactions = onOpenTransactions,
            onBackHome = onBackHome,
        )
    }
}

@Composable
private fun ReceiptActions(
    onOpenTransactions: () -> Unit,
    onBackHome: () -> Unit,
) {
    PaymentActionGroup(
        primary = {
            PaymentButton(
                text = stringResource(R.string.receipt_home),
                onClick = onBackHome,
            )
        },
        secondary = {
            PaymentButton(
                text = stringResource(R.string.receipt_transactions),
                onClick = onOpenTransactions,
                style = PaymentButtonStyle.Secondary,
            )
        },
    )
}

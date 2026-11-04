package br.com.paymentsimulator.ui.checkout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.paymentsimulator.R
import br.com.paymentsimulator.presentation.checkout.CheckoutLineUi
import br.com.paymentsimulator.presentation.checkout.CheckoutUiState
import br.com.paymentsimulator.presentation.checkout.CheckoutViewModel
import br.com.paymentsimulator.ui.component.PaymentButton
import br.com.paymentsimulator.ui.component.PaymentCard
import br.com.paymentsimulator.ui.component.PaymentDetailRow
import br.com.paymentsimulator.ui.component.PaymentLoadingIndicator
import br.com.paymentsimulator.ui.component.PaymentScreenContent
import br.com.paymentsimulator.ui.component.UserMessage
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CheckoutRoute(
    onConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<CheckoutViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.confirmed.collect { onConfirmed() }
    }
    CheckoutScreen(
        state = state,
        onConfirm = viewModel::confirm,
        modifier = modifier,
    )
}

@Composable
fun CheckoutScreen(
    state: CheckoutUiState,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        CheckoutUiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            PaymentLoadingIndicator()
        }
        CheckoutUiState.Error -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.cart_load_error),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        is CheckoutUiState.Success -> CheckoutContent(state, onConfirm, modifier)
    }
}

@Composable
private fun CheckoutContent(
    state: CheckoutUiState.Success,
    onConfirm: () -> Unit,
    modifier: Modifier,
) {
    val content = state.content
    PaymentScreenContent(modifier = modifier) {
        content.message?.let { message -> UserMessage(message) }
        if (content.lines.isEmpty()) {
            PaymentCard(
                title = stringResource(R.string.cart_empty_title),
                supporting = stringResource(R.string.cart_empty_body),
            )
        } else {
            content.lines.forEach { line ->
                CheckoutLine(line)
            }
        }
        PaymentCard(
            title = stringResource(R.string.checkout_summary_title),
            supporting = stringResource(R.string.checkout_summary_body),
        ) {
            PaymentDetailRow(
                label = stringResource(R.string.cart_total),
                value = content.total,
                emphasizeValue = true,
            )
        }
        PaymentButton(
            text = stringResource(R.string.checkout_confirm),
            onClick = onConfirm,
            enabled = content.canConfirm,
        )
    }
}

@Composable
private fun CheckoutLine(line: CheckoutLineUi) {
    PaymentCard(title = line.name) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PaymentDetailRow(
                label = stringResource(R.string.checkout_quantity),
                value = line.quantity.toString(),
            )
            PaymentDetailRow(
                label = stringResource(R.string.cart_subtotal),
                value = line.subtotal,
            )
        }
    }
}

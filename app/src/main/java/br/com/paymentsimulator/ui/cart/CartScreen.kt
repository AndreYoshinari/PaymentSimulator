package br.com.paymentsimulator.ui.cart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.paymentsimulator.R
import br.com.paymentsimulator.presentation.cart.CartLineUi
import br.com.paymentsimulator.presentation.cart.CartUiState
import br.com.paymentsimulator.presentation.cart.CartViewModel
import br.com.paymentsimulator.ui.component.PaymentButton
import br.com.paymentsimulator.ui.component.PaymentButtonStyle
import br.com.paymentsimulator.ui.component.PaymentCard
import br.com.paymentsimulator.ui.component.PaymentDetailRow
import br.com.paymentsimulator.ui.component.PaymentLoadingIndicator
import br.com.paymentsimulator.ui.component.PaymentScreenContent
import br.com.paymentsimulator.ui.component.UserMessage
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CartRoute(
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<CartViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    CartScreen(
        state = state,
        onIncrease = viewModel::increase,
        onDecrease = viewModel::decrease,
        onRemove = viewModel::remove,
        onCheckout = onCheckout,
        modifier = modifier,
    )
}

@Composable
fun CartScreen(
    state: CartUiState,
    onIncrease: (String) -> Unit,
    onDecrease: (String) -> Unit,
    onRemove: (String) -> Unit,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        CartUiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            PaymentLoadingIndicator()
        }
        CartUiState.Error -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.cart_load_error),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        is CartUiState.Success -> CartContent(
            state = state,
            onIncrease = onIncrease,
            onDecrease = onDecrease,
            onRemove = onRemove,
            onCheckout = onCheckout,
            modifier = modifier,
        )
    }
}

@Composable
private fun CartContent(
    state: CartUiState.Success,
    onIncrease: (String) -> Unit,
    onDecrease: (String) -> Unit,
    onRemove: (String) -> Unit,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier,
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
                CartLine(
                    line = line,
                    onIncrease = { onIncrease(line.id) },
                    onDecrease = { onDecrease(line.id) },
                    onRemove = { onRemove(line.id) },
                )
            }
        }
        PaymentCard(title = stringResource(R.string.cart_title)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PaymentDetailRow(
                    label = stringResource(R.string.cart_subtotal),
                    value = content.subtotal,
                )
                PaymentDetailRow(
                    label = stringResource(R.string.cart_total),
                    value = content.total,
                    emphasizeValue = true,
                )
            }
        }
        if (!content.canCheckout) {
            Text(
                text = stringResource(R.string.cart_checkout_blocked),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PaymentButton(
            text = stringResource(R.string.cart_checkout),
            onClick = onCheckout,
            enabled = content.canCheckout,
        )
    }
}

@Composable
private fun CartLine(
    line: CartLineUi,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
) {
    PaymentCard(title = line.name) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PaymentDetailRow(
                label = stringResource(R.string.cart_subtotal),
                value = line.subtotal,
            )
            QuantityStepper(
                name = line.name,
                quantity = line.quantity,
                onIncrease = onIncrease,
                onDecrease = onDecrease,
            )
            PaymentButton(
                text = stringResource(R.string.cart_remove, line.name),
                onClick = onRemove,
                style = PaymentButtonStyle.Text,
            )
        }
    }
}

@Composable
private fun QuantityStepper(
    name: String,
    quantity: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StepButton(
            symbol = "−",
            description = stringResource(R.string.cart_decrease, name),
            enabled = quantity > 1,
            onClick = onDecrease,
        )
        val quantityDescription = stringResource(R.string.cart_quantity, name, quantity)
        Text(
            text = quantity.toString(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { contentDescription = quantityDescription },
        )
        StepButton(
            symbol = "+",
            description = stringResource(R.string.cart_increase, name),
            enabled = true,
            onClick = onIncrease,
        )
    }
}

@Composable
private fun StepButton(
    symbol: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = description },
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(text = symbol)
    }
}

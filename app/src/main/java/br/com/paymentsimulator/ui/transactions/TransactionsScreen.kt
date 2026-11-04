package br.com.paymentsimulator.ui.transactions

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import br.com.paymentsimulator.domain.model.PaymentStatus
import br.com.paymentsimulator.presentation.transactions.TransactionDetailsUi
import br.com.paymentsimulator.presentation.transactions.TransactionsUiState
import br.com.paymentsimulator.presentation.transactions.TransactionsViewModel
import br.com.paymentsimulator.ui.component.PaymentButton
import br.com.paymentsimulator.ui.component.PaymentCard
import br.com.paymentsimulator.ui.component.PaymentDetailRow
import br.com.paymentsimulator.ui.component.PaymentLoadingIndicator
import br.com.paymentsimulator.ui.component.PaymentScreenContent
import br.com.paymentsimulator.ui.format.label
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TransactionsRoute(
    onOpenReceipt: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<TransactionsViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    TransactionsScreen(
        state = state,
        onRetry = viewModel::retry,
        onSelectFilter = viewModel::selectFilter,
        onSelect = viewModel::select,
        onOpenReceipt = onOpenReceipt,
        modifier = modifier,
    )
}

@Composable
fun TransactionsScreen(
    state: TransactionsUiState,
    onRetry: () -> Unit,
    onSelectFilter: (PaymentStatus?) -> Unit,
    onSelect: (String) -> Unit,
    onOpenReceipt: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        TransactionsUiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            PaymentLoadingIndicator()
        }
        TransactionsUiState.Error -> PaymentScreenContent(modifier = modifier) {
            Text(
                text = stringResource(R.string.transactions_load_error),
                style = MaterialTheme.typography.bodyLarge,
            )
            PaymentButton(
                text = stringResource(R.string.home_retry),
                onClick = onRetry,
            )
        }
        is TransactionsUiState.Success -> TransactionsContent(
            state = state,
            onSelectFilter = onSelectFilter,
            onSelect = onSelect,
            onOpenReceipt = onOpenReceipt,
            modifier = modifier,
        )
    }
}

@Composable
private fun TransactionsContent(
    state: TransactionsUiState.Success,
    onSelectFilter: (PaymentStatus?) -> Unit,
    onSelect: (String) -> Unit,
    onOpenReceipt: (String) -> Unit,
    modifier: Modifier,
) {
    val content = state.content
    PaymentScreenContent(modifier = modifier) {
        if (!content.hasAnyPayment) {
            PaymentCard(
                title = stringResource(R.string.transactions_empty_title),
                supporting = stringResource(R.string.transactions_empty_body),
            )
            return@PaymentScreenContent
        }
        StatusFilter(selected = content.filter, onSelect = onSelectFilter)
        val selected = content.selected
        if (selected == null) {
            Text(
                text = stringResource(R.string.transactions_select),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            TransactionDetails(details = selected, onOpenReceipt = { onOpenReceipt(selected.id) })
        }
        if (content.transactions.isEmpty()) {
            PaymentCard(
                title = stringResource(R.string.transactions_filter_empty),
            )
        } else {
            content.transactions.forEach { item ->
                TransactionRow(
                    item = item,
                    selected = item.id == selected?.id,
                    onSelect = { onSelect(item.id) },
                )
            }
        }
    }
}

@Composable
private fun StatusFilter(
    selected: PaymentStatus?,
    onSelect: (PaymentStatus?) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.transactions_filter_all)) },
            modifier = Modifier.heightIn(min = 48.dp),
        )
        PaymentStatus.entries.forEach { status ->
            FilterChip(
                selected = selected == status,
                onClick = { onSelect(status) },
                label = { Text(status.label()) },
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
    }
}

@Composable
private fun TransactionRow(
    item: TransactionDetailsUi,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val description = stringResource(R.string.transactions_open_details, item.amount)
    Card(
        onClick = onSelect,
        modifier = Modifier.semantics { contentDescription = description },
        colors = if (selected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PaymentDetailRow(
                label = item.createdAt,
                value = item.amount,
                emphasizeValue = true,
            )
            Text(
                text = "${item.method.label()} · ${item.status.label()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TransactionDetails(
    details: TransactionDetailsUi,
    onOpenReceipt: () -> Unit,
) {
    PaymentCard(title = stringResource(R.string.transactions_details)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PaymentDetailRow(
                label = stringResource(R.string.receipt_id),
                value = details.id,
            )
            PaymentDetailRow(
                label = stringResource(R.string.receipt_date),
                value = details.createdAt,
            )
            PaymentDetailRow(
                label = stringResource(R.string.receipt_amount),
                value = details.amount,
                emphasizeValue = true,
            )
            PaymentDetailRow(
                label = stringResource(R.string.receipt_method),
                value = details.method.label(),
            )
            PaymentDetailRow(
                label = stringResource(R.string.receipt_status),
                value = details.status.label(),
            )
            PaymentButton(
                text = stringResource(R.string.transactions_open_receipt),
                onClick = onOpenReceipt,
            )
        }
    }
}

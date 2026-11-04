package br.com.paymentsimulator.ui.home

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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.paymentsimulator.R
import br.com.paymentsimulator.presentation.home.HomeProductUi
import br.com.paymentsimulator.presentation.home.HomeUiState
import br.com.paymentsimulator.presentation.home.HomeViewModel
import br.com.paymentsimulator.ui.component.PaymentActionGroup
import br.com.paymentsimulator.ui.component.PaymentButton
import br.com.paymentsimulator.ui.component.PaymentButtonStyle
import br.com.paymentsimulator.ui.component.PaymentCard
import br.com.paymentsimulator.ui.component.PaymentLoadingIndicator
import br.com.paymentsimulator.ui.component.PaymentScreenContent
import br.com.paymentsimulator.ui.component.UserMessage
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeRoute(
    onOpenCart: () -> Unit,
    onOpenTransactions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<HomeViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onAdd = viewModel::add,
        onRetry = viewModel::load,
        onOpenCart = onOpenCart,
        onOpenTransactions = onOpenTransactions,
        modifier = modifier,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onAdd: (String) -> Unit,
    onRetry: () -> Unit,
    onOpenCart: () -> Unit,
    onOpenTransactions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        HomeUiState.Loading -> CenteredStatus(modifier) {
            PaymentLoadingIndicator()
        }
        HomeUiState.Error -> CenteredStatus(modifier) {
            Text(
                text = stringResource(R.string.home_load_error),
                style = MaterialTheme.typography.bodyLarge,
            )
            PaymentButton(
                text = stringResource(R.string.home_retry),
                onClick = onRetry,
            )
        }
        is HomeUiState.Success -> HomeContent(
            state = state,
            onAdd = onAdd,
            onOpenCart = onOpenCart,
            onOpenTransactions = onOpenTransactions,
            modifier = modifier,
        )
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState.Success,
    onAdd: (String) -> Unit,
    onOpenCart: () -> Unit,
    onOpenTransactions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PaymentScreenContent(modifier = modifier) {
        Text(
            text = pluralStringResource(R.plurals.cart_item_count, state.itemCount, state.itemCount),
            style = MaterialTheme.typography.titleMedium,
        )
        state.message?.let { message -> UserMessage(message) }
        if (state.products.isEmpty()) {
            PaymentCard(
                title = stringResource(R.string.home_card_title),
                supporting = stringResource(R.string.home_empty_catalog),
            )
        } else {
            state.products.forEach { product ->
                ProductCard(product = product, onAdd = { onAdd(product.id) })
            }
        }
        PaymentActionGroup(
            primary = {
                PaymentButton(
                    text = stringResource(R.string.home_open_cart),
                    onClick = onOpenCart,
                )
            },
            secondary = {
                PaymentButton(
                    text = stringResource(R.string.home_open_transactions),
                    onClick = onOpenTransactions,
                    style = PaymentButtonStyle.Secondary,
                )
            },
        )
    }
}

@Composable
private fun ProductCard(
    product: HomeProductUi,
    onAdd: () -> Unit,
) {
    val addDescription = stringResource(R.string.home_add_description, product.name)
    PaymentCard(
        title = product.name,
        supporting = product.description,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = product.price,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.home_quantity_in_cart, product.quantityInCart),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PaymentButton(
                text = stringResource(R.string.home_add),
                onClick = onAdd,
                modifier = Modifier.semantics { contentDescription = addDescription },
            )
        }
    }
}

@Composable
private fun CenteredStatus(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            content()
        }
    }
}

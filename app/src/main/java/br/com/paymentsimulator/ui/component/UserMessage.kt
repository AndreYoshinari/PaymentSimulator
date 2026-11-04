package br.com.paymentsimulator.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import br.com.paymentsimulator.R
import br.com.paymentsimulator.presentation.CartUserMessage

@Composable
fun CartUserMessage.text(): String {
    return when (this) {
        CartUserMessage.InvalidQuantity -> stringResource(R.string.error_invalid_quantity)
        CartUserMessage.ProductNotFound -> stringResource(R.string.error_product_not_found)
        CartUserMessage.ProductNotInCart -> stringResource(R.string.error_product_not_in_cart)
        CartUserMessage.EmptyCart -> stringResource(R.string.error_empty_cart)
        CartUserMessage.NonPositiveAmount -> stringResource(R.string.error_non_positive_amount)
        CartUserMessage.UpdateFailed -> stringResource(R.string.error_cart_update)
        CartUserMessage.ProductsUnavailable -> stringResource(R.string.home_load_error)
    }
}

@Composable
fun UserMessage(
    message: CartUserMessage,
    modifier: Modifier = Modifier,
) {
    Text(
        text = message.text(),
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
    )
}

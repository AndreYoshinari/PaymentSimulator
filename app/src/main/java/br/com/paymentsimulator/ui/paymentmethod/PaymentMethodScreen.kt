package br.com.paymentsimulator.ui.paymentmethod

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.paymentsimulator.R
import br.com.paymentsimulator.presentation.paymentmethod.CardScenarioUi
import br.com.paymentsimulator.presentation.paymentmethod.PaymentChoice
import br.com.paymentsimulator.presentation.paymentmethod.PaymentMethodUiState
import br.com.paymentsimulator.presentation.paymentmethod.PaymentMethodViewModel
import br.com.paymentsimulator.ui.component.PaymentButton
import br.com.paymentsimulator.ui.component.PaymentCard
import br.com.paymentsimulator.ui.component.PaymentDetailRow
import br.com.paymentsimulator.ui.component.PaymentLoadingIndicator
import br.com.paymentsimulator.ui.component.PaymentScreenContent
import br.com.paymentsimulator.ui.component.UserMessage
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PaymentMethodRoute(
    onProcessingStarted: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<PaymentMethodViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.started.collect(onProcessingStarted)
    }
    PaymentMethodScreen(
        state = state,
        onSelectPix = viewModel::selectPix,
        onSelectCard = viewModel::selectCard,
        onSelectScenario = viewModel::selectScenario,
        onConfirm = viewModel::confirm,
        modifier = modifier,
    )
}

@Composable
fun PaymentMethodScreen(
    state: PaymentMethodUiState,
    onSelectPix: () -> Unit,
    onSelectCard: () -> Unit,
    onSelectScenario: (CardScenarioUi) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        PaymentMethodUiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            PaymentLoadingIndicator()
        }
        PaymentMethodUiState.Error -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.cart_load_error),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        is PaymentMethodUiState.Success -> PaymentMethodContent(
            state = state,
            onSelectPix = onSelectPix,
            onSelectCard = onSelectCard,
            onSelectScenario = onSelectScenario,
            onConfirm = onConfirm,
            modifier = modifier,
        )
    }
}

@Composable
private fun PaymentMethodContent(
    state: PaymentMethodUiState.Success,
    onSelectPix: () -> Unit,
    onSelectCard: () -> Unit,
    onSelectScenario: (CardScenarioUi) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier,
) {
    val content = state.content
    PaymentScreenContent(modifier = modifier) {
        Text(
            text = stringResource(R.string.payment_method_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content.message?.let { message -> UserMessage(message) }
        PaymentChoiceRow(
            title = stringResource(R.string.payment_method_pix),
            supporting = stringResource(R.string.payment_method_pix_support),
            selected = content.choice == PaymentChoice.PIX,
            onClick = onSelectPix,
        )
        PaymentChoiceRow(
            title = stringResource(R.string.payment_method_card),
            supporting = stringResource(R.string.payment_method_card_support),
            selected = content.choice == PaymentChoice.CREDIT_CARD,
            onClick = onSelectCard,
        )
        when (content.choice) {
            PaymentChoice.PIX -> PixDetails(total = content.total)
            PaymentChoice.CREDIT_CARD -> CardDetails(
                total = content.total,
                scenario = content.scenario,
                onSelectScenario = onSelectScenario,
            )
            null -> Text(
                text = stringResource(R.string.payment_method_choose),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PaymentButton(
            text = stringResource(R.string.payment_method_confirm),
            onClick = onConfirm,
            enabled = content.canConfirm,
            loading = content.confirming,
        )
    }
}

@Composable
private fun PixDetails(total: String) {
    PaymentCard(title = stringResource(R.string.payment_method_pix)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PaymentDetailRow(
                label = stringResource(R.string.payment_method_receiver_label),
                value = stringResource(R.string.payment_method_receiver),
            )
            PaymentDetailRow(
                label = stringResource(R.string.payment_method_pix_key_label),
                value = stringResource(R.string.payment_method_pix_key),
            )
            PaymentDetailRow(
                label = stringResource(R.string.cart_total),
                value = total,
                emphasizeValue = true,
            )
        }
    }
}

@Composable
private fun CardDetails(
    total: String,
    scenario: CardScenarioUi,
    onSelectScenario: (CardScenarioUi) -> Unit,
) {
    PaymentCard(title = stringResource(R.string.payment_method_card)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PaymentDetailRow(
                label = stringResource(R.string.payment_method_card_holder_label),
                value = stringResource(R.string.payment_method_card_holder),
            )
            PaymentDetailRow(
                label = stringResource(R.string.payment_method_card_number_label),
                value = stringResource(R.string.payment_method_card_number),
            )
            PaymentDetailRow(
                label = stringResource(R.string.payment_method_card_expiry_label),
                value = stringResource(R.string.payment_method_card_expiry),
            )
            PaymentDetailRow(
                label = stringResource(R.string.cart_total),
                value = total,
                emphasizeValue = true,
            )
            Text(
                text = stringResource(R.string.payment_method_no_cvv),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Text(
        text = stringResource(R.string.payment_method_scenario),
        style = MaterialTheme.typography.titleMedium,
    )
    CardScenarioUi.entries.forEach { option ->
        PaymentChoiceRow(
            title = option.label(),
            supporting = null,
            selected = scenario == option,
            onClick = { onSelectScenario(option) },
        )
    }
}

@Composable
private fun PaymentChoiceRow(
    title: String,
    supporting: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Column {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CardScenarioUi.label(): String {
    return when (this) {
        CardScenarioUi.APPROVED -> stringResource(R.string.scenario_approved)
        CardScenarioUi.DECLINED -> stringResource(R.string.scenario_declined)
        CardScenarioUi.TIMEOUT -> stringResource(R.string.scenario_timeout)
        CardScenarioUi.UNAVAILABLE -> stringResource(R.string.scenario_unavailable)
        CardScenarioUi.DUPLICATE_RESPONSE -> stringResource(R.string.scenario_duplicate)
        CardScenarioUi.UNKNOWN_THEN_APPROVED -> stringResource(R.string.scenario_unknown_then_approved)
    }
}

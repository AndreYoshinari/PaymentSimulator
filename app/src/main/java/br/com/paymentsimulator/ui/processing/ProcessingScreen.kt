package br.com.paymentsimulator.ui.processing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.paymentsimulator.R
import br.com.paymentsimulator.presentation.processing.ProcessingUiState
import br.com.paymentsimulator.presentation.processing.ProcessingViewModel
import br.com.paymentsimulator.ui.component.PaymentLoadingIndicator
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProcessingRoute(
    onResolved: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<ProcessingViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.resolved.collect(onResolved)
    }
    ProcessingScreen(state = state, modifier = modifier)
}

@Composable
fun ProcessingScreen(
    state: ProcessingUiState,
    modifier: Modifier = Modifier,
) {
    val waiting = state !is ProcessingUiState.NotFound
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            if (waiting) {
                PaymentLoadingIndicator(
                    label = if (state is ProcessingUiState.Loading) {
                        stringResource(R.string.loading)
                    } else {
                        stringResource(R.string.processing_body)
                    },
                )
            }
            if (state is ProcessingUiState.Processing || state is ProcessingUiState.Resolved) {
                val paymentId = when (state) {
                    is ProcessingUiState.Processing -> state.paymentId
                    is ProcessingUiState.Resolved -> state.paymentId
                    else -> ""
                }
                Text(
                    text = stringResource(R.string.processing_payment_id, paymentId),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            if (state is ProcessingUiState.NotFound) {
                Text(
                    text = stringResource(R.string.processing_not_found),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

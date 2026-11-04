package br.com.paymentsimulator.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.com.paymentsimulator.R

@Composable
fun PaymentLoadingIndicator(
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.loading),
) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(48.dp)
                .clearAndSetSemantics {},
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

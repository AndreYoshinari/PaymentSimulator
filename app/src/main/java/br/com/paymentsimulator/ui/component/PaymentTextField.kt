package br.com.paymentsimulator.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PaymentTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    errorText: String? = null,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        label = { Text(text = label) },
        supportingText = {
            val message = errorText ?: supportingText
            if (message != null) {
                Text(text = message)
            }
        },
        isError = errorText != null,
        singleLine = singleLine,
    )
}

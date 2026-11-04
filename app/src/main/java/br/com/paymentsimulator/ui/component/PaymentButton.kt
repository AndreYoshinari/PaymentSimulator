package br.com.paymentsimulator.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class PaymentButtonStyle {
    Primary,
    Secondary,
    Text,
}

@Composable
fun PaymentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PaymentButtonStyle = PaymentButtonStyle.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val buttonModifier = modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
    val content: @Composable RowScope.() -> Unit = {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text)
    }
    val clickEnabled = enabled && !loading
    when (style) {
        PaymentButtonStyle.Primary -> Button(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = clickEnabled,
            content = content,
        )
        PaymentButtonStyle.Secondary -> OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = clickEnabled,
            content = content,
        )
        PaymentButtonStyle.Text -> TextButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = clickEnabled,
            content = content,
        )
    }
}

@Composable
fun PaymentActionGroup(
    primary: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    secondary: (@Composable () -> Unit)? = null,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val secondaryAction = secondary
        if (secondaryAction != null && maxWidth >= 520.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) { secondaryAction() }
                Box(modifier = Modifier.weight(1f)) { primary() }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                primary()
                secondaryAction?.invoke()
            }
        }
    }
}

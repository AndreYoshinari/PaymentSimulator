package br.com.paymentsimulator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import br.com.paymentsimulator.navigation.PaymentSimulatorNavHost
import br.com.paymentsimulator.ui.theme.PaymentSimulatorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PaymentSimulatorTheme {
                PaymentSimulatorNavHost(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

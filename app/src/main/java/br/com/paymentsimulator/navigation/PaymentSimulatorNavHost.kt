package br.com.paymentsimulator.navigation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.paymentsimulator.R
import br.com.paymentsimulator.ui.cart.CartRoute
import br.com.paymentsimulator.ui.checkout.CheckoutRoute
import br.com.paymentsimulator.ui.component.PaymentTopBar
import br.com.paymentsimulator.ui.home.HomeRoute
import br.com.paymentsimulator.ui.paymentmethod.PaymentMethodRoute
import br.com.paymentsimulator.ui.paymentresult.PaymentResultRoute
import br.com.paymentsimulator.ui.processing.ProcessingRoute
import br.com.paymentsimulator.ui.receipt.ReceiptRoute
import br.com.paymentsimulator.ui.transactions.TransactionsRoute

@Composable
fun PaymentSimulatorNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = AppDestination.fromRoute(backStackEntry?.destination?.route)
    val showBottomBar = destination in AppDestination.sections

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PaymentTopBar(
                title = stringResource(destination.title),
                onBack = if (showBottomBar) {
                    null
                } else {
                    { navController.popBackStack() }
                },
            )
        },
        bottomBar = {
            if (showBottomBar) {
                PaymentBottomBar(
                    selected = destination,
                    onSelect = { section -> navController.openSection(section) },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Home.route,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            composable(AppDestination.Home.route) {
                HomeRoute(
                    onOpenCart = { navController.openSection(AppDestination.Cart) },
                    onOpenTransactions = { navController.openSection(AppDestination.Transactions) },
                )
            }
            composable(AppDestination.Cart.route) {
                CartRoute(
                    onCheckout = { navController.navigate(AppDestination.Checkout.route) },
                )
            }
            composable(AppDestination.Checkout.route) {
                CheckoutRoute(
                    onConfirmed = { navController.navigate(AppDestination.PaymentMethod.route) },
                )
            }
            composable(AppDestination.PaymentMethod.route) {
                PaymentMethodRoute(
                    onProcessingStarted = { paymentId ->
                        navController.navigate(AppDestination.Processing.filled(paymentId)) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(
                route = AppDestination.Processing.route,
                arguments = listOf(navArgument("paymentId") { type = NavType.StringType }),
            ) {
                ProcessingRoute(
                    onResolved = { paymentId ->
                        navController.navigate(AppDestination.PaymentResult.filled(paymentId)) {
                            popUpTo(AppDestination.Processing.filled(paymentId)) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(
                route = AppDestination.PaymentResult.route,
                arguments = listOf(navArgument("paymentId") { type = NavType.StringType }),
            ) {
                PaymentResultRoute(
                    onOpenReceipt = { paymentId ->
                        navController.navigate(AppDestination.Receipt.filled(paymentId))
                    },
                    onBackHome = { navController.returnHome() },
                )
            }
            composable(
                route = AppDestination.Receipt.route,
                arguments = listOf(navArgument("paymentId") { type = NavType.StringType }),
            ) {
                ReceiptRoute(
                    onOpenTransactions = { navController.openSection(AppDestination.Transactions) },
                    onBackHome = { navController.returnHome() },
                )
            }
            composable(AppDestination.Transactions.route) {
                TransactionsRoute(
                    onOpenReceipt = { paymentId ->
                        navController.navigate(AppDestination.Receipt.filled(paymentId))
                    },
                )
            }
        }
    }
}

private val AppDestination.title: Int
    get() = when (this) {
        AppDestination.Home -> R.string.home_title
        AppDestination.Cart -> R.string.cart_title
        AppDestination.Checkout -> R.string.checkout_title
        AppDestination.PaymentMethod -> R.string.payment_method_title
        AppDestination.Processing -> R.string.processing_title
        AppDestination.PaymentResult -> R.string.payment_result_title
        AppDestination.Receipt -> R.string.receipt_title
        AppDestination.Transactions -> R.string.transactions_title
    }

private fun NavHostController.openSection(destination: AppDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.returnHome() {
    popBackStack(AppDestination.Home.route, inclusive = false)
}

@Composable
private fun PaymentBottomBar(
    selected: AppDestination,
    onSelect: (AppDestination) -> Unit,
) {
    NavigationBar {
        SectionItem(
            destination = AppDestination.Home,
            icon = Icons.Filled.Home,
            label = stringResource(R.string.nav_home),
            selected = selected == AppDestination.Home,
            onSelect = onSelect,
        )
        SectionItem(
            destination = AppDestination.Cart,
            icon = Icons.Filled.ShoppingCart,
            label = stringResource(R.string.nav_cart),
            selected = selected == AppDestination.Cart,
            onSelect = onSelect,
        )
        SectionItem(
            destination = AppDestination.Transactions,
            icon = Icons.AutoMirrored.Filled.List,
            label = stringResource(R.string.nav_transactions),
            selected = selected == AppDestination.Transactions,
            onSelect = onSelect,
        )
    }
}

@Composable
private fun RowScope.SectionItem(
    destination: AppDestination,
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onSelect: (AppDestination) -> Unit,
) {
    NavigationBarItem(
        selected = selected,
        onClick = { onSelect(destination) },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
            )
        },
        label = { Text(text = label) },
        alwaysShowLabel = true,
    )
}

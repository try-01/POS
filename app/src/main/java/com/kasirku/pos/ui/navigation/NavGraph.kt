package com.kasirku.pos.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kasirku.pos.ui.screens.history.HistoryScreen
import com.kasirku.pos.ui.screens.inventory.InventoryScreen
import com.kasirku.pos.ui.screens.pos.PosScreen
import com.kasirku.pos.ui.screens.receipt.ReceiptScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    data object Pos : Screen("pos", "Kasir", Icons.Default.PointOfSale)
    data object Inventory : Screen("inventory", "Inventaris", Icons.Default.Inventory)
    data object History : Screen("history", "Riwayat", Icons.Default.History)
    data object Receipt : Screen("receipt/{transactionId}", "Struk") {
        fun createRoute(transactionId: Long) = "receipt/\$transactionId"
    }
}

private val bottomNavItems = listOf(Screen.Pos, Screen.Inventory, Screen.History)

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            val showBottomBar = bottomNavItems.any { screen ->
                currentDestination?.hierarchy?.any { it.route == screen.route } == true
            }

            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { screen.icon?.let { Icon(it, screen.title) } },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Pos.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Pos.route) {
                PosScreen(onNavigateToReceipt = { txId -> navController.navigate(Screen.Receipt.createRoute(txId)) })
            }
            composable(Screen.Inventory.route) { InventoryScreen() }
            composable(Screen.History.route) {
                HistoryScreen(onViewReceipt = { txId -> navController.navigate(Screen.Receipt.createRoute(txId)) })
            }
            composable(
                route = Screen.Receipt.route,
                arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
            ) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: 0L
                ReceiptScreen(transactionId = transactionId, onBack = { navController.popBackStack() })
            }
        }
    }
}

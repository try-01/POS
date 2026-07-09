package com.example.kasirpos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kasirpos.ui.inventory.InventoryScreen
import com.example.kasirpos.ui.inventory.InventoryViewModel
import com.example.kasirpos.ui.pos.PosScreen
import com.example.kasirpos.ui.pos.PosViewModel
import com.example.kasirpos.ui.receipt.ReceiptScreen
import com.example.kasirpos.ui.receipt.ReceiptViewModel
import com.example.kasirpos.ui.report.ReportScreen
import com.example.kasirpos.ui.report.ReportViewModel
import com.example.kasirpos.ui.theme.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as KasirApp

        setContent {
            KasirPOSTheme {
                MainNavHost(app)
            }
        }
    }
}

/** Tema global KasirPOS — Material 3 dark theme */
@Composable
fun KasirPOSTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = AccentBlue,
            secondary = AccentGreen,
            background = PrimaryDark,
            surface = SurfaceDark,
            onPrimary = PrimaryDark,
            onSecondary = PrimaryDark,
            onBackground = TextPrimary,
            onSurface = TextPrimary,
            error = AccentRed
        ),
        content = content
    )
}

/** Bottom navigation tabs */
private data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem("pos", "Kasir", Icons.Filled.PointOfSale, Icons.Outlined.PointOfSale),
    BottomNavItem("inventory", "Inventaris", Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
    BottomNavItem("report", "Laporan", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    @Suppress("DEPRECATION")
    BottomNavItem("receipt", "Struk", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainNavHost(app: KasirApp) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = PrimaryDark,
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                contentColor = TextPrimary,
                tonalElevation = 0.dp
            ) {
                bottomNavItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == index) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = {
                            Text(
                                item.label,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentBlue,
                            selectedTextColor = AccentBlue,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = AccentBlue.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> {
                    val vm: PosViewModel = viewModel(
                        factory = PosViewModel.Factory(
                            app.productRepository,
                            app.cartRepository,
                            app.transactionRepository
                        )
                    )
                    PosScreen(vm)
                }
                1 -> {
                    val vm: InventoryViewModel = viewModel(
                        factory = InventoryViewModel.Factory(app.productRepository)
                    )
                    InventoryScreen(vm)
                }
                2 -> {
                    val vm: ReportViewModel = viewModel(
                        factory = ReportViewModel.Factory(app.transactionRepository)
                    )
                    ReportScreen(vm)
                }
                3 -> {
                    val vm: ReceiptViewModel = viewModel(
                        factory = ReceiptViewModel.Factory(app.transactionRepository)
                    )
                    ReceiptScreen(vm)
                }
            }
        }
    }
}

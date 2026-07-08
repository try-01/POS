package com.example.posoffline.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.posoffline.PosApplication
import com.example.posoffline.ui.screen.history.HistoryScreen
import com.example.posoffline.ui.screen.inventory.InventoryScreen
import com.example.posoffline.ui.screen.pos.PosScreen
import com.example.posoffline.ui.theme.AppColors
import com.example.posoffline.ui.theme.OfflinePosTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as PosApplication).container

        setContent {
            OfflinePosTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AppColors.Bg0)
                    ) {
                        AppRoot(container)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRoot(container: com.example.posoffline.AppContainer) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "pos") {
        composable("pos") {
            PosScreen(
                container = container,
                onNavigateInventory = { nav.navigate("inventory") },
                onNavigateHistory = { nav.navigate("history") }
            )
        }
        composable("inventory") {
            InventoryScreen(
                container = container,
                onBack = { nav.popBackStack() }
            )
        }
        composable("history") {
            HistoryScreen(
                container = container,
                onBack = { nav.popBackStack() }
            )
        }
    }
}

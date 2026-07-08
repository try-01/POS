package com.kasirku.pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.kasirku.pos.ui.navigation.NavGraph
import com.kasirku.pos.ui.theme.KasirKuTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity - Single Activity Architecture
 * Semua navigasi ditangani oleh Compose Navigation
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            KasirKuTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NavGraph()
                }
            }
        }
    }
}

package com.calogeroturco.binauralcompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.calogeroturco.binauralcompanion.ui.BinauralCompanionApp
import com.calogeroturco.binauralcompanion.ui.theme.BinauralCompanionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BinauralCompanionTheme {
                BinauralCompanionApp()
            }
        }
    }
}

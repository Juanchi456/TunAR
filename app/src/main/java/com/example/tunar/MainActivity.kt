package com.example.tunar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.tunar.presentation.navigation.TunARApp
import com.example.tunar.presentation.theme.TunARTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as TunARApplication).container
        setContent {
            TunARTheme {
                TunARApp(container)
            }
        }
    }
}

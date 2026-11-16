package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.FinAdvisorApp
import com.example.ui.theme.FinAdvisorTheme
import com.example.util.configureAppCheck

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent before rendering UI or creating ViewModels.
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            FinAdvisorTheme {
                FinAdvisorApp()
            }
        }
    }
}

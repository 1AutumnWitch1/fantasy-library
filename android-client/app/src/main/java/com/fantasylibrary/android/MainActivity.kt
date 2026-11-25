package com.fantasylibrary.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.fantasylibrary.android.ui.AppNavHost
import com.fantasylibrary.android.ui.theme.LibraryTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        val container = (application as FantasyLibraryApp).container
        if (BuildConfig.DEBUG) {
            intent?.getStringExtra("base_url")?.takeIf { it.isNotBlank() }?.let { preset ->
                lifecycleScope.launch(Dispatchers.IO) { container.settingsRepository.saveBaseUrl(preset) }
            }
        }
        setContent { LibraryTheme { AppNavHost(container) } }
    }
}

package com.fantasylibrary.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.fantasylibrary.android.data.settings.AppSettings
import com.fantasylibrary.android.ui.AppNavHost
import com.fantasylibrary.android.ui.components.libraryBackground
import com.fantasylibrary.android.ui.theme.Brass
import com.fantasylibrary.android.ui.theme.LibraryTheme
import com.fantasylibrary.android.ui.theme.Parchment
import com.fantasylibrary.android.ui.theme.ParchmentDim
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as FantasyLibraryApp).container

        // 仅 Debug：允许通过 adb 预置服务器地址，方便联调；Release 构建不读取任何外部输入地址。
        //   adb shell am start -n com.fantasylibrary.android.debug/com.fantasylibrary.android.MainActivity \
        //       -e base_url http://10.0.2.2:8080
        if (BuildConfig.DEBUG) {
            intent?.getStringExtra(EXTRA_BASE_URL)?.takeIf { it.isNotBlank() }?.let { preset ->
                lifecycleScope.launch(Dispatchers.IO) {
                    container.settingsRepository.saveBaseUrl(preset)
                }
            }
        }

        setContent {
            LibraryTheme {
                val settings by produceState<AppSettings?>(initialValue = null) {
                    container.settingsRepository.settings.collect { value = it }
                }

                var startDestination by rememberSaveable { mutableStateOf<String?>(null) }
                val loaded = settings
                if (startDestination == null && loaded != null) {
                    startDestination = if (loaded.configured) "home" else "config"
                }

                val destination = startDestination
                if (destination == null) {
                    SplashScreen()
                } else {
                    AppNavHost(container = container, startDestination = destination)
                }
            }
        }
    }

    companion object {
        /** 仅 Debug 可用：启动时预填服务器地址。 */
        const val EXTRA_BASE_URL = "base_url"
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .libraryBackground(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "奇幻图书馆",
                style = MaterialTheme.typography.displaySmall,
                color = Parchment,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "ANDROID 联动测试版",
                style = MaterialTheme.typography.labelSmall,
                color = ParchmentDim,
            )
            Spacer(Modifier.height(18.dp))
            CircularProgressIndicator(
                color = Brass,
                strokeWidth = 2.dp,
            )
        }
    }
}

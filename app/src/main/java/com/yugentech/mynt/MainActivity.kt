package com.yugentech.mynt

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.yugentech.mynt.auth.viewmodel.AuthViewModel
import com.yugentech.mynt.navigation.host.AppNavHost
import com.yugentech.mynt.theme.viewmodel.ThemeViewModel
import com.yugentech.mynt.ui.shared.util.LocalCurrencySymbol
import com.yugentech.mynt.ui.tabs.profileScreen.viewmodel.SettingsViewModel
import com.yugentech.theme.MyntTheme
import com.yugentech.theme.models.ThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        val animationReady = MutableStateFlow(false)
        lifecycleScope.launch {
            delay(1000.milliseconds)
            animationReady.value = true
        }

        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition {
            !animationReady.value || authViewModel.authState.value.isInitializing
        }

        setContent {
            val navController = rememberNavController()

            val themeViewModel: ThemeViewModel = koinViewModel()
            val themeConfiguration by themeViewModel.themeConfiguration.collectAsStateWithLifecycle()
            val darkTheme = when (themeConfiguration.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            val settingsViewModel: SettingsViewModel = koinViewModel()
            val settings by settingsViewModel.settingsConfiguration.collectAsStateWithLifecycle()

            enableEdgeToEdge(
                statusBarStyle = if (darkTheme) {
                    SystemBarStyle.dark(scrim = Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(scrim = Color.TRANSPARENT, darkScrim = Color.TRANSPARENT)
                },
                navigationBarStyle = if (darkTheme) {
                    SystemBarStyle.dark(scrim = Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(scrim = Color.TRANSPARENT, darkScrim = Color.TRANSPARENT)
                }
            )

            MyntTheme(
                themeConfiguration = themeConfiguration
            ) {
                CompositionLocalProvider(LocalCurrencySymbol provides settings.currencySymbol) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val authState by authViewModel.authState.collectAsStateWithLifecycle()

                        if (!authState.isInitializing) {
                            AppNavHost(
                                navController = navController,
                                webClientId = BuildConfig.WEB_CLIENT_ID,
                                authViewModel = authViewModel,
                                themeViewModel = themeViewModel,
                                settingsViewModel = settingsViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

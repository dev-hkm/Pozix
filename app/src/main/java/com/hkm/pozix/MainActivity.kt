package com.hkm.pozix

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import android.app.Activity
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalView

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.hkm.pozix.data.repository.SettingsRepository
import com.hkm.pozix.ui.components.richcontent.LocalCodeHighlight
import com.hkm.pozix.ui.theme.PozixTheme
import com.hkm.pozix.util.LocaleHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import androidx.activity.SystemBarStyle

class MainActivity : ComponentActivity() {
    
    private lateinit var settingsRepository: SettingsRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configure transparent edge-to-edge system bars matching light/dark mode
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        
        settingsRepository = SettingsRepository(applicationContext)
        
        lifecycleScope.launch {
            val language = settingsRepository.getLanguage().first()
            val font = settingsRepository.getFont().first()
            applyLocale(language)
            
            setContent {
                var currentFont by remember { mutableStateOf(font) }
                val themeMode by settingsRepository.getThemeMode().collectAsState(initial = "system")
                val isSystemDark = isSystemInDarkTheme()
                val isDark = when (themeMode) {
                    "light" -> false
                    "dark" -> true
                    else -> isSystemDark
                }

                val view = LocalView.current
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = (view.context as Activity).window
                        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
                        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark
                    }
                }

                // Observe font changes
                LaunchedEffect(Unit) {
                    settingsRepository.getFont().collect { newFont ->
                        currentFont = newFont
                    }
                }
                
                val codeHighlight by settingsRepository.getCodeHighlight().collectAsState(initial = true)
                PozixTheme(
                    darkTheme = isDark,
                    dynamicColor = true,
                    fontFamily = currentFont
                ) {
                    androidx.compose.material3.Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        CompositionLocalProvider(LocalCodeHighlight provides codeHighlight) {
                            PozixApp(
                                onLanguageChanged = {
                                    lifecycleScope.launch {
                                        val newLanguage = settingsRepository.getLanguage().first()
                                        applyLocale(newLanguage)
                                        // Recreate activity to apply new locale
                                        recreate()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    private fun applyLocale(languageCode: String) {
        val context = LocaleHelper.setLocale(this, languageCode)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(context.resources.configuration, context.resources.displayMetrics)
    }
    
    override fun attachBaseContext(newBase: Context) {
        val settingsRepo = SettingsRepository(newBase)
        var languageCode = "en"
        
        // Try to get saved language synchronously
        try {
            kotlinx.coroutines.runBlocking {
                languageCode = settingsRepo.getLanguage().first()
            }
        } catch (e: Exception) {
            // Use default
        }
        
        val context = LocaleHelper.setLocale(newBase, languageCode)
        super.attachBaseContext(context)
    }
}

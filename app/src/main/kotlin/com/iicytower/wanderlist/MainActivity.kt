package com.iicytower.wanderlist

import android.Manifest
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import com.iicytower.wanderlist.domain.repository.SettingsRepository
import com.iicytower.wanderlist.navigation.WanderListNavGraph
import com.iicytower.wanderlist.ui.theme.WanderListTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {

    private val settingsRepository: SettingsRepository by inject()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* permission result handled silently — UI shows guidance when needed */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)

        // appLanguage z Ustawień steruje też językiem UI; AppCompatDelegate
        // przeładowuje aktywność przy zmianie locale
        lifecycleScope.launch {
            settingsRepository.getSettings()
                .map { it.appLanguage }
                .distinctUntilChanged()
                .collect { language ->
                    val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
                    if (current != language) {
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
                    }
                }
        }

        setContent {
            WanderListTheme {
                WanderListNavGraph()
            }
        }
    }
}

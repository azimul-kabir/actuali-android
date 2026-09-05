package com.azimulkabir.actuali

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.azimulkabir.actuali.ui.navigation.AppNavigation
import com.azimulkabir.actuali.ui.theme.ActualiAndroidTheme
import com.azimulkabir.actuali.data.sync.ActualSyncScheduler
import com.azimulkabir.actuali.data.preferences.DisplayPreferences

class MainActivity : ComponentActivity() {
    private var foregroundGeneration by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActualSyncScheduler.schedulePeriodic(this)
        enableEdgeToEdge()
        setContent {
            var appearance by remember { mutableStateOf(DisplayPreferences(this).appearance) }
            ActualiAndroidTheme(appearance = appearance) {
                AppNavigation(
                    modifier = Modifier.fillMaxSize(),
                    foregroundGeneration = foregroundGeneration,
                    onAppearanceChange = { appearance = it },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        foregroundGeneration += 1
    }
}

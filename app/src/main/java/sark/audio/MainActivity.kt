package sark.audio

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import sark.audio.ui.screens.*
import sark.audio.ui.theme.SarkAudioTheme

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        setContent {
            SarkAudioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf("dashboard") }

                    when (currentScreen) {
                        "dashboard" -> AudioDashboardScreen(
                            onNavigateToTool = { toolId -> currentScreen = toolId }
                        )
                        "recorder" -> AudioRecorderScreen(
                            onBack = { currentScreen = "dashboard" }
                        )
                        "trimmer" -> AudioTrimmerScreen(
                            onBack = { currentScreen = "dashboard" }
                        )
                        "merger" -> AudioMergerScreen(
                            onBack = { currentScreen = "dashboard" }
                        )
                        "effects" -> AudioEffectsScreen(
                            onBack = { currentScreen = "dashboard" }
                        )
                        "inspector" -> AudioInspectorScreen(
                            onBack = { currentScreen = "dashboard" }
                        )
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        val neededPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (neededPermissions.isNotEmpty()) {
            permissionLauncher.launch(neededPermissions.toTypedArray())
        }
    }
}

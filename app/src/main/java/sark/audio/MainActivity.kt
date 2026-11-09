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
                        "creator", "dtmf_creator", "silence_creator" -> AudioCreatorScreen(
                            onBack = { currentScreen = "dashboard" }
                        )
                        "recorder" -> AudioRecorderScreen(
                            onBack = { currentScreen = "dashboard" }
                        )
                        "editor", "trimmer", "fade_editor", "silence_strip" -> AudioTrimmerScreen(
                            onBack = { currentScreen = "dashboard" }
                        )
                        "merger" -> AudioMergerScreen(
                            onBack = { currentScreen = "dashboard" }
                        )
                        "mixer" -> AudioMixerScreen(
                            onBack = { currentScreen = "dashboard" }
                        )
                        "vocal_remover", "autotune", "voice_changer", "stems_splitter", "voice_clone" -> AudioVocalSuiteScreen(
                            onBack = { currentScreen = "dashboard" }
                        )
                        "mastering", "equalizer", "eight_d", "chorus", "denoise" -> AudioMasteringAndEffectsScreen(
                            onBack = { currentScreen = "dashboard" }
                        )
                        "effects", "compressor", "delay", "distortion", "bitcrusher", "channel_processor", "loop_creator", "sfx_manager" -> AudioEffectsScreen(
                            onBack = { currentScreen = "dashboard" }
                        )
                        "track_splitter", "bulk_converter", "video_to_audio" -> AudioConverterAndSplitterScreen(
                            onBack = { currentScreen = "dashboard" }
                        )
                        "tuner", "analyzer" -> AudioAnalyzerScreen(
                            onBack = { currentScreen = "dashboard" }
                        )
                        "inspector", "bitdepth" -> AudioInspectorScreen(
                            onBack = { currentScreen = "dashboard" }
                        )
                        "tag_editor" -> AudioTagEditorScreen(
                            onBack = { currentScreen = "dashboard" }
                        )
                        "manager" -> AudioFileManagerScreen(
                            onBack = { currentScreen = "dashboard" }
                        )
                        else -> AudioDashboardScreen(
                            onNavigateToTool = { toolId -> currentScreen = toolId }
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

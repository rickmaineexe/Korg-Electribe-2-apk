package com.hardtekk.electribe

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.hardtekk.electribe.model.AppState
import com.hardtekk.electribe.model.PadConfig
import com.hardtekk.electribe.model.Pattern
import com.hardtekk.electribe.model.SampleData
import com.hardtekk.electribe.model.SequencerMode
import com.hardtekk.electribe.ui.MainScreen
import com.hardtekk.electribe.ui.theme.HardtekkColors
import com.hardtekk.electribe.ui.theme.HardtekkElectribeTheme
import com.hardtekk.electribe.ui.theme.HardtekkType
import kotlinx.coroutines.launch

/**
 * Main Activity - Fullscreen landscape groovebox interface
 */
class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    // State
    private var appState = AppState()
    private var patterns = Array(4) { index -> Pattern.createEmpty(index, 16, 140) }
    private var padConfigs = List(16) { index -> PadConfig.defaultForPad(index) }
    private var padLabels = List(16) { "" }
    private var activePads = mutableListOf<Int>()
    private var loadedSample: SampleData? = null
    private var waveformData = FloatArray(0)
    private var sliceStart = 0.0f
    private var sliceEnd = 1.0f
    private var playheadPosition = 0.0f
    private var isPlayingSlice = false
    private var padAssignments = mutableListOf<Int>()

    // Audio engine
    private var audioEngine = application.hardtekkElectribeApplication.getAudioEngine()

    // Permission request
    private val requestPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        permissions.forEach { (permission, granted) ->
            Log.d(TAG, "Permission $permission: $granted")
        }
    }

    // File picker for loading samples
    private val pickAudioFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleAudioFileSelected(it) }
    }

    // File picker for loading songs (slicer)
    private val pickSongFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleSongFileSelected(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen landscape
        requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.setFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // Hide system bars
        hideSystemBars()

        // Check permissions
        checkPermissions()

        // Initialize audio engine when ready
        lifecycleScope.launch {
            while (audioEngine == null) {
                audioEngine = application.hardtekkElectribeApplication.getAudioEngine()
                if (audioEngine == null) {
                    kotlinx.coroutines.delay(100)
                }
            }
            setupAudioEngineCallbacks()
        }

        setContent {
            HardtekkElectribeTheme {
                MainScreenContent()
            }
        }
    }

    private fun hideSystemBars() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
        } else {
            // Android 12 and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }

        if (permissions.isNotEmpty()) {
            requestPermissions.launch(permissions.toTypedArray())
        }
    }

    private fun setupAudioEngineCallbacks() {
        audioEngine?.onStepCallback = { step ->
            runOnUiThread {
                appState = appState.copy(currentStep = step)
                // Recomposition will happen via state
            }
        }

        audioEngine?.onPatternChangeCallback = { patternIndex ->
            runOnUiThread {
                appState = appState.copy(currentPatternIndex = patternIndex)
            }
        }
    }

    private fun handleAudioFileSelected(uri: Uri) {
        lifecycleScope.launch {
            try {
                val repository = application.hardtekkElectribeApplication.getSampleRepository()
                val fileName = com.hardtekk.electribe.utils.FileUtils.getFileName(this@MainActivity, uri) ?: "Sample"
                val destDir = com.hardtekk.electribe.utils.FileUtils.getAudioFilesDir(this@MainActivity)
                val destFile = com.hardtekk.electribe.utils.FileUtils.createUniqueFileName(destDir, fileName, "wav")

                if (com.hardtekk.electribe.utils.FileUtils.copyUriToFile(this@MainActivity, uri, destFile)) {
                    val sample = repository?.importUserSample(destFile, fileName)
                    sample?.let { s ->
                        // Assign to selected pad
                        val padIndex = appState.selectedPadIndex
                        padConfigs[padIndex] = padConfigs[padIndex].copy(
                            samplePath = s.filePath,
                            sampleName = s.name
                        )
                        padLabels[padIndex] = s.name.take(8)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle audio file", e)
            }
        }
    }

    private fun handleSongFileSelected(uri: Uri) {
        lifecycleScope.launch {
            try {
                val repository = application.hardtekkElectribeApplication.getSampleRepository()
                val fileName = com.hardtekk.electribe.utils.FileUtils.getFileName(this@MainActivity, uri) ?: "Song"
                val destDir = com.hardtekk.electribe.utils.FileUtils.getAudioFilesDir(this@MainActivity)
                val destFile = com.hardtekk.electribe.utils.FileUtils.createUniqueFileName(destDir, fileName, "wav")

                if (com.hardtekk.electribe.utils.FileUtils.copyUriToFile(this@MainActivity, uri, destFile)) {
                    val sample = repository?.importUserSample(destFile, fileName, SampleData.SampleCategory.CUSTOM)
                    sample?.let { s ->
                        loadedSample = s
                        // Load waveform data
                        com.hardtekk.electribe.utils.AudioFileLoader.loadWaveformData(destFile)?.let { waveData ->
                            waveformData = waveData
                        }
                        sliceStart = 0.0f
                        sliceEnd = 1.0f
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle song file", e)
            }
        }
    }

    // Compose UI
    @Composable
    fun MainScreenContent() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(HardtekkColors.Anthracite)
        ) {
            MainScreen(
                appState = appState,
                patterns = patterns.toList(),
                padConfigs = padConfigs,
                padLabels = padLabels,
                activePads = activePads,
                loadedSample = loadedSample,
                waveformData = waveformData,
                sliceStart = sliceStart,
                sliceEnd = sliceEnd,
                playheadPosition = playheadPosition,
                isPlayingSlice = isPlayingSlice,
                padAssignments = padAssignments,
                // Callbacks
                onPadTrigger = { padIndex, velocity ->
                    activePads.add(padIndex)
                    audioEngine?.triggerPad(padIndex, velocity)
                    // Remove from active after short delay
                    lifecycleScope.launch {
                        kotlinx.coroutines.delay(100)
                        activePads.remove(padIndex)
                    }
                },
                onPadLongPress = { padIndex ->
                    // Open sample picker for this pad
                    pickAudioFile.launch("audio/*")
                },
                onPatternChange = { patternIndex ->
                    appState = appState.copy(currentPatternIndex = patternIndex)
                    audioEngine?.setPattern(patternIndex)
                },
                onPlayClick = {
                    if (appState.isPlaying) {
                        audioEngine?.stop()
                        appState = appState.copy(isPlaying = false)
                    } else {
                        audioEngine?.play()
                        appState = appState.copy(isPlaying = true)
                    }
                },
                onStopClick = {
                    audioEngine?.stop()
                    appState = appState.copy(isPlaying = false, isRecording = false, currentStep = 0)
                },
                onRecordClick = {
                    val newRecording = !appState.isRecording
                    audioEngine?.setRecording(newRecording)
                    appState = appState.copy(isRecording = newRecording)
                    if (newRecording && !appState.isPlaying) {
                        audioEngine?.play()
                        appState = appState.copy(isPlaying = true)
                    }
                },
                onBpmIncrement = {
                    val newBpm = (appState.currentBpm + 1).coerceIn(AppState.MIN_BPM, AppState.MAX_BPM)
                    audioEngine?.setBpm(newBpm)
                    appState = appState.withBpm(newBpm)
                },
                onBpmDecrement = {
                    val newBpm = (appState.currentBpm - 1).coerceIn(AppState.MIN_BPM, AppState.MAX_BPM)
                    audioEngine?.setBpm(newBpm)
                    appState = appState.withBpm(newBpm)
                },
                onBpmTap = {
                    // Tap tempo implementation
                },
                onFilterCutoffChange = { value ->
                    appState = appState.copy(masterFilterCutoff = value)
                    audioEngine?.setMasterFilter(
                        com.hardtekk.electribe.audio.BiquadFilter.FilterType.LOW_PASS,
                        value,
                        appState.masterFilterResonance
                    )
                },
                onFilterResonanceChange = { value ->
                    appState = appState.copy(masterFilterResonance = value)
                    audioEngine?.setMasterFilter(
                        com.hardtekk.electribe.audio.BiquadFilter.FilterType.LOW_PASS,
                        appState.masterFilterCutoff,
                        value
                    )
                },
                onDistortionChange = { value ->
                    appState = appState.copy(masterDistortion = value)
                    audioEngine?.setMasterDistortion(
                        com.hardtekk.electribe.audio.Distortion.DistortionType.SOFT_CLIP,
                        value,
                        value
                    )
                },
                onDelayChange = { value ->
                    appState = appState.copy(masterDelayAmount = value)
                    audioEngine?.setMasterDelay(
                        value * 2000f,
                        appState.masterDelayFeedback,
                        value
                    )
                },
                onVolumeChange = { value ->
                    appState = appState.copy(masterVolume = value)
                },
                onStepToggle = { padIndex, stepIndex ->
                    val newPattern = patterns[appState.currentPatternIndex].togglePadAtStep(stepIndex, padIndex)
                    patterns[appState.currentPatternIndex] = newPattern
                },
                onStepVelocityChange = { padIndex, stepIndex, velocity ->
                    // Update velocity in pattern
                    val pattern = patterns[appState.currentPatternIndex]
                    val stepData = pattern.getStep(stepIndex)
                    if (stepData != null && stepData.triggeredPads.contains(padIndex)) {
                        val newVelocities = stepData.velocities.toMutableMap()
                        newVelocities[padIndex] = velocity
                        val newStepData = stepData.copy(velocities = newVelocities)
                        patterns[appState.currentPatternIndex] = pattern.withStep(stepIndex, newStepData)
                    }
                },
                onPadSelect = { padIndex ->
                    appState = appState.copy(selectedPadIndex = padIndex)
                },
                onSwingChange = { swing ->
                    appState = appState.copy(swing = swing)
                },
                onClearPattern = {
                    patterns[appState.currentPatternIndex] = patterns[appState.currentPatternIndex].clearAllSteps()
                },
                onCopyPattern = {
                    // Copy current pattern to clipboard
                },
                onPastePattern = {
                    // Paste pattern from clipboard
                },
                onSliceStartChange = { value ->
                    sliceStart = value.coerceIn(0.0f, sliceEnd - 0.01f)
                },
                onSliceEndChange = { value ->
                    sliceEnd = value.coerceIn(sliceStart + 0.01f, 1.0f)
                },
                onScrub = { position ->
                    playheadPosition = position
                    // Seek in preview
                },
                onPreviewSlice = {
                    // Preview the slice
                    isPlayingSlice = true
                    // TODO: Play slice audio
                    lifecycleScope.launch {
                        kotlinx.coroutines.delay(2000)
                        isPlayingSlice = false
                    }
                },
                onAssignToPad = { padIndex ->
                    // Assign current slice to pad
                    if (loadedSample != null) {
                        val sliceName = "${loadedSample!!.name}_${(sliceStart * 100).toInt()}-${(sliceEnd * 100).toInt()}"
                        // In real implementation, extract slice and save as new sample
                        padAssignments.add(padIndex)
                        padLabels[padIndex] = sliceName.take(8)
                    }
                },
                onLoadSong = {
                    pickSongFile.launch("audio/*")
                },
                onModeChange = { mode ->
                    appState = appState.copy(sequencerMode = mode)
                }
            )
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    override fun onDestroy() {
        audioEngine?.shutdown()
        super.onDestroy()
    }
}
package com.hardtekk.electribe.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hardtekk.electribe.R
import com.hardtekk.electribe.model.AppState
import com.hardtekk.electribe.model.PadConfig
import com.hardtekk.electribe.model.Pattern
import com.hardtekk.electribe.model.SampleData
import com.hardtekk.electribe.model.SequencerMode
import com.hardtekk.electribe.ui.components.ModeSelector
import com.hardtekk.electribe.ui.screens.SampleSlicerScreen
import com.hardtekk.electribe.ui.screens.StepSequencerScreen
import com.hardtekk.electribe.ui.screens.TriggerModeScreen
import com.hardtekk.electribe.ui.theme.HardtekkColors
import com.hardtekk.electribe.ui.theme.HardtekkType

/**
 * Main Screen - Routes to appropriate mode screen
 */
@Composable
fun MainScreen(
    // App State
    appState: AppState,
    patterns: List<Pattern>,
    padConfigs: List<PadConfig>,
    padLabels: List<String>,
    activePads: List<Int>,
    loadedSample: SampleData?,
    waveformData: FloatArray,
    sliceStart: Float,
    sliceEnd: Float,
    playheadPosition: Float,
    isPlayingSlice: Boolean,
    padAssignments: List<Int>,
    // Callbacks
    onPadTrigger: (Int, Float) -> Unit,
    onPadLongPress: (Int) -> Unit,
    onPatternChange: (Int) -> Unit,
    onPlayClick: () -> Unit,
    onStopClick: () -> Unit,
    onRecordClick: () -> Unit,
    onBpmIncrement: () -> Unit,
    onBpmDecrement: () -> Unit,
    onBpmTap: () -> Unit,
    onFilterCutoffChange: (Float) -> Unit,
    onFilterResonanceChange: (Float) -> Unit,
    onDistortionChange: (Float) -> Unit,
    onDelayChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onStepToggle: (Int, Int) -> Unit,
    onStepVelocityChange: (Int, Int, Float) -> Unit,
    onPadSelect: (Int) -> Unit,
    onSwingChange: (Float) -> Unit,
    onClearPattern: () -> Unit,
    onCopyPattern: () -> Unit,
    onPastePattern: () -> Unit,
    onSliceStartChange: (Float) -> Unit,
    onSliceEndChange: (Float) -> Unit,
    onScrub: (Float) -> Unit,
    onPreviewSlice: () -> Unit,
    onAssignToPad: (Int) -> Unit,
    onLoadSong: () -> Unit,
    onModeChange: (SequencerMode) -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    val currentPattern = patterns[appState.currentPatternIndex]

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HardtekkColors.Anthracite)
    ) {
        when (appState.sequencerMode) {
            SequencerMode.TRIGGER -> TriggerModeScreen(
                selectedPad = appState.selectedPadIndex,
                activePads = activePads,
                padLabels = padLabels,
                padConfigs = padConfigs,
                bpm = appState.currentBpm,
                currentPattern = appState.currentPatternIndex,
                isPlaying = appState.isPlaying,
                isRecording = appState.isRecording,
                masterFilterCutoff = appState.masterFilterCutoff,
                masterFilterResonance = appState.masterFilterResonance,
                masterDistortion = appState.masterDistortion,
                masterDelay = appState.masterDelayAmount,
                masterVolume = appState.masterVolume,
                onPadTrigger = onPadTrigger,
                onPadLongPress = onPadLongPress,
                onPatternChange = onPatternChange,
                onPlayClick = onPlayClick,
                onStopClick = onStopClick,
                onRecordClick = onRecordClick,
                onBpmIncrement = onBpmIncrement,
                onBpmDecrement = onBpmDecrement,
                onFilterCutoffChange = onFilterCutoffChange,
                onFilterResonanceChange = onFilterResonanceChange,
                onDistortionChange = onDistortionChange,
                onDelayChange = onDelayChange,
                onVolumeChange = onVolumeChange,
                modifier = Modifier.fillMaxSize()
            )

            SequencerMode.STEP_SEQUENCER -> StepSequencerScreen(
                pattern = currentPattern,
                currentStep = appState.currentStep,
                selectedPad = appState.selectedPadIndex,
                selectedStep = appState.currentStep,
                bpm = appState.currentBpm,
                currentPattern = appState.currentPatternIndex,
                isPlaying = appState.isPlaying,
                isRecording = appState.isRecording,
                swing = appState.swing,
                onStepToggle = onStepToggle,
                onStepVelocityChange = onStepVelocityChange,
                onPadSelect = onPadSelect,
                onPatternChange = onPatternChange,
                onPlayClick = onPlayClick,
                onStopClick = onStopClick,
                onRecordClick = onRecordClick,
                onBpmIncrement = onBpmIncrement,
                onBpmDecrement = onBpmDecrement,
                onSwingChange = onSwingChange,
                onModeChange = onModeChange,
                onClearPattern = onClearPattern,
                onCopyPattern = onCopyPattern,
                onPastePattern = onPastePattern,
                modifier = Modifier.fillMaxSize()
            )

            SequencerMode.SAMPLE_SLICER -> SampleSlicerScreen(
                loadedSample = loadedSample,
                waveformData = waveformData,
                sliceStart = sliceStart,
                sliceEnd = sliceEnd,
                playheadPosition = playheadPosition,
                isPlayingSlice = isPlayingSlice,
                selectedPad = appState.selectedPadIndex,
                padAssignments = padAssignments,
                onLoadSong = onLoadSong,
                onSliceStartChange = onSliceStartChange,
                onSliceEndChange = onSliceEndChange,
                onScrub = onScrub,
                onPreviewSlice = onPreviewSlice,
                onAssignToPad = onAssignToPad,
                onPadSelect = onPadSelect,
                onModeChange = onModeChange,
                onBpmIncrement = onBpmIncrement,
                onBpmDecrement = onBpmDecrement,
                onPlayClick = onPlayClick,
                onStopClick = onStopClick,
                onRecordClick = onRecordClick,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
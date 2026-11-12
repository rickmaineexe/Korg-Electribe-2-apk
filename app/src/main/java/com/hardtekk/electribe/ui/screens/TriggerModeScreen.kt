package com.hardtekk.electribe.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.spacedBy
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawRect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hardtekk.electribe.R
import com.hardtekk.electribe.model.PadConfig
import com.hardtekk.electribe.ui.components.KnobRow
import com.hardtekk.electribe.ui.components.KnobConfig
import com.hardtekk.electribe.ui.components.PatternSelector
import com.hardtekk.electribe.ui.components.TriggerPadGrid
import com.hardtekk.electribe.ui.theme.HardtekkColors
import com.hardtekk.electribe.ui.theme.HardtekkType

/**
 * Trigger Mode Screen - Main performance view with 16 pads and effects knobs
 */
@Composable
fun TriggerModeScreen(
    // State
    selectedPad: Int,
    activePads: List<Int>,
    padLabels: List<String>,
    padConfigs: List<PadConfig>,
    bpm: Int,
    currentPattern: Int,
    isPlaying: Boolean,
    isRecording: Boolean,
    // Effects
    masterFilterCutoff: Float,
    masterFilterResonance: Float,
    masterDistortion: Float,
    masterDelay: Float,
    masterVolume: Float,
    // Callbacks
    onPadTrigger: (Int, Float) -> Unit,
    onPadLongPress: (Int) -> Unit,
    onPatternChange: (Int) -> Unit,
    onPlayClick: () -> Unit,
    onStopClick: () -> Unit,
    onRecordClick: () -> Unit,
    onBpmIncrement: () -> Unit,
    onBpmDecrement: () -> Unit,
    onFilterCutoffChange: (Float) -> Unit,
    onFilterResonanceChange: (Float) -> Unit,
    onDistortionChange: (Float) -> Unit,
    onDelayChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    // Effects knobs configuration
    val knobs = remember {
        listOf(
            KnobConfig(
                label = "LPF",
                value = masterFilterCutoff,
                formatter = { "%.0f%%".format(it * 100) },
                color = HardtekkColors.NeonPink
            ),
            KnobConfig(
                label = "HPF",
                value = masterFilterResonance,
                formatter = { "%.0f%%".format(it * 100) },
                color = HardtekkColors.Magenta
            ),
            KnobConfig(
                label = "PITCH",
                value = 0.5f, // Center = 0 semitones
                minValue = 0.0f,
                maxValue = 1.0f,
                formatter = { val semitones = (it - 0.5f) * 48; "%.0f".format(semitones) + (if (semitones > 0) "+" else "") + "st" },
                color = HardtekkColors.LedBlue
            ),
            KnobConfig(
                label = "DRIVE",
                value = masterDistortion,
                formatter = { "%.0f%%".format(it * 100) },
                color = HardtekkColors.LedAmber
            ),
            KnobConfig(
                label = "DELAY",
                value = masterDelay,
                formatter = { "%.0f%%".format(it * 100) },
                color = HardtekkColors.LedGreen
            ),
            KnobConfig(
                label = "MASTER",
                value = masterVolume,
                formatter = { "%.0f%%".format(it * 100) },
                color = HardtekkColors.NeonPinkBright
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HardtekkColors.Anthracite)
            .padding(vertical = 12.dp)
    ) {
        // Top Section: BPM, Transport, Pattern, Mode
        TopBar(
            bpm = bpm,
            isPlaying = isPlaying,
            isRecording = isRecording,
            currentPattern = currentPattern,
            onBpmIncrement = onBpmIncrement,
            onBpmDecrement = onBpmDecrement,
            onPlayClick = onPlayClick,
            onStopClick = onStopClick,
            onRecordClick = onRecordClick,
            onPatternChange = onPatternChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, bottom = 8.dp)
        )

        // Middle Section: 16 Trigger Pads
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .weight(1f, fill = false)
        ) {
            TriggerPadGrid(
                selectedPad = selectedPad,
                activePads = activePads,
                padLabels = padLabels,
                onPadTrigger = onPadTrigger,
                onPadLongPress = onPadLongPress,
                modifier = Modifier.fillMaxSize(),
                showVelocity = true
            )
        }

        // Bottom Section: Effects Knobs
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, top = 8.dp, bottom = 12.dp)
                .height(120.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Knob labels row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    knobs.forEach { knob ->
                        Text(
                            text = knob.label,
                            fontFamily = HardtekkType.SansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = HardtekkColors.TextMuted,
                            letterSpacing = 0.1.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Knob row
                KnobRow(
                    knobs = knobs,
                    onKnobChange = { index, value ->
                        when (index) {
                            0 -> onFilterCutoffChange(value)
                            1 -> onFilterResonanceChange(value)
                            2 -> { /* Pitch - handled separately */ }
                            3 -> onDistortionChange(value)
                            4 -> onDelayChange(value)
                            5 -> onVolumeChange(value)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun TopBar(
    bpm: Int,
    isPlaying: Boolean,
    isRecording: Boolean,
    currentPattern: Int,
    onBpmIncrement: () -> Unit,
    onBpmDecrement: () -> Unit,
    onPlayClick: () -> Unit,
    onStopClick: () -> Unit,
    onRecordClick: () -> Unit,
    onPatternChange: (Int) -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // BPM Control
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(60.dp)
                .background(HardtekkColors.AnthraciteCard, RoundedCornerShape(8.dp))
                .border(2.dp, HardtekkColors.NeonPink, RoundedCornerShape(8.dp))
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // BPM value
                    Text(
                        text = bpm.toString(),
                        fontFamily = HardtekkType.MonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = HardtekkColors.NeonPink
                    )
                }
                Text(
                    text = stringResource(R.string.bpm_label),
                    fontFamily = HardtekkType.SansFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 9.sp,
                    color = HardtekkColors.TextMuted,
                    letterSpacing = 0.15.sp
                )
            }
        }

        // BPM +/- buttons
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            IconButton(
                icon = "+",
                onClick = onBpmIncrement,
                modifier = Modifier.size(40.dp)
            )
            IconButton(
                icon = "−",
                onClick = onBpmDecrement,
                modifier = Modifier.size(40.dp)
            )
        }

        // Transport Controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TransportButton(
                label = stringResource(R.string.stop),
                color = HardtekkColors.LedRed,
                isActive = !isPlaying,
                onClick = onStopClick
            )
            TransportButton(
                label = stringResource(R.string.play),
                color = HardtekkColors.LedGreen,
                isActive = isPlaying && !isRecording,
                onClick = onPlayClick
            )
            TransportButton(
                label = stringResource(R.string.rec),
                color = HardtekkColors.LedRed,
                isActive = isRecording,
                onClick = onRecordClick
            )
        }

        // Pattern Selector
        PatternSelector(
            currentPattern = currentPattern,
            onPatternSelect = onPatternChange,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun TransportButton(
    label: String,
    color: Color,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val backgroundColor = if (isPressed) color.copy(alpha = 0.9f)
    else if (isActive) color
    else HardtekkColors.AnthraciteElevated

    val textColor = if (isActive) Color.Black else HardtekkColors.TextSecondary

    Box(
        modifier = modifier
            .size(60.dp, 48.dp)
            .pointerInput(Unit) {
                androidx.compose.foundation.gestures.detectTapGestures(
                    onPress = { isPressed = true },
                    onTap = {
                        onClick()
                        isPressed = false
                    }
                )
            }
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .border(if (isActive) 0.dp else 1.dp, color, RoundedCornerShape(8.dp))
    ) {
        Text(
            text = label,
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center),
            fontFamily = HardtekkType.SansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = textColor,
            letterSpacing = 0.1.sp
        )
    }
}

@Composable
fun IconButton(
    icon: String,
    onClick: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val bgColor = if (isPressed) HardtekkColors.NeonPink else HardtekkColors.AnthraciteElevated
    val textColor = if (isPressed) Color.Black else HardtekkColors.TextSecondary

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                androidx.compose.foundation.gestures.detectTapGestures(
                    onPress = { isPressed = true },
                    onTap = {
                        onClick()
                        isPressed = false
                    }
                )
            }
            .background(bgColor, RoundedCornerShape(6.dp))
            .border(1.dp, HardtekkColors.GrayBorder, RoundedCornerShape(6.dp))
    ) {
        Text(
            text = icon,
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center),
            fontFamily = HardtekkType.MonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = textColor
        )
    }
}
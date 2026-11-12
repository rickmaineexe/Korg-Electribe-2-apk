package com.hardtekk.electribe.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.weight
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
import com.hardtekk.electribe.model.Pattern
import com.hardtekk.electribe.model.SequencerMode
import com.hardtekk.electribe.ui.components.KnobRow
import com.hardtekk.electribe.ui.components.KnobConfig
import com.hardtekk.electribe.ui.components.ModeSelector
import com.hardtekk.electribe.ui.components.PatternSelector
import com.hardtekk.electribe.ui.components.StepSequencerPad
import com.hardtekk.electribe.ui.theme.HardtekkColors
import com.hardtekk.electribe.ui.theme.HardtekkType

/**
 * Step Sequencer Screen - 16/32 step programming interface
 */
@Composable
fun StepSequencerScreen(
    // State
    pattern: Pattern,
    currentStep: Int,
    selectedPad: Int,
    selectedStep: Int,
    bpm: Int,
    currentPattern: Int,
    isPlaying: Boolean,
    isRecording: Boolean,
    swing: Float,
    // Callbacks
    onStepToggle: (Int, Int) -> Unit, // padIndex, stepIndex
    onStepVelocityChange: (Int, Int, Float) -> Unit,
    onPadSelect: (Int) -> Unit,
    onPatternChange: (Int) -> Unit,
    onPlayClick: () -> Unit,
    onStopClick: () -> Unit,
    onRecordClick: () -> Unit,
    onBpmIncrement: () -> Unit,
    onBpmDecrement: () -> Unit,
    onSwingChange: (Float) -> Unit,
    onModeChange: (SequencerMode) -> Unit,
    onClearPattern: () -> Unit,
    onCopyPattern: () -> Unit,
    onPastePattern: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    val stepsPerRow = 8
    val numRows = pattern.steps / stepsPerRow

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HardtekkColors.Anthracite)
            .padding(vertical = 12.dp)
    ) {
        // Top Bar
        SequencerTopBar(
            bpm = bpm,
            isPlaying = isPlaying,
            isRecording = isRecording,
            currentPattern = currentPattern,
            swing = swing,
            onBpmIncrement = onBpmIncrement,
            onBpmDecrement = onBpmDecrement,
            onPlayClick = onPlayClick,
            onStopClick = onStopClick,
            onRecordClick = onRecordClick,
            onPatternChange = onPatternChange,
            onSwingChange = onSwingChange,
            onClearPattern = onClearPattern,
            onCopyPattern = onCopyPattern,
            onPastePattern = onPastePattern,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, bottom = 8.dp)
        )

        // Mode Selector
        ModeSelector(
            currentMode = SequencerMode.STEP_SEQUENCER,
            onModeChange = onModeChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, bottom = 8.dp)
        )

        // Pad Selector (left side) + Step Grid (right side)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .weight(1f, fill = false),
            verticalAlignment = Alignment.Top
        ) {
            // Pad Selector Column
            Column(
                modifier = Modifier
                    .width(70.dp)
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pad selector label
                Text(
                    text = "PAD",
                    fontFamily = HardtekkType.SansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = HardtekkColors.TextMuted,
                    letterSpacing = 0.1.sp
                )

                // 16 pad selector buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (row in 0..1) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (col in 0..7) {
                                val padIndex = row * 8 + col
                                PadSelectorButton(
                                    padIndex = padIndex,
                                    isSelected = padIndex == selectedPad,
                                    color = HardtekkColors.PadColors[padIndex],
                                    onClick = { onPadSelect(padIndex) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Step Grid
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (row in 0 until numRows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (col in 0 until stepsPerRow) {
                                val stepIndex = row * stepsPerRow + col
                                val stepData = pattern.getStep(stepIndex)
                                val isOn = stepData?.triggeredPads?.contains(selectedPad) == true
                                val hasParamLock = stepData?.parameterLocks?.containsKey(selectedPad) == true

                                StepSequencerPad(
                                    stepIndex = stepIndex,
                                    isOn = isOn,
                                    isCurrentStep = stepIndex == currentStep && isPlaying,
                                    hasParameterLock = hasParamLock,
                                    padColor = HardtekkColors.PadColors[selectedPad],
                                    onToggle = { onStepToggle(selectedPad, stepIndex) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Step numbers
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (stepIndex in 0 until pattern.steps) {
                            val isCurrent = stepIndex == currentStep && isPlaying
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(16.dp)
                            ) {
                                Text(
                                    text = "${stepIndex + 1}",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .wrapContentSize(Alignment.Center),
                                    fontFamily = HardtekkType.MonoFontFamily,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 9.sp,
                                    color = if (isCurrent) HardtekkColors.LedGreen else HardtekkColors.TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom: Velocity/Parameter knobs for selected pad
        SequencerBottomKnobs(
            selectedPad = selectedPad,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, top = 8.dp, bottom = 12.dp)
                .height(100.dp)
        )
    }
}

@Composable
fun SequencerTopBar(
    bpm: Int,
    isPlaying: Boolean,
    isRecording: Boolean,
    currentPattern: Int,
    swing: Float,
    onBpmIncrement: () -> Unit,
    onBpmDecrement: () -> Unit,
    onPlayClick: () -> Unit,
    onStopClick: () -> Unit,
    onRecordClick: () -> Unit,
    onPatternChange: (Int) -> Unit,
    onSwingChange: (Float) -> Unit,
    onClearPattern: () -> Unit,
    onCopyPattern: () -> Unit,
    onPastePattern: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // BPM Display
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(60.dp)
                .background(HardtekkColors.AnthraciteCard, RoundedCornerShape(8.dp))
                .border(2.dp, HardtekkColors.NeonPink, RoundedCornerShape(8.dp))
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = bpm.toString(),
                    fontFamily = HardtekkType.MonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = HardtekkColors.NeonPink
                )
                Text(
                    text = "BPM",
                    fontFamily = HardtekkType.SansFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 9.sp,
                    color = HardtekkColors.TextMuted
                )
            }
        }

        // BPM +/-
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            SmallIconButton("+", onBpmIncrement)
            SmallIconButton("−", onBpmDecrement)
        }

        // Transport
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SeqTransportButton("STOP", HardtekkColors.LedRed, !isPlaying, onStopClick)
            SeqTransportButton("PLAY", HardtekkColors.LedGreen, isPlaying && !isRecording, onPlayClick)
            SeqTransportButton("REC", HardtekkColors.LedRed, isRecording, onRecordClick)
        }

        // Pattern
        PatternSelector(
            currentPattern = currentPattern,
            onPatternSelect = onPatternChange,
            modifier = Modifier.weight(1f)
        )

        // Swing knob
        Column(
            modifier = Modifier.width(80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SWING",
                fontFamily = HardtekkType.SansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                color = HardtekkColors.TextMuted
            )
            androidx.compose.material3.Slider(
                value = (swing + 1f) / 2f, // Map -1..1 to 0..1
                onValueChange = { onSwingChange((it * 2f) - 1f) },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = HardtekkColors.LedAmber,
                    activeTrackColor = HardtekkColors.LedAmber,
                    inactiveTrackColor = HardtekkColors.GrayDark
                )
            )
            Text(
                text = "%.0f%%".format(swing * 50),
                fontFamily = HardtekkType.MonoFontFamily,
                fontSize = 10.sp,
                color = HardtekkColors.LedAmber
            )
        }

        // Pattern actions
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SmallTextButton("CLR", onClearPattern)
            SmallTextButton("CPY", onCopyPattern)
            SmallTextButton("PST", onPastePattern)
        }
    }
}

@Composable
fun SmallIconButton(
    icon: String,
    onClick: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .pointerInput(Unit) {
                androidx.compose.foundation.gestures.detectTapGestures(onTap = onClick)
            }
            .background(HardtekkColors.AnthraciteElevated, RoundedCornerShape(4.dp))
            .border(1.dp, HardtekkColors.GrayBorder, RoundedCornerShape(4.dp))
    ) {
        Text(
            text = icon,
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center),
            fontFamily = HardtekkType.MonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = HardtekkColors.NeonPink
        )
    )
}

@Composable
fun SmallTextButton(
    label: String,
    onClick: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    Box(
        modifier = modifier
            .width(40.dp)
            .height(28.dp)
            .pointerInput(Unit) {
                androidx.compose.foundation.gestures.detectTapGestures(onTap = onClick)
            }
            .background(HardtekkColors.AnthraciteElevated, RoundedCornerShape(4.dp))
            .border(1.dp, HardtekkColors.GrayBorder, RoundedCornerShape(4.dp))
    ) {
        Text(
            text = label,
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center),
            fontFamily = HardtekkType.MonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 8.sp,
            color = HardtekkColors.TextSecondary
        )
    )
}

@Composable
fun SeqTransportButton(
    label: String,
    color: Color,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(50.dp, 40.dp)
            .pointerInput(Unit) {
                androidx.compose.foundation.gestures.detectTapGestures(onTap = onClick)
            }
            .background(if (isActive) color else HardtekkColors.AnthraciteElevated, RoundedCornerShape(6.dp))
            .border(if (isActive) 0.dp else 1.dp, color, RoundedCornerShape(6.dp))
    ) {
        Text(
            text = label,
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center),
            fontFamily = HardtekkType.SansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            color = if (isActive) Color.Black else HardtekkColors.TextSecondary
        )
    )
}

@Composable
fun PadSelectorButton(
    padIndex: Int,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    val backgroundColor = if (isSelected) color else HardtekkColors.AnthraciteElevated
    val textColor = if (isSelected) Color.Black else HardtekkColors.TextSecondary

    Box(
        modifier = modifier
            .height(36.dp)
            .fillMaxWidth()
            .pointerInput(Unit) {
                androidx.compose.foundation.gestures.detectTapGestures(onTap = onClick)
            }
            .background(backgroundColor, RoundedCornerShape(6.dp))
            .border(if (isSelected) 0.dp else 1.dp, color, RoundedCornerShape(6.dp))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = (padIndex + 1).toString(),
                fontFamily = HardtekkType.MonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = textColor
            )
        }
    }
}

@Composable
fun SequencerBottomKnobs(
    selectedPad: Int,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    // Placeholder for pad-specific parameter knobs
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(HardtekkColors.AnthraciteCard, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PAD ${selectedPad + 1} PARAMETERS",
                fontFamily = HardtekkType.SansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = HardtekkColors.TextMuted
            )

            // Velocity knob
            KnobConfig(
                label = "VEL",
                value = 1.0f,
                color = HardtekkColors.NeonPink
            ).let { config ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = config.label,
                        fontFamily = HardtekkType.SansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = HardtekkColors.TextMuted
                    )
                    // Simplified knob representation
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(HardtekkColors.GrayDark, RoundedCornerShape(4.dp))
                            .padding(top = 4.dp)
                    ) {
                        Text(
                            text = "100%",
                            fontFamily = HardtekkType.MonoFontFamily,
                            fontSize = 10.sp,
                            color = HardtekkColors.NeonPink,
                            modifier = Modifier
                                .fillMaxSize()
                                .wrapContentSize(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}
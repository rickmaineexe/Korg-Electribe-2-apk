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
import com.hardtekk.electribe.model.SampleData
import com.hardtekk.electribe.model.SequencerMode
import com.hardtekk.electribe.ui.components.KnobRow
import com.hardtekk.electribe.ui.components.KnobConfig
import com.hardtekk.electribe.ui.components.MiniWaveform
import com.hardtekk.electribe.ui.components.ModeSelector
import com.hardtekk.electribe.ui.components.WaveformView
import com.hardtekk.electribe.ui.theme.HardtekkColors
import com.hardtekk.electribe.ui.theme.HardtekkType

/**
 * Sample Slicer Screen - Load song, visualize waveform, slice and assign to pads
 */
@Composable
fun SampleSlicerScreen(
    // State
    loadedSample: SampleData?,
    waveformData: FloatArray,
    sliceStart: Float,
    sliceEnd: Float,
    playheadPosition: Float,
    isPlayingSlice: Boolean,
    selectedPad: Int,
    padAssignments: List<Int>, // sliceId -> padIndex
    // Callbacks
    onLoadSong: () -> Unit,
    onSliceStartChange: (Float) -> Unit,
    onSliceEndChange: (Float) -> Unit,
    onScrub: (Float) -> Unit,
    onPreviewSlice: () -> Unit,
    onAssignToPad: (Int) -> Unit,
    onPadSelect: (Int) -> Unit,
    onModeChange: (SequencerMode) -> Unit,
    onBpmIncrement: () -> Unit,
    onBpmDecrement: () -> Unit,
    onPlayClick: () -> Unit,
    onStopClick: () -> Unit,
    onRecordClick: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HardtekkColors.Anthracite)
            .padding(vertical = 12.dp)
    ) {
        // Top Bar
        SlicerTopBar(
            bpm = 140, // Fixed for slicer
            isPlaying = isPlayingSlice,
            isRecording = false,
            onBpmIncrement = onBpmIncrement,
            onBpmDecrement = onBpmDecrement,
            onPlayClick = onPlayClick,
            onStopClick = onStopClick,
            onRecordClick = onRecordClick,
            onLoadSong = onLoadSong,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, bottom = 8.dp)
        )

        // Mode Selector
        ModeSelector(
            currentMode = SequencerMode.SAMPLE_SLICER,
            onModeChange = onModeChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, bottom = 8.dp)
        )

        // Main Waveform View
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .weight(1f, fill = false)
                .height(180.dp)
        ) {
            if (loadedSample != null && waveformData.isNotEmpty()) {
                WaveformView(
                    waveformData = waveformData,
                    playheadPosition = playheadPosition,
                    sliceStart = sliceStart,
                    sliceEnd = sliceEnd,
                    onSliceStartChange = onSliceStartChange,
                    onSliceEndChange = onSliceEndChange,
                    onScrub = onScrub,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(HardtekkColors.AnthraciteCard, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "LOAD A SONG TO SLICE",
                            fontFamily = HardtekkType.SansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = HardtekkColors.TextMuted
                        )
                        Text(
                            text = "Tap LOAD SONG to select an audio file",
                            fontFamily = HardtekkType.SansFontFamily,
                            fontSize = 12.sp,
                            color = HardtekkColors.TextMuted.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        // Slice Controls
        SliceControls(
            sliceStart = sliceStart,
            sliceEnd = sliceEnd,
            onPreviewClick = onPreviewSlice,
            onAssignClick = { onAssignToPad(selectedPad) },
            selectedPad = selectedPad,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Pad Assignment Grid
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, top = 8.dp)
                .height(120.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ASSIGN TO PAD",
                        fontFamily = HardtekkType.SansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = HardtekkColors.TextMuted
                    )

                    // Pad selector
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (row in 0..1) {
                            for (col in 0..7) {
                                val padIndex = row * 8 + col
                                PadAssignButton(
                                    padIndex = padIndex,
                                    isSelected = padIndex == selectedPad,
                                    hasSlice = padAssignments.contains(padIndex),
                                    color = HardtekkColors.PadColors[padIndex],
                                    onClick = { onPadSelect(padIndex) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .weight(1f)
                                )
                            }
                        }
                    }
                }

                // Slice info
                if (loadedSample != null) {
                    SliceInfoBar(
                        sampleName = loadedSample.name,
                        sliceStart = sliceStart,
                        sliceEnd = sliceEnd,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun SlicerTopBar(
    bpm: Int,
    isPlaying: Boolean,
    isRecording: Boolean,
    onBpmIncrement: () -> Unit,
    onBpmDecrement: () -> Unit,
    onPlayClick: () -> Unit,
    onStopClick: () -> Unit,
    onRecordClick: () -> Unit,
    onLoadSong: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Load Song Button
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(60.dp)
                .pointerInput(Unit) {
                    androidx.compose.foundation.gestures.detectTapGestures(onTap = onLoadSong)
                }
                .background(HardtekkColors.LedBlue, RoundedCornerShape(8.dp))
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.load_song),
                    fontFamily = HardtekkType.SansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.Black,
                    letterSpacing = 0.1.sp
                )
            }
        }

        // Transport (for preview)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SlicerTransportButton("STOP", HardtekkColors.LedRed, !isPlaying, onStopClick)
            SlicerTransportButton("PLAY", HardtekkColors.LedGreen, isPlaying, onPlayClick)
        }

        // Spacer
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun SlicerTransportButton(
    label: String,
    color: Color,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(60.dp, 48.dp)
            .pointerInput(Unit) {
                androidx.compose.foundation.gestures.detectTapGestures(onTap = onClick)
            }
            .background(if (isActive) color else HardtekkColors.AnthraciteElevated, RoundedCornerShape(8.dp))
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
            color = if (isActive) Color.Black else HardtekkColors.TextSecondary
        )
    }
}

@Composable
fun SliceControls(
    sliceStart: Float,
    sliceEnd: Float,
    onPreviewClick: () -> Unit,
    onAssignClick: () -> Unit,
    selectedPad: Int,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(HardtekkColors.AnthraciteCard, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Slice start
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "START",
                    fontFamily = HardtekkType.SansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    color = HardtekkColors.TextMuted
                )
                Text(
                    text = "${(sliceStart * 100).toInt()}%",
                    fontFamily = HardtekkType.MonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = HardtekkColors.LedGreen
                )
            }

            // Slice end
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "END",
                    fontFamily = HardtekkType.SansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    color = HardtekkColors.TextMuted
                )
                Text(
                    text = "${(sliceEnd * 100).toInt()}%",
                    fontFamily = HardtekkType.MonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = HardtekkColors.LedRed
                )
            }

            // Duration
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "LEN",
                    fontFamily = HardtekkType.SansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    color = HardtekkColors.TextMuted
                )
                Text(
                    text = "${((sliceEnd - sliceStart) * 100).toInt()}%",
                    fontFamily = HardtekkType.MonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = HardtekkColors.LedAmber
                )
            )

            // Preview button
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(48.dp)
                    .pointerInput(Unit) {
                        androidx.compose.foundation.gestures.detectTapGestures(onTap = onPreviewClick)
                    }
                    .background(HardtekkColors.LedAmber, RoundedCornerShape(8.dp))
            ) {
                Text(
                    text = stringResource(R.string.preview_slice),
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center),
                    fontFamily = HardtekkType.SansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Color.Black
                )
            }

            // Assign button
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(48.dp)
                    .pointerInput(Unit) {
                        androidx.compose.foundation.gestures.detectTapGestures(onTap = onAssignClick)
                    }
                    .background(HardtekkColors.NeonPink, RoundedCornerShape(8.dp))
            ) {
                Text(
                    text = "${stringResource(R.string.assign_to_pad)} ${selectedPad + 1}",
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center),
                    fontFamily = HardtekkType.SansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
fun PadAssignButton(
    padIndex: Int,
    isSelected: Boolean,
    hasSlice: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    val backgroundColor = when {
        isSelected -> color
        hasSlice -> color.copy(alpha = 0.4f)
        else -> HardtekkColors.AnthraciteElevated
    }

    val borderColor = when {
        isSelected -> color
        hasSlice -> color.copy(alpha = 0.6f)
        else -> HardtekkColors.GrayBorder
    }

    val textColor = when {
        isSelected -> Color.Black
        hasSlice -> color
        else -> HardtekkColors.TextSecondary
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                androidx.compose.foundation.gestures.detectTapGestures(onTap = onClick)
            }
            .background(backgroundColor, RoundedCornerShape(6.dp))
            .border(if (isSelected || hasSlice) 2.dp else 1.dp, borderColor, RoundedCornerShape(6.dp))
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
                fontSize = 12.sp,
                color = textColor
            )
            if (hasSlice && !isSelected) {
                Text(
                    text = "●",
                    fontFamily = HardtekkType.MonoFontFamily,
                    fontSize = 8.sp,
                    color = color
                )
            }
        }
    }
}

@Composable
fun SliceInfoBar(
    sampleName: String,
    sliceStart: Float,
    sliceEnd: Float,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(HardtekkColors.AnthraciteCard, RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sampleName,
                fontFamily = HardtekkType.SansFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = HardtekkColors.TextPrimary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.overflow.TextOverflow.Ellipsis
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Slice: ${(sliceStart * 100).toInt()}% - ${(sliceEnd * 100).toInt()}%",
                    fontFamily = HardtekkType.MonoFontFamily,
                    fontSize = 11.sp,
                    color = HardtekkColors.TextSecondary
                )
                Text(
                    text = "Len: ${((sliceEnd - sliceStart) * 100).toInt()}%",
                    fontFamily = HardtekkType.MonoFontFamily,
                    fontSize = 11.sp,
                    color = HardtekkColors.LedAmber
                )
            }
        }
    }
}
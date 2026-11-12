package com.hardtekk.electribe.ui.components

import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawLine
import androidx.compose.ui.graphics.drawscope.drawPath
import androidx.compose.ui.graphics.drawscope.drawRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hardtekk.electribe.R
import com.hardtekk.electribe.ui.theme.HardtekkColors
import com.hardtekk.electribe.ui.theme.HardtekkType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Waveform View Component - Displays audio waveform with playhead and slice markers
 */
@Composable
fun WaveformView(
    waveformData: FloatArray, // Normalized 0.0 - 1.0 amplitude values
    playheadPosition: Float = 0.0f, // 0.0 - 1.0
    sliceStart: Float = 0.0f, // 0.0 - 1.0
    sliceEnd: Float = 1.0f, // 0.0 - 1.0
    onSliceStartChange: (Float) -> Unit,
    onSliceEndChange: (Float) -> Unit,
    onScrub: (Float) -> Unit,
    modifier: Modifier = Modifier,
    showGrid: Boolean = true
) {
    val waveformHeight = 120.dp
    val startDragOffset by remember { mutableStateOf(0f) }
    val endDragOffset by remember { mutableStateOf(0f) }
    val isDraggingStart by remember { mutableStateOf(false) }
    val isDraggingEnd by remember { mutableStateOf(false) }

    val animatedPlayhead by animateFloatAsState(
        targetValue = playheadPosition,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessVeryHigh)
    )

    val animatedSliceStart by animateFloatAsState(
        targetValue = sliceStart,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessHigh)
    )

    val animatedSliceEnd by animateFloatAsState(
        targetValue = sliceEnd,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessHigh)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(waveformHeight)
            .background(HardtekkColors.WaveformBg, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { /* start */ },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        // Handle horizontal scrubbing
                        val width = change.size.width
                        val deltaX = dragAmount.x / width
                        val newPos = (playheadPosition + deltaX).coerceIn(0.0f, 1.0f)
                        onScrub(newPos)
                    },
                    onDragEnd = { /* end */ }
                )
            }
    ) {
        // Grid lines
        if (showGrid) {
            WaveformGrid(modifier = Modifier.fillMaxSize())
        }

        // Selection highlight
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val left = animatedSliceStart * size.width
                    val right = animatedSliceEnd * size.width
                    drawRect(
                        color = HardtekkColors.WaveformSelection,
                        topLeft = Offset(left, 0f),
                        size = androidx.compose.ui.geometry.Size(right - left, size.height)
                    )
                }
        )

        // Waveform drawing
        WaveformPath(
            waveformData = waveformData,
            modifier = Modifier.fillMaxSize()
        )

        // Slice start marker
        SliceMarker(
            position = animatedSliceStart,
            color = HardtekkColors.WaveformMarkerStart,
            label = stringResource(R.string.slice_start),
            isDragging = isDraggingStart,
            onDrag = { delta ->
                startDragOffset += delta
                val newPos = (sliceStart + startDragOffset).coerceIn(0.0f, sliceEnd - 0.01f)
                onSliceStartChange(newPos)
            },
            onDragStart = { isDraggingStart = true; startDragOffset = 0f },
            onDragEnd = { isDraggingStart = false }
        )

        // Slice end marker
        SliceMarker(
            position = animatedSliceEnd,
            color = HardtekkColors.WaveformMarkerEnd,
            label = stringResource(R.string.slice_end),
            isDragging = isDraggingEnd,
            onDrag = { delta ->
                endDragOffset += delta
                val newPos = (sliceEnd + endDragOffset).coerceIn(sliceStart + 0.01f, 1.0f)
                onSliceEndChange(newPos)
            },
            onDragStart = { isDraggingEnd = true; endDragOffset = 0f },
            onDragEnd = { isDraggingEnd = false }
        )

        // Playhead
        PlayheadMarker(
            position = animatedPlayhead,
            color = HardtekkColors.WaveformPlayhead,
            modifier = Modifier.fillMaxSize()
        )

        // Info overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            androidx.compose.material3.Text(
                text = "Slice: ${(sliceStart * 100).toInt()}% - ${(sliceEnd * 100).toInt()}%",
                fontFamily = HardtekkType.MonoFontFamily,
                fontSize = 10.sp,
                color = HardtekkColors.TextSecondary
            )
            androidx.compose.material3.Text(
                text = "Duration: ${((sliceEnd - sliceStart) * 100).toInt()}%",
                fontFamily = HardtekkType.MonoFontFamily,
                fontSize = 10.sp,
                color = HardtekkColors.TextMuted
            )
        }
    }
}

@Composable
private fun WaveformGrid(modifier: Modifier) {
    Box(modifier = modifier.drawBehind {
        val width = size.width
        val height = size.height
        val gridColor = HardtekkColors.GrayDark.copy(alpha = 0.3f)

        // Vertical grid lines (beats)
        for (i in 0..16) {
            val x = (i / 16f) * width
            drawLine(
                color = if (i % 4 == 0) gridColor.copy(alpha = 0.5f) else gridColor,
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = if (i % 4 == 0) 1.dp.toPx() else 0.5.dp.toPx()
            )
        }

        // Horizontal center line
        drawLine(
            color = gridColor.copy(alpha = 0.5f),
            start = Offset(0f, height / 2f),
            end = Offset(width, height / 2f),
            strokeWidth = 0.5.dp.toPx()
        )
    })
}

@Composable
private fun WaveformPath(
    waveformData: FloatArray,
    modifier: Modifier
) {
    Box(modifier = modifier.drawBehind {
        if (waveformData.isEmpty()) return@drawBehind

        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val scaleY = (height * 0.4f) // 40% of height for amplitude

        val path = Path().apply {
            moveTo(0f, centerY)
            val step = width / waveformData.size
            for (i in waveformData.indices) {
                val x = i * step
                val amplitude = waveformData[i]
                val y = centerY - amplitude * scaleY
                lineTo(x, y)
            }
            // Mirror for negative side
            for (i in waveformData.indices.reversed()) {
                val x = i * step
                val amplitude = waveformData[i]
                val y = centerY + amplitude * scaleY
                lineTo(x, y)
            }
            close()
        }

        // Fill
        drawPath(
            path = path,
            color = HardtekkColors.WaveformWave.copy(alpha = 0.3f),
            style = androidx.compose.ui.graphics.Stroke(width = 0f)
        )

        // Outline
        val outlinePath = Path().apply {
            moveTo(0f, centerY)
            val step = width / waveformData.size
            for (i in waveformData.indices) {
                val x = i * step
                val amplitude = waveformData[i]
                val y = centerY - amplitude * scaleY
                lineTo(x, y)
            }
        }
        drawPath(
            path = outlinePath,
            color = HardtekkColors.WaveformWave,
            style = androidx.compose.ui.graphics.Stroke(width = 1.5.dp.toPx())
        )

        // Negative outline
        val negOutlinePath = Path().apply {
            moveTo(width, centerY)
            val step = width / waveformData.size
            for (i in waveformData.indices.reversed()) {
                val x = i * step
                val amplitude = waveformData[i]
                val y = centerY + amplitude * scaleY
                lineTo(x, y)
            }
        }
        drawPath(
            path = negOutlinePath,
            color = HardtekkColors.WaveformWave,
            style = androidx.compose.ui.graphics.Stroke(width = 1.5.dp.toPx())
        )
    })
}

@Composable
private fun SliceMarker(
    position: Float,
    color: Color,
    label: String,
    isDragging: Boolean,
    onDrag: (Float) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = onDragStart,
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val deltaX = dragAmount.x / change.size.width
                        onDrag(deltaX)
                    },
                    onDragEnd = onDragEnd
                )
            }
    ) {
        val x = position * size.width

        // Marker line
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .offset(x = x - 1.dp.toPx())
                .background(color)
        )

        // Marker handle (top)
        Box(
            modifier = Modifier
                .size(16.dp, 24.dp)
                .offset(x = x - 8.dp.toPx(), y = -4.dp.toPx())
                .background(color, RoundedCornerShape(4.dp))
                .drawBehind {
                    // Triangle indicator
                    val path = Path().apply {
                        moveTo(size.width / 2f, 0f)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(path, color = Color.Black.copy(alpha = 0.3f))
                }
        )

        // Label
        if (isDragging) {
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .offset(x = x - 20.dp.toPx(), y = -36.dp.toPx())
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .background(HardtekkColors.AnthraciteCard, RoundedCornerShape(4.dp))
                    .border(1.dp, color, RoundedCornerShape(4.dp))
            ) {
                androidx.compose.material3.Text(
                    text = label,
                    fontFamily = HardtekkType.MonoFontFamily,
                    fontSize = 9.sp,
                    color = color
                )
            }
        }
    }
}

@Composable
private fun PlayheadMarker(
    position: Float,
    color: Color,
    modifier: Modifier
) {
    Box(modifier = modifier.drawBehind {
        val x = position * size.width
        val height = size.height

        // Playhead line
        drawLine(
            color = color,
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 2.dp.toPx()
        )

        // Playhead triangle (top)
        val triangleSize = 12f
        val path = Path().apply {
            moveTo(x, 0f)
            lineTo(x - triangleSize / 2, triangleSize)
            lineTo(x + triangleSize / 2, triangleSize)
            close()
        }
        drawPath(path, color = color)

        // Playhead triangle (bottom)
        val bottomPath = Path().apply {
            moveTo(x, height)
            lineTo(x - triangleSize / 2, height - triangleSize)
            lineTo(x + triangleSize / 2, height - triangleSize)
            close()
        }
        drawPath(bottomPath, color = color)
    })
}

/**
 * Simplified waveform for pad assignment preview
 */
@Composable
fun MiniWaveform(
    waveformData: FloatArray,
    modifier: Modifier = Modifier,
    color: Color = HardtekkColors.NeonPink
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .fillMaxWidth()
            .background(HardtekkColors.Anthracite, RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
            .drawBehind {
                if (waveformData.isEmpty()) return@drawBehind

                val width = size.width
                val height = size.height
                val centerY = height / 2f
                val scaleY = height * 0.4f

                val path = Path().apply {
                    moveTo(0f, centerY)
                    val step = width / waveformData.size
                    for (i in waveformData.indices) {
                        val x = i * step
                        val y = centerY - waveformData[i] * scaleY
                        lineTo(x, y)
                    }
                    for (i in waveformData.indices.reversed()) {
                        val x = i * step
                        val y = centerY + waveformData[i] * scaleY
                        lineTo(x, y)
                    }
                    close()
                }

                drawPath(
                    path = path,
                    color = color.copy(alpha = 0.3f)
                )

                val outlinePath = Path().apply {
                    moveTo(0f, centerY)
                    val step = width / waveformData.size
                    for (i in waveformData.indices) {
                        val x = i * step
                        val y = centerY - waveformData[i] * scaleY
                        lineTo(x, y)
                    }
                }
                drawPath(
                    path = outlinePath,
                    color = color,
                    style = androidx.compose.ui.graphics.Stroke(width = 1.dp.toPx())
                )
            }
    )
}
package com.hardtekk.electribe.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradient
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hardtekk.electribe.R
import com.hardtekk.electribe.ui.theme.HardtekkColors
import com.hardtekk.electribe.ui.theme.HardtekkType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Transport Control Buttons - Play, Stop, Record
 * Hardware-style buttons with LED indicators
 */
@Composable
fun TransportControls(
    isPlaying: Boolean,
    isRecording: Boolean,
    onPlayClick: () -> Unit,
    onStopClick: () -> Unit,
    onRecordClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playColor = HardtekkColors.LedGreen
    val stopColor = HardtekkColors.LedRed
    val recordColor = HardtekkColors.LedRed

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // STOP Button
            TransportButton(
                label = stringResource(R.string.stop),
                color = stopColor,
                isActive = !isPlaying,
                isPressed = false,
                onClick = onStopClick,
                modifier = Modifier.weight(1f)
            )

            // PLAY Button
            TransportButton(
                label = stringResource(R.string.play),
                color = playColor,
                isActive = isPlaying && !isRecording,
                isPressed = false,
                onClick = onPlayClick,
                modifier = Modifier.weight(1f)
            )

            // REC Button
            TransportButton(
                label = stringResource(R.string.rec),
                color = recordColor,
                isActive = isRecording,
                isPressed = false,
                onClick = onRecordClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TransportButton(
    label: String,
    color: Color,
    isActive: Boolean,
    isPressed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isPressed -> color.copy(alpha = 0.9f)
            isActive -> color
            else -> HardtekkColors.AnthraciteElevated
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh)
    )

    val textColor by animateColorAsState(
        targetValue = if (isActive) Color.Black else HardtekkColors.TextSecondary
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isActive) 1.0f else 0.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium)
    )

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh)
    )

    Box(
        modifier = modifier
            .height(48.dp)
            .fillMaxWidth()
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .pointerInput(Unit) {
                detectTapGestures(onTap = onClick)
            }
            .clip(RoundedCornerShape(8.dp))
    ) {
        // Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .clip(RoundedCornerShape(8.dp))
        )

        // Glow effect
        if (glowAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val glowColor = color.copy(alpha = glowAlpha * 0.5f)
                        drawRect(
                            brush = androidx.compose.ui.graphics.ShaderBrush(
                                androidx.compose.ui.graphics.LinearGradient(
                                    colors = listOf(glowColor, Color.Transparent, glowColor),
                                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                    end = androidx.compose.ui.geometry.Offset(size.width, size.height)
                                )
                            ),
                            style = androidx.compose.ui.graphics.Stroke(width = 3.dp.toPx()),
                            size = size
                        )
                    }
                    .clip(RoundedCornerShape(8.dp))
            )
        }

        // Button content
        androidx.compose.material3.Text(
            text = label,
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center),
            fontFamily = HardtekkType.SansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = textColor,
            letterSpacing = 0.1.sp
        )
    }
}

/**
 * BPM Display with Tap Tempo
 */
@Composable
fun BPMDisplay(
    bpm: Int,
    onBpmChange: (Int) -> Unit,
    onTapTempo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tapCount by remember { mutableStateOf(0) }
    val lastTapTime by remember { mutableStateOf(0L) }

    Box(
        modifier = modifier
            .width(120.dp)
            .height(60.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onTapTempo()
                        val now = System.currentTimeMillis()
                        if (now - lastTapTime < 2000) {
                            // Could calculate BPM from taps here
                        }
                        lastTapTime = now
                    }
                )
            }
    ) {
        // BPM Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(HardtekkColors.AnthraciteCard, RoundedCornerShape(8.dp))
                .border(2.dp, HardtekkColors.NeonPink, RoundedCornerShape(8.dp))
        )

        // BPM Value
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            androidx.compose.material3.Text(
                text = bpm.toString(),
                fontFamily = HardtekkType.MonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                color = HardtekkColors.NeonPink,
                letterSpacing = 0.2.sp
            )

            androidx.compose.material3.Text(
                text = stringResource(R.string.bpm_label),
                fontFamily = HardtekkType.SansFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                color = HardtekkColors.TextMuted,
                letterSpacing = 0.15.sp
            )
        }
    }
}

/**
 * BPM Control with +/- buttons
 */
@Composable
fun BPMControl(
    bpm: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Decrement button
        IconButton(
            icon = "−",
            onClick = onDecrement,
            modifier = Modifier.size(48.dp)
        )

        // BPM Display
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(48.dp)
                .background(HardtekkColors.AnthraciteCard, RoundedCornerShape(6.dp))
                .border(1.dp, HardtekkColors.GrayBorder, RoundedCornerShape(6.dp))
        ) {
            androidx.compose.material3.Text(
                text = bpm.toString(),
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center),
                fontFamily = HardtekkType.MonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = HardtekkColors.NeonPink
            )
        }

        // Increment button
        IconButton(
            icon = "+",
            onClick = onIncrement,
            modifier = Modifier.size(48.dp)
        )
    }
}

@Composable
fun IconButton(
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = HardtekkColors.NeonPink,
    backgroundColor: Color = HardtekkColors.AnthraciteElevated
) {
    var isPressed by remember { mutableStateOf(false) }

    val bgColor by animateColorAsState(
        targetValue = if (isPressed) color else backgroundColor,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh)
    )

    val textColor by animateColorAsState(
        targetValue = if (isPressed) Color.Black else color,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh)
    )

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { isPressed = true },
                    onTap = {
                        onClick()
                        isPressed = false
                    }
                )
            }
            .background(bgColor, RoundedCornerShape(6.dp))
            .border(if (isPressed) 0.dp else 1.dp, color, RoundedCornerShape(6.dp))
    ) {
        androidx.compose.material3.Text(
            text = icon,
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center),
            fontFamily = HardtekkType.MonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = textColor
        )
    }
}
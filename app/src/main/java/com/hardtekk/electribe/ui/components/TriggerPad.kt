package com.hardtekk.electribe.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.pointerInput
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradient
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawRect
import androidx.compose.ui.graphics.drawscope.drawRoundRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
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
 * Trigger Pad Component - Electribe-style rubber pad with LED glow
 * Supports tap, pressure sensitivity visualization, and active states
 */
@Composable
fun TriggerPad(
    padIndex: Int, // 0-15
    label: String = "",
    isActive: Boolean = false,
    isPressed: Boolean = false,
    velocity: Float = 1.0f,
    onTrigger: (Float) -> Unit,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    showVelocity: Boolean = false
) {
    val padColor = HardtekkColors.PadColors[padIndex % 16]
    val activeColor = HardtekkColors.PadActiveColors[padIndex % 16]

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isPressed -> activeColor.copy(alpha = 0.9f)
            isActive -> activeColor
            else -> padColor
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh)
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isActive || isPressed) 1.0f else 0.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium)
    )

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh)
    )

    val velocityScale by animateFloatAsState(
        targetValue = 1.0f + (velocity - 1.0f) * 0.1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessVeryHigh)
    )

    val coroutineScope = rememberCoroutineScope()
    var isPressedState by remember { mutableStateOf(false) }
    var currentVelocity by remember { mutableStateOf(1.0f) }

    Box(
        modifier = modifier
            .size(56.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressedState = true
                        currentVelocity = 1.0f + (Math.random() * 0.3).toFloat()
                        onTrigger(currentVelocity)
                        try awaitPointerEventScope {
                            awaitPointerEvent()
                        } finally {
                            isPressedState = false
                        }
                    },
                    onLongPress = { onLongPress?.invoke() }
                )
            }
            .clip(RoundedCornerShape(8.dp))
    ) {
        // Background with gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .clip(RoundedCornerShape(8.dp))
        ) {
            // Gradient overlay for 3D effect
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val brush = ShaderBrush(
                            LinearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.15f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.2f)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(size.width, size.height)
                            )
                        )
                        drawRect(brush, size = size)
                    }
                    .clip(RoundedCornerShape(8.dp))
            )
        }

        // Glow effect when active
        if (glowAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val glowColor = activeColor.copy(alpha = glowAlpha * 0.6f)
                        drawRoundRect(
                            color = glowColor,
                            cornerRadius = 8.dp.toPx(),
                            style = Stroke(width = 4.dp.toPx())
                        )
                        // Inner glow
                        drawRoundRect(
                            color = glowColor.copy(alpha = glowAlpha * 0.3f),
                            cornerRadius = 6.dp.toPx(),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                    .clip(RoundedCornerShape(8.dp))
            )
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = pressScale * velocityScale
                    scaleY = pressScale * velocityScale
                },
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pad number
            Text(
                text = (padIndex + 1).toString(),
                fontFamily = HardtekkType.MonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = if (backgroundColor.luminance() > 0.5) Color.Black else Color.White
            )

            // Optional label
            if (label.isNotBlank()) {
                Text(
                    text = label,
                    fontFamily = HardtekkType.SansFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 8.sp,
                    color = if (backgroundColor.luminance() > 0.5) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.overflow.TextOverflow.Ellipsis
                )
            }

            // Velocity indicator
            if (showVelocity && velocity != 1.0f) {
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    repeat(4) { i ->
                        val active = i < (velocity * 4).toInt()
                        Box(
                            modifier = Modifier
                                .size(3.dp, 2.dp)
                                .padding(horizontal = 1.dp)
                                .background(if (active) activeColor else Color.White.copy(alpha = 0.3f))
                                .clip(RoundedCornerShape(1.dp))
                        )
                    }
                }
            }
        }
    }
}

/**
 * Grid of 16 trigger pads (2 rows of 8)
 */
@Composable
fun TriggerPadGrid(
    selectedPad: Int,
    activePads: List<Int>,
    padLabels: List<String> = List(16) { "" },
    onPadTrigger: (Int, Float) -> Unit,
    onPadLongPress: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
    showVelocity: Boolean = false
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: Pads 1-8
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            for (i in 0..7) {
                TriggerPad(
                    padIndex = i,
                    label = padLabels.getOrElse(i) { "" },
                    isActive = i in activePads,
                    isPressed = i == selectedPad && i in activePads,
                    onTrigger = { vel -> onPadTrigger(i, vel) },
                    onLongPress = { onPadLongPress?.invoke(i) },
                    modifier = Modifier.weight(1f),
                    showVelocity = showVelocity
                )
            }
        }

        // Row 2: Pads 9-16
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            for (i in 8..15) {
                TriggerPad(
                    padIndex = i,
                    label = padLabels.getOrElse(i) { "" },
                    isActive = i in activePads,
                    isPressed = i == selectedPad && i in activePads,
                    onTrigger = { vel -> onPadTrigger(i, vel) },
                    onLongPress = { onPadLongPress?.invoke(i) },
                    modifier = Modifier.weight(1f),
                    showVelocity = showVelocity
                )
            }
        }
    }
}

/**
 * Step Sequencer Pad - used in step sequencer mode
 * Shows step state (on/off) and current play position
 */
@Composable
fun StepSequencerPad(
    stepIndex: Int, // 0-15 or 0-31
    isOn: Boolean,
    isCurrentStep: Boolean = false,
    hasParameterLock: Boolean = false,
    padColor: Color = HardtekkColors.NeonPink,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color by animateColorAsState(
        targetValue = when {
            isCurrentStep && isOn -> HardtekkColors.LedGreen
            isCurrentStep -> HardtekkColors.LedGreen.copy(alpha = 0.5f)
            isOn -> padColor
            else -> HardtekkColors.SeqLedDim
        }
    )

    val borderColor by animateColorAsState(
        targetValue = if (hasParameterLock) HardtekkColors.LedAmber else Color.Transparent
    )

    Box(
        modifier = modifier
            .size(40.dp)
            .background(color, HardtekkShapes.PadShape)
            .border(if (hasParameterLock) 2.dp else 0.dp, borderColor, HardtekkShapes.PadShape)
            .pointerInput(Unit) {
                androidx.compose.foundation.gestures.detectTapGestures(onTap = onToggle)
            }
    ) {
        // Step number
        androidx.compose.material3.Text(
            text = (stepIndex + 1).toString(),
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 2.dp),
            fontFamily = HardtekkType.MonoFontFamily,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            fontSize = 9.sp,
            color = if (isOn || isCurrentStep) Color.Black else Color.White.copy(alpha = 0.5f),
            textAlign = androidx.compose.ui.text.TextAlign.Center
        )
    }
}
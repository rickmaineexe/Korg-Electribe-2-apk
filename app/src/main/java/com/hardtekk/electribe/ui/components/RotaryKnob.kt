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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradient
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawArc
import androidx.compose.ui.graphics.drawscope.drawCircle
import androidx.compose.ui.graphics.drawscope.drawLine
import androidx.compose.ui.graphics.drawscope.drawRect
import androidx.compose.ui.graphics.drawscope.drawRoundRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hardtekk.electribe.ui.theme.HardtekkColors
import com.hardtekk.electribe.ui.theme.HardtekkType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Rotary Knob Component - Hardware-style potentiometer
 * Supports drag rotation, value display, and parameter labeling
 */
@Composable
fun RotaryKnob(
    label: String,
    value: Float, // 0.0 - 1.0
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    minValue: Float = 0.0f,
    maxValue: Float = 1.0f,
    showValue: Boolean = true,
    valueFormatter: (Float) -> String = { it.toString() },
    knobColor: androidx.compose.ui.graphics.Color = HardtekkColors.NeonPink,
    trackColor: androidx.compose.ui.graphics.Color = HardtekkColors.KnobTrack,
    indicatorColor: androidx.compose.ui.graphics.Color = HardtekkColors.KnobIndicator,
    disabled: Boolean = false
) {
    val angleRange = 270f // Degrees (from -135 to +135)
    val startAngle = -135f
    val currentAngle = startAngle + value * angleRange

    val displayAngle by animateFloatAsState(
        targetValue = currentAngle,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium)
    )

    val isDragging by remember { mutableStateOf(false) }
    val displayValue = remember { mutableStateOf(value) }
    displayValue.value = value

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Knob
        Box(
            modifier = Modifier
                .size(64.dp)
                .pointerInput(Unit) {
                    if (!disabled) {
                        detectDragGestures(
                            onDragStart = { /* drag started */ },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                // Calculate angle change from vertical drag
                                val sensitivity = 0.004f // Adjust for feel
                                val delta = -dragAmount.y * sensitivity
                                val newValue = (displayValue.value + delta).coerceIn(minValue, maxValue)
                                displayValue.value = newValue
                                onValueChange(newValue)
                            },
                            onDragEnd = { /* drag ended */ }
                        )
                    }
                }
                .graphicsLayer {
                    rotationZ = displayAngle
                }
        ) {
            // Knob body with gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = min(size.width, size.height) / 2f

                        // Outer ring
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.8f),
                            radius = radius
                        )

                        // Gradient body
                        val brush = ShaderBrush(
                            LinearGradient(
                                colors = listOf(
                                    knobColor.copy(alpha = 0.9f),
                                    knobColor.copy(alpha = 0.6f),
                                    Color.Black.copy(alpha = 0.8f)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(size.width, size.height)
                            )
                        )
                        drawCircle(brush, radius = radius * 0.9f)

                        // Inner highlight
                        drawCircle(
                            color = Color.White.copy(alpha = 0.1f),
                            radius = radius * 0.7f
                        )

                        // Knob indicator line
                        val indicatorAngle = (displayAngle - 90) * PI / 180
                        val indicatorLength = radius * 0.65f
                        val indicatorWidth = 3f

                        drawLine(
                            color = indicatorColor,
                            start = center,
                            end = Offset(
                                center.x + cos(indicatorAngle) * indicatorLength,
                                center.y + sin(indicatorAngle) * indicatorLength
                            ),
                            strokeWidth = indicatorWidth,
                            cap = androidx.compose.ui.graphics.Cap.Round
                        )

                        // Center dot
                        drawCircle(
                            color = Color.White.copy(alpha = 0.5f),
                            radius = 4f,
                            center = center
                        )
                    }
                    .clip(CircleShape)
            )
        }

        // Label
        androidx.compose.material3.Text(
            text = label,
            fontFamily = HardtekkType.SansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = HardtekkColors.TextSecondary,
            letterSpacing = 0.1.sp,
            modifier = Modifier.padding(top = 6.dp)
        )

        // Value display
        if (showValue) {
            androidx.compose.material3.Text(
                text = valueFormatter(displayValue.value),
                fontFamily = HardtekkType.MonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = HardtekkColors.TextPrimary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/**
 * Horizontal Knob Row for the effects section
 */
@Composable
fun KnobRow(
    knobs: List<KnobConfig>,
    onKnobChange: (Int, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        knobs.forEachIndexed { index, config ->
            RotaryKnob(
                label = config.label,
                value = config.value,
                onValueChange = { newValue -> onKnobChange(index, newValue) },
                modifier = Modifier.weight(1f),
                minValue = config.minValue,
                maxValue = config.maxValue,
                showValue = config.showValue,
                valueFormatter = config.formatter,
                knobColor = config.color,
                disabled = config.disabled
            )
        }
    }
}

data class KnobConfig(
    val label: String,
    val value: Float,
    val minValue: Float = 0.0f,
    val maxValue: Float = 1.0f,
    val showValue: Boolean = true,
    val formatter: (Float) -> String = { "%.0f%%".format(it * 100) },
    val color: androidx.compose.ui.graphics.Color = HardtekkColors.NeonPink,
    val disabled: Boolean = false
)
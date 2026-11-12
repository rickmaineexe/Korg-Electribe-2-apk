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

/**
 * Pattern Selector - 4 pattern buttons with active indication
 */
@Composable
fun PatternSelector(
    currentPattern: Int,
    onPatternSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val patternLabels = listOf("1", "2", "3", "4")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pattern label
            androidx.compose.material3.Text(
                text = stringResource(R.string.pattern),
                fontFamily = HardtekkType.SansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = HardtekkColors.TextMuted,
                letterSpacing = 0.1.sp,
                modifier = Modifier
                    .width(48.dp)
                    .wrapContentSize(Alignment.Center)
            )

            // Pattern buttons
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
            ) {
                patternLabels.forEachIndexed { index, label ->
                    PatternButton(
                        label = label,
                        isSelected = index == currentPattern,
                        onClick = { onPatternSelect(index) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Copy/Paste buttons
            Row(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    icon = "CPY",
                    onClick = { /* Copy pattern */ },
                    modifier = Modifier.size(40.dp, 36.dp),
                    fontSize = 9.sp
                )
                IconButton(
                    icon = "PST",
                    onClick = { /* Paste pattern */ },
                    modifier = Modifier.size(40.dp, 36.dp),
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
fun PatternButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) HardtekkColors.NeonPink else HardtekkColors.AnthraciteElevated,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh)
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.Black else HardtekkColors.TextSecondary,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh)
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) HardtekkColors.NeonPinkBright else HardtekkColors.GrayBorder,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh)
    )

    val pressScale by animateFloatAsState(
        targetValue = 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh)
    )

    Box(
        modifier = modifier
            .height(36.dp)
            .fillMaxWidth()
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .pointerInput(Unit) {
                detectTapGestures(onTap = onClick)
            }
            .background(backgroundColor, RoundedCornerShape(6.dp))
            .border(2.dp, borderColor, RoundedCornerShape(6.dp))
    ) {
        androidx.compose.material3.Text(
            text = label,
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center),
            fontFamily = HardtekkType.MonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = textColor
        )
    }
}

@Composable
fun IconButton(
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: Int = 11
) {
    var isPressed by remember { mutableStateOf(false) }

    val bgColor by animateColorAsState(
        targetValue = if (isPressed) HardtekkColors.NeonPink else HardtekkColors.AnthraciteElevated,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh)
    )

    val textColor by animateColorAsState(
        targetValue = if (isPressed) Color.Black else HardtekkColors.TextSecondary,
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
            .background(bgColor, RoundedCornerShape(4.dp))
            .border(1.dp, HardtekkColors.GrayBorder, RoundedCornerShape(4.dp))
    ) {
        androidx.compose.material3.Text(
            text = icon,
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center),
            fontFamily = HardtekkType.MonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize.sp,
            color = textColor
        )
    }
}
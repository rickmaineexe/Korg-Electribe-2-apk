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
import com.hardtekk.electribe.model.SequencerMode
import com.hardtekk.electribe.ui.theme.HardtekkColors
import com.hardtekk.electribe.ui.theme.HardtekkType

/**
 * Mode Selector - Switch between Trigger, Step Sequencer, and Sample Slicer modes
 */
@Composable
fun ModeSelector(
    currentMode: SequencerMode,
    onModeChange: (SequencerMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf(
        ModeItem(SequencerMode.TRIGGER, stringResource(R.string.mode_trigger), "TRG"),
        ModeItem(SequencerMode.STEP_SEQUENCER, stringResource(R.string.mode_sequencer), "SEQ"),
        ModeItem(SequencerMode.SAMPLE_SLICER, stringResource(R.string.mode_slicer), "SLC")
    )

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
            modes.forEach { item ->
                ModeButton(
                    mode = item.mode,
                    label = item.shortLabel,
                    isSelected = item.mode == currentMode,
                    onClick = { onModeChange(item.mode) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

data class ModeItem(
    val mode: SequencerMode,
    val fullLabel: String,
    val shortLabel: String
)

@Composable
fun ModeButton(
    mode: SequencerMode,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val modeColor = when (mode) {
        SequencerMode.TRIGGER -> HardtekkColors.NeonPink
        SequencerMode.STEP_SEQUENCER -> HardtekkColors.LedGreen
        SequencerMode.SAMPLE_SLICER -> HardtekkColors.LedBlue
    }

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) modeColor else HardtekkColors.AnthraciteElevated,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh)
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.Black else HardtekkColors.TextSecondary,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh)
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) modeColor.copy(alpha = 0.8f) else HardtekkColors.GrayBorder,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh)
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1.0f else 0.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium)
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
        // Glow effect
        if (glowAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val glowColor = modeColor.copy(alpha = glowAlpha * 0.4f)
                        drawRect(
                            brush = androidx.compose.ui.graphics.ShaderBrush(
                                androidx.compose.ui.graphics.LinearGradient(
                                    colors = listOf(glowColor, Color.Transparent, glowColor),
                                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                    end = androidx.compose.ui.geometry.Offset(size.width, size.height)
                                )
                            ),
                            style = androidx.compose.ui.graphics.Stroke(width = 2.dp.toPx()),
                            size = size
                        )
                    }
                    .clip(RoundedCornerShape(6.dp))
            )
        }

        androidx.compose.material3.Text(
            text = label,
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center),
            fontFamily = HardtekkType.SansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = textColor,
            letterSpacing = 0.05.sp
        )
    }
}
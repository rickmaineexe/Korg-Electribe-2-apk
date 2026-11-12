package com.hardtekk.electribe.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography for Hardtekk Electribe
 * Monospace fonts for readouts, clean sans for UI
 */
object HardtekkType {
    // Font families
    val MonoFontFamily = FontFamily.Default // Will use system monospace
    val SansFontFamily = FontFamily.Default

    // Text styles
    val DisplayLarge = TextStyle(
        fontFamily = SansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = 0.1.sp
    )

    val DisplayMedium = TextStyle(
        fontFamily = SansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = 0.05.sp
    )

    val DisplaySmall = TextStyle(
        fontFamily = SansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp
    )

    // BPM Display - large monospace
    val BpmDisplay = TextStyle(
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        letterSpacing = 0.2.sp
    )

    val BpmLabel = TextStyle(
        fontFamily = SansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.15.sp
    )

    // Pad labels
    val PadLabel = TextStyle(
        fontFamily = SansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 0.05.sp
    )

    val PadLabelSmall = TextStyle(
        fontFamily = SansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp
    )

    // Knob labels
    val KnobLabel = TextStyle(
        fontFamily = SansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 0.1.sp
    )

    val KnobValue = TextStyle(
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp
    )

    // Transport buttons
    val TransportButton = TextStyle(
        fontFamily = SansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 0.1.sp
    )

    // Mode selectors
    val ModeButton = TextStyle(
        fontFamily = SansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 0.05.sp
    )

    // Pattern buttons
    val PatternButton = TextStyle(
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    )

    // Step sequencer step numbers
    val StepNumber = TextStyle(
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp
    )

    // Waveform info
    val WaveformInfo = TextStyle(
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.Regular,
        fontSize = 11.sp
    )

    // Dialog/text
    val BodyLarge = TextStyle(
        fontFamily = SansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    )

    val BodyMedium = TextStyle(
        fontFamily = SansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    )

    val LabelSmall = TextStyle(
        fontFamily = SansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.05.sp
    )

    // Complete Typography
    val Typography = Typography(
        displayLarge = DisplayLarge,
        displayMedium = DisplayMedium,
        displaySmall = DisplaySmall,
        headlineLarge = DisplayMedium,
        headlineMedium = DisplaySmall,
        headlineSmall = DisplaySmall,
        titleLarge = DisplaySmall,
        titleMedium = KnobLabel,
        titleSmall = PadLabel,
        bodyLarge = BodyLarge,
        bodyMedium = BodyMedium,
        bodySmall = LabelSmall,
        labelLarge = TransportButton,
        labelMedium = ModeButton,
        labelSmall = StepNumber
    )
}
package com.hardtekk.electribe.ui.theme

import androidx.compose.ui.graphics.Color

// Hardtekk Electribe Color Palette
// Dark anthracite background with neon pink/magenta accents

object HardtekkColors {
    // Primary - Neon Pink
    val NeonPink = Color(0xFFFF007F)
    val NeonPinkBright = Color(0xFFFF3399)
    val NeonPinkDim = Color(0xFFCC0066)
    val NeonPinkGlow = Color(0xFFFF007F)

    // Secondary - Magenta
    val Magenta = Color(0xFFFF00FF)
    val MagentaBright = Color(0xFFFF33FF)
    val MagentaDim = Color(0xFFCC00CC)

    // Background - Dark Anthracite
    val Anthracite = Color(0xFF121212)
    val AnthraciteLight = Color(0xFF1A1A1A)
    val AnthraciteLighter = Color(0xFF222222)
    val AnthraciteCard = Color(0xFF1E1E1E)
    val AnthraciteElevated = Color(0xFF2A2A2A)

    // LED Colors
    val LedGreen = Color(0xFF00FF44)
    val LedRed = Color(0xFFFF0033)
    val LedAmber = Color(0xFFFFB300)
    val LedBlue = Color(0xFF00B3FF)
    val LedWhite = Color(0xFFFFFFFF)
    val LedOff = Color(0xFF333333)

    // Grays
    val GrayDark = Color(0xFF333333)
    val GrayMedium = Color(0xFF555555)
    val GrayLight = Color(0xFF888888)
    val GrayBorder = Color(0xFF444444)

    // Text
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFCCCCCC)
    val TextMuted = Color(0xFF888888)
    val TextOnPink = Color(0xFF000000)
    val TextOnDark = Color(0xFFFFFFFF)

    // Pad Colors (16 distinct colors for the 16 pads)
    val PadColors = arrayOf(
        Color(0xFFFF007F), // 1 - Neon Pink
        Color(0xFFFF3399), // 2 - Bright Pink
        Color(0xFFFF00CC), // 3 - Deep Magenta
        Color(0xFFFF00FF), // 4 - Magenta
        Color(0xFFCC00FF), // 5 - Purple Magenta
        Color(0xFF9900FF), // 6 - Violet
        Color(0xFF6600FF), // 7 - Electric Purple
        Color(0xFF3300FF), // 8 - Blue Violet
        Color(0xFF0033FF), // 9 - Electric Blue
        Color(0xFF0066FF), // 10 - Bright Blue
        Color(0xFF0099FF), // 11 - Sky Blue
        Color(0xFF00CCFF), // 12 - Cyan
        Color(0xFF00FFCC), // 13 - Teal
        Color(0xFF00FF99), // 14 - Spring Green
        Color(0xFF00FF66), // 15 - Neon Green
        Color(0xFF00FF33), // 16 - Bright Green
    )

    // Pad Active Colors (brighter when triggered)
    val PadActiveColors = arrayOf(
        Color(0xFFFF66B2), // 1
        Color(0xFFFF66B2), // 2
        Color(0xFFFF66E5), // 3
        Color(0xFFFF66FF), // 4
        Color(0xFFE566FF), // 5
        Color(0xFFB266FF), // 6
        Color(0xFF9966FF), // 7
        Color(0xFF6699FF), // 8
        Color(0xFF66B2FF), // 9
        Color(0xFF66CCFF), // 10
        Color(0xFF66E5FF), // 11
        Color(0xFF66FFFF), // 12
        Color(0xFF66FFE5), // 13
        Color(0xFF66FFB2), // 14
        Color(0xFF66FF99), // 15
        Color(0xFF66FF66), // 16
    )

    // Knob
    val KnobTrack = Color(0xFF333333)
    val KnobIndicator = NeonPink
    val KnobIndicatorActive = NeonPinkBright

    // Sequencer LEDs
    val SeqLedActive = NeonPink
    val SeqLedCurrent = LedGreen
    val SeqLedInactive = Color(0xFF222222)
    val SeqLedDim = Color(0xFF111111)

    // Waveform
    val WaveformBg = Anthracite
    val WaveformWave = NeonPink
    val WaveformPlayhead = LedGreen
    val WaveformMarkerStart = LedGreen
    val WaveformMarkerEnd = LedRed
    val WaveformSelection = Color(0x44FF007F)

    // Dividers and Shadows
    val Divider = Color(0xFF333333)
    val Shadow = Color(0xFF000000)
    val GlowPink = NeonPink
    val GlowGreen = LedGreen

    // Semantic mappings for Material3
    val Primary = NeonPink
    val PrimaryVariant = NeonPinkDim
    val Secondary = Magenta
    val SecondaryVariant = MagentaDim
    val Background = Anthracite
    val Surface = AnthraciteCard
    val Error = LedRed
    val OnPrimary = TextOnPink
    val OnSecondary = TextOnPink
    val OnBackground = TextOnDark
    val OnSurface = TextOnDark
    val OnError = TextOnPink
}
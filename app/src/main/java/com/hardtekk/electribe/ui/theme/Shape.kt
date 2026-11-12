package com.hardtekk.electribe.ui.theme

import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shapes for Hardtekk Electribe
 * Sharp, hardware-like corners with slight rounding
 */
object HardtekkShapes {
    val Shapes = Shapes(
        extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),   // Buttons, small elements
        small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),        // Cards, pads
        medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),      // Dialogs, sheets
        large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),       // Bottom sheets
        extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)   // Full screen modals
    )

    // Custom shapes
    val PadShape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
    val KnobShape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp) // Circular
    val TransportButtonShape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
    val PatternButtonShape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
    val ModeButtonShape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
    val DisplayShape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
    val PanelShape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
}
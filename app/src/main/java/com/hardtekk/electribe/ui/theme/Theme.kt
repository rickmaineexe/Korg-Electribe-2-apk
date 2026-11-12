package com.hardtekk.electribe.ui.theme

import android.app.Activity
import android.view.Window
import android.view.WindowInsetsController
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.hardtekk.electribe.ui.theme.HardtekkColors.*
import com.hardtekk.electribe.ui.theme.HardtekkShapes.Shapes
import com.hardtekk.electribe.ui.theme.HardtekkType.Typography

private val DarkColorScheme = darkColorScheme(
    primary = NeonPink,
    primaryContainer = NeonPinkDim,
    secondary = Magenta,
    secondaryContainer = MagentaDim,
    tertiary = LedGreen,
    tertiaryContainer = LedGreen.copy(alpha = 0.2f),
    error = LedRed,
    errorContainer = LedRed.copy(alpha = 0.2f),
    background = Anthracite,
    onBackground = TextOnDark,
    surface = AnthraciteCard,
    onSurface = TextOnDark,
    surfaceVariant = AnthraciteLighter,
    onSurfaceVariant = TextSecondary,
    outline = GrayBorder,
    outlineVariant = GrayDark,
    shadow = Shadow,
    scrim = Shadow,
    surfaceTint = NeonPink,
    inverseSurface = Color.White,
    inverseOnSurface = Anthracite,
    inversePrimary = NeonPinkDim
)

private val LightColorScheme = lightColorScheme(
    primary = NeonPinkDim,
    primaryContainer = NeonPink,
    secondary = MagentaDim,
    secondaryContainer = Magenta,
    tertiary = LedGreen,
    tertiaryContainer = LedGreen.copy(alpha = 0.2f),
    error = LedRed,
    background = Color.White,
    surface = Color(0xFFF5F5F5),
    onBackground = Color.Black,
    onSurface = Color.Black
)

/**
 * Hardtekk Electribe Theme
 * Dark-only theme with neon pink/magenta accents
 */
@Composable
fun HardtekkElectribeTheme(
    darkTheme: Boolean = true, // Force dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    val context = LocalContext.current

    // Set system bar colors
    SideEffect {
        val activity = (context as? Activity) ?: return@SideEffect
        val window: Window = activity.window
        window.statusBarColor = Anthracite.toArgb()
        window.navigationBarColor = Anthracite.toArgb()
        val insetsController = window.insetsController
        if (insetsController != null) {
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

/**
 * Preview helper
 */
@Composable
fun HardtekkElectribeThemePreview(content: @Composable () -> Unit) {
    HardtekkElectribeTheme(content = content)
}
package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = SpaceBluePrimary,
    secondary = SpaceCyanAccent,
    tertiary = NebulaPurpleAccent,
    background = DeepSpaceBlack,
    surface = SlateSpaceCard,
    onPrimary = DeepSpaceBlack,
    onSecondary = DeepSpaceBlack,
    onTertiary = DeepSpaceBlack,
    onBackground = CosmicWhiteText,
    onSurface = CosmicWhiteText
  )

private val LightColorScheme =
  lightColorScheme(
    primary = CleanMinimalPrimary,
    secondary = CleanMinimalPrimary,
    secondaryContainer = CleanMinimalContainer,
    tertiary = CleanMinimalTerminalBg,
    background = CleanMinimalBg,
    surface = CleanMinimalSurface,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF001453),
    onTertiary = CleanMinimalTerminalText,
    onBackground = CleanMinimalText,
    onSurface = CleanMinimalText,
    surfaceVariant = CleanMinimalInputBg,
    onSurfaceVariant = CleanMinimalText
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color by default to prioritize the branded "Clean Minimalism" look
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

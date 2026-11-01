package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
  primary = ElectricBlue,
  onPrimary = NavyDark,
  primaryContainer = ElectricBlueVariant,
  onPrimaryContainer = TextPrimaryDark,
  secondary = VividViolet,
  onSecondary = NavyDark,
  secondaryContainer = VividVioletDark,
  onSecondaryContainer = TextPrimaryDark,
  tertiary = EmeraldSuccess,
  onTertiary = NavyDark,
  background = NavyDark,
  onBackground = TextPrimaryDark,
  surface = NavySurface,
  onSurface = TextPrimaryDark,
  surfaceVariant = NavySurfaceVariant,
  onSurfaceVariant = TextSecondaryDark,
  outline = NavyCardBorder,
  error = RoseError,
  onError = TextPrimaryDark
)

private val LightColorScheme = lightColorScheme(
  primary = ElectricBlueVariant,
  onPrimary = LightSurface,
  primaryContainer = Color(0xFFBAE6FD),
  onPrimaryContainer = Color(0xFF0369A1),
  secondary = VividVioletDark,
  onSecondary = LightSurface,
  secondaryContainer = Color(0xFFE0E7FF),
  onSecondaryContainer = Color(0xFF3730A3),
  tertiary = EmeraldSuccess,
  onTertiary = LightSurface,
  background = LightBackground,
  onBackground = TextPrimaryLight,
  surface = LightSurface,
  onSurface = TextPrimaryLight,
  surfaceVariant = LightSurfaceVariant,
  onSurfaceVariant = TextSecondaryLight,
  outline = LightOutline,
  error = RoseError,
  onError = LightSurface
)

@Composable
fun FormPilotTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Preserve brand colors by default
  content: @Composable () -> Unit
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

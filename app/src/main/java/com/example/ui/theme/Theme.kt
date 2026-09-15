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
  primary = CyanAccent,
  onPrimary = Slate950,
  primaryContainer = Slate800,
  onPrimaryContainer = CyanAccent,
  secondary = IndigoAccent,
  onSecondary = Color.White,
  tertiary = EmeraldAccent,
  onTertiary = Slate950,
  background = Slate950,
  onBackground = Color.White,
  surface = Slate900,
  onSurface = Color.White,
  surfaceVariant = Slate850,
  onSurfaceVariant = Slate400,
  outline = Slate700,
  error = CoralAccent,
  onError = Color.White
)

private val LightColorScheme = lightColorScheme(
  primary = CyanAccentDark,
  onPrimary = Color.White,
  primaryContainer = Slate200,
  onPrimaryContainer = Slate900,
  secondary = IndigoAccent,
  onSecondary = Color.White,
  tertiary = EmeraldAccent,
  onTertiary = Color.White,
  background = Color(0xFFF8FAFC),
  onBackground = Slate900,
  surface = Color.White,
  onSurface = Slate900,
  surfaceVariant = Slate200,
  onSurfaceVariant = Slate700,
  outline = Slate400,
  error = CoralAccent,
  onError = Color.White
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
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

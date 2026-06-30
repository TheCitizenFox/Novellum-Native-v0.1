package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NovellumColorScheme =
  darkColorScheme(
    primary = IgniteOrange,
    onPrimary = TextPrimary,
    secondary = MutedGrayPanel,
    onSecondary = TextPrimary,
    tertiary = DividerGray,
    background = CharcoalBackground,
    onBackground = TextPrimary,
    surface = CharcoalSurface,
    onSurface = TextPrimary,
    surfaceVariant = MutedGrayPanel,
    onSurfaceVariant = TextSecondary,
    outline = DividerGray
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color disabled for Novellum strict dark theme
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(colorScheme = NovellumColorScheme, typography = Typography, content = content)
}

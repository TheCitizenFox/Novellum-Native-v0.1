package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// The donor project references EB Garamond and Plus Jakarta Sans conceptually.
// The clean production app does not bundle external font files, so the native
// serif/sans families preserve offline operation and avoid introducing a new
// runtime or licensing dependency during the visual transplant.
val ManuscriptSerif = FontFamily.Serif
val UiSans = FontFamily.SansSerif

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = ManuscriptSerif,
        fontWeight = FontWeight.Light,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.04.sp
    ),
    displayMedium = TextStyle(
        fontFamily = ManuscriptSerif,
        fontWeight = FontWeight.Light,
        fontSize = 26.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.03.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = ManuscriptSerif,
        fontWeight = FontWeight.Light,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.01.sp
    ),
    titleMedium = TextStyle(
        fontFamily = ManuscriptSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.01.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = ManuscriptSerif,
        fontWeight = FontWeight.Light,
        fontSize = 17.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.012.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = UiSans,
        fontWeight = FontWeight.Light,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.01.sp
    ),
    labelLarge = TextStyle(
        fontFamily = UiSans,
        fontWeight = FontWeight.Light,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.01.sp
    ),
    labelMedium = TextStyle(
        fontFamily = UiSans,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.12.sp
    ),
    labelSmall = TextStyle(
        fontFamily = UiSans,
        fontWeight = FontWeight.Light,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.02.sp
    )
)

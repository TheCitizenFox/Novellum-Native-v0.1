package com.example.ui.features.workspace

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object WorkspaceColors {
    val Void = Color(0xFF070B0E)
    val Deep = Color(0xFF0A1014)
    val Panel = Color(0xFF0D1418)
    val PanelRaised = Color(0xFF11191E)
    val PanelSoft = Color(0xFF141D22)
    val Editor = Color(0xFF0A1014)
    val Accent = Color(0xFFF18A38)
    val AccentBright = Color(0xFFFFA45D)
    val AccentMuted = Color(0xFF6B3A1D)
    val AccentWash = Color(0x1FF18A38)
    val TextPrimary = Color(0xFFE8E3DC)
    val TextSecondary = Color(0xFFAAA8A4)
    val TextMuted = Color(0xFF6F767A)
    val Hairline = Color(0xFF202A30)
    val HairlineBright = Color(0xFF2A363D)
    val Success = Color(0xFF8FB49C)
    val Warning = Color(0xFFE6AD66)
    val Danger = Color(0xFFD87870)
    val Scrim = Color(0xA6000000)
}

internal object WorkspaceMetrics {
    val OuterPadding = 10.dp
    val PanelGap = 10.dp
    val PanelRadius = 14.dp
    val ControlRadius = 9.dp
    val TopBarHeight = 62.dp
    val LeftPanelWidth = 278.dp
    val RightPanelWidth = 292.dp
    val OverlayPanelWidth = 306.dp
}

internal object WorkspaceType {
    val Brand = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        letterSpacing = 4.sp,
        color = WorkspaceColors.TextPrimary
    )

    val Eyebrow = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.2.sp,
        color = WorkspaceColors.TextMuted
    )

    val Ui = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = WorkspaceColors.TextSecondary
    )

    val UiStrong = Ui.copy(
        fontWeight = FontWeight.Medium,
        color = WorkspaceColors.TextPrimary
    )

    val UiSmall = Ui.copy(fontSize = 11.sp, lineHeight = 15.sp)

    val SceneTitle = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 23.sp,
        lineHeight = 29.sp,
        color = WorkspaceColors.TextPrimary
    )

    val Manuscript = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 33.sp,
        letterSpacing = 0.15.sp,
        color = WorkspaceColors.TextPrimary
    )

    val PreviewTitle = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        color = WorkspaceColors.TextPrimary
    )
}

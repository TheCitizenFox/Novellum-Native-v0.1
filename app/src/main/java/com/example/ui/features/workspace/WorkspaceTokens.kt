package com.example.ui.features.workspace

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object WorkspaceColors {
    val Void = Color(0xFF07090B)
    val Deep = Color(0xFF0A0D0F)
    val Panel = Color(0xFF0E1113)
    val PanelRaised = Color(0xFF121619)
    val PanelHighlight = Color(0xFF171B1E)
    val PanelSoft = Color(0xFF111518)
    val Editor = Color(0xFF0A0D0F)
    val Accent = Color(0xFFE98A3E)
    val AccentBright = Color(0xFFF1A064)
    val AccentMuted = Color(0xFF5A321C)
    val AccentWash = Color(0x14E98A3E)
    val TextPrimary = Color(0xFFD8D3CC)
    val ManuscriptText = Color(0xFFC3BDB6)
    val TextSecondary = Color(0xFF979A99)
    val TextMuted = Color(0xFF71787B)
    val Hairline = Color(0xFF1E292F)
    val HairlineBright = Color(0xFF29363D)
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
    val RightPanelWidth = 300.dp
    val OverlayPanelWidth = 306.dp
}

internal object WorkspaceType {
    val Brand = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
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
        fontSize = 22.sp,
        lineHeight = 28.sp,
        color = WorkspaceColors.TextPrimary
    )

    val Manuscript = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Light,
        fontSize = 16.25.sp,
        lineHeight = 27.sp,
        letterSpacing = 0.12.sp,
        color = WorkspaceColors.ManuscriptText
    )

    val PreviewTitle = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.5.sp,
        lineHeight = 23.sp,
        color = WorkspaceColors.TextPrimary
    )
}

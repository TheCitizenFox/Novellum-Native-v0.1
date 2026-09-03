package com.example.ui.features.workspace

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object WorkspaceColors {
    // Near-black graphite palette tuned to the latest reference. The previous
    // clean-room pass read too blue/raised; these surfaces intentionally sit
    // much closer together so the workspace feels continuous rather than carded.
    val Void = Color(0xFF020506)
    val Deep = Color(0xFF040708)
    val Panel = Color(0xFF060A0C)
    val PanelRaised = Color(0xFF090E11)
    val PanelHighlight = Color(0xFF0C1215)
    val PanelSoft = Color(0xFF080D10)
    val Editor = Color(0xFF05090B)
    val Accent = Color(0xFFE98A3E)
    val AccentBright = Color(0xFFF1A064)
    val AccentMuted = Color(0xFF5A321C)
    val AccentWash = Color(0x12E98A3E)
    val TextPrimary = Color(0xFFD2CEC8)
    val ManuscriptText = Color(0xFFC6C1B9)
    val TextSecondary = Color(0xFF8D9292)
    val TextMuted = Color(0xFF666D70)
    val Hairline = Color(0xFF151D21)
    val HairlineBright = Color(0xFF20292E)
    val Success = Color(0xFF8FB49C)
    val Warning = Color(0xFFE6AD66)
    val Danger = Color(0xFFD87870)
    val Scrim = Color(0xA6000000)
}

internal object WorkspaceMetrics {
    val OuterPadding = 0.dp
    val PanelGap = 1.dp
    val PanelRadius = 0.dp
    val ControlRadius = 7.dp
    val TopBarHeight = 64.dp
    val LeftPanelWidth = 272.dp
    val RightPanelWidth = 308.dp
    val OverlayPanelWidth = 300.dp
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
        fontWeight = FontWeight.Normal,
        fontSize = 20.5.sp,
        lineHeight = 27.sp,
        color = WorkspaceColors.TextPrimary
    )

    val Manuscript = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 17.25.sp,
        lineHeight = 28.5.sp,
        letterSpacing = 0.12.sp,
        color = WorkspaceColors.ManuscriptText
    )

    val PreviewTitle = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.5.sp,
        color = WorkspaceColors.TextPrimary
    )
}

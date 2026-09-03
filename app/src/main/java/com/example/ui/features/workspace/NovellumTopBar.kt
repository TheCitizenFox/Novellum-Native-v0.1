package com.example.ui.features.workspace

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text

internal enum class WorkspaceMode(
    val label: String,
    val icon: WorkspaceIcon
) {
    Editor("Editor", WorkspaceIcon.Editor),
    Cards("Structure", WorkspaceIcon.Cards),
    Vault("Vault", WorkspaceIcon.Vault),
    Library("Library", WorkspaceIcon.Library),
    Manuscript("Manuscript", WorkspaceIcon.Manuscript)
}

@Composable
internal fun NovellumTopBar(
    activeMode: WorkspaceMode,
    leftPanelOpen: Boolean,
    rightPanelOpen: Boolean,
    showBrandText: Boolean,
    showModeLabels: Boolean,
    onModeSelected: (WorkspaceMode) -> Unit,
    onToggleLeftPanel: () -> Unit,
    onToggleRightPanel: () -> Unit,
    onUnavailableUtility: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(WorkspaceMetrics.TopBarHeight)
            .background(WorkspaceColors.Void.copy(alpha = .92f))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.width(if (showBrandText) 290.dp else 56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NovellumIcon(
                icon = WorkspaceIcon.Brand,
                tint = WorkspaceColors.Accent,
                modifier = Modifier.size(29.dp)
            )
            if (showBrandText) {
                Spacer(Modifier.width(15.dp))
                Text("NOVELLUM", style = WorkspaceType.Brand)
            }
        }

        Row(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WorkspaceMode.entries.forEach { mode ->
                TopModeButton(
                    mode = mode,
                    selected = mode == activeMode,
                    showLabel = showModeLabels,
                    onClick = { onModeSelected(mode) }
                )
            }
        }

        Row(
            modifier = Modifier.width(if (showModeLabels) 220.dp else 164.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showModeLabels) {
                CompactIconButton(
                    icon = WorkspaceIcon.History,
                    description = "History",
                    onClick = { onUnavailableUtility("History") }
                )
                CompactIconButton(
                    icon = WorkspaceIcon.Settings,
                    description = "Settings",
                    onClick = { onUnavailableUtility("Settings") }
                )
            }
            CompactIconButton(
                icon = WorkspaceIcon.PanelLeft,
                description = if (leftPanelOpen) "Hide manuscript panel" else "Show manuscript panel",
                onClick = onToggleLeftPanel
            )
            CompactIconButton(
                icon = WorkspaceIcon.PanelRight,
                description = if (rightPanelOpen) "Hide auxiliary panel" else "Show auxiliary panel",
                onClick = onToggleRightPanel
            )
            CompactIconButton(
                icon = WorkspaceIcon.More,
                description = "More workspace actions",
                onClick = { onUnavailableUtility("Additional workspace actions") }
            )
        }
    }
}

@Composable
private fun TopModeButton(
    mode: WorkspaceMode,
    selected: Boolean,
    showLabel: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val tint by animateColorAsState(
        targetValue = if (selected) WorkspaceColors.AccentBright else WorkspaceColors.TextSecondary,
        animationSpec = spring(stiffness = 430f, dampingRatio = .86f),
        label = "topModeTint"
    )
    val modeWidth = when {
        !showLabel -> 48.dp
        mode == WorkspaceMode.Manuscript -> 132.dp
        mode == WorkspaceMode.Library -> 116.dp
        else -> 106.dp
    }
    Box(
        modifier = Modifier
            .width(modeWidth)
            .fillMaxHeight()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = if (showLabel) 10.dp else 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NovellumIcon(mode.icon, tint, Modifier.size(18.dp))
            if (showLabel) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = mode.label,
                    style = WorkspaceType.Ui.copy(
                        color = tint,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                    )
                )
            }
        }
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .width(if (showLabel) modeWidth - 18.dp else 30.dp)
                    .height(2.dp)
                    .background(WorkspaceColors.Accent)
            )
        }
    }
}

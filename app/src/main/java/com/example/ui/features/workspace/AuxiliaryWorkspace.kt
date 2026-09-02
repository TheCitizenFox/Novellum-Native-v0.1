package com.example.ui.features.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.entity.ChapterEntity
import com.example.data.entity.ProjectEntity
import com.example.data.entity.SceneEntity

internal enum class AuxiliaryTab(val label: String) {
    Vault("Vault"),
    Library("Library"),
    Notes("Notes")
}

@Composable
internal fun AuxiliaryWorkspace(
    selectedTab: AuxiliaryTab,
    selectedProject: ProjectEntity?,
    selectedChapter: ChapterEntity?,
    selectedScene: SceneEntity?,
    onTabSelected: (AuxiliaryTab) -> Unit,
    onUnavailableAction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(WorkspaceMetrics.PanelGap)
    ) {
        PanelSurface(Modifier.weight(1.14f).fillMaxWidth()) {
            AuxiliaryLibraryPanel(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                onUnavailableAction = onUnavailableAction,
                modifier = Modifier.fillMaxHeight()
            )
        }
        PanelSurface(Modifier.weight(.78f).fillMaxWidth()) {
            CurrentFocusPanel(
                project = selectedProject,
                chapter = selectedChapter,
                scene = selectedScene,
                modifier = Modifier.fillMaxHeight()
            )
        }
    }
}

@Composable
private fun AuxiliaryLibraryPanel(
    selectedTab: AuxiliaryTab,
    onTabSelected: (AuxiliaryTab) -> Unit,
    onUnavailableAction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember(selectedTab) { mutableStateOf("") }
    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AuxiliaryTab.entries.forEach { tab ->
                AuxiliaryTabButton(
                    tab = tab,
                    selected = tab == selectedTab,
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Hairline()
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchShell(
                value = query,
                placeholder = "Search ${selectedTab.label.lowercase()}…",
                onValueChange = { query = it },
                onFilter = { onUnavailableAction("${selectedTab.label} filters") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            CompactIconButton(
                icon = WorkspaceIcon.Add,
                description = "New ${selectedTab.label} entry",
                onClick = { onUnavailableAction("New ${selectedTab.label} entry") },
                selected = true,
                size = 40.dp,
                iconSize = 18.dp
            )
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AuxiliaryEmptyGlyph(selectedTab)
            Text(
                text = "Your ${selectedTab.label.lowercase()} is ready.",
                style = WorkspaceType.UiStrong,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 18.dp)
            )
            Text(
                text = when (selectedTab) {
                    AuxiliaryTab.Vault -> "Capture fragments, research, and durable story material here."
                    AuxiliaryTab.Library -> "Reference material will remain close to the manuscript."
                    AuxiliaryTab.Notes -> "Keep scene-specific thinking beside the draft."
                },
                style = WorkspaceType.UiSmall.copy(color = WorkspaceColors.TextMuted),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )
            CompactTextButton(
                label = "New entry",
                leadingIcon = WorkspaceIcon.Add,
                onClick = { onUnavailableAction("New ${selectedTab.label} entry") },
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun AuxiliaryEmptyGlyph(tab: AuxiliaryTab) {
    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(WorkspaceColors.AccentWash),
        contentAlignment = Alignment.Center
    ) {
        NovellumIcon(
            icon = when (tab) {
                AuxiliaryTab.Vault -> WorkspaceIcon.Vault
                AuxiliaryTab.Library -> WorkspaceIcon.Library
                AuxiliaryTab.Notes -> WorkspaceIcon.Document
            },
            tint = WorkspaceColors.Accent,
            modifier = Modifier.size(43.dp)
        )
    }
}

@Composable
private fun AuxiliaryTabButton(
    tab: AuxiliaryTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val color = if (selected) WorkspaceColors.AccentBright else WorkspaceColors.TextSecondary
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            tab.label,
            style = WorkspaceType.Ui.copy(
                color = color,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
            )
        )
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(.72f)
                    .height(2.dp)
                    .background(WorkspaceColors.Accent)
            )
        }
    }
}

@Composable
private fun CurrentFocusPanel(
    project: ProjectEntity?,
    chapter: ChapterEntity?,
    scene: SceneEntity?,
    modifier: Modifier = Modifier
) {
    Column(modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
        Text("CURRENT FOCUS", style = WorkspaceType.Eyebrow)
        Spacer(Modifier.height(12.dp))
        FocusRow(WorkspaceIcon.Project, "Project", project?.title ?: "None")
        FocusRow(WorkspaceIcon.Chapter, "Chapter", chapter?.title ?: "None")
        FocusRow(WorkspaceIcon.Document, "Scene", scene?.title ?: "Preview")
        Hairline(Modifier.padding(vertical = 4.dp))
        FocusRow(
            WorkspaceIcon.Words,
            "Words",
            scene?.prose?.let(::wordCount)?.toString() ?: "—"
        )
    }
}

@Composable
private fun FocusRow(icon: WorkspaceIcon, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().height(42.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NovellumIcon(icon, WorkspaceColors.TextSecondary, Modifier.size(19.dp))
        Text(label, style = WorkspaceType.Ui, modifier = Modifier.padding(start = 11.dp))
        Spacer(Modifier.weight(1f))
        Text(
            value,
            style = WorkspaceType.Ui.copy(color = WorkspaceColors.AccentBright),
            modifier = Modifier.padding(start = 8.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

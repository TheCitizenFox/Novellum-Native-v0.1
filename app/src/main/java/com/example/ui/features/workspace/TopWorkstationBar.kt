package com.example.ui.features.workspace

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ManuscriptSerif
import com.example.ui.theme.NovellumEmeraldPulse
import com.example.ui.theme.NovellumObsidian
import com.example.ui.theme.NovellumOutline
import com.example.ui.theme.NovellumOutlineVariant
import com.example.ui.theme.NovellumPrimary
import com.example.ui.theme.NovellumPrimaryContainer
import com.example.ui.theme.NovellumSecondaryContainer
import com.example.ui.theme.NovellumSurfaceContainerLow
import com.example.ui.theme.UiSans

@Composable
fun TopWorkstationBar(
    projectTitle: String?,
    totalWords: Int,
    activeTab: WorkspaceTab,
    onTabSelected: (WorkspaceTab) -> Unit,
    onBackup: () -> Unit,
    onExport: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(62.dp)
            .background(NovellumObsidian)
            .border(1.dp, NovellumOutlineVariant.copy(alpha = 0.28f))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "✦",
                fontFamily = ManuscriptSerif,
                fontSize = 20.sp,
                color = NovellumPrimary
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "NOVELLUM",
                fontFamily = ManuscriptSerif,
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 4.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(16.dp))
            Box(Modifier.width(1.dp).height(22.dp).background(NovellumOutlineVariant.copy(alpha = 0.45f)))
            Spacer(Modifier.width(16.dp))
            Text(
                text = projectTitle ?: "No manuscript selected",
                fontFamily = ManuscriptSerif,
                fontStyle = FontStyle.Italic,
                fontSize = 15.sp,
                color = if (projectTitle == null) NovellumOutline else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 220.dp)
            )
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            NavTabItem("Editor", activeTab == WorkspaceTab.EDITOR) { onTabSelected(WorkspaceTab.EDITOR) }
            NavTabItem("Structure", activeTab == WorkspaceTab.STRUCTURE) { onTabSelected(WorkspaceTab.STRUCTURE) }
            NavTabItem("Vault", activeTab == WorkspaceTab.VAULT) { onTabSelected(WorkspaceTab.VAULT) }
            NavTabItem("Library", activeTab == WorkspaceTab.LIBRARY) { onTabSelected(WorkspaceTab.LIBRARY) }
            NavTabItem("Manuscript", activeTab == WorkspaceTab.MANUSCRIPT) { onTabSelected(WorkspaceTab.MANUSCRIPT) }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(NovellumSurfaceContainerLow)
                    .border(1.dp, NovellumOutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(NovellumEmeraldPulse))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "%,d".format(totalWords),
                    fontFamily = UiSans,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Text("WDS", fontFamily = UiSans, fontSize = 10.sp, color = NovellumOutline)
            }

            Spacer(Modifier.width(6.dp))
            UtilityIconButton(Icons.Default.SaveAlt, "Backup", false, onBackup)
            Spacer(Modifier.width(6.dp))
            UtilityIconButton(Icons.Default.FileDownload, "Export Markdown", false, onExport)
            Spacer(Modifier.width(6.dp))
            UtilityIconButton(Icons.Default.Settings, "Settings", false, onOpenSettings)
        }
    }
}

@Composable
private fun NavTabItem(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val textColor by animateColorAsState(
        targetValue = if (isSelected) NovellumPrimary else NovellumOutline,
        label = "navTabTextColor"
    )
    val underlineWidth by animateDpAsState(if (isSelected) 24.dp else 0.dp, label = "navUnderline")

    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            fontFamily = UiSans,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Normal else FontWeight.Light,
            color = textColor
        )
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .width(underlineWidth)
                .height(2.dp)
                .background(if (isSelected) NovellumPrimaryContainer else Color.Transparent, RoundedCornerShape(1.dp))
        )
    }
}

@Composable
private fun UtilityIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    active: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (active) NovellumSecondaryContainer.copy(alpha = 0.35f) else NovellumObsidian)
            .border(
                1.dp,
                if (active) NovellumPrimaryContainer else NovellumOutlineVariant.copy(alpha = 0.3f),
                RoundedCornerShape(4.dp)
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (active) NovellumPrimary else NovellumOutline,
            modifier = Modifier.size(17.dp)
        )
    }
}

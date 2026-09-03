package com.example.ui.features.workspace

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun PanelSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(WorkspaceMetrics.PanelRadius))
            .background(
                Brush.verticalGradient(
                    0f to WorkspaceColors.PanelHighlight,
                    .14f to WorkspaceColors.PanelRaised,
                    .62f to WorkspaceColors.Panel,
                    1f to WorkspaceColors.Deep
                )
            )
            .border(
                width = .75.dp,
                color = WorkspaceColors.HairlineBright.copy(alpha = .58f),
                shape = RoundedCornerShape(WorkspaceMetrics.PanelRadius)
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // Extremely restrained deterministic matte grain. It should register as material,
            // not as a visible texture pattern.
            val step = 23f
            var y = 9f
            var row = 0
            while (y < size.height) {
                var x = ((row * 17) % 29).toFloat()
                while (x < size.width) {
                    val alpha = if (((x.toInt() + y.toInt()) / 23) % 3 == 0) .026f else .014f
                    drawCircle(WorkspaceColors.TextPrimary.copy(alpha = alpha), radius = .55f, center = androidx.compose.ui.geometry.Offset(x, y))
                    x += step * 2.35f
                }
                y += step
                row++
            }
        }
        content()
    }
}

@Composable
internal fun Hairline(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(WorkspaceColors.Hairline.copy(alpha = .58f))
    )
}

@Composable
internal fun CompactIconButton(
    icon: WorkspaceIcon,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    size: Dp = 34.dp,
    iconSize: Dp = 18.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val background by animateColorAsState(
        targetValue = if (selected) WorkspaceColors.AccentWash else Color.Transparent,
        animationSpec = spring(stiffness = 520f, dampingRatio = .82f),
        label = "iconBackground"
    )
    val tint by animateColorAsState(
        targetValue = when {
            !enabled -> WorkspaceColors.TextMuted.copy(alpha = .42f)
            selected -> WorkspaceColors.Accent
            else -> WorkspaceColors.TextSecondary
        },
        animationSpec = spring(stiffness = 520f, dampingRatio = .82f),
        label = "iconTint"
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) .94f else 1f,
        animationSpec = spring(stiffness = 650f, dampingRatio = .72f),
        label = "iconPress"
    )
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(WorkspaceMetrics.ControlRadius))
            .background(background)
            .then(
                if (selected) {
                    Modifier.border(
                        1.dp,
                        WorkspaceColors.AccentMuted.copy(alpha = .5f),
                        RoundedCornerShape(WorkspaceMetrics.ControlRadius)
                    )
                } else Modifier
            )
            .semantics { contentDescription = description }
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        NovellumIcon(icon, tint, Modifier.size(iconSize))
    }
}

@Composable
internal fun CompactTextButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    outlined: Boolean = false,
    enabled: Boolean = true,
    leadingIcon: WorkspaceIcon? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val tint by animateColorAsState(
        targetValue = when {
            !enabled -> WorkspaceColors.TextMuted.copy(alpha = .42f)
            selected -> WorkspaceColors.AccentBright
            else -> WorkspaceColors.TextSecondary
        },
        animationSpec = spring(stiffness = 520f, dampingRatio = .82f),
        label = "textButtonTint"
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) .975f else 1f,
        animationSpec = spring(stiffness = 650f, dampingRatio = .74f),
        label = "textButtonPress"
    )
    Row(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .height(34.dp)
            .clip(RoundedCornerShape(WorkspaceMetrics.ControlRadius))
            .background(
                when {
                    selected -> WorkspaceColors.AccentWash
                    outlined -> WorkspaceColors.Deep.copy(alpha = .72f)
                    else -> Color.Transparent
                }
            )
            .then(
                when {
                    selected -> Modifier.border(
                        .75.dp,
                        WorkspaceColors.AccentMuted.copy(alpha = .44f),
                        RoundedCornerShape(WorkspaceMetrics.ControlRadius)
                    )
                    outlined -> Modifier.border(
                        .75.dp,
                        WorkspaceColors.HairlineBright.copy(alpha = .68f),
                        RoundedCornerShape(WorkspaceMetrics.ControlRadius)
                    )
                    else -> Modifier
                }
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (leadingIcon != null) {
            NovellumIcon(leadingIcon, tint, Modifier.size(16.dp))
            Spacer(Modifier.width(7.dp))
        }
        Text(
            text = label,
            style = WorkspaceType.UiSmall.copy(
                color = tint,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun SearchShell(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onFilter: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(WorkspaceMetrics.ControlRadius))
            .background(WorkspaceColors.Deep.copy(alpha = .76f))
            .border(
                .75.dp,
                WorkspaceColors.Hairline.copy(alpha = .64f),
                RoundedCornerShape(WorkspaceMetrics.ControlRadius)
            )
            .padding(start = 11.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NovellumIcon(
            WorkspaceIcon.Search,
            WorkspaceColors.TextMuted,
            Modifier.size(17.dp)
        )
        Spacer(Modifier.width(9.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = WorkspaceType.UiSmall.copy(color = WorkspaceColors.TextMuted),
                    maxLines = 1
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = WorkspaceType.UiSmall.copy(color = WorkspaceColors.TextPrimary),
                cursorBrush = SolidColor(WorkspaceColors.Accent)
            )
        }
        if (onFilter != null) {
            CompactIconButton(
                icon = WorkspaceIcon.Filter,
                description = "Filter",
                onClick = onFilter,
                size = 31.dp,
                iconSize = 16.dp
            )
        }
    }
}

@Composable
internal fun EntityEditorDialog(
    heading: String,
    initialTitle: String,
    destructiveLabel: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onRequestDelete: () -> Unit
) {
    var value by remember(initialTitle) { mutableStateOf(initialTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WorkspaceColors.PanelRaised,
        titleContentColor = WorkspaceColors.TextPrimary,
        textContentColor = WorkspaceColors.TextSecondary,
        title = { Text(heading, style = WorkspaceType.UiStrong.copy(fontSize = 16.sp)) },
        text = {
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                cursorBrush = SolidColor(WorkspaceColors.Accent),
                textStyle = WorkspaceType.UiStrong,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WorkspaceMetrics.ControlRadius))
                    .background(WorkspaceColors.Deep)
                    .border(
                        1.dp,
                        WorkspaceColors.HairlineBright,
                        RoundedCornerShape(WorkspaceMetrics.ControlRadius)
                    )
                    .padding(horizontal = 12.dp, vertical = 11.dp)
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = value.trim()
                    if (trimmed.isNotEmpty()) onSave(trimmed)
                }
            ) {
                Text("Save", color = WorkspaceColors.Accent)
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onRequestDelete) {
                    Text(destructiveLabel, color = WorkspaceColors.Danger)
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = WorkspaceColors.TextSecondary)
                }
            }
        }
    )
}

@Composable
internal fun ConfirmDeleteDialog(
    label: String,
    detail: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WorkspaceColors.PanelRaised,
        title = {
            Text("Move $label to recovery?", style = WorkspaceType.UiStrong.copy(fontSize = 16.sp))
        },
        text = {
            Text(detail, style = WorkspaceType.Ui.copy(color = WorkspaceColors.TextSecondary))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Confirm", color = WorkspaceColors.Danger)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep it", color = WorkspaceColors.TextSecondary)
            }
        }
    )
}

internal fun wordCount(text: String): Int =
    text.trim().takeIf { it.isNotEmpty() }?.split(Regex("\\s+"))?.size ?: 0

internal fun String.safeDocumentName(fallback: String): String {
    val cleaned = trim()
        .replace(Regex("[^A-Za-z0-9._ -]"), "_")
        .replace(Regex("\\s+"), "_")
        .trim('_', '.')
    return cleaned.ifEmpty { fallback }
}

internal val toolbarLetterStyle = TextStyle(
    fontFamily = FontFamily.Serif,
    fontWeight = FontWeight.Bold,
    fontSize = 18.sp,
    color = WorkspaceColors.TextPrimary
)

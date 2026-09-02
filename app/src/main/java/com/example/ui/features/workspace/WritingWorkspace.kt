package com.example.ui.features.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ChapterEntity
import com.example.data.entity.ProjectEntity
import com.example.data.entity.SceneEntity
import com.example.ui.viewmodel.SaveState
import java.text.DateFormat
import java.util.Date

@Composable
internal fun WritingWorkspace(
    selectedProject: ProjectEntity?,
    currentScene: SceneEntity?,
    previewChapter: ChapterEntity?,
    previewScenes: List<SceneEntity>,
    saveState: SaveState,
    lastSavedTime: Long?,
    onSyncScene: (String, String) -> Unit,
    onProseChanged: (String) -> Unit,
    onSaveNow: () -> Unit,
    onConfirmIntentionalClear: () -> Unit,
    onEditScene: (SceneEntity) -> Unit,
    onSelectScene: (String) -> Unit,
    onCreateChapter: () -> Unit,
    onUnavailableAction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    PanelSurface(modifier = modifier.fillMaxHeight()) {
        when {
            currentScene != null -> SceneEditor(
                scene = currentScene,
                saveState = saveState,
                lastSavedTime = lastSavedTime,
                onSyncScene = onSyncScene,
                onProseChanged = onProseChanged,
                onSaveNow = onSaveNow,
                onConfirmIntentionalClear = onConfirmIntentionalClear,
                onEditScene = { onEditScene(currentScene) },
                onUnavailableAction = onUnavailableAction,
                modifier = Modifier.fillMaxSize()
            )
            previewChapter != null -> ChapterPreview(
                chapter = previewChapter,
                scenes = previewScenes,
                onSelectScene = onSelectScene,
                modifier = Modifier.fillMaxSize()
            )
            else -> EmptyWritingWorkspace(
                hasProject = selectedProject != null,
                projectTitle = selectedProject?.title,
                onCreateChapter = onCreateChapter,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun SceneEditor(
    scene: SceneEntity,
    saveState: SaveState,
    lastSavedTime: Long?,
    onSyncScene: (String, String) -> Unit,
    onProseChanged: (String) -> Unit,
    onSaveNow: () -> Unit,
    onConfirmIntentionalClear: () -> Unit,
    onEditScene: () -> Unit,
    onUnavailableAction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var value by remember(scene.id) {
        mutableStateOf(TextFieldValue(scene.prose, TextRange(scene.prose.length)))
    }
    val undoStack = remember(scene.id) { mutableStateListOf<TextFieldValue>() }
    val redoStack = remember(scene.id) { mutableStateListOf<TextFieldValue>() }
    var showClearConfirmation by remember(scene.id) { mutableStateOf(false) }

    LaunchedEffect(scene.id) {
        onSyncScene(scene.id, scene.prose)
    }

    fun commit(next: TextFieldValue, recordHistory: Boolean = true) {
        if (next == value) return
        if (recordHistory) {
            undoStack.add(value)
            if (undoStack.size > 120) undoStack.removeAt(0)
            redoStack.clear()
        }
        value = next
        onProseChanged(next.text)
    }

    fun wrapSelection(open: String, close: String = open) {
        val start = value.selection.min
        val end = value.selection.max
        val selected = value.text.substring(start, end)
        val replacement = open + selected + close
        val nextText = value.text.replaceRange(start, end, replacement)
        val nextSelection = if (selected.isEmpty()) {
            TextRange(start + open.length)
        } else {
            TextRange(start + open.length, start + open.length + selected.length)
        }
        commit(TextFieldValue(nextText, nextSelection))
    }

    fun prefixSelectedLines(prefixForIndex: (Int) -> String) {
        val text = value.text
        val selectionStart = value.selection.min
        val selectionEnd = value.selection.max
        val blockStart = if (selectionStart == 0) {
            0
        } else {
            text.lastIndexOf('\n', selectionStart - 1).let { if (it < 0) 0 else it + 1 }
        }
        val nextBreak = text.indexOf('\n', selectionEnd)
        val blockEnd = if (nextBreak < 0) text.length else nextBreak
        val block = text.substring(blockStart, blockEnd)
        val replacement = block.lines().mapIndexed { index, line -> prefixForIndex(index) + line }
            .joinToString("\n")
        val nextText = text.replaceRange(blockStart, blockEnd, replacement)
        commit(TextFieldValue(nextText, TextRange(blockStart, blockStart + replacement.length)))
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val previous = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(value)
        value = previous
        onProseChanged(previous.text)
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val next = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(value)
        value = next
        onProseChanged(next.text)
    }

    Column(modifier) {
        SceneHeader(
            title = scene.title,
            saveState = saveState,
            lastSavedTime = lastSavedTime,
            onEditScene = onEditScene,
            onSaveNow = {
                if (saveState == SaveState.BLOCKED_EMPTY_CLEAR) showClearConfirmation = true
                else onSaveNow()
            },
            modifier = Modifier.padding(start = 28.dp, end = 20.dp, top = 20.dp)
        )

        EditorToolbar(
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty(),
            onBold = { wrapSelection("**") },
            onItalic = { wrapSelection("_") },
            onQuote = { prefixSelectedLines { "> " } },
            onBulletedList = { prefixSelectedLines { "• " } },
            onNumberedList = { prefixSelectedLines { index -> "${index + 1}. " } },
            onUndo = ::undo,
            onRedo = ::redo,
            onUnavailableAction = onUnavailableAction,
            modifier = Modifier.padding(start = 28.dp, end = 24.dp, top = 18.dp)
        )

        ManuscriptTextField(
            value = value,
            onValueChange = { commit(it) },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 18.dp)
        )

        EditorFooter(text = value.text)
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            containerColor = WorkspaceColors.PanelRaised,
            title = {
                Text("Clear this scene?", style = WorkspaceType.UiStrong.copy(fontSize = 16.sp))
            },
            text = {
                Text(
                    "Novellum blocked an empty overwrite. Confirm only if removing all prose is intentional; existing safety history remains managed by the application.",
                    style = WorkspaceType.Ui
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmation = false
                        onConfirmIntentionalClear()
                    }
                ) { Text("Clear intentionally", color = WorkspaceColors.Danger) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Cancel", color = WorkspaceColors.TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SceneHeader(
    title: String,
    saveState: SaveState,
    lastSavedTime: Long?,
    onEditScene: () -> Unit,
    onSaveNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = WorkspaceType.SceneTitle,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onEditScene),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        SaveStatus(saveState, lastSavedTime, onSaveNow)
    }
}

@Composable
private fun SaveStatus(
    saveState: SaveState,
    lastSavedTime: Long?,
    onClick: () -> Unit
) {
    val (label, color) = when (saveState) {
        SaveState.SAVED -> {
            val time = lastSavedTime?.let {
                DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it))
            }
            (time?.let { "Saved $it" } ?: "Saved") to WorkspaceColors.Success
        }
        SaveState.UNSAVED -> "Unsaved — tap to save" to WorkspaceColors.Warning
        SaveState.AUTOSAVING -> "Saving…" to WorkspaceColors.Accent
        SaveState.BLOCKED_EMPTY_CLEAR -> "Empty overwrite blocked" to WorkspaceColors.Danger
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(WorkspaceMetrics.ControlRadius))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(color))
        Text(
            label,
            style = WorkspaceType.UiSmall.copy(color = color),
            modifier = Modifier.padding(start = 7.dp)
        )
    }
}

@Composable
private fun EditorToolbar(
    canUndo: Boolean,
    canRedo: Boolean,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onQuote: () -> Unit,
    onBulletedList: () -> Unit,
    onNumberedList: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onUnavailableAction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(WorkspaceMetrics.ControlRadius))
            .background(WorkspaceColors.PanelSoft.copy(alpha = .74f))
            .border(
                1.dp,
                WorkspaceColors.Hairline.copy(alpha = .9f),
                RoundedCornerShape(WorkspaceMetrics.ControlRadius)
            )
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolbarLetterButton("B", toolbarLetterStyle, "Bold", onBold)
        ToolbarLetterButton("I", toolbarLetterStyle.copy(fontStyle = FontStyle.Italic), "Italic", onItalic)
        ToolbarDivider()
        ToolbarIconButton(WorkspaceIcon.Quote, "Quote", onQuote)
        ToolbarIconButton(WorkspaceIcon.BulletedList, "Bulleted list", onBulletedList)
        ToolbarIconButton(WorkspaceIcon.NumberedList, "Numbered list", onNumberedList)
        ToolbarDivider()
        ToolbarIconButton(WorkspaceIcon.Undo, "Undo", onUndo, enabled = canUndo)
        ToolbarIconButton(WorkspaceIcon.Redo, "Redo", onRedo, enabled = canRedo)
        ToolbarDivider()
        CompactTextButton(
            label = "Split",
            leadingIcon = WorkspaceIcon.Split,
            enabled = false,
            onClick = { onUnavailableAction("Split Screen") }
        )
        CompactTextButton(
            label = "Clean Text",
            leadingIcon = WorkspaceIcon.Clean,
            enabled = false,
            onClick = { onUnavailableAction("Clean Text") }
        )
    }
}

@Composable
private fun ToolbarLetterButton(
    label: String,
    style: TextStyle,
    description: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(7.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = style)
    }
}

@Composable
private fun ToolbarIconButton(
    icon: WorkspaceIcon,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    CompactIconButton(
        icon = icon,
        description = description,
        onClick = onClick,
        enabled = enabled,
        size = 36.dp,
        iconSize = 18.dp
    )
}

@Composable
private fun ToolbarDivider() {
    Spacer(
        Modifier
            .padding(horizontal = 5.dp)
            .width(1.dp)
            .height(24.dp)
            .background(WorkspaceColors.HairlineBright)
    )
}

@Composable
private fun ManuscriptTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectionColors = TextSelectionColors(
        handleColor = WorkspaceColors.Accent,
        backgroundColor = WorkspaceColors.Accent.copy(alpha = .26f)
    )
    CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
        Box(
            modifier = modifier
                .widthIn(max = 760.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(WorkspaceColors.Editor.copy(alpha = .55f))
                .padding(horizontal = 22.dp, vertical = 19.dp)
        ) {
            if (value.text.isEmpty()) {
                Text(
                    "Begin this scene…",
                    style = WorkspaceType.Manuscript.copy(color = WorkspaceColors.TextMuted)
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxSize(),
                textStyle = WorkspaceType.Manuscript,
                cursorBrush = SolidColor(WorkspaceColors.Accent)
            )
        }
    }
}

@Composable
private fun EditorFooter(text: String) {
    Hairline()
    Row(
        modifier = Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Words: ${wordCount(text)}", style = WorkspaceType.UiSmall)
        Text("Characters: ${text.length}", style = WorkspaceType.UiSmall, modifier = Modifier.padding(start = 22.dp))
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .width(112.dp)
                .height(2.dp)
                .background(WorkspaceColors.HairlineBright)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(.34f)
                    .height(2.dp)
                    .background(WorkspaceColors.Accent)
            )
        }
        Text("Draft", style = WorkspaceType.UiSmall, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun ChapterPreview(
    chapter: ChapterEntity,
    scenes: List<SceneEntity>,
    onSelectScene: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("COMPOSED MANUSCRIPT", style = WorkspaceType.Eyebrow)
                Text(chapter.title, style = WorkspaceType.SceneTitle, modifier = Modifier.padding(top = 3.dp))
            }
            CompactTextButton(
                label = "Read-only preview",
                leadingIcon = WorkspaceIcon.Manuscript,
                selected = true,
                enabled = false,
                onClick = {}
            )
        }
        Hairline()
        if (scenes.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NovellumIcon(WorkspaceIcon.Document, WorkspaceColors.TextMuted, Modifier.size(40.dp))
                Text("This chapter has no scenes.", style = WorkspaceType.Ui, modifier = Modifier.padding(top = 14.dp))
            }
        } else {
            SelectionContainer {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 42.dp,
                        end = 42.dp,
                        top = 30.dp,
                        bottom = 54.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(30.dp)
                ) {
                    items(scenes.sortedBy { it.orderIndex }, key = { it.id }) { scene ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 760.dp)
                                .clickable { onSelectScene(scene.id) }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(scene.title, style = WorkspaceType.PreviewTitle, modifier = Modifier.weight(1f))
                                Text(
                                    "${wordCount(scene.prose)} words",
                                    style = WorkspaceType.UiSmall.copy(color = WorkspaceColors.TextMuted)
                                )
                            }
                            Text(
                                text = scene.prose.ifBlank { "Empty scene" },
                                style = WorkspaceType.Manuscript.copy(
                                    color = if (scene.prose.isBlank()) WorkspaceColors.TextMuted else WorkspaceColors.TextPrimary
                                ),
                                modifier = Modifier.padding(top = 12.dp)
                            )
                            Hairline(Modifier.padding(top = 24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyWritingWorkspace(
    hasProject: Boolean,
    projectTitle: String?,
    onCreateChapter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        NovellumIcon(
            icon = if (hasProject) WorkspaceIcon.Editor else WorkspaceIcon.Project,
            tint = WorkspaceColors.Accent.copy(alpha = .78f),
            modifier = Modifier.size(52.dp)
        )
        Text(
            text = if (hasProject) projectTitle.orEmpty() else "The writing room is ready",
            style = WorkspaceType.SceneTitle,
            modifier = Modifier.padding(top = 18.dp)
        )
        Text(
            text = if (hasProject) {
                "Choose a scene from the manuscript, or create the first chapter."
            } else {
                "Choose or create a project from the manuscript panel."
            },
            style = WorkspaceType.Ui.copy(color = WorkspaceColors.TextMuted),
            modifier = Modifier.padding(top = 8.dp)
        )
        if (hasProject) {
            CompactTextButton(
                label = "New chapter",
                leadingIcon = WorkspaceIcon.Add,
                selected = true,
                onClick = onCreateChapter,
                modifier = Modifier.padding(top = 20.dp)
            )
        }
    }
}

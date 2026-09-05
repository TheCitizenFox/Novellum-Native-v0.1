package com.example.ui.features.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ChapterEntity
import com.example.data.entity.ProjectEntity
import com.example.data.entity.SceneEntity
import com.example.ui.theme.ManuscriptSerif
import com.example.ui.theme.NovellumBlocked
import com.example.ui.theme.NovellumEmeraldPulse
import com.example.ui.theme.NovellumObsidian
import com.example.ui.theme.NovellumOutline
import com.example.ui.theme.NovellumOutlineVariant
import com.example.ui.theme.NovellumPrimary
import com.example.ui.theme.NovellumPrimaryContainer
import com.example.ui.theme.NovellumSurface
import com.example.ui.theme.NovellumSurfaceContainer
import com.example.ui.theme.NovellumSurfaceContainerLow
import com.example.ui.theme.NovellumWarning
import com.example.ui.theme.UiSans
import com.example.ui.viewmodel.SaveState
import java.text.DateFormat
import java.util.Date

@Composable
fun EditorPane(
    project: ProjectEntity?,
    chapter: ChapterEntity?,
    chapterPosition: Int,
    currentScene: SceneEntity?,
    scenePosition: Int,
    scenesInChapter: List<SceneEntity>,
    saveState: SaveState,
    lastSavedTime: Long?,
    fontSizePt: Int,
    isZenMode: Boolean,
    editorSession: EditorSessionState,
    onProseChanged: (String) -> Unit,
    onSaveNow: () -> Unit,
    onConfirmIntentionalClear: () -> Unit,
    onCycleFontSize: () -> Unit,
    onToggleZen: () -> Unit,
    onSelectScene: (String) -> Unit,
    onCreateChapter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().background(NovellumSurface)) {
        if (currentScene != null && chapter != null) {
            SceneEditor(
                chapter = chapter,
                chapterPosition = chapterPosition,
                scene = currentScene,
                scenePosition = scenePosition,
                sceneCount = scenesInChapter.size,
                saveState = saveState,
                lastSavedTime = lastSavedTime,
                fontSizePt = fontSizePt,
                isZenMode = isZenMode,
                editorSession = editorSession,
                onProseChanged = onProseChanged,
                onSaveNow = onSaveNow,
                onConfirmIntentionalClear = onConfirmIntentionalClear,
                onCycleFontSize = onCycleFontSize,
                onToggleZen = onToggleZen,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            ScopeOverview(
                project = project,
                chapter = chapter,
                chapterPosition = chapterPosition,
                scenes = scenesInChapter,
                onSelectScene = onSelectScene,
                onCreateChapter = onCreateChapter,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun SceneEditor(
    chapter: ChapterEntity,
    chapterPosition: Int,
    scene: SceneEntity,
    scenePosition: Int,
    sceneCount: Int,
    saveState: SaveState,
    lastSavedTime: Long?,
    fontSizePt: Int,
    isZenMode: Boolean,
    editorSession: EditorSessionState,
    onProseChanged: (String) -> Unit,
    onSaveNow: () -> Unit,
    onConfirmIntentionalClear: () -> Unit,
    onCycleFontSize: () -> Unit,
    onToggleZen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val value = editorSession.value
    val undoStack = remember(scene.id) { mutableStateListOf<TextFieldValue>() }
    val redoStack = remember(scene.id) { mutableStateListOf<TextFieldValue>() }
    var showClearConfirmation by remember(scene.id) { mutableStateOf(false) }

    fun commit(next: TextFieldValue, recordHistory: Boolean = true) {
        if (next == value) return
        if (recordHistory) {
            undoStack.add(value)
            if (undoStack.size > 120) undoStack.removeAt(0)
            redoStack.clear()
        }
        if (editorSession.update(next)) onProseChanged(next.text)
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
        val blockStart = if (selectionStart == 0) 0 else text.lastIndexOf('\n', selectionStart - 1).let { if (it < 0) 0 else it + 1 }
        val nextBreak = text.indexOf('\n', selectionEnd)
        val blockEnd = if (nextBreak < 0) text.length else nextBreak
        val block = text.substring(blockStart, blockEnd)
        val replacement = block.lines().mapIndexed { index, line -> prefixForIndex(index) + line }.joinToString("\n")
        val nextText = text.replaceRange(blockStart, blockEnd, replacement)
        commit(TextFieldValue(nextText, TextRange(blockStart, blockStart + replacement.length)))
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val previous = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(value)
        if (editorSession.update(previous)) onProseChanged(previous.text)
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val next = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(value)
        if (editorSession.update(next)) onProseChanged(next.text)
    }

    Column(modifier) {
        EditorSubheader(
            scene = scene,
            scenePosition = scenePosition,
            saveState = saveState,
            lastSavedTime = lastSavedTime,
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty(),
            fontSizePt = fontSizePt,
            isZenMode = isZenMode,
            onUndo = ::undo,
            onRedo = ::redo,
            onBold = { wrapSelection("**") },
            onItalic = { wrapSelection("_") },
            onQuote = { prefixSelectedLines { "> " } },
            onBullets = { prefixSelectedLines { "• " } },
            onNumbered = { prefixSelectedLines { index -> "${index + 1}. " } },
            onCycleFontSize = onCycleFontSize,
            onToggleZen = onToggleZen,
            onSave = {
                if (saveState == SaveState.BLOCKED_EMPTY_CLEAR) showClearConfirmation = true else onSaveNow()
            }
        )

        Box(Modifier.weight(1f).fillMaxWidth().background(NovellumSurface)) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxHeight()
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .padding(start = 32.dp, end = 32.dp, top = 34.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CHAPTER ${romanNumeral(chapterPosition + 1)}",
                    fontFamily = UiSans,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 3.sp,
                    color = NovellumPrimary
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = chapterHeadingTitle(chapter, chapterPosition),
                    fontFamily = ManuscriptSerif,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(10.dp))
                OrnamentDivider()
                Spacer(Modifier.height(28.dp))

                val selectionColors = TextSelectionColors(
                    handleColor = NovellumPrimaryContainer,
                    backgroundColor = NovellumPrimaryContainer.copy(alpha = 0.25f)
                )
                CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
                    BasicTextField(
                        value = value,
                        onValueChange = { commit(it) },
                        textStyle = TextStyle(
                            fontFamily = ManuscriptSerif,
                            fontWeight = FontWeight.Light,
                            fontSize = fontSizePt.sp,
                            lineHeight = 28.sp,
                            letterSpacing = 0.18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(NovellumPrimaryContainer),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        decorationBox = { inner ->
                            Box {
                                if (value.text.isEmpty()) {
                                    Text(
                                        "Begin this scene…",
                                        fontFamily = ManuscriptSerif,
                                        fontStyle = FontStyle.Italic,
                                        fontSize = fontSizePt.sp,
                                        color = NovellumOutline.copy(alpha = 0.6f)
                                    )
                                }
                                inner()
                            }
                        }
                    )
                }

                Box(Modifier.fillMaxWidth().height(1.dp).background(NovellumOutlineVariant.copy(alpha = 0.25f)))
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Scene ${scenePosition + 1} of $sceneCount · %,d Words · %,d Characters".format(wordCount(value.text), value.text.length),
                        fontFamily = UiSans,
                        fontSize = 11.sp,
                        color = NovellumOutline
                    )
                    Text(
                        if (isZenMode) "Sanctuary Mode" else "Drafting Mode",
                        fontFamily = UiSans,
                        fontSize = 11.sp,
                        color = if (isZenMode) NovellumPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            containerColor = NovellumSurfaceContainer,
            title = {
                Text("Clear this scene?", fontFamily = ManuscriptSerif, fontSize = 19.sp, color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Text(
                    "Novellum blocked an empty overwrite. Confirm only if removing all prose is intentional. The existing checkpoint and revision safeguards remain in control of the write.",
                    fontFamily = UiSans,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirmation = false
                    onConfirmIntentionalClear()
                }) { Text("Clear intentionally", color = NovellumBlocked) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) { Text("Cancel", color = NovellumOutline) }
            }
        )
    }
}

@Composable
private fun EditorSubheader(
    scene: SceneEntity,
    scenePosition: Int,
    saveState: SaveState,
    lastSavedTime: Long?,
    canUndo: Boolean,
    canRedo: Boolean,
    fontSizePt: Int,
    isZenMode: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onQuote: () -> Unit,
    onBullets: () -> Unit,
    onNumbered: () -> Unit,
    onCycleFontSize: () -> Unit,
    onToggleZen: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(NovellumSurface.copy(alpha = 0.97f))
            .border(1.dp, NovellumOutlineVariant.copy(alpha = 0.25f))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Scene ${scenePosition + 1}",
                fontFamily = ManuscriptSerif,
                fontSize = 17.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onSurface
            )
            sceneCustomTitle(scene)?.let { custom ->
                Spacer(Modifier.width(10.dp))
                Box(Modifier.width(1.dp).height(14.dp).background(NovellumOutlineVariant.copy(alpha = 0.4f)))
                Spacer(Modifier.width(10.dp))
                Text(
                    custom,
                    fontFamily = ManuscriptSerif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 190.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            SaveBadge(saveState, lastSavedTime, onSave)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            FormattingButton(Icons.Default.FormatBold, "Bold", onBold)
            FormattingButton(Icons.Default.FormatItalic, "Italic", onItalic)
            FormattingButton(Icons.Default.FormatQuote, "Quote", onQuote)
            FormattingButton(Icons.Default.FormatListBulleted, "Bulleted list", onBullets)
            FormattingButton(Icons.Default.FormatListNumbered, "Numbered list", onNumbered)
            Spacer(Modifier.width(4.dp))
            FormattingButton(Icons.Default.Undo, "Undo", onUndo, canUndo)
            FormattingButton(Icons.Default.Redo, "Redo", onRedo, canRedo)
            Spacer(Modifier.width(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(NovellumSurfaceContainerLow)
                    .border(1.dp, NovellumOutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    .clickable(onClick = onCycleFontSize)
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Icon(Icons.Default.FormatSize, null, tint = NovellumPrimary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
                Text("Garamond · ${fontSizePt}pt", fontFamily = UiSans, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.width(4.dp))
            FormattingButton(Icons.Default.Save, "Save now", onSave)
            FormattingButton(Icons.Default.CenterFocusStrong, "Sanctuary mode", onToggleZen, active = isZenMode)
        }
    }
}

@Composable
private fun FormattingButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    active: Boolean = false
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(31.dp)) {
        Icon(
            icon,
            description,
            tint = when {
                active -> NovellumPrimary
                enabled -> NovellumOutline
                else -> NovellumOutline.copy(alpha = 0.28f)
            },
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun SaveBadge(saveState: SaveState, lastSavedTime: Long?, onClick: () -> Unit) {
    val (label, color) = when (saveState) {
        SaveState.SAVED -> {
            val time = lastSavedTime?.let { DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it)) }
            (time?.let { "Saved $it" } ?: "Saved") to NovellumEmeraldPulse
        }
        SaveState.UNSAVED -> "Unsaved" to NovellumWarning
        SaveState.AUTOSAVING -> "Saving…" to NovellumPrimary
        SaveState.BLOCKED_EMPTY_CLEAR -> "Clear blocked" to NovellumBlocked
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(NovellumSurfaceContainerLow)
            .border(1.dp, NovellumOutlineVariant.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Box(Modifier.size(5.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(5.dp))
        Text(label, fontFamily = UiSans, fontSize = 10.sp, color = color)
    }
}

@Composable
private fun OrnamentDivider() {
    Row(
        modifier = Modifier.width(200.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.weight(1f).height(1.dp).background(
                Brush.horizontalGradient(listOf(Color.Transparent, NovellumOutlineVariant))
            )
        )
        Text("✦ § ✦", fontFamily = ManuscriptSerif, fontSize = 12.sp, color = NovellumPrimary, modifier = Modifier.padding(horizontal = 8.dp))
        Box(
            Modifier.weight(1f).height(1.dp).background(
                Brush.horizontalGradient(listOf(NovellumOutlineVariant, Color.Transparent))
            )
        )
    }
}

@Composable
private fun ScopeOverview(
    project: ProjectEntity?,
    chapter: ChapterEntity?,
    chapterPosition: Int,
    scenes: List<SceneEntity>,
    onSelectScene: (String) -> Unit,
    onCreateChapter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.background(NovellumSurface), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth().padding(horizontal = 48.dp, vertical = 54.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                project == null -> {
                    Text("NOVELLUM", fontFamily = UiSans, fontSize = 11.sp, fontWeight = FontWeight.Light, letterSpacing = 3.sp, color = NovellumPrimary)
                    Spacer(Modifier.height(8.dp))
                    Text("Choose a manuscript", fontFamily = ManuscriptSerif, fontSize = 30.sp, fontWeight = FontWeight.Light, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(12.dp))
                    OrnamentDivider()
                    Spacer(Modifier.height(20.dp))
                    Text("Select a manuscript from the left index to begin.", fontFamily = UiSans, fontSize = 12.sp, color = NovellumOutline)
                }
                chapter != null -> {
                    Text("CHAPTER ${romanNumeral(chapterPosition + 1)}", fontFamily = UiSans, fontSize = 11.sp, fontWeight = FontWeight.Light, letterSpacing = 3.sp, color = NovellumPrimary)
                    Spacer(Modifier.height(6.dp))
                    Text(chapterHeadingTitle(chapter, chapterPosition), fontFamily = ManuscriptSerif, fontSize = 32.sp, fontWeight = FontWeight.Light, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(10.dp))
                    OrnamentDivider()
                    Spacer(Modifier.height(30.dp))
                    scenes.forEachIndexed { index, scene ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onSelectScene(scene.id) }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("%02d".format(index + 1), fontFamily = UiSans, fontSize = 10.sp, color = NovellumPrimary, modifier = Modifier.width(32.dp))
                            Text(sceneDisplayTitle(scene, index), fontFamily = ManuscriptSerif, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            Text("%,d wds".format(wordCount(scene.prose)), fontFamily = UiSans, fontSize = 10.sp, color = NovellumOutline)
                        }
                    }
                }
                else -> {
                    Text("MANUSCRIPT", fontFamily = UiSans, fontSize = 11.sp, fontWeight = FontWeight.Light, letterSpacing = 3.sp, color = NovellumPrimary)
                    Spacer(Modifier.height(8.dp))
                    Text(projectDisplayTitle(project), fontFamily = ManuscriptSerif, fontSize = 32.sp, fontWeight = FontWeight.Light, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(10.dp))
                    OrnamentDivider()
                    Spacer(Modifier.height(22.dp))
                    Text("Choose a chapter or scene from the manuscript index.", fontFamily = UiSans, fontSize = 12.sp, color = NovellumOutline)
                    Spacer(Modifier.height(18.dp))
                    TextButton(onClick = onCreateChapter) { Text("Create next chapter", color = NovellumPrimary) }
                }
            }
        }
    }
}

private fun romanNumeral(value: Int): String {
    if (value <= 0) return value.toString()
    var number = value
    val pairs = listOf(
        1000 to "M", 900 to "CM", 500 to "D", 400 to "CD", 100 to "C", 90 to "XC",
        50 to "L", 40 to "XL", 10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I"
    )
    return buildString {
        for ((amount, glyph) in pairs) {
            while (number >= amount) {
                append(glyph)
                number -= amount
            }
        }
    }
}

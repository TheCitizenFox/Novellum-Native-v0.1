package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.ChapterEntity
import com.example.data.entity.SceneEntity
import com.example.ui.viewmodel.EditorViewModel
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

private val NovellumBlack = Color(0xFF0B0B0C)
private val NovellumHeader = Color(0xFF111113)
private val NovellumSidebar = Color(0xFF151517)
private val NovellumCanvas = Color(0xFF0F0F11)
private val NovellumEditor = Color(0xFF121214)
private val NovellumLine = Color(0xFF2A2A2D)
private val NovellumLineSoft = Color(0xFF222225)
private val NovellumText = Color(0xFFE7E4DF)
private val NovellumTextSoft = Color(0xFFA5A29D)
private val NovellumTextDim = Color(0xFF6F6D69)
private val NovellumAccent = Color(0xFFC97942)
private val NovellumAccentSoft = Color(0xFF2B211B)
private val NovellumDanger = Color(0xFFC86D72)

private sealed class CreateRequest {
    data object Project : CreateRequest()
    data object Chapter : CreateRequest()
    data class Scene(val chapterId: String) : CreateRequest()
}

@Composable
fun EditorShellScreen(viewModel: EditorViewModel) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val selectedProjectId by viewModel.selectedProjectId.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val projectScenes by viewModel.projectScenes.collectAsStateWithLifecycle()
    val selectedSceneId by viewModel.selectedSceneId.collectAsStateWithLifecycle()
    val currentScene by viewModel.currentScene.collectAsStateWithLifecycle()
    val uiMessage by viewModel.uiMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var createRequest by remember { mutableStateOf<CreateRequest?>(null) }

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUiMessage()
        }
    }

    val selectedProject = projects.firstOrNull { it.id == selectedProjectId }
    val selectedChapter = currentScene?.let { scene ->
        chapters.firstOrNull { it.id == scene.chapterId }
    }

    Scaffold(
        containerColor = NovellumBlack,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(NovellumBlack)
        ) {
            NovellumTopBar(hasScene = currentScene != null)

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NovellumCanvas)
            ) {
                ManuscriptSidebar(
                    modifier = Modifier
                        .width(292.dp)
                        .fillMaxHeight(),
                    projectTitle = selectedProject?.title,
                    selectedProjectId = selectedProjectId,
                    selectedSceneId = selectedSceneId,
                    projects = projects.map { it.id to it.title },
                    chapters = chapters,
                    scenes = projectScenes,
                    onRequestCreateProject = { createRequest = CreateRequest.Project },
                    onSelectProject = viewModel::selectProject,
                    onRenameProject = viewModel::renameProject,
                    onBackToProjects = viewModel::clearProjectSelection,
                    onRequestCreateChapter = { createRequest = CreateRequest.Chapter },
                    onRequestCreateScene = { chapterId ->
                        createRequest = CreateRequest.Scene(chapterId)
                    },
                    onSelectScene = viewModel::selectScene,
                    onRenameChapter = viewModel::renameChapter,
                    onRenameScene = viewModel::renameScene
                )

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(NovellumLine)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(NovellumCanvas)
                ) {
                    val scene = currentScene
                    if (scene == null) {
                        EmptyEditorState()
                    } else {
                        SceneEditor(
                            scene = scene,
                            chapterTitle = selectedChapter?.title,
                            onRenameScene = { title ->
                                viewModel.renameScene(scene.id, title)
                            },
                            onSave = { text, clearIntent ->
                                viewModel.saveSceneProse(
                                    sceneId = scene.id,
                                    newProse = text,
                                    isUserIntentClear = clearIntent
                                )
                            },
                            onDelete = { viewModel.deleteScene(scene.id) }
                        )
                    }
                }
            }
        }
    }

    createRequest?.let { request ->
        val title = when (request) {
            CreateRequest.Project -> "New Project"
            CreateRequest.Chapter -> "New Chapter"
            is CreateRequest.Scene -> "New Scene"
        }
        val label = when (request) {
            CreateRequest.Project -> "Project title"
            CreateRequest.Chapter -> "Chapter title"
            is CreateRequest.Scene -> "Scene title"
        }

        NameDialog(
            title = title,
            label = label,
            onDismiss = { createRequest = null },
            onConfirm = { name ->
                when (request) {
                    CreateRequest.Project -> viewModel.createProject(name)
                    CreateRequest.Chapter -> viewModel.createChapter(name)
                    is CreateRequest.Scene -> viewModel.createScene(request.chapterId, name)
                }
                createRequest = null
            }
        )
    }
}

@Composable
private fun NovellumTopBar(hasScene: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(NovellumHeader)
            .border(width = 1.dp, color = NovellumLineSoft)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "NOVELLUM",
            color = NovellumText,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.2.sp,
            modifier = Modifier.width(258.dp)
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopNavItem("Library", false)
            TopNavItem("Vault", false)
            TopNavItem("Editor", true)
            TopNavItem("Cards", false)
            TopNavItem("Manuscript", false)
        }

        Row(
            modifier = Modifier.width(258.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        if (hasScene) NovellumAccent else NovellumTextDim,
                        RoundedCornerShape(50)
                    )
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (hasScene) "Saved locally" else "No scene selected",
                color = NovellumTextSoft,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun TopNavItem(label: String, active: Boolean) {
    Column(
        modifier = Modifier
            .height(56.dp)
            .padding(horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = if (active) NovellumText else NovellumTextDim,
            fontSize = 12.sp,
            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .height(2.dp)
                .width(26.dp)
                .background(if (active) NovellumAccent else Color.Transparent)
        )
    }
}

@Composable
private fun ManuscriptSidebar(
    modifier: Modifier,
    projectTitle: String?,
    selectedProjectId: String?,
    selectedSceneId: String?,
    projects: List<Pair<String, String>>,
    chapters: List<ChapterEntity>,
    scenes: List<SceneEntity>,
    onRequestCreateProject: () -> Unit,
    onSelectProject: (String) -> Unit,
    onRenameProject: (String, String) -> Unit,
    onBackToProjects: () -> Unit,
    onRequestCreateChapter: () -> Unit,
    onRequestCreateScene: (String) -> Unit,
    onSelectScene: (String) -> Unit,
    onRenameChapter: (String, String) -> Unit,
    onRenameScene: (String, String) -> Unit
) {
    Column(modifier = modifier.background(NovellumSidebar)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MANUSCRIPT",
                color = NovellumTextDim,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                modifier = Modifier.weight(1f)
            )

            if (selectedProjectId == null) {
                MiniAction("+", onClick = onRequestCreateProject)
            } else {
                MiniAction("‹ Projects", onClick = onBackToProjects)
            }
        }

        DividerLine()

        if (selectedProjectId == null) {
            ProjectList(
                projects = projects,
                onRequestCreateProject = onRequestCreateProject,
                onSelectProject = onSelectProject,
                onRenameProject = onRenameProject
            )
        } else {
            ProjectManuscript(
                projectId = selectedProjectId,
                projectTitle = projectTitle ?: "Untitled Project",
                chapters = chapters,
                scenes = scenes,
                selectedSceneId = selectedSceneId,
                onRenameProject = onRenameProject,
                onRequestCreateChapter = onRequestCreateChapter,
                onRequestCreateScene = onRequestCreateScene,
                onSelectScene = onSelectScene,
                onRenameChapter = onRenameChapter,
                onRenameScene = onRenameScene
            )
        }
    }
}

@Composable
private fun ProjectList(
    projects: List<Pair<String, String>>,
    onRequestCreateProject: () -> Unit,
    onSelectProject: (String) -> Unit,
    onRenameProject: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Projects",
                color = NovellumText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            MiniAction("+ New", onClick = onRequestCreateProject)
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(projects, key = { it.first }) { project ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NovellumEditor, RoundedCornerShape(5.dp))
                        .border(1.dp, NovellumLineSoft, RoundedCornerShape(5.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("▱", color = NovellumAccent, fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    InlineTitle(
                        id = project.first,
                        title = project.second,
                        modifier = Modifier.weight(1f),
                        color = NovellumText,
                        fontSize = 13.sp,
                        onTap = { onSelectProject(project.first) },
                        onRename = { onRenameProject(project.first, it) }
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Text(
            "Tap to open • hold a title to rename",
            color = NovellumTextDim,
            fontSize = 9.sp,
            modifier = Modifier.padding(horizontal = 3.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun ProjectManuscript(
    projectId: String,
    projectTitle: String,
    chapters: List<ChapterEntity>,
    scenes: List<SceneEntity>,
    selectedSceneId: String?,
    onRenameProject: (String, String) -> Unit,
    onRequestCreateChapter: () -> Unit,
    onRequestCreateScene: (String) -> Unit,
    onSelectScene: (String) -> Unit,
    onRenameChapter: (String, String) -> Unit,
    onRenameScene: (String, String) -> Unit
) {
    var projectExpanded by rememberSaveable(projectId) { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DisclosureChevron(
                expanded = projectExpanded,
                visible = chapters.isNotEmpty(),
                onClick = { projectExpanded = !projectExpanded }
            )

            InlineTitle(
                id = projectId,
                title = projectTitle,
                modifier = Modifier.weight(1f),
                color = NovellumText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                onRename = { onRenameProject(projectId, it) }
            )

            MiniAction("+", onClick = onRequestCreateChapter)
        }

        if (projectExpanded) {
            Text(
                text = "CHAPTERS",
                color = NovellumTextDim,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.3.sp,
                modifier = Modifier.padding(start = 36.dp, top = 4.dp, bottom = 4.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 12.dp)
            ) {
                items(chapters, key = { it.id }) { chapter ->
                    ChapterBlock(
                        chapter = chapter,
                        scenes = scenes.filter { it.chapterId == chapter.id },
                        selectedSceneId = selectedSceneId,
                        onRequestCreateScene = { onRequestCreateScene(chapter.id) },
                        onSelectScene = onSelectScene,
                        onRenameChapter = { onRenameChapter(chapter.id, it) },
                        onRenameScene = onRenameScene
                    )
                }
            }
        } else {
            Spacer(Modifier.weight(1f))
        }

        DividerLine()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Backup JSON", color = NovellumTextDim, fontSize = 9.sp)
            Text("  •  ", color = NovellumLine, fontSize = 9.sp)
            Text("Export MD", color = NovellumTextDim, fontSize = 9.sp)
            Spacer(Modifier.weight(1f))
            Text("hold title = rename", color = NovellumTextDim, fontSize = 8.sp)
        }
    }
}

@Composable
private fun ChapterBlock(
    chapter: ChapterEntity,
    scenes: List<SceneEntity>,
    selectedSceneId: String?,
    onRequestCreateScene: () -> Unit,
    onSelectScene: (String) -> Unit,
    onRenameChapter: (String) -> Unit,
    onRenameScene: (String, String) -> Unit
) {
    var expanded by rememberSaveable(chapter.id) { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DisclosureChevron(
                expanded = expanded,
                visible = scenes.isNotEmpty(),
                onClick = { expanded = !expanded }
            )

            InlineTitle(
                id = chapter.id,
                title = chapter.title,
                modifier = Modifier.weight(1f),
                color = NovellumTextSoft,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                uppercaseWhenDisplay = true,
                onRename = onRenameChapter
            )

            MiniAction("+", onClick = onRequestCreateScene)
        }

        if (expanded) {
            scenes.forEach { scene ->
                val selected = scene.id == selectedSceneId

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 27.dp, top = 1.dp, bottom = 1.dp)
                        .background(
                            if (selected) NovellumAccentSoft else Color.Transparent,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 7.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(17.dp)
                            .background(
                                if (selected) NovellumAccent else Color.Transparent,
                                RoundedCornerShape(50)
                            )
                    )
                    Spacer(Modifier.width(8.dp))

                    InlineTitle(
                        id = scene.id,
                        title = scene.title,
                        modifier = Modifier.weight(1f),
                        color = if (selected) NovellumText else NovellumTextSoft,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                        onTap = { onSelectScene(scene.id) },
                        onRename = { onRenameScene(scene.id, it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DisclosureChevron(
    expanded: Boolean,
    visible: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(27.dp)
            .height(30.dp),
        contentAlignment = Alignment.Center
    ) {
        if (visible) {
            Text(
                text = if (expanded) "⌄" else "›",
                color = NovellumTextDim,
                fontSize = 15.sp,
                modifier = Modifier
                    .clickable(onClick = onClick)
                    .padding(horizontal = 7.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
private fun InlineTitle(
    id: String,
    title: String,
    modifier: Modifier = Modifier,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight = FontWeight.Normal,
    uppercaseWhenDisplay: Boolean = false,
    onTap: (() -> Unit)? = null,
    onRename: (String) -> Unit
) {
    var editing by remember(id) { mutableStateOf(false) }
    var draft by remember(id, title) { mutableStateOf(title) }
    var hadFocus by remember(id) { mutableStateOf(false) }
    val focusRequester = remember(id) { FocusRequester() }

    fun commit() {
        val clean = draft.trim()
        hadFocus = false
        editing = false
        if (clean.isNotEmpty() && clean != title) onRename(clean)
        else draft = title
    }

    if (editing) {
        BackHandler(enabled = true) {
            hadFocus = false
            draft = title
            editing = false
        }

        BasicTextField(
            value = draft,
            onValueChange = { draft = it },
            singleLine = true,
            textStyle = TextStyle(
                color = NovellumText,
                fontSize = fontSize,
                fontWeight = fontWeight
            ),
            cursorBrush = SolidColor(NovellumAccent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { commit() }),
            modifier = modifier
                .background(NovellumBlack, RoundedCornerShape(3.dp))
                .border(1.dp, NovellumAccent, RoundedCornerShape(3.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        hadFocus = true
                    } else if (hadFocus && editing) {
                        commit()
                    }
                }
        )

        LaunchedEffect(editing) {
            if (editing) focusRequester.requestFocus()
        }
    } else {
        Text(
            text = if (uppercaseWhenDisplay) title.uppercase() else title,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier.pointerInput(id, title) {
                detectTapGestures(
                    onTap = { onTap?.invoke() },
                    onLongPress = {
                        draft = title
                        editing = true
                    }
                )
            }
        )
    }
}

@Composable
private fun SceneEditor(
    scene: SceneEntity,
    chapterTitle: String?,
    onRenameScene: (String) -> Unit,
    onSave: (String, Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var editorValue by remember(scene.id) {
        mutableStateOf(
            TextFieldValue(
                text = scene.prose,
                selection = TextRange(scene.prose.length)
            )
        )
    }
    val undoStack = remember(scene.id) { mutableStateListOf<TextFieldValue>() }
    val redoStack = remember(scene.id) { mutableStateListOf<TextFieldValue>() }
    var showDeleteConfirm by remember(scene.id) { mutableStateOf(false) }
    var saveLabel by remember(scene.id) { mutableStateOf("Autosaved") }
    val clipboard = LocalClipboardManager.current

    val isDirty = editorValue.text != scene.prose
    val isClearingExistingText = editorValue.text.isEmpty() && scene.prose.isNotEmpty()

    fun pushUndo(value: TextFieldValue) {
        undoStack.add(value)
        if (undoStack.size > 120) undoStack.removeAt(0)
    }

    fun applyValue(newValue: TextFieldValue, recordUndo: Boolean = true) {
        if (newValue == editorValue) return
        if (recordUndo && newValue.text != editorValue.text) {
            pushUndo(editorValue)
            redoStack.clear()
        }
        editorValue = newValue
    }

    fun selectionBounds(): Pair<Int, Int> {
        val start = min(editorValue.selection.start, editorValue.selection.end)
        val end = max(editorValue.selection.start, editorValue.selection.end)
        return start to end
    }

    fun replaceSelection(replacement: String, cursorOffset: Int = replacement.length) {
        val (start, end) = selectionBounds()
        val newText = editorValue.text.substring(0, start) +
            replacement +
            editorValue.text.substring(end)
        val cursor = start + cursorOffset
        applyValue(
            TextFieldValue(
                text = newText,
                selection = TextRange(cursor.coerceIn(0, newText.length))
            )
        )
    }

    fun copySelection() {
        val (start, end) = selectionBounds()
        if (start < end) {
            clipboard.setText(AnnotatedString(editorValue.text.substring(start, end)))
        }
    }

    fun cutSelection() {
        val (start, end) = selectionBounds()
        if (start < end) {
            clipboard.setText(AnnotatedString(editorValue.text.substring(start, end)))
            replaceSelection("")
        }
    }

    fun pasteClipboard() {
        val text = clipboard.getText()?.text ?: return
        if (text.isNotEmpty()) replaceSelection(text)
    }

    fun wrapSelection(marker: String) {
        val (start, end) = selectionBounds()
        if (start < end) {
            val selected = editorValue.text.substring(start, end)
            val replacement = marker + selected + marker
            val newText = editorValue.text.substring(0, start) +
                replacement +
                editorValue.text.substring(end)

            applyValue(
                TextFieldValue(
                    text = newText,
                    selection = TextRange(
                        start + marker.length,
                        start + marker.length + selected.length
                    )
                )
            )
        } else {
            val replacement = marker + marker
            replaceSelection(replacement, marker.length)
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val previous = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(editorValue)
        editorValue = previous
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val next = redoStack.removeAt(redoStack.lastIndex)
        pushUndo(editorValue)
        editorValue = next
    }

    LaunchedEffect(editorValue.text, scene.prose, scene.id) {
        when {
            editorValue.text == scene.prose -> saveLabel = "Autosaved"
            isClearingExistingText -> saveLabel = "Clear needs confirmation"
            else -> {
                saveLabel = "Unsaved"
                delay(1100)
                if (editorValue.text != scene.prose && editorValue.text.isNotEmpty()) {
                    saveLabel = "Saving…"
                    onSave(editorValue.text, false)
                }
            }
        }
    }

    val wordCount = remember(editorValue.text) {
        if (editorValue.text.isBlank()) 0
        else editorValue.text.trim().split(Regex("\\s+")).size
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NovellumCanvas)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .background(NovellumEditor)
                .padding(horizontal = 26.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                InlineTitle(
                    id = scene.id + "-header",
                    title = scene.title,
                    color = NovellumText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    onRename = onRenameScene
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = chapterTitle ?: "Scene",
                    color = NovellumTextDim,
                    fontSize = 10.sp
                )
            }

            Text(
                text = saveLabel,
                color = when (saveLabel) {
                    "Autosaved" -> NovellumTextDim
                    "Saving…" -> NovellumTextSoft
                    else -> NovellumAccent
                },
                fontSize = 10.sp
            )
        }

        DividerLine()

        EditorToolbar(
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty(),
            hasSelection = editorValue.selection.start != editorValue.selection.end,
            onUndo = ::undo,
            onRedo = ::redo,
            onCut = ::cutSelection,
            onCopy = ::copySelection,
            onPaste = ::pasteClipboard,
            onBold = { wrapSelection("**") },
            onItalic = { wrapSelection("_") },
            onSaveNow = {
                if (!isClearingExistingText && isDirty) {
                    onSave(editorValue.text, false)
                }
            }
        )

        DividerLine()

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(NovellumCanvas)
                .padding(horizontal = 28.dp, vertical = 18.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 840.dp)
                    .fillMaxWidth()
                    .background(NovellumEditor, RoundedCornerShape(5.dp))
                    .border(1.dp, NovellumLineSoft, RoundedCornerShape(5.dp))
                    .padding(horizontal = 36.dp, vertical = 25.dp)
            ) {
                BasicTextField(
                    value = editorValue,
                    onValueChange = { applyValue(it) },
                    modifier = Modifier.fillMaxSize(),
                    textStyle = TextStyle(
                        color = NovellumText,
                        fontSize = 16.sp,
                        lineHeight = 27.sp
                    ),
                    cursorBrush = SolidColor(NovellumAccent),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (editorValue.text.isEmpty()) {
                                Text(
                                    "Write here…",
                                    color = NovellumTextDim,
                                    fontSize = 16.sp,
                                    lineHeight = 27.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(NovellumHeader)
                .border(1.dp, NovellumLineSoft)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "$wordCount word${if (wordCount == 1) "" else "s"}",
                color = NovellumTextDim,
                fontSize = 9.sp
            )

            Spacer(Modifier.weight(1f))

            if (isClearingExistingText) {
                MiniAction(
                    label = "Confirm clear",
                    accent = NovellumDanger,
                    onClick = { onSave(editorValue.text, true) }
                )
                Spacer(Modifier.width(14.dp))
            }

            Text(
                "Delete scene",
                color = NovellumDanger,
                fontSize = 9.sp,
                modifier = Modifier
                    .clickable { showDeleteConfirm = true }
                    .padding(vertical = 7.dp)
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = NovellumSidebar,
            titleContentColor = NovellumText,
            textContentColor = NovellumTextSoft,
            title = { Text("Delete Scene?") },
            text = {
                Text(
                    "This removes “${scene.title}” from the manuscript. " +
                        "This action cannot be undone from this screen."
                )
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = NovellumTextSoft)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = NovellumDanger)
                }
            }
        )
    }
}

@Composable
private fun EditorToolbar(
    canUndo: Boolean,
    canRedo: Boolean,
    hasSelection: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onCut: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onSaveNow: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(NovellumHeader)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolButton("↶", enabled = canUndo, onClick = onUndo)
        ToolButton("↷", enabled = canRedo, onClick = onRedo)
        ToolbarDivider()
        ToolButton("Cut", enabled = hasSelection, onClick = onCut)
        ToolButton("Copy", enabled = hasSelection, onClick = onCopy)
        ToolButton("Paste", onClick = onPaste)
        ToolbarDivider()
        ToolButton("B", fontWeight = FontWeight.Bold, onClick = onBold)
        ToolButton("I", fontStyle = FontStyle.Italic, onClick = onItalic)
        ToolbarDivider()
        ToolButton("Save", onClick = onSaveNow)
    }
}

@Composable
private fun ToolButton(
    label: String,
    enabled: Boolean = true,
    fontWeight: FontWeight = FontWeight.Medium,
    fontStyle: FontStyle = FontStyle.Normal,
    onClick: () -> Unit
) {
    Text(
        text = label,
        color = if (enabled) NovellumTextSoft else NovellumTextDim.copy(alpha = 0.45f),
        fontSize = 11.sp,
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 8.dp)
    )
}

@Composable
private fun ToolbarDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .width(1.dp)
            .height(17.dp)
            .background(NovellumLine)
    )
}

@Composable
private fun NameDialog(
    title: String,
    label: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember(title) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NovellumSidebar,
        titleContentColor = NovellumText,
        textContentColor = NovellumTextSoft,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (value.isNotBlank()) onConfirm(value)
                    }
                )
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = NovellumTextSoft)
            }
        },
        confirmButton = {
            TextButton(
                enabled = value.isNotBlank(),
                onClick = { onConfirm(value) }
            ) {
                Text(
                    "Create",
                    color = if (value.isNotBlank()) NovellumAccent else NovellumTextDim
                )
            }
        }
    )
}

@Composable
private fun EmptyEditorState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NovellumCanvas),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("✎", color = NovellumTextDim, fontSize = 30.sp)
            Spacer(Modifier.height(13.dp))
            Text(
                "Select a scene to start writing.",
                color = NovellumTextSoft,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Choose a scene from the manuscript panel.",
                color = NovellumTextDim,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(NovellumLineSoft)
    )
}

@Composable
private fun MiniAction(
    label: String,
    accent: Color = NovellumAccent,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Text(
        text = label,
        color = if (enabled) accent else NovellumTextDim,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 6.dp)
    )
}

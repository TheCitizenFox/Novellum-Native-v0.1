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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.ChapterEntity
import com.example.data.entity.SceneEntity
import com.example.ui.viewmodel.EditorViewModel
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

private val NovellumBlack = Color(0xFF0A0B0D)
private val NovellumHeader = Color(0xFF0F1114)
private val NovellumSidebar = Color(0xFF131519)
private val NovellumCanvas = Color(0xFF0C0E11)
private val NovellumEditor = Color(0xFF101318)
private val NovellumEditorRaised = Color(0xFF141821)
private val NovellumLine = Color(0xFF2B3038)
private val NovellumLineSoft = Color(0xFF1B1F25)
private val NovellumText = Color(0xFFE8E2D8)
private val NovellumTextSoft = Color(0xFFBBB2A6)
private val NovellumTextDim = Color(0xFF8C857D)
private val NovellumAccent = Color(0xFFC97942)
private val NovellumAccentSoft = Color(0xFF2B211B)
private val NovellumDanger = Color(0xFFC86D72)

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
    var manuscriptPanelOpen by rememberSaveable { mutableStateOf(true) }
    var workspacePanelOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUiMessage()
        }
    }

    val selectedProject = projects.firstOrNull { it.id == selectedProjectId }
    val selectedProjectIndex = projects.indexOfFirst { it.id == selectedProjectId }.coerceAtLeast(0)
    val selectedProjectDisplayTitle = selectedProject?.let {
        projectDisplayTitle(it.title, selectedProjectIndex)
    }
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
            NovellumTopBar(
                hasScene = currentScene != null,
                manuscriptPanelOpen = manuscriptPanelOpen,
                workspacePanelOpen = workspacePanelOpen,
                onToggleManuscript = { manuscriptPanelOpen = !manuscriptPanelOpen },
                onToggleWorkspace = { workspacePanelOpen = !workspacePanelOpen }
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NovellumCanvas)
            ) {
                if (manuscriptPanelOpen) {
                    ManuscriptSidebar(
                        modifier = Modifier
                            .width(314.dp)
                            .fillMaxHeight(),
                        projectTitle = selectedProject?.title,
                        projectDisplayTitle = selectedProjectDisplayTitle,
                        selectedProjectId = selectedProjectId,
                        selectedSceneId = selectedSceneId,
                        projects = projects.map { it.id to it.title },
                        chapters = chapters,
                        scenes = projectScenes,
                        onCreateProject = viewModel::createNextProject,
                        onSelectProject = viewModel::selectProject,
                        onRenameProject = viewModel::renameProject,
                        onDeleteProject = viewModel::deleteProject,
                        onBackToProjects = viewModel::clearProjectSelection,
                        onCreateChapter = viewModel::createNextChapter,
                        onCreateScene = viewModel::createNextScene,
                        onSelectScene = viewModel::selectScene,
                        onRenameChapter = viewModel::renameChapter,
                        onDeleteChapter = viewModel::deleteChapter,
                        onRenameScene = viewModel::renameScene,
                        onDeleteScene = viewModel::deleteScene
                    )
                    WorkspaceRail(symbol = "‹", onClick = { manuscriptPanelOpen = false })
                } else {
                    WorkspaceRail(symbol = "›", onClick = { manuscriptPanelOpen = true })
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(NovellumCanvas)
                ) {
                    val scene = currentScene
                    if (scene == null) {
                        EmptyEditorState(
                            manuscriptPanelOpen = manuscriptPanelOpen,
                            workspacePanelOpen = workspacePanelOpen,
                            onOpenManuscript = { manuscriptPanelOpen = true },
                            onOpenWorkspace = { workspacePanelOpen = true }
                        )
                    } else {
                        val chapterIndex = chapters.indexOfFirst { it.id == scene.chapterId }
                        val scenesInChapter = projectScenes.filter { it.chapterId == scene.chapterId }
                        val sceneIndex = scenesInChapter.indexOfFirst { it.id == scene.id }

                        SceneEditor(
                            scene = scene,
                            chapterTitle = selectedChapter?.let {
                                chapterDisplayTitle(it.title, chapterIndex.coerceAtLeast(0))
                            },
                            sceneDisplayTitle = sceneDisplayTitle(
                                scene.title,
                                sceneIndex.coerceAtLeast(0)
                            ),
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

                if (workspacePanelOpen) {
                    WorkspaceRail(symbol = "›", onClick = { workspacePanelOpen = false })
                    AuxiliaryWorkspacePanel(
                        modifier = Modifier
                            .width(276.dp)
                            .fillMaxHeight(),
                        projectTitle = selectedProjectDisplayTitle,
                        chapterTitle = selectedChapter?.let {
                            chapterDisplayTitle(
                                it.title,
                                chapters.indexOfFirst { chapter -> chapter.id == it.id }.coerceAtLeast(0)
                            )
                        },
                        sceneTitle = currentScene?.let { scene ->
                            val scenesInChapter = projectScenes.filter { it.chapterId == scene.chapterId }
                            sceneDisplayTitle(
                                scene.title,
                                scenesInChapter.indexOfFirst { it.id == scene.id }.coerceAtLeast(0)
                            )
                        },
                        sceneWordCount = currentScene?.prose?.let(::countWords) ?: 0,
                        chapterWordCount = selectedChapter?.let { chapter ->
                            projectScenes.filter { it.chapterId == chapter.id }.sumOf { countWords(it.prose) }
                        } ?: 0,
                        projectWordCount = projectScenes.sumOf { countWords(it.prose) },
                        hasScene = currentScene != null
                    )
                } else {
                    WorkspaceRail(symbol = "‹", onClick = { workspacePanelOpen = true })
                }
            }
        }
    }
}

@Composable
private fun NovellumTopBar(
    hasScene: Boolean,
    manuscriptPanelOpen: Boolean,
    workspacePanelOpen: Boolean,
    onToggleManuscript: () -> Unit,
    onToggleWorkspace: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(NovellumHeader)
            .border(width = 1.dp, color = NovellumLineSoft)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.width(290.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(24.dp)
                    .background(NovellumAccent, RoundedCornerShape(50))
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "NOVELLUM",
                    color = NovellumText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.1.sp
                )
                Text(
                    text = "Native writing workspace",
                    color = NovellumTextDim,
                    fontSize = 9.sp,
                    letterSpacing = 0.4.sp
                )
            }
        }

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
            modifier = Modifier.widthIn(min = 310.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WorkspaceToggleChip(
                label = "Manuscript",
                active = manuscriptPanelOpen,
                onClick = onToggleManuscript
            )
            Spacer(Modifier.width(8.dp))
            WorkspaceToggleChip(
                label = "Workspace",
                active = workspacePanelOpen,
                onClick = onToggleWorkspace
            )
            Spacer(Modifier.width(14.dp))
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(
                        if (hasScene) NovellumAccent else NovellumTextDim,
                        RoundedCornerShape(50)
                    )
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (hasScene) "Saved locally" else "No scene selected",
                color = if (hasScene) NovellumTextSoft else NovellumTextDim,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun WorkspaceToggleChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                if (active) NovellumAccentSoft else NovellumEditor,
                RoundedCornerShape(999.dp)
            )
            .border(
                1.dp,
                if (active) NovellumAccent.copy(alpha = 0.55f) else NovellumLine,
                RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = if (active) NovellumText else NovellumTextSoft,
            fontSize = 10.sp,
            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun TopNavItem(label: String, active: Boolean) {
    Column(
        modifier = Modifier
            .height(60.dp)
            .padding(horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            color = if (active) NovellumText else NovellumTextSoft,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal
        )
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .height(3.dp)
                .width(32.dp)
                .background(
                    if (active) NovellumAccent else Color.Transparent,
                    RoundedCornerShape(50)
                )
        )
    }
}

private fun isDefaultProjectTitle(title: String): Boolean =
    Regex("(?i)^(new\\s+)?project(\\s+\\d+)?$").matches(title.trim())

private fun isDefaultChapterTitle(title: String): Boolean =
    Regex("(?i)^(new\\s+)?chapter(\\s+\\d+)?$").matches(title.trim())

private fun isDefaultSceneTitle(title: String): Boolean =
    Regex("(?i)^(new\\s+)?scene(\\s+\\d+)?$").matches(title.trim())

private fun projectDisplayTitle(title: String, index: Int): String =
    if (isDefaultProjectTitle(title)) "Project ${index + 1}" else title

private fun chapterDisplayTitle(title: String, index: Int): String =
    if (isDefaultChapterTitle(title)) "Chapter ${index + 1}"
    else "Ch ${index + 1} · $title"

private fun sceneDisplayTitle(title: String, index: Int): String =
    if (isDefaultSceneTitle(title)) "Scene ${index + 1}"
    else "Sc ${index + 1} · $title"

private fun countWords(text: String): Int =
    if (text.isBlank()) 0 else text.trim().split(Regex("\\s+")).size

@Composable
private fun ManuscriptSidebar(
    modifier: Modifier,
    projectTitle: String?,
    projectDisplayTitle: String?,
    selectedProjectId: String?,
    selectedSceneId: String?,
    projects: List<Pair<String, String>>,
    chapters: List<ChapterEntity>,
    scenes: List<SceneEntity>,
    onCreateProject: () -> Unit,
    onSelectProject: (String) -> Unit,
    onRenameProject: (String, String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onBackToProjects: () -> Unit,
    onCreateChapter: () -> Unit,
    onCreateScene: (String) -> Unit,
    onSelectScene: (String) -> Unit,
    onRenameChapter: (String, String) -> Unit,
    onDeleteChapter: (String) -> Unit,
    onRenameScene: (String, String) -> Unit,
    onDeleteScene: (String) -> Unit
) {
    Column(modifier = modifier.background(NovellumSidebar)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "MANUSCRIPT",
                    color = NovellumTextSoft,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp
                )
                Text(
                    text = if (selectedProjectId == null) "Project browser" else "Project structure",
                    color = NovellumTextDim,
                    fontSize = 9.sp
                )
            }

            if (selectedProjectId == null) {
                TreeControl("+", description = "New project", onClick = onCreateProject)
            } else {
                MiniAction("‹ Projects", onClick = onBackToProjects)
            }
        }

        DividerLine()

        if (selectedProjectId == null) {
            ProjectList(
                projects = projects,
                onCreateProject = onCreateProject,
                onSelectProject = onSelectProject,
                onRenameProject = onRenameProject
            )
        } else {
            ProjectManuscript(
                projectId = selectedProjectId,
                projectTitle = projectTitle ?: "Project",
                projectDisplayTitle = projectDisplayTitle ?: "Project",
                chapters = chapters,
                scenes = scenes,
                selectedSceneId = selectedSceneId,
                onRenameProject = onRenameProject,
                onDeleteProject = { onDeleteProject(selectedProjectId) },
                onCreateChapter = onCreateChapter,
                onCreateScene = onCreateScene,
                onSelectScene = onSelectScene,
                onRenameChapter = onRenameChapter,
                onDeleteChapter = onDeleteChapter,
                onRenameScene = onRenameScene,
                onDeleteScene = onDeleteScene
            )
        }
    }
}

@Composable
private fun ProjectList(
    projects: List<Pair<String, String>>,
    onCreateProject: () -> Unit,
    onSelectProject: (String) -> Unit,
    onRenameProject: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 12.dp)
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
            TreeControl("+", description = "New project", onClick = onCreateProject)
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            itemsIndexed(projects, key = { _, item -> item.first }) { index, project ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NovellumEditor, RoundedCornerShape(6.dp))
                        .border(1.dp, NovellumLineSoft, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("▱", color = NovellumAccent, fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    ManageableTitle(
                        id = project.first,
                        rawTitle = project.second,
                        displayTitle = projectDisplayTitle(project.second, index),
                        emptyDraftWhenDefault = isDefaultProjectTitle(project.second),
                        modifier = Modifier.weight(1f),
                        color = NovellumText,
                        fontSize = 13.sp,
                        onTap = { onSelectProject(project.first) },
                        onRename = { onRenameProject(project.first, it) },
                        onDeleteRequest = null
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Text(
            "Tap to open • hold a title to manage",
            color = NovellumTextDim,
            fontSize = 9.sp,
            modifier = Modifier.padding(horizontal = 3.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ProjectManuscript(
    projectId: String,
    projectTitle: String,
    projectDisplayTitle: String,
    chapters: List<ChapterEntity>,
    scenes: List<SceneEntity>,
    selectedSceneId: String?,
    onRenameProject: (String, String) -> Unit,
    onDeleteProject: () -> Unit,
    onCreateChapter: () -> Unit,
    onCreateScene: (String) -> Unit,
    onSelectScene: (String) -> Unit,
    onRenameChapter: (String, String) -> Unit,
    onDeleteChapter: (String) -> Unit,
    onRenameScene: (String, String) -> Unit,
    onDeleteScene: (String) -> Unit
) {
    var projectExpanded by rememberSaveable(projectId) { mutableStateOf(true) }
    var showProjectDelete by remember(projectId) { mutableStateOf(false) }

    val projectIsEmpty = chapters.isEmpty() && scenes.isEmpty()
    val projectWordCount = scenes.sumOf { countWords(it.prose) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DisclosureChevron(
                expanded = projectExpanded,
                visible = chapters.isNotEmpty(),
                onClick = { projectExpanded = !projectExpanded }
            )

            ManageableTitle(
                id = projectId,
                rawTitle = projectTitle,
                displayTitle = projectDisplayTitle,
                emptyDraftWhenDefault = isDefaultProjectTitle(projectTitle),
                modifier = Modifier.weight(1f),
                color = NovellumText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                onRename = { onRenameProject(projectId, it) },
                onDeleteRequest = {
                    if (projectIsEmpty) onDeleteProject()
                    else showProjectDelete = true
                }
            )

            TreeControl(
                symbol = "+",
                description = "New chapter",
                onClick = {
                    projectExpanded = true
                    onCreateChapter()
                }
            )
        }

        if (projectExpanded) {
            Text(
                text = "CHAPTERS",
                color = NovellumTextSoft,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.3.sp,
                modifier = Modifier.padding(start = 42.dp, top = 2.dp, bottom = 6.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 12.dp)
            ) {
                itemsIndexed(chapters, key = { _, item -> item.id }) { chapterIndex, chapter ->
                    ChapterBlock(
                        chapter = chapter,
                        chapterIndex = chapterIndex,
                        scenes = scenes.filter { it.chapterId == chapter.id },
                        selectedSceneId = selectedSceneId,
                        onCreateScene = { onCreateScene(chapter.id) },
                        onSelectScene = onSelectScene,
                        onRenameChapter = { onRenameChapter(chapter.id, it) },
                        onDeleteChapter = { onDeleteChapter(chapter.id) },
                        onRenameScene = onRenameScene,
                        onDeleteScene = onDeleteScene
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
                .height(44.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Backup JSON", color = NovellumTextDim, fontSize = 9.sp)
            Text("  •  ", color = NovellumLine, fontSize = 9.sp)
            Text("Export MD", color = NovellumTextDim, fontSize = 9.sp)
            Spacer(Modifier.weight(1f))
            Text("hold title = manage", color = NovellumTextDim, fontSize = 8.sp)
        }
    }

    if (showProjectDelete) {
        TypedDeleteDialog(
            title = "Delete project?",
            summary = buildString {
                append("This project contains ${chapters.size} chapter")
                if (chapters.size != 1) append("s")
                append(" and ${scenes.size} scene")
                if (scenes.size != 1) append("s")
                if (projectWordCount > 0) append(" (${projectWordCount} words)")
                append(". Type DELETE to move it to Trash.")
            },
            onDismiss = { showProjectDelete = false },
            onConfirm = {
                showProjectDelete = false
                onDeleteProject()
            }
        )
    }
}

@Composable
private fun ChapterBlock(
    chapter: ChapterEntity,
    chapterIndex: Int,
    scenes: List<SceneEntity>,
    selectedSceneId: String?,
    onCreateScene: () -> Unit,
    onSelectScene: (String) -> Unit,
    onRenameChapter: (String) -> Unit,
    onDeleteChapter: () -> Unit,
    onRenameScene: (String, String) -> Unit,
    onDeleteScene: (String) -> Unit
) {
    var expanded by rememberSaveable(chapter.id) { mutableStateOf(true) }
    var showChapterDelete by remember(chapter.id) { mutableStateOf(false) }
    var sceneDeleteTarget by remember(chapter.id) { mutableStateOf<SceneEntity?>(null) }

    val chapterWordCount = scenes.sumOf { countWords(it.prose) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DisclosureChevron(
                expanded = expanded,
                visible = scenes.isNotEmpty(),
                onClick = { expanded = !expanded }
            )

            ManageableTitle(
                id = chapter.id,
                rawTitle = chapter.title,
                displayTitle = chapterDisplayTitle(chapter.title, chapterIndex),
                emptyDraftWhenDefault = isDefaultChapterTitle(chapter.title),
                modifier = Modifier.weight(1f),
                color = NovellumTextSoft,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                onRename = onRenameChapter,
                onDeleteRequest = {
                    if (scenes.isEmpty()) onDeleteChapter()
                    else showChapterDelete = true
                }
            )

            TreeControl(
                symbol = "+",
                description = "New scene",
                onClick = {
                    expanded = true
                    onCreateScene()
                }
            )
        }

        if (expanded) {
            scenes.forEachIndexed { sceneIndex, scene ->
                val selected = scene.id == selectedSceneId

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 34.dp, top = 2.dp, bottom = 2.dp)
                        .height(38.dp)
                        .background(
                            if (selected) NovellumAccentSoft else Color.Transparent,
                            RoundedCornerShape(5.dp)
                        )
                        .border(
                            1.dp,
                            if (selected) NovellumAccent.copy(alpha = 0.28f) else Color.Transparent,
                            RoundedCornerShape(5.dp)
                        )
                        .padding(start = 9.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(20.dp)
                            .background(
                                if (selected) NovellumAccent else Color.Transparent,
                                RoundedCornerShape(50)
                            )
                    )
                    Spacer(Modifier.width(8.dp))

                    ManageableTitle(
                        id = scene.id,
                        rawTitle = scene.title,
                        displayTitle = sceneDisplayTitle(scene.title, sceneIndex),
                        emptyDraftWhenDefault = isDefaultSceneTitle(scene.title),
                        modifier = Modifier.weight(1f),
                        color = if (selected) NovellumText else NovellumTextSoft,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                        onTap = { onSelectScene(scene.id) },
                        onRename = { onRenameScene(scene.id, it) },
                        onDeleteRequest = {
                            if (scene.prose.isBlank()) onDeleteScene(scene.id)
                            else sceneDeleteTarget = scene
                        }
                    )
                }
            }
        }
    }

    if (showChapterDelete) {
        TypedDeleteDialog(
            title = "Delete ${chapterDisplayTitle(chapter.title, chapterIndex)}?",
            summary = buildString {
                append("This chapter contains ${scenes.size} scene")
                if (scenes.size != 1) append("s")
                if (chapterWordCount > 0) append(" and ${chapterWordCount} words")
                append(". Type DELETE to move it to Trash.")
            },
            onDismiss = { showChapterDelete = false },
            onConfirm = {
                showChapterDelete = false
                onDeleteChapter()
            }
        )
    }

    sceneDeleteTarget?.let { scene ->
        HoldDeleteDialog(
            title = "Delete scene?",
            summary = "This scene contains ${countWords(scene.prose)} words. Press and hold DELETE to move it to Trash.",
            onDismiss = { sceneDeleteTarget = null },
            onConfirm = {
                sceneDeleteTarget = null
                onDeleteScene(scene.id)
            }
        )
    }
}

@Composable
private fun DisclosureChevron(
    expanded: Boolean,
    visible: Boolean,
    onClick: () -> Unit
) {
    TreeControl(
        symbol = if (expanded) "⌄" else "›",
        description = if (expanded) "Collapse" else "Expand",
        enabled = visible,
        invisibleWhenDisabled = true,
        onClick = onClick
    )
}

@Composable
private fun TreeControl(
    symbol: String,
    description: String,
    enabled: Boolean = true,
    invisibleWhenDisabled: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (enabled || !invisibleWhenDisabled) {
            Text(
                text = symbol,
                color = if (enabled) NovellumTextSoft else NovellumTextDim.copy(alpha = 0.35f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ManageableTitle(
    id: String,
    rawTitle: String,
    displayTitle: String,
    emptyDraftWhenDefault: Boolean,
    modifier: Modifier = Modifier,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight = FontWeight.Normal,
    onTap: (() -> Unit)? = null,
    onRename: (String) -> Unit,
    onDeleteRequest: (() -> Unit)?
) {
    var editing by remember(id) { mutableStateOf(false) }
    var draft by remember(id, rawTitle) { mutableStateOf(rawTitle) }
    var hadFocus by remember(id) { mutableStateOf(false) }
    val focusRequester = remember(id) { FocusRequester() }

    fun beginEditing() {
        draft = if (emptyDraftWhenDefault) "" else rawTitle
        editing = true
    }

    fun commit() {
        val clean = draft.trim()
        hadFocus = false
        editing = false
        if (clean.isNotEmpty() && clean != rawTitle) onRename(clean)
        else draft = rawTitle
    }

    if (editing) {
        BackHandler(enabled = true) {
            hadFocus = false
            draft = rawTitle
            editing = false
        }

        Row(
            modifier = modifier
                .background(NovellumBlack, RoundedCornerShape(4.dp))
                .border(1.dp, NovellumAccent, RoundedCornerShape(4.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 7.dp, vertical = 5.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { state ->
                        if (state.isFocused) {
                            hadFocus = true
                        } else if (hadFocus && editing) {
                            commit()
                        }
                    }
            )

            if (onDeleteRequest != null) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clickable {
                            hadFocus = false
                            editing = false
                            onDeleteRequest()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "×",
                        color = NovellumDanger,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        LaunchedEffect(editing) {
            if (editing) focusRequester.requestFocus()
        }
    } else {
        Text(
            text = displayTitle,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier.pointerInput(id, rawTitle, displayTitle) {
                detectTapGestures(
                    onTap = { onTap?.invoke() },
                    onLongPress = { beginEditing() }
                )
            }
        )
    }
}

@Composable
private fun SceneEditor(
    scene: SceneEntity,
    chapterTitle: String?,
    sceneDisplayTitle: String,
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
    var showSceneDelete by remember(scene.id) { mutableStateOf(false) }
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

    val wordCount = remember(editorValue.text) { countWords(editorValue.text) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NovellumCanvas)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(NovellumHeader)
                .padding(horizontal = 28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                ManageableTitle(
                    id = scene.id + "-header",
                    rawTitle = scene.title,
                    displayTitle = sceneDisplayTitle,
                    emptyDraftWhenDefault = isDefaultSceneTitle(scene.title),
                    color = NovellumText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    onRename = onRenameScene,
                    onDeleteRequest = {
                        if (scene.prose.isBlank()) onDelete()
                        else showSceneDelete = true
                    }
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = chapterTitle ?: "Scene",
                    color = NovellumTextDim,
                    fontSize = 10.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = saveLabel,
                    color = when (saveLabel) {
                        "Autosaved" -> NovellumTextDim
                        "Saving…" -> NovellumTextSoft
                        else -> NovellumAccent
                    },
                    fontSize = 10.sp
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "$wordCount words",
                    color = NovellumTextDim,
                    fontSize = 9.sp
                )
            }
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
                .padding(horizontal = 30.dp, vertical = 20.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 920.dp)
                    .fillMaxWidth()
                    .background(NovellumEditor, RoundedCornerShape(8.dp))
                    .border(1.dp, NovellumLineSoft, RoundedCornerShape(8.dp))
                    .padding(horizontal = 42.dp, vertical = 30.dp)
            ) {
                BasicTextField(
                    value = editorValue,
                    onValueChange = { applyValue(it) },
                    modifier = Modifier.fillMaxSize(),
                    textStyle = TextStyle(
                        color = NovellumText,
                        fontSize = 16.sp,
                        lineHeight = 28.sp
                    ),
                    cursorBrush = SolidColor(NovellumAccent),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (editorValue.text.isEmpty()) {
                                Text(
                                    "Write here…",
                                    color = NovellumTextDim,
                                    fontSize = 16.sp,
                                    lineHeight = 28.sp
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
                .height(42.dp)
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
        }
    }

    if (showSceneDelete) {
        HoldDeleteDialog(
            title = "Delete scene?",
            summary = "This scene contains ${countWords(scene.prose)} words. Press and hold DELETE to move it to Trash.",
            onDismiss = { showSceneDelete = false },
            onConfirm = {
                showSceneDelete = false
                onDelete()
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
            .height(46.dp)
            .background(NovellumHeader)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolButton("↶", enabled = canUndo, onClick = onUndo)
        Spacer(Modifier.width(4.dp))
        ToolButton("↷", enabled = canRedo, onClick = onRedo)
        ToolbarDivider()
        ToolButton("Cut", enabled = hasSelection, onClick = onCut)
        Spacer(Modifier.width(6.dp))
        ToolButton("Copy", enabled = hasSelection, onClick = onCopy)
        Spacer(Modifier.width(6.dp))
        ToolButton("Paste", onClick = onPaste)
        ToolbarDivider()
        ToolButton("B", fontWeight = FontWeight.Bold, onClick = onBold)
        Spacer(Modifier.width(6.dp))
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
    Box(
        modifier = Modifier
            .background(
                if (enabled) NovellumEditor else NovellumEditor.copy(alpha = 0.5f),
                RoundedCornerShape(5.dp)
            )
            .border(1.dp, NovellumLineSoft, RoundedCornerShape(5.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = if (enabled) NovellumTextSoft else NovellumTextDim.copy(alpha = 0.45f),
            fontSize = 11.sp,
            fontWeight = fontWeight,
            fontStyle = fontStyle
        )
    }
}

@Composable
private fun ToolbarDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .width(1.dp)
            .height(18.dp)
            .background(NovellumLine)
    )
}

@Composable
private fun WorkspaceRail(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(18.dp)
            .fillMaxHeight()
            .background(NovellumHeader)
            .border(1.dp, NovellumLineSoft)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            color = NovellumTextDim,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun AuxiliaryWorkspacePanel(
    modifier: Modifier,
    projectTitle: String?,
    chapterTitle: String?,
    sceneTitle: String?,
    sceneWordCount: Int,
    chapterWordCount: Int,
    projectWordCount: Int,
    hasScene: Boolean
) {
    Column(
        modifier = modifier
            .background(NovellumSidebar)
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "WORKSPACE",
            color = NovellumTextSoft,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Auxiliary panel",
            color = NovellumTextDim,
            fontSize = 9.sp
        )

        Spacer(Modifier.height(14.dp))

        WorkspaceSectionCard(title = "Current focus") {
            InfoLine("Project", projectTitle ?: "None")
            InfoLine("Chapter", chapterTitle ?: "None")
            InfoLine("Scene", sceneTitle ?: "None")
        }

        Spacer(Modifier.height(12.dp))

        WorkspaceSectionCard(title = "Session") {
            InfoLine("Scene words", if (hasScene) sceneWordCount.toString() else "0")
            InfoLine("Chapter words", chapterWordCount.toString())
            InfoLine("Project words", projectWordCount.toString())
        }

        Spacer(Modifier.height(12.dp))

        WorkspaceSectionCard(title = "Quick notes") {
            Text(
                text = "Reserved for future Vault / Notes / Inspector content. This shell is intentionally lightweight for now.",
                color = NovellumTextDim,
                fontSize = 10.sp,
                lineHeight = 16.sp
            )
        }

        Spacer(Modifier.height(12.dp))

        WorkspaceSectionCard(title = "Gesture hints") {
            Text(
                text = "Tap to select scenes. Hold titles to rename or delete. Tap-to-copy and line expansion behavior remain intact.",
                color = NovellumTextDim,
                fontSize = 10.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun WorkspaceSectionCard(
    title: String,
    content: @Composable Column.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NovellumEditor, RoundedCornerShape(8.dp))
            .border(1.dp, NovellumLineSoft, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            text = title,
            color = NovellumText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = NovellumTextDim,
            fontSize = 10.sp,
            modifier = Modifier.width(86.dp)
        )
        Text(
            text = value,
            color = NovellumTextSoft,
            fontSize = 10.sp,
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun HoldDeleteDialog(
    title: String,
    summary: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NovellumSidebar,
        titleContentColor = NovellumText,
        textContentColor = NovellumTextSoft,
        title = { Text(title) },
        text = { Text(summary) },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = NovellumTextSoft)
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .background(
                        NovellumDanger.copy(alpha = 0.12f),
                        RoundedCornerShape(5.dp)
                    )
                    .border(
                        1.dp,
                        NovellumDanger.copy(alpha = 0.55f),
                        RoundedCornerShape(5.dp)
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = { onConfirm() })
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "HOLD DELETE",
                    color = NovellumDanger,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.7.sp
                )
            }
        }
    )
}

@Composable
private fun TypedDeleteDialog(
    title: String,
    summary: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var confirmation by remember(title) { mutableStateOf("") }
    val confirmed = confirmation == "DELETE"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NovellumSidebar,
        titleContentColor = NovellumText,
        textContentColor = NovellumTextSoft,
        title = { Text(title) },
        text = {
            Column {
                Text(summary)
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it },
                    singleLine = true,
                    label = { Text("Type DELETE") }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = NovellumTextSoft)
            }
        },
        confirmButton = {
            TextButton(enabled = confirmed, onClick = onConfirm) {
                Text(
                    "Move to Trash",
                    color = if (confirmed) NovellumDanger else NovellumTextDim
                )
            }
        }
    )
}

@Composable
private fun EmptyEditorState(
    manuscriptPanelOpen: Boolean,
    workspacePanelOpen: Boolean,
    onOpenManuscript: () -> Unit,
    onOpenWorkspace: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NovellumCanvas),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("✎", color = NovellumTextDim, fontSize = 28.sp)
            Spacer(Modifier.height(14.dp))
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
            Spacer(Modifier.height(16.dp))
            Row {
                if (!manuscriptPanelOpen) {
                    MiniAction(label = "Open manuscript", onClick = onOpenManuscript)
                }
                if (!manuscriptPanelOpen && !workspacePanelOpen) {
                    Spacer(Modifier.width(8.dp))
                }
                if (!workspacePanelOpen) {
                    MiniAction(label = "Open workspace", onClick = onOpenWorkspace)
                }
            }
        }
    }
}

@Composable
private fun MiniAction(
    label: String,
    accent: Color = NovellumAccent,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(accent.copy(alpha = 0.10f), RoundedCornerShape(999.dp))
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = NovellumTextSoft,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
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

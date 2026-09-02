package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.ChapterEntity
import com.example.data.entity.SceneEntity
import com.example.ui.viewmodel.EditorViewModel
import com.example.ui.viewmodel.SaveState
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Target visual system: blue-black glass, quiet graphite depth, and a single ember accent.
private val NovellumBlack = Color(0xFF060A0E)
private val NovellumHeader = Color(0xFF080D12)
private val NovellumSidebar = Color(0xFF0D1319)
private val NovellumEditor = Color(0xFF0B1117)
private val NovellumRaisedSoft = Color(0xFF121920)
private val NovellumInset = Color(0xFF090E13)
private val NovellumLine = Color(0xFF26303A)
private val NovellumLineSoft = Color(0xFF1B242D)
private val NovellumHighlight = Color(0xFF35414C)
private val NovellumText = Color(0xFFE6E2DC)
private val NovellumTextSoft = Color(0xFFBDBCB9)
private val NovellumTextDim = Color(0xFF777F88)
private val NovellumAccent = Color(0xFFEA752F)
private val NovellumAccentGlow = Color(0xFFF28A45)
private val NovellumAccentSoft = Color(0xFF281710)
private val NovellumDanger = Color(0xFFD96E75)
private val SidebarIconMuted = Color(0xFF7D858F)

@Composable
fun EditorShellScreen(viewModel: EditorViewModel) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val selectedProjectId by viewModel.selectedProjectId.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val projectScenes by viewModel.projectScenes.collectAsStateWithLifecycle()
    val selectedSceneId by viewModel.selectedSceneId.collectAsStateWithLifecycle()
    val currentScene by viewModel.currentScene.collectAsStateWithLifecycle()
    val uiMessage by viewModel.uiMessage.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var pendingExportProjectId by remember { mutableStateOf<String?>(null) }
    var manuscriptPanelOpen by rememberSaveable { mutableStateOf(true) }
    var workspacePanelOpen by rememberSaveable { mutableStateOf(true) }
    var selectedChapterPreviewId by rememberSaveable { mutableStateOf<String?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                viewModel.forceSaveCurrentScene()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUiMessage()
        }
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val projectId = pendingExportProjectId
        pendingExportProjectId = null
        if (uri != null && projectId != null) {
            scope.launch {
                try {
                    val content = viewModel.getProjectBackupJson(projectId)
                        ?: throw IllegalStateException("The selected project could not be exported.")
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(content.toByteArray())
                    } ?: throw IllegalStateException("Android could not open the selected backup destination.")
                    snackbarHostState.showSnackbar("Backup exported.")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(e.message ?: "Backup export failed.")
                }
            }
        }
    }

    val exportMarkdownLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        val projectId = pendingExportProjectId
        pendingExportProjectId = null
        if (uri != null && projectId != null) {
            scope.launch {
                try {
                    val content = viewModel.getProjectMarkdown(projectId)
                        ?: throw IllegalStateException("The selected project could not be exported.")
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(content.toByteArray())
                    } ?: throw IllegalStateException("Android could not open the selected manuscript destination.")
                    snackbarHostState.showSnackbar("Manuscript exported.")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(e.message ?: "Manuscript export failed.")
                }
            }
        }
    }

    val selectedProject = projects.firstOrNull { it.id == selectedProjectId }
    val selectedProjectIndex = projects.indexOfFirst { it.id == selectedProjectId }.coerceAtLeast(0)
    val selectedProjectDisplayTitle = selectedProject?.let {
        projectDisplayTitle(it.title, selectedProjectIndex)
    }
    val selectedChapter = currentScene?.let { scene ->
        chapters.firstOrNull { it.id == scene.chapterId }
    } ?: chapters.firstOrNull { it.id == selectedChapterPreviewId }

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
                chapterPreviewActive = selectedChapterPreviewId != null,
                manuscriptPanelOpen = manuscriptPanelOpen,
                workspacePanelOpen = workspacePanelOpen,
                onToggleManuscript = { manuscriptPanelOpen = !manuscriptPanelOpen },
                onToggleWorkspace = { workspacePanelOpen = !workspacePanelOpen }
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF090E13), NovellumBlack)
                        )
                    )
                    .padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
            ) {
                if (manuscriptPanelOpen) {
                    ManuscriptSidebar(
                        modifier = Modifier
                            .width(264.dp)
                            .fillMaxHeight(),
                        projectTitle = selectedProject?.title,
                        projectDisplayTitle = selectedProjectDisplayTitle,
                        selectedProjectId = selectedProjectId,
                        selectedSceneId = selectedSceneId,
                        selectedChapterPreviewId = selectedChapterPreviewId,
                        projects = projects.map { it.id to it.title },
                        chapters = chapters,
                        scenes = projectScenes,
                        onCreateProject = viewModel::createNextProject,
                        onSelectProject = { projectId ->
                            selectedChapterPreviewId = null
                            viewModel.selectProject(projectId)
                        },
                        onRenameProject = viewModel::renameProject,
                        onDeleteProject = viewModel::deleteProject,
                        onBackToProjects = {
                            selectedChapterPreviewId = null
                            viewModel.clearProjectSelection()
                        },
                        onCreateChapter = viewModel::createNextChapter,
                        onCreateScene = viewModel::createNextScene,
                        onSelectChapter = { chapterId ->
                            selectedChapterPreviewId = chapterId
                            viewModel.clearSceneSelection()
                        },
                        onSelectScene = { sceneId ->
                            selectedChapterPreviewId = null
                            viewModel.selectScene(sceneId)
                        },
                        onRenameChapter = viewModel::renameChapter,
                        onDeleteChapter = viewModel::deleteChapter,
                        onRenameScene = viewModel::renameScene,
                        onDeleteScene = viewModel::deleteScene,
                        onBackupJson = {
                            selectedProjectId?.let { projectId ->
                                pendingExportProjectId = projectId
                                val timestamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())
                                exportJsonLauncher.launch("Novellum_Backup_${timestamp}.json")
                            }
                        },
                        onExportMarkdown = {
                            selectedProjectId?.let { projectId ->
                                pendingExportProjectId = projectId
                                val timestamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())
                                exportMarkdownLauncher.launch("Novellum_Manuscript_${timestamp}.md")
                            }
                        }
                    )
                    Spacer(Modifier.width(12.dp))
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    val scene = currentScene
                    if (scene == null) {
                        val previewChapter = chapters.firstOrNull { it.id == selectedChapterPreviewId }
                        if (previewChapter != null) {
                            val chapterIndex = chapters.indexOfFirst { it.id == previewChapter.id }.coerceAtLeast(0)
                            ChapterPreview(
                                projectTitle = selectedProjectDisplayTitle ?: selectedProject?.title ?: "Novellum",
                                chapterTitle = chapterDisplayTitle(previewChapter.title, chapterIndex),
                                scenes = projectScenes.filter { it.chapterId == previewChapter.id }
                            )
                        } else {
                            EmptyEditorState()
                        }
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
                            saveState = saveState,
                            onRenameScene = { title -> viewModel.renameScene(scene.id, title) },
                            onSceneLoaded = { prose -> viewModel.syncSceneState(scene.id, prose) },
                            onProseChanged = viewModel::onProseChanged,
                            onSaveNow = { viewModel.forceSaveCurrentScene() },
                            onConfirmClear = { viewModel.forceSaveCurrentScene(isUserIntentClear = true) },
                            onDelete = { viewModel.deleteScene(scene.id) }
                        )
                    }
                }

                if (workspacePanelOpen) {
                    Spacer(Modifier.width(12.dp))
                    AuxiliaryWorkspacePanel(
                        modifier = Modifier
                            .width(292.dp)
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
                        projectWordCount = projectScenes.sumOf { countWords(it.prose) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NovellumTopBar(
    hasScene: Boolean,
    chapterPreviewActive: Boolean,
    manuscriptPanelOpen: Boolean,
    workspacePanelOpen: Boolean,
    onToggleManuscript: () -> Unit,
    onToggleWorkspace: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B1117), NovellumHeader)
                )
            )
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.width(252.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NovellumMark(Modifier.size(31.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                "NOVELLUM",
                color = NovellumText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Serif,
                letterSpacing = 3.2.sp
            )
        }

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopNavItem("✎", "Editor", !chapterPreviewActive)
            TopNavItem("▱", "Cards", false)
            TopNavItem("▣", "Vault", false)
            TopNavItem("▥", "Library", false)
            TopNavItem("▤", "Manuscript", chapterPreviewActive)
        }

        Row(
            modifier = Modifier.width(188.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WorkspaceToggleChip("◷", active = false, onClick = {})
            Spacer(Modifier.width(6.dp))
            WorkspaceToggleChip("⚙", active = false, onClick = {})
            Spacer(Modifier.width(6.dp))
            HeaderDivider()
            Spacer(Modifier.width(6.dp))
            WorkspaceToggleChip("▥", active = workspacePanelOpen, onClick = onToggleWorkspace)
            Spacer(Modifier.width(6.dp))
            WorkspaceToggleChip("☷", active = manuscriptPanelOpen, onClick = onToggleManuscript)
        }
    }
}

@Composable
private fun NovellumMark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        val inner = size.minDimension * 0.18f
        val outer = size.minDimension * 0.48f
        for (index in 0 until 8) {
            val angle = Math.toRadians((index * 45.0) - 90.0)
            val start = androidx.compose.ui.geometry.Offset(
                center.x + kotlin.math.cos(angle).toFloat() * inner,
                center.y + kotlin.math.sin(angle).toFloat() * inner
            )
            val end = androidx.compose.ui.geometry.Offset(
                center.x + kotlin.math.cos(angle).toFloat() * outer,
                center.y + kotlin.math.sin(angle).toFloat() * outer
            )
            drawLine(
                color = NovellumAccent,
                start = start,
                end = end,
                strokeWidth = if (index % 2 == 0) 1.35.dp.toPx() else 0.85.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        drawCircle(NovellumAccentGlow, radius = 2.1.dp.toPx(), center = center)
    }
}

@Composable
private fun HeaderDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(22.dp)
            .background(NovellumLineSoft)
    )
}

@Composable
private fun WorkspaceToggleChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (active) NovellumText else NovellumTextSoft,
            fontSize = 17.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = FontFamily.SansSerif
        )
    }
}

@Composable
private fun TopNavItem(symbol: String, label: String, active: Boolean) {
    Column(
        modifier = Modifier
            .height(64.dp)
            .width(96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(symbol, color = if (active) NovellumAccent else NovellumTextDim, fontSize = 16.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                color = if (active) NovellumAccentGlow else NovellumTextSoft,
                fontSize = 12.sp,
                fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                fontFamily = FontFamily.SansSerif
            )
        }
        Box(
            Modifier
                .width(if (active) 80.dp else 0.dp)
                .height(2.dp)
                .background(NovellumAccent, RoundedCornerShape(50))
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
    selectedChapterPreviewId: String?,
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
    onSelectChapter: (String) -> Unit,
    onSelectScene: (String) -> Unit,
    onRenameChapter: (String, String) -> Unit,
    onDeleteChapter: (String) -> Unit,
    onRenameScene: (String, String) -> Unit,
    onDeleteScene: (String) -> Unit,
    onBackupJson: () -> Unit,
    onExportMarkdown: () -> Unit
) {
    val panelShape = RoundedCornerShape(13.dp)
    Column(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF10171D), Color(0xFF0B1117))
                ),
                panelShape
            )
            .border(1.dp, NovellumLineSoft.copy(alpha = 0.86f), panelShape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp)
                .padding(start = 18.dp, end = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MANUSCRIPT",
                color = NovellumTextSoft,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.weight(1f)
            )

            if (selectedProjectId == null) {
                BareHeaderControl("+", onClick = onCreateProject)
            } else {
                BareHeaderControl("‹", onClick = onBackToProjects)
            }
            Spacer(Modifier.width(4.dp))
            MoreDotsGlyph(color = NovellumTextSoft, modifier = Modifier.size(26.dp))
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
                selectedChapterPreviewId = selectedChapterPreviewId,
                onRenameProject = onRenameProject,
                onDeleteProject = { onDeleteProject(selectedProjectId) },
                onCreateChapter = onCreateChapter,
                onCreateScene = onCreateScene,
                onSelectChapter = onSelectChapter,
                onSelectScene = onSelectScene,
                onRenameChapter = onRenameChapter,
                onDeleteChapter = onDeleteChapter,
                onRenameScene = onRenameScene,
                onDeleteScene = onDeleteScene,
                onBackupJson = onBackupJson,
                onExportMarkdown = onExportMarkdown
            )
        }
    }
}

@Composable
private fun BareHeaderControl(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            color = NovellumTextSoft,
            fontSize = 20.sp,
            fontFamily = FontFamily.SansSerif
        )
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
            .padding(horizontal = 13.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "PROJECTS",
                color = NovellumText,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.9.sp,
                modifier = Modifier.weight(1f)
            )
            TreeControl("+", description = "New project", compact = true, onClick = onCreateProject)
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            itemsIndexed(projects, key = { _, item -> item.first }) { index, project ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NovellumRaisedSoft.copy(alpha = 0.68f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 11.dp, vertical = 10.dp),
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
        Text("Tap to open · hold to manage", color = NovellumTextDim, fontSize = 9.sp)
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
    selectedChapterPreviewId: String?,
    onRenameProject: (String, String) -> Unit,
    onDeleteProject: () -> Unit,
    onCreateChapter: () -> Unit,
    onCreateScene: (String) -> Unit,
    onSelectChapter: (String) -> Unit,
    onSelectScene: (String) -> Unit,
    onRenameChapter: (String, String) -> Unit,
    onDeleteChapter: (String) -> Unit,
    onRenameScene: (String, String) -> Unit,
    onDeleteScene: (String) -> Unit,
    onBackupJson: () -> Unit,
    onExportMarkdown: () -> Unit
) {
    var projectExpanded by rememberSaveable(projectId) { mutableStateOf(true) }
    var showProjectDelete by remember(projectId) { mutableStateOf(false) }

    val projectIsEmpty = chapters.isEmpty() && scenes.isEmpty()
    val projectWordCount = scenes.sumOf { countWords(it.prose) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, top = 14.dp, end = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "PROJECT",
                    color = NovellumTextDim,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.7.sp
                )
                Spacer(Modifier.height(3.dp))
                ManageableTitle(
                    id = projectId,
                    rawTitle = projectTitle,
                    displayTitle = projectDisplayTitle,
                    emptyDraftWhenDefault = isDefaultProjectTitle(projectTitle),
                    color = NovellumAccentGlow,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif,
                    onRename = { onRenameProject(projectId, it) },
                    onDeleteRequest = {
                        if (projectIsEmpty) onDeleteProject() else showProjectDelete = true
                    }
                )
            }
            DisclosureChevron(
                expanded = projectExpanded,
                visible = chapters.isNotEmpty(),
                onClick = { projectExpanded = !projectExpanded }
            )
            TreeControl(
                symbol = "+",
                description = "New chapter",
                compact = true,
                onClick = {
                    projectExpanded = true
                    onCreateChapter()
                }
            )
        }

        SidebarSearchBox()
        Spacer(Modifier.height(8.dp))

        if (projectExpanded) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(chapters, key = { _, item -> item.id }) { chapterIndex, chapter ->
                    ChapterBlock(
                        chapter = chapter,
                        chapterIndex = chapterIndex,
                        scenes = scenes.filter { it.chapterId == chapter.id },
                        selectedSceneId = selectedSceneId,
                        selectedAsPreview = chapter.id == selectedChapterPreviewId,
                        onCreateScene = { onCreateScene(chapter.id) },
                        onSelectChapter = { onSelectChapter(chapter.id) },
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
                .height(64.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SidebarMetric("WORDS", String.format(Locale.US, "%,d", projectWordCount), Modifier.weight(1f))
            SidebarMetric("SCENES", scenes.size.toString(), Modifier.weight(1f))
            SidebarMetric("CHAPTERS", chapters.size.toString(), Modifier.weight(1f))
        }
        DividerLine()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SidebarFooterAction("⌕", "Search", onClick = {})
            SidebarFooterAction("▽", "Filter", onClick = {})
            SidebarFooterAction("▥", "Stats", onClick = {})
            SidebarFooterAction("⇧", "Backup", onClick = onBackupJson)
            SidebarFooterAction("⇩", "Export", onClick = onExportMarkdown)
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
            onConfirm = { showProjectDelete = false; onDeleteProject() }
        )
    }
}

@Composable
private fun SidebarSearchBox() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(40.dp)
            .background(NovellumInset.copy(alpha = 0.86f), RoundedCornerShape(8.dp))
            .border(1.dp, NovellumLineSoft, RoundedCornerShape(8.dp))
            .padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("⌕", color = NovellumTextSoft, fontSize = 18.sp)
        Spacer(Modifier.width(9.dp))
        Text("Search manuscript", color = NovellumTextDim, fontSize = 10.sp, modifier = Modifier.weight(1f))
        Text("≛", color = NovellumTextDim, fontSize = 15.sp)
    }
}

@Composable
private fun SidebarMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Text(label, color = NovellumTextDim, fontSize = 8.sp, letterSpacing = 0.45.sp)
        Spacer(Modifier.height(3.dp))
        Text(value, color = NovellumAccentGlow, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SidebarFooterAction(symbol: String, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(46.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(symbol, color = NovellumTextSoft, fontSize = 18.sp)
        Spacer(Modifier.height(2.dp))
        Text(label, color = NovellumTextDim, fontSize = 8.sp)
    }
}

@Composable
private fun ChapterBlock(
    chapter: ChapterEntity,
    chapterIndex: Int,
    scenes: List<SceneEntity>,
    selectedSceneId: String?,
    selectedAsPreview: Boolean,
    onCreateScene: () -> Unit,
    onSelectChapter: () -> Unit,
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
            .padding(bottom = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .background(
                    if (selectedAsPreview) NovellumRaisedSoft.copy(alpha = 0.76f) else Color.Transparent,
                    RoundedCornerShape(7.dp)
                )
                .padding(end = 1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DisclosureChevron(expanded = expanded, visible = scenes.isNotEmpty(), onClick = { expanded = !expanded })
            FolderGlyph(
                color = if (selectedAsPreview) NovellumAccentGlow else NovellumTextSoft,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(8.dp))

            ManageableTitle(
                id = chapter.id,
                rawTitle = chapter.title,
                displayTitle = chapterDisplayTitle(chapter.title, chapterIndex),
                emptyDraftWhenDefault = isDefaultChapterTitle(chapter.title),
                modifier = Modifier.weight(1f),
                color = if (selectedAsPreview) NovellumText else NovellumTextSoft.copy(alpha = 0.92f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                onTap = onSelectChapter,
                onRename = onRenameChapter,
                onDeleteRequest = {
                    if (scenes.isEmpty()) onDeleteChapter() else showChapterDelete = true
                }
            )

            MoreDotsGlyph(color = NovellumTextDim.copy(alpha = 0.82f), modifier = Modifier.size(18.dp))
            TreeControl(
                symbol = "+",
                description = "New scene",
                compact = true,
                onClick = { expanded = true; onCreateScene() }
            )
        }

        if (expanded) {
            scenes.forEachIndexed { sceneIndex, scene ->
                val selected = scene.id == selectedSceneId
                val rowShape = RoundedCornerShape(8.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 31.dp, top = 1.dp, bottom = 1.dp)
                        .height(37.dp)
                        .background(
                            brush = if (selected) {
                                Brush.horizontalGradient(
                                    listOf(NovellumAccentSoft.copy(alpha = 0.78f), NovellumRaisedSoft)
                                )
                            } else {
                                SolidColor(Color.Transparent)
                            },
                            shape = rowShape
                        )
                        .then(
                            if (selected) Modifier.border(
                                1.dp,
                                NovellumAccent.copy(alpha = 0.20f),
                                rowShape
                            ) else Modifier
                        )
                        .padding(horizontal = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DocumentGlyph(
                        color = if (selected) NovellumAccentGlow else SidebarIconMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(9.dp))
                    ManageableTitle(
                        id = scene.id,
                        rawTitle = scene.title,
                        displayTitle = sceneDisplayTitle(scene.title, sceneIndex),
                        emptyDraftWhenDefault = isDefaultSceneTitle(scene.title),
                        modifier = Modifier.weight(1f),
                        color = if (selected) NovellumAccentGlow else NovellumTextSoft.copy(alpha = 0.78f),
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                        fontFamily = FontFamily.SansSerif,
                        onTap = { onSelectScene(scene.id) },
                        onRename = { onRenameScene(scene.id, it) },
                        onDeleteRequest = {
                            if (scene.prose.isBlank()) onDeleteScene(scene.id) else sceneDeleteTarget = scene
                        }
                    )
                    Box(
                        Modifier
                            .size(if (selected) 7.dp else 4.dp)
                            .background(
                                if (selected) NovellumAccent else NovellumTextDim.copy(alpha = 0.28f),
                                RoundedCornerShape(50)
                            )
                    )
                    Spacer(Modifier.width(9.dp))
                    MoreDotsGlyph(color = NovellumTextDim.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
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
            onConfirm = { showChapterDelete = false; onDeleteChapter() }
        )
    }

    sceneDeleteTarget?.let { scene ->
        HoldDeleteDialog(
            title = "Delete scene?",
            summary = "This scene contains ${countWords(scene.prose)} words. Press and hold DELETE to move it to Trash.",
            onDismiss = { sceneDeleteTarget = null },
            onConfirm = { sceneDeleteTarget = null; onDeleteScene(scene.id) }
        )
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
            .size(27.dp)
            .clickable(enabled = visible, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (visible) {
            Text(
                text = if (expanded) "⌄" else "›",
                color = NovellumTextSoft,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}

@Composable
private fun TreeControl(
    symbol: String,
    description: String,
    enabled: Boolean = true,
    invisibleWhenDisabled: Boolean = false,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    val size = if (compact) 28.dp else 34.dp
    val radius = if (compact) 8.dp else 10.dp
    Box(
        modifier = Modifier
            .size(size)
            .background(if (enabled) NovellumRaisedSoft else Color.Transparent, RoundedCornerShape(radius))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (enabled || !invisibleWhenDisabled) {
            Text(
                text = symbol,
                color = if (enabled) NovellumTextSoft else NovellumTextDim.copy(alpha = 0.35f),
                fontSize = if (compact) 18.sp else 19.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}

@Composable
private fun FolderGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 1.7.dp.toPx(), cap = StrokeCap.Round)
        val path = Path().apply {
            moveTo(size.width * 0.10f, size.height * 0.30f)
            lineTo(size.width * 0.38f, size.height * 0.30f)
            lineTo(size.width * 0.47f, size.height * 0.42f)
            lineTo(size.width * 0.90f, size.height * 0.42f)
            lineTo(size.width * 0.90f, size.height * 0.82f)
            lineTo(size.width * 0.10f, size.height * 0.82f)
            close()
        }
        drawPath(path, color = color, style = stroke)
    }
}

@Composable
private fun DocumentGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = 1.55.dp.toPx()
        val stroke = Stroke(width = sw, cap = StrokeCap.Round)
        val path = Path().apply {
            moveTo(size.width * 0.23f, size.height * 0.10f)
            lineTo(size.width * 0.62f, size.height * 0.10f)
            lineTo(size.width * 0.80f, size.height * 0.28f)
            lineTo(size.width * 0.80f, size.height * 0.90f)
            lineTo(size.width * 0.23f, size.height * 0.90f)
            close()
            moveTo(size.width * 0.62f, size.height * 0.10f)
            lineTo(size.width * 0.62f, size.height * 0.29f)
            lineTo(size.width * 0.80f, size.height * 0.29f)
        }
        drawPath(path, color = color, style = stroke)
        drawLine(color, androidx.compose.ui.geometry.Offset(size.width * 0.35f, size.height * 0.52f), androidx.compose.ui.geometry.Offset(size.width * 0.68f, size.height * 0.52f), sw, StrokeCap.Round)
        drawLine(color, androidx.compose.ui.geometry.Offset(size.width * 0.35f, size.height * 0.68f), androidx.compose.ui.geometry.Offset(size.width * 0.63f, size.height * 0.68f), sw, StrokeCap.Round)
    }
}

@Composable
private fun MoreDotsGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val r = 1.35.dp.toPx()
        val cx = size.width / 2f
        drawCircle(color, r, androidx.compose.ui.geometry.Offset(cx, size.height * 0.32f))
        drawCircle(color, r, androidx.compose.ui.geometry.Offset(cx, size.height * 0.50f))
        drawCircle(color, r, androidx.compose.ui.geometry.Offset(cx, size.height * 0.68f))
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
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight = FontWeight.Normal,
    fontFamily: FontFamily = FontFamily.SansSerif,
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
                .background(NovellumInset, RoundedCornerShape(8.dp))
                .border(1.dp, NovellumAccent.copy(alpha = 0.65f), RoundedCornerShape(8.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = draft,
                onValueChange = { draft = it },
                singleLine = true,
                textStyle = TextStyle(
                    color = NovellumText,
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    fontFamily = fontFamily
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
            fontFamily = fontFamily,
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
    saveState: SaveState,
    onRenameScene: (String) -> Unit,
    onSceneLoaded: (String) -> Unit,
    onProseChanged: (String) -> Unit,
    onSaveNow: () -> Unit,
    onConfirmClear: () -> Unit,
    onDelete: () -> Unit
) {
    var editorValue by remember(scene.id) { mutableStateOf(TextFieldValue(scene.prose, TextRange(scene.prose.length))) }
    val undoStack = remember(scene.id) { mutableStateListOf<TextFieldValue>() }
    val redoStack = remember(scene.id) { mutableStateListOf<TextFieldValue>() }
    var showSceneDelete by remember(scene.id) { mutableStateOf(false) }

    LaunchedEffect(scene.id) { onSceneLoaded(scene.prose) }

    fun pushUndo(value: TextFieldValue) {
        undoStack.add(value)
        if (undoStack.size > 120) undoStack.removeAt(0)
    }
    fun applyValue(newValue: TextFieldValue, recordUndo: Boolean = true) {
        val previous = editorValue
        if (newValue == previous) return
        val textChanged = newValue.text != previous.text
        if (recordUndo && textChanged) { pushUndo(previous); redoStack.clear() }
        editorValue = newValue
        if (textChanged) onProseChanged(newValue.text)
    }
    fun selectionBounds(): Pair<Int, Int> = min(editorValue.selection.start, editorValue.selection.end) to max(editorValue.selection.start, editorValue.selection.end)
    fun replaceSelection(replacement: String, cursorOffset: Int = replacement.length) {
        val (start, end) = selectionBounds()
        val newText = editorValue.text.substring(0, start) + replacement + editorValue.text.substring(end)
        applyValue(TextFieldValue(newText, TextRange((start + cursorOffset).coerceIn(0, newText.length))))
    }
    fun wrapSelection(marker: String) {
        val (start, end) = selectionBounds()
        if (start < end) {
            val selected = editorValue.text.substring(start, end)
            val replacement = marker + selected + marker
            val newText = editorValue.text.substring(0, start) + replacement + editorValue.text.substring(end)
            applyValue(TextFieldValue(newText, TextRange(start + marker.length, start + marker.length + selected.length)))
        } else replaceSelection(marker + marker, marker.length)
    }
    fun undo() { if (undoStack.isNotEmpty()) { val previous = undoStack.removeAt(undoStack.lastIndex); redoStack.add(editorValue); applyValue(previous, false) } }
    fun redo() { if (redoStack.isNotEmpty()) { val next = redoStack.removeAt(redoStack.lastIndex); pushUndo(editorValue); applyValue(next, false) } }

    val saveLabel = when (saveState) {
        SaveState.SAVED -> "Saved just now"
        SaveState.UNSAVED -> "Unsaved"
        SaveState.AUTOSAVING -> "Saving…"
        SaveState.BLOCKED_EMPTY_CLEAR -> "Clear needs confirmation"
    }
    val wordCount = remember(editorValue.text) { if (editorValue.text.isBlank()) 0 else editorValue.text.trim().split(Regex("\\s+")).size }
    val sceneHasMeaningfulContent = editorValue.text.isNotBlank() || scene.prose.isNotBlank()
    val shellShape = RoundedCornerShape(13.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D141A), NovellumEditor)
                ),
                shellShape
            )
            .border(1.dp, NovellumLineSoft.copy(alpha = 0.90f), shellShape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
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
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.Serif,
                    onRename = onRenameScene,
                    onDeleteRequest = { if (sceneHasMeaningfulContent) showSceneDelete = true else onDelete() }
                )
                if (chapterTitle != null) {
                    Text(chapterTitle, color = NovellumTextDim, fontSize = 8.sp, fontFamily = FontFamily.SansSerif)
                }
            }
            Spacer(Modifier.width(14.dp))
            HeaderDivider()
            Spacer(Modifier.width(14.dp))
            Box(
                Modifier
                    .size(7.dp)
                    .background(
                        if (saveState == SaveState.SAVED) NovellumAccent else NovellumDanger,
                        RoundedCornerShape(50)
                    )
            )
            Spacer(Modifier.width(7.dp))
            Text(
                saveLabel,
                color = if (saveState == SaveState.SAVED) NovellumTextSoft else NovellumAccentGlow,
                fontSize = 9.sp
            )
            Spacer(Modifier.width(12.dp))
            HeaderAction("↶", enabled = undoStack.isNotEmpty(), onClick = ::undo)
            HeaderAction("↷", enabled = redoStack.isNotEmpty(), onClick = ::redo)
            Spacer(Modifier.width(6.dp))
            HeaderDivider()
            Spacer(Modifier.width(8.dp))
            HeaderAction("▥  Split", onClick = {})
            HeaderAction("✦  Clean Text", accent = true, onClick = {})
        }

        Box(
            Modifier
                .fillMaxWidth()
                .padding(start = 34.dp, end = 34.dp)
        ) {
            EditorToolbar(
                canUndo = undoStack.isNotEmpty(), canRedo = redoStack.isNotEmpty(), canSave = saveState == SaveState.UNSAVED,
                onUndo = ::undo, onRedo = ::redo, onBold = { wrapSelection("**") }, onItalic = { wrapSelection("_") }, onSaveNow = onSaveNow
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 30.dp, vertical = 12.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 820.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 18.dp)
            ) {
                BasicTextField(
                    value = editorValue,
                    onValueChange = { applyValue(it) },
                    modifier = Modifier.fillMaxSize(),
                    textStyle = TextStyle(
                        color = Color(0xFFD9D6D0),
                        fontSize = 18.sp,
                        lineHeight = 30.sp,
                        fontFamily = FontFamily.Serif,
                        letterSpacing = 0.12.sp
                    ),
                    cursorBrush = SolidColor(NovellumAccent),
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxSize()) {
                            if (editorValue.text.isEmpty()) Text("Write here…", color = NovellumTextDim, fontSize = 17.sp, fontFamily = FontFamily.Serif)
                            inner()
                        }
                    }
                )
            }
        }

        DividerLine()
        Row(
            Modifier
                .fillMaxWidth()
                .height(34.dp)
                .padding(horizontal = 26.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Words: $wordCount", color = NovellumTextDim, fontSize = 9.sp)
            Spacer(Modifier.width(18.dp))
            Text("Chars: ${editorValue.text.length}", color = NovellumTextDim, fontSize = 9.sp)
            Spacer(Modifier.width(24.dp))
            EditorProgress(Modifier.weight(1f))
            Spacer(Modifier.width(22.dp))
            if (saveState == SaveState.BLOCKED_EMPTY_CLEAR) {
                MiniAction("Confirm clear", accent = NovellumDanger, onClick = onConfirmClear)
                Spacer(Modifier.width(14.dp))
            }
            Text("Page 1 of 1", color = NovellumTextDim, fontSize = 9.sp)
        }
    }

    if (showSceneDelete) {
        HoldDeleteDialog(
            title = "Delete scene?",
            summary = "This scene contains $wordCount words. Press and hold DELETE to move it to Trash.",
            onDismiss = { showSceneDelete = false },
            onConfirm = { showSceneDelete = false; onDelete() }
        )
    }
}

@Composable
private fun HeaderAction(
    label: String,
    enabled: Boolean = true,
    accent: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = when {
                !enabled -> NovellumTextDim.copy(alpha = 0.35f)
                accent -> NovellumTextSoft
                else -> NovellumTextSoft
            },
            fontSize = 11.sp,
            fontFamily = FontFamily.SansSerif
        )
    }
}

@Composable
private fun EditorProgress(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.height(12.dp)) {
        val y = size.height / 2f
        drawLine(
            color = NovellumLine,
            start = androidx.compose.ui.geometry.Offset(0f, y),
            end = androidx.compose.ui.geometry.Offset(size.width, y),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = NovellumAccent.copy(alpha = 0.55f),
            start = androidx.compose.ui.geometry.Offset(0f, y),
            end = androidx.compose.ui.geometry.Offset(size.width * 0.58f, y),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(
            color = NovellumAccent,
            radius = 3.4.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(size.width * 0.58f, y)
        )
    }
}

@Composable
private fun EditorToolbar(
    canUndo: Boolean,
    canRedo: Boolean,
    canSave: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onSaveNow: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(
                Brush.verticalGradient(listOf(Color(0xFF141B21), Color(0xFF10161C))),
                RoundedCornerShape(8.dp)
            )
            .border(1.dp, NovellumLineSoft.copy(alpha = 0.82f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolButton("B", fontWeight = FontWeight.Bold, onClick = onBold)
        ToolButton("I", fontStyle = FontStyle.Italic, onClick = onItalic)
        ToolButton("❞", onClick = {})
        ToolbarDivider()
        ToolButton("☷", onClick = {})
        ToolButton("≣", onClick = {})
        Spacer(Modifier.weight(1f))
        ToolButton("↶", enabled = canUndo, onClick = onUndo)
        ToolButton("↷", enabled = canRedo, onClick = onRedo)
        ToolbarDivider()
        ToolButton("▥  Split", onClick = {})
        if (canSave) { Spacer(Modifier.width(6.dp)); ToolButton("Save", accent = true, onClick = onSaveNow) }
    }
}

@Composable
private fun ToolButton(
    label: String,
    enabled: Boolean = true,
    fontWeight: FontWeight = FontWeight.Medium,
    fontStyle: FontStyle = FontStyle.Normal,
    accent: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 1.dp)
            .background(if (accent) NovellumAccentSoft else Color.Transparent, RoundedCornerShape(7.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = when { !enabled -> NovellumTextDim.copy(alpha = 0.35f); accent -> NovellumAccentGlow; else -> NovellumTextSoft },
            fontSize = 11.sp,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontFamily = FontFamily.SansSerif
        )
    }
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
private fun AuxiliaryWorkspacePanel(
    modifier: Modifier,
    projectTitle: String?,
    chapterTitle: String?,
    sceneTitle: String?,
    sceneWordCount: Int,
    chapterWordCount: Int,
    projectWordCount: Int
) {
    var tab by rememberSaveable { mutableStateOf("Vault") }
    Column(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .weight(1.58f)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF10171D), Color(0xFF0B1117))),
                    RoundedCornerShape(13.dp)
                )
                .border(1.dp, NovellumLineSoft.copy(alpha = 0.86f), RoundedCornerShape(13.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                listOf("Vault", "Library", "Notes").forEach { label ->
                    val active = tab == label
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { tab = label },
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                color = if (active) NovellumAccentGlow else NovellumTextSoft,
                                fontSize = 11.sp,
                                fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                        Box(
                            Modifier
                                .width(if (active) 66.dp else 0.dp)
                                .height(2.dp)
                                .background(NovellumAccent, RoundedCornerShape(50))
                        )
                    }
                }
            }
            DividerLine()

            if (tab == "Notes") {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text("SCENE NOTES", color = NovellumAccentGlow, fontSize = 10.sp, letterSpacing = 0.9.sp)
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(NovellumInset, RoundedCornerShape(9.dp))
                            .border(1.dp, NovellumLineSoft, RoundedCornerShape(9.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            "Jot down ideas, reminders, or scratchpad notes for this scene…",
                            color = NovellumTextDim,
                            fontSize = 11.sp,
                            lineHeight = 18.sp,
                            fontFamily = FontFamily.Serif
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .background(NovellumInset, RoundedCornerShape(8.dp))
                                .border(1.dp, NovellumLineSoft, RoundedCornerShape(8.dp))
                                .padding(horizontal = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⌕", color = NovellumTextSoft, fontSize = 17.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (tab == "Vault") "Search vault…" else "Search library…",
                                color = NovellumTextDim,
                                fontSize = 10.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text("≛", color = NovellumTextDim, fontSize = 15.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .size(38.dp)
                                .background(NovellumRaisedSoft, RoundedCornerShape(8.dp))
                                .border(1.dp, NovellumLineSoft, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", color = NovellumTextSoft, fontSize = 20.sp)
                        }
                    }

                    Spacer(Modifier.weight(0.75f))
                    VaultEmptyIllustration()
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (tab == "Vault") "Your vault is empty." else "Your library is empty.",
                        color = NovellumText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (tab == "Vault") "Capture ideas, fragments,\nand research here."
                        else "Collect references and\nstaging material here.",
                        color = NovellumTextDim,
                        fontSize = 10.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .background(NovellumInset, RoundedCornerShape(7.dp))
                            .border(1.dp, NovellumLineSoft, RoundedCornerShape(7.dp))
                            .padding(horizontal = 18.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("+", color = NovellumAccent, fontSize = 16.sp)
                        Spacer(Modifier.width(9.dp))
                        Text("New Entry", color = NovellumAccentGlow, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF0F161C), Color(0xFF0B1117))),
                    RoundedCornerShape(13.dp)
                )
                .border(1.dp, NovellumLineSoft.copy(alpha = 0.86f), RoundedCornerShape(13.dp))
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Text("CURRENT FOCUS", color = NovellumTextDim, fontSize = 9.sp, letterSpacing = 0.7.sp)
            Spacer(Modifier.height(12.dp))
            FocusLine("▥", "Project", projectTitle ?: "None")
            DividerLine()
            FocusLine("▣", "Chapter", chapterTitle ?: "None")
            DividerLine()
            FocusLine("▤", "Scene", sceneTitle ?: "None")
            DividerLine()
            FocusLine("W", "Words", String.format(Locale.US, "%,d", sceneWordCount))
            Spacer(Modifier.weight(1f))
            Text(
                "Chapter ${String.format(Locale.US, "%,d", chapterWordCount)}  ·  Project ${String.format(Locale.US, "%,d", projectWordCount)}",
                color = NovellumTextDim.copy(alpha = 0.70f),
                fontSize = 8.sp
            )
        }
    }
}

@Composable
private fun VaultEmptyIllustration() {
    Canvas(modifier = Modifier.size(104.dp)) {
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        drawCircle(NovellumAccent.copy(alpha = 0.10f), size.minDimension * 0.45f, center, style = Stroke(1.dp.toPx()))
        drawCircle(NovellumAccent.copy(alpha = 0.18f), size.minDimension * 0.31f, center, style = Stroke(1.dp.toPx()))
        drawRoundRect(
            color = NovellumTextDim.copy(alpha = 0.45f),
            topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.27f, size.height * 0.30f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.48f, size.height * 0.52f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()),
            style = Stroke(1.dp.toPx())
        )
        drawRoundRect(
            color = NovellumTextSoft,
            topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.34f, size.height * 0.23f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.48f, size.height * 0.52f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()),
            style = Stroke(1.25.dp.toPx())
        )
        drawLine(
            NovellumAccent,
            androidx.compose.ui.geometry.Offset(center.x - 8.dp.toPx(), center.y),
            androidx.compose.ui.geometry.Offset(center.x + 8.dp.toPx(), center.y),
            1.4.dp.toPx(),
            StrokeCap.Round
        )
        drawLine(
            NovellumAccent,
            androidx.compose.ui.geometry.Offset(center.x, center.y - 8.dp.toPx()),
            androidx.compose.ui.geometry.Offset(center.x, center.y + 8.dp.toPx()),
            1.4.dp.toPx(),
            StrokeCap.Round
        )
    }
}

@Composable
private fun FocusLine(symbol: String, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(symbol, color = NovellumTextSoft, fontSize = 13.sp, modifier = Modifier.width(28.dp))
        Text(label, color = NovellumTextSoft, fontSize = 10.sp, modifier = Modifier.width(70.dp))
        Text(
            value,
            color = NovellumAccentGlow,
            fontSize = 10.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
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
private fun ChapterPreview(
    projectTitle: String,
    chapterTitle: String,
    scenes: List<SceneEntity>,
    pageNumber: Int = 1,
    pageCount: Int = 1
) {
    val cardShape = RoundedCornerShape(13.dp)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF0D141A), NovellumEditor)),
                cardShape
            )
            .border(1.dp, NovellumLineSoft.copy(alpha = 0.90f), cardShape),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 980.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("◎", color = NovellumTextDim, fontSize = 10.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "READ-ONLY MANUSCRIPT",
                        color = NovellumTextDim,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.1.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "You are viewing this chapter",
                    color = NovellumTextDim.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(Modifier.weight(1f))
                Text("⛶", color = NovellumTextDim, fontSize = 12.sp)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 58.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "CHAPTER ${chapterOrdinalOnly(chapterTitle)}",
                    color = NovellumAccentGlow.copy(alpha = 0.92f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 1.8.sp
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = chapterTitleOnly(chapterTitle),
                    color = NovellumText,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 0.3.sp
                )
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(1.dp)
                            .background(NovellumHighlight.copy(alpha = 0.45f))
                    )
                    Text(
                        text = "  ✦  ",
                        color = NovellumTextDim.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(1.dp)
                            .background(NovellumHighlight.copy(alpha = 0.45f))
                    )
                }
                Spacer(Modifier.height(34.dp))

                if (scenes.isEmpty()) {
                    Text(
                        text = "This chapter has no scenes yet.",
                        color = NovellumTextDim,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Serif
                    )
                } else {
                    scenes.forEachIndexed { index, scene ->
                        ManuscriptScenePreview(scene = scene, useDropCap = index == 0)
                        if (index != scenes.lastIndex) {
                            Spacer(Modifier.height(30.dp))
                            Text(
                                text = "•   •   •",
                                color = NovellumTextDim.copy(alpha = 0.45f),
                                fontSize = 9.sp,
                                letterSpacing = 4.sp
                            )
                            Spacer(Modifier.height(30.dp))
                        }
                    }
                }
                Spacer(Modifier.height(40.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(Modifier.width(28.dp).height(1.dp).background(NovellumLineSoft))
                Spacer(Modifier.width(10.dp))
                Text("$pageNumber of $pageCount", color = NovellumTextDim, fontSize = 10.sp, fontFamily = FontFamily.SansSerif)
                Spacer(Modifier.width(10.dp))
                Box(Modifier.width(28.dp).height(1.dp).background(NovellumLineSoft))
            }
        }
    }
}

private fun chapterOrdinalOnly(chapterTitle: String): String =
    Regex("Ch\\s*(\\d+)").find(chapterTitle)?.groupValues?.get(1)
        ?: Regex("Chapter\\s*(\\d+)").find(chapterTitle)?.groupValues?.get(1)
        ?: "1"

private fun chapterTitleOnly(chapterTitle: String): String =
    chapterTitle.substringAfter("· ", chapterTitle).trim()

@Composable
private fun ManuscriptScenePreview(
    scene: SceneEntity,
    useDropCap: Boolean
) {
    Column(
        modifier = Modifier
            .widthIn(max = 760.dp)
            .fillMaxWidth()
    ) {
        if (!isDefaultSceneTitle(scene.title)) {
            Text(
                text = scene.title,
                color = NovellumTextDim,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 0.7.sp
            )
            Spacer(Modifier.height(18.dp))
        }

        val prose = scene.prose.trim()
        if (prose.isBlank()) {
            Text(
                text = "Empty scene",
                color = NovellumTextDim.copy(alpha = 0.65f),
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic,
                fontFamily = FontFamily.Serif
            )
            return@Column
        }

        if (useDropCap && prose.length > 1) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = prose.take(1),
                    color = NovellumAccent.copy(alpha = 0.85f),
                    fontSize = 58.sp,
                    lineHeight = 58.sp,
                    fontFamily = FontFamily.Serif
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = prose.drop(1),
                    color = NovellumTextSoft.copy(alpha = 0.84f),
                    fontSize = 18.sp,
                    lineHeight = 31.sp,
                    fontFamily = FontFamily.Serif,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Text(
                text = prose,
                color = NovellumTextSoft.copy(alpha = 0.84f),
                fontSize = 18.sp,
                lineHeight = 31.sp,
                fontFamily = FontFamily.Serif
            )
        }
    }
}

@Composable
private fun EmptyEditorState() {
    val shape = RoundedCornerShape(13.dp)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF0D141A), NovellumEditor)),
                shape
            )
            .border(1.dp, NovellumLineSoft.copy(alpha = 0.90f), shape),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 390.dp)
                .padding(horizontal = 34.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(NovellumInset, RoundedCornerShape(12.dp))
                    .border(1.dp, NovellumLineSoft, RoundedCornerShape(12.dp))
                    ,
                contentAlignment = Alignment.Center
            ) {
                Text("✎", color = NovellumAccent, fontSize = 24.sp)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Select a scene to start writing",
                color = NovellumText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif
            )
            Spacer(Modifier.height(7.dp))
            Text(
                "Choose a scene from the manuscript panel.",
                color = NovellumTextDim,
                fontSize = 11.sp,
                fontFamily = FontFamily.SansSerif
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
            .background(NovellumHighlight.copy(alpha = 0.06f))
    )
}

@Composable
private fun MiniAction(
    label: String,
    accent: Color = NovellumAccent,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                if (enabled) NovellumRaisedSoft else NovellumInset,
                RoundedCornerShape(8.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = if (enabled) accent else NovellumTextDim,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.SansSerif
        )
    }
}

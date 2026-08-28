package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.draw.shadow
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

private val NovellumBlack = Color(0xFF0A0D12)
private val NovellumHeader = Color(0xFF0F131A)
private val NovellumSidebar = Color(0xFF141922)
private val NovellumCanvas = Color(0xFF0D1218)
private val NovellumEditor = Color(0xFF151B24)
private val NovellumRaised = Color(0xFF1D2430)
private val NovellumRaisedSoft = Color(0xFF191F2A)
private val NovellumInset = Color(0xFF0B1017)
private val NovellumLine = Color(0xFF2A313C)
private val NovellumLineSoft = Color(0xFF202630)
private val NovellumHighlight = Color(0xFF313A47)
private val NovellumShadow = Color(0xFF07090D)
private val NovellumText = Color(0xFFF4F1ED)
private val NovellumTextSoft = Color(0xFFCAC8C7)
private val NovellumTextDim = Color(0xFF858995)
private val NovellumAccent = Color(0xFFFF6138)
private val NovellumAccentGlow = Color(0xFFFF8B5F)
private val NovellumAccentSoft = Color(0xFF4A241A)
private val NovellumDanger = Color(0xFFD96E75)

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
    var workspacePanelOpen by rememberSaveable { mutableStateOf(false) }
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
                        projectWordCount = projectScenes.sumOf { countWords(it.prose) }
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
            .height(72.dp)
            .background(NovellumBlack)
            .padding(horizontal = 18.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.width(250.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .shadow(12.dp, RoundedCornerShape(12.dp), ambientColor = NovellumAccent.copy(alpha = 0.30f), spotColor = NovellumShadow)
                    .size(40.dp)
                    .background(NovellumAccent, RoundedCornerShape(12.dp))
                    ,
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "N",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Novellum",
                color = NovellumText,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif
            )
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .shadow(7.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(alpha = 0.30f), spotColor = NovellumShadow)
                .background(NovellumInset, RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopNavItem("Editor", true)
            TopNavItem("Cards", false)
            TopNavItem("Vault", false)
            TopNavItem("Library", false)
            TopNavItem("Manuscript", false)
        }

        Row(
            modifier = Modifier.widthIn(min = 286.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WorkspaceToggleChip(
                label = "☰",
                active = manuscriptPanelOpen,
                onClick = onToggleManuscript
            )
            Spacer(Modifier.width(8.dp))
            WorkspaceToggleChip(
                label = "▥",
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
                text = if (hasScene) "Saved" else "No scene selected",
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
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .size(42.dp)
            .shadow(
                if (active) 8.dp else 5.dp,
                shape,
                ambientColor = if (active) NovellumAccent.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.28f),
                spotColor = NovellumShadow
            )
            .background(if (active) NovellumRaised else NovellumInset, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (active) NovellumAccentGlow else NovellumTextSoft,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif
        )
    }
}

@Composable
private fun TopNavItem(label: String, active: Boolean) {
    val shape = RoundedCornerShape(13.dp)
    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .shadow(
                if (active) 10.dp else 0.dp,
                shape,
                ambientColor = if (active) NovellumAccent.copy(alpha = 0.20f) else Color.Transparent,
                spotColor = NovellumShadow
            )
            .background(if (active) NovellumRaised else Color.Transparent, shape)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (active && label == "Editor") "✎  $label" else label,
            color = if (active) NovellumAccentGlow else NovellumTextSoft,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            fontFamily = FontFamily.SansSerif
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
    Column(
        modifier = modifier
            .background(NovellumCanvas)
            .padding(start = 10.dp, top = 10.dp, bottom = 10.dp)
            .shadow(14.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.38f), spotColor = NovellumShadow)
            .background(NovellumSidebar, RoundedCornerShape(20.dp))
            
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(NovellumRaisedSoft, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MANUSCRIPT",
                color = NovellumTextSoft,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                modifier = Modifier.weight(1f)
            )

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
private fun ProjectList(
    projects: List<Pair<String, String>>,
    onCreateProject: () -> Unit,
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
            TreeControl("+", description = "New project", onClick = onCreateProject)
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            itemsIndexed(projects, key = { _, item -> item.first }) { index, project ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(12.dp), ambientColor = Color.Black.copy(alpha = 0.30f), spotColor = NovellumShadow)
                        .background(NovellumRaisedSoft, RoundedCornerShape(12.dp))
                        
                        .padding(horizontal = 11.dp, vertical = 9.dp),
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
            modifier = Modifier.padding(horizontal = 3.dp, vertical = 7.dp)
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
                .height(50.dp)
                .padding(horizontal = 9.dp),
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
                modifier = Modifier.padding(start = 42.dp, top = 2.dp, bottom = 3.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 7.dp, end = 7.dp, bottom = 12.dp)
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .shadow(7.dp, RoundedCornerShape(14.dp), ambientColor = Color.Black.copy(alpha = 0.30f), spotColor = NovellumShadow)
                .background(NovellumRaisedSoft, RoundedCornerShape(14.dp))
                
                .height(42.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Backup JSON",
                color = NovellumTextDim,
                fontSize = 9.sp,
                modifier = Modifier
                    .clickable(onClick = onBackupJson)
                    .padding(vertical = 8.dp)
            )
            Text("  •  ", color = NovellumLine, fontSize = 9.sp)
            Text(
                "Export MD",
                color = NovellumTextDim,
                fontSize = 9.sp,
                modifier = Modifier
                    .clickable(onClick = onExportMarkdown)
                    .padding(vertical = 8.dp)
            )
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
            .padding(bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .shadow(
                    if (selectedAsPreview) 8.dp else 0.dp,
                    RoundedCornerShape(12.dp),
                    ambientColor = Color.Black.copy(alpha = 0.30f),
                    spotColor = NovellumShadow
                )
                .background(
                    if (selectedAsPreview) NovellumRaised else Color.Transparent,
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 2.dp),
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
                color = if (selectedAsPreview) NovellumText else NovellumTextSoft,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                onTap = onSelectChapter,
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
                        .padding(start = 34.dp, top = 1.dp, bottom = 1.dp)
                        .height(40.dp)
                        .shadow(
                            if (selected) 12.dp else 0.dp,
                            RoundedCornerShape(10.dp),
                            ambientColor = if (selected) NovellumAccent.copy(alpha = 0.24f) else Color.Transparent,
                            spotColor = NovellumShadow
                        )
                        .background(
                            if (selected) NovellumAccent.copy(alpha = 0.96f) else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(start = 9.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(20.dp)
                            .background(
                                if (selected) Color.White.copy(alpha = 0.70f) else Color.Transparent,
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
                        color = if (selected) Color.White else NovellumTextSoft,
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
            .shadow(
                if (enabled) 4.dp else 0.dp,
                RoundedCornerShape(11.dp),
                ambientColor = Color.Black.copy(alpha = 0.28f),
                spotColor = NovellumShadow
            )
            .background(
                if (enabled) NovellumRaisedSoft else Color.Transparent,
                RoundedCornerShape(11.dp)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (enabled || !invisibleWhenDisabled) {
            Text(
                text = symbol,
                color = if (enabled) NovellumTextSoft else NovellumTextDim.copy(alpha = 0.35f),
                fontSize = 19.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif
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
    fontSize: androidx.compose.ui.unit.TextUnit,
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
                .shadow(6.dp, RoundedCornerShape(8.dp), ambientColor = Color.Black.copy(alpha = 0.30f), spotColor = NovellumShadow)
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
                    fontFamily = FontFamily.SansSerif
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
            fontFamily = FontFamily.SansSerif,
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

    LaunchedEffect(scene.id) {
        onSceneLoaded(scene.prose)
    }

    fun pushUndo(value: TextFieldValue) {
        undoStack.add(value)
        if (undoStack.size > 120) undoStack.removeAt(0)
    }

    fun applyValue(newValue: TextFieldValue, recordUndo: Boolean = true) {
        val previous = editorValue
        if (newValue == previous) return

        val textChanged = newValue.text != previous.text
        if (recordUndo && textChanged) {
            pushUndo(previous)
            redoStack.clear()
        }

        editorValue = newValue

        // Cursor-only/selection-only movement must not restart autosave.
        if (textChanged) {
            onProseChanged(newValue.text)
        }
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
        applyValue(previous, recordUndo = false)
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val next = redoStack.removeAt(redoStack.lastIndex)
        pushUndo(editorValue)
        applyValue(next, recordUndo = false)
    }

    val saveLabel = when (saveState) {
        SaveState.SAVED -> "Autosaved"
        SaveState.UNSAVED -> "Unsaved"
        SaveState.AUTOSAVING -> "Saving…"
        SaveState.BLOCKED_EMPTY_CLEAR -> "Clear needs confirmation"
    }

    val wordCount = remember(editorValue.text) {
        if (editorValue.text.isBlank()) 0
        else editorValue.text.trim().split(Regex("\\s+")).size
    }

    val sceneHasMeaningfulContent = editorValue.text.isNotBlank() || scene.prose.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NovellumCanvas)
            .padding(8.dp)
            .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(alpha = 0.34f), spotColor = NovellumShadow)
            .background(NovellumEditor, RoundedCornerShape(16.dp))
            
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(NovellumEditor, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .padding(horizontal = 24.dp),
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
                        if (sceneHasMeaningfulContent) showSceneDelete = true
                        else onDelete()
                    }
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
                color = when (saveState) {
                    SaveState.SAVED -> NovellumTextDim
                    SaveState.AUTOSAVING -> NovellumTextSoft
                    SaveState.UNSAVED,
                    SaveState.BLOCKED_EMPTY_CLEAR -> NovellumAccent
                },
                fontSize = 10.sp
            )
        }

        DividerLine()

        EditorToolbar(
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty(),
            canSave = saveState == SaveState.UNSAVED,
            onUndo = ::undo,
            onRedo = ::redo,
            onBold = { wrapSelection("**") },
            onItalic = { wrapSelection("_") },
            onSaveNow = onSaveNow
        )

        DividerLine()

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(NovellumEditor)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 940.dp)
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(alpha = 0.36f), spotColor = NovellumShadow)
                    .background(NovellumInset, RoundedCornerShape(16.dp))
                    
                    .padding(horizontal = 42.dp, vertical = 30.dp)
            ) {
                BasicTextField(
                    value = editorValue,
                    onValueChange = { applyValue(it) },
                    modifier = Modifier.fillMaxSize(),
                    textStyle = TextStyle(
                        color = NovellumText,
                        fontSize = 16.sp,
                        lineHeight = 28.sp,
                        fontFamily = FontFamily.SansSerif
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
                .height(40.dp)
                .background(NovellumEditor, RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "$wordCount word${if (wordCount == 1) "" else "s"}",
                color = NovellumTextDim,
                fontSize = 9.sp
            )

            Spacer(Modifier.weight(1f))

            if (saveState == SaveState.BLOCKED_EMPTY_CLEAR) {
                MiniAction(
                    label = "Confirm clear",
                    accent = NovellumDanger,
                    onClick = onConfirmClear
                )
                Spacer(Modifier.width(14.dp))
            }
        }
    }

    if (showSceneDelete) {
        HoldDeleteDialog(
            title = "Delete scene?",
            summary = "This scene contains $wordCount words. Press and hold DELETE to move it to Trash.",
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
            .background(NovellumInset)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolButton("↶", enabled = canUndo, onClick = onUndo)
        Spacer(Modifier.width(6.dp))
        ToolButton("↷", enabled = canRedo, onClick = onRedo)
        ToolbarDivider()
        ToolButton("B", fontWeight = FontWeight.Bold, onClick = onBold)
        Spacer(Modifier.width(6.dp))
        ToolButton("I", fontStyle = FontStyle.Italic, onClick = onItalic)
        ToolbarDivider()
        ToolButton("Save", enabled = canSave, accent = true, onClick = onSaveNow)
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
    val shape = RoundedCornerShape(9.dp)
    Box(
        modifier = Modifier
            .shadow(
                if (enabled) 5.dp else 0.dp,
                shape,
                ambientColor = Color.Black.copy(alpha = 0.32f),
                spotColor = NovellumShadow
            )
            .background(
                when {
                    !enabled -> NovellumInset.copy(alpha = 0.70f)
                    accent -> NovellumAccentSoft
                    else -> NovellumRaised
                },
                shape
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = when {
                !enabled -> NovellumTextDim.copy(alpha = 0.42f)
                accent -> NovellumAccentGlow
                else -> NovellumTextSoft
            },
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
private fun WorkspaceRail(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(18.dp)
            .fillMaxHeight()
            .background(NovellumCanvas)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 14.dp, height = 42.dp)
                .shadow(4.dp, RoundedCornerShape(9.dp), ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = NovellumShadow)
                .background(NovellumRaisedSoft, RoundedCornerShape(9.dp))
                ,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = symbol,
                color = NovellumTextDim,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif
            )
        }
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
    projectWordCount: Int
) {
    var tab by rememberSaveable { mutableStateOf("Vault") }
    Column(
        modifier = modifier
            .background(NovellumCanvas)
            .padding(end = 10.dp, top = 10.dp, bottom = 10.dp)
            .shadow(14.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.38f), spotColor = NovellumShadow)
            .background(NovellumSidebar, RoundedCornerShape(20.dp))
            
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = Color.Black.copy(alpha = 0.34f), spotColor = NovellumShadow)
                .background(NovellumInset, RoundedCornerShape(18.dp))
                
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("Vault", "Library", "Notes").forEach { label ->
                val active = tab == label
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .shadow(
                            if (active) 8.dp else 0.dp,
                            RoundedCornerShape(14.dp),
                            ambientColor = if (active) NovellumAccent.copy(alpha = 0.30f) else Color.Transparent,
                            spotColor = NovellumShadow
                        )
                        .background(
                            if (active) NovellumAccent else Color.Transparent,
                            RoundedCornerShape(14.dp)
                        )

                        .clickable { tab = label }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (active) Color.White else NovellumTextSoft,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        when (tab) {
            "Notes" -> {
                Text(
                    text = "SCENE NOTES",
                    color = NovellumAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .shadow(10.dp, RoundedCornerShape(18.dp), ambientColor = Color.Black.copy(alpha = 0.35f), spotColor = NovellumShadow)
                        .background(NovellumRaised, RoundedCornerShape(18.dp))
                        
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Jot down ideas, reminders, or scratchpad notes for this scene…",
                        color = NovellumTextDim,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontFamily = FontFamily.Serif
                    )
                }
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .shadow(5.dp, RoundedCornerShape(18.dp), ambientColor = Color.Black.copy(alpha = 0.30f), spotColor = NovellumShadow)
                        .background(NovellumInset, RoundedCornerShape(18.dp))
                        
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = if (tab == "Vault") "Search snippets…" else "Search staging…",
                        color = NovellumTextDim,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                Spacer(Modifier.weight(1f))

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .shadow(12.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.38f), spotColor = NovellumShadow)
                            .size(78.dp)
                            .background(NovellumRaised, RoundedCornerShape(20.dp))
                            ,
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (tab == "Vault") "▣" else "▱",
                            color = NovellumAccent,
                            fontSize = 28.sp
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = if (tab == "Vault") "Vault is Empty" else "Library is Empty",
                        color = NovellumText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (tab == "Vault") {
                            "Add snippets from the main Vault view."
                        } else {
                            "Add items from the main Library view."
                        },
                        color = NovellumTextDim,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                Spacer(Modifier.weight(1f))

                WorkspaceSectionCard("Current focus") {
                    InfoLine("Project", projectTitle ?: "None")
                    InfoLine("Chapter", chapterTitle ?: "None")
                    InfoLine("Scene", sceneTitle ?: "None")
                    InfoLine("Words", sceneWordCount.toString())
                }
            }
        }
    }
}

@Composable
private fun WorkspaceSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(9.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(alpha = 0.34f), spotColor = NovellumShadow)
            .background(NovellumRaisedSoft, RoundedCornerShape(16.dp))
            
            .padding(14.dp)
    ) {
        Text(
            text = title,
            color = NovellumText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.SansSerif
        )
        Spacer(Modifier.height(12.dp))
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
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.width(86.dp)
        )
        Text(
            text = value,
            color = NovellumTextSoft,
            fontSize = 10.sp,
            fontFamily = FontFamily.SansSerif,
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
private fun ChapterPreview(
    projectTitle: String,
    chapterTitle: String,
    scenes: List<SceneEntity>
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NovellumCanvas)
            .padding(14.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 980.dp)
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(18.dp), ambientColor = Color.Black.copy(alpha = 0.42f), spotColor = NovellumShadow)
                .background(NovellumEditor, RoundedCornerShape(18.dp))
                
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 58.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .background(NovellumRaisedSoft, RoundedCornerShape(999.dp))
                    
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "READ-ONLY CHAPTER PREVIEW",
                    color = NovellumTextDim,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.1.sp,
                    fontFamily = FontFamily.SansSerif
                )
            }

            Spacer(Modifier.height(28.dp))
            Text(
                text = projectTitle,
                color = NovellumText.copy(alpha = 0.92f),
                fontSize = 28.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.Serif,
                letterSpacing = 0.8.sp
            )
            Spacer(Modifier.height(38.dp))
            Text(
                text = chapterTitle.uppercase(Locale.US),
                color = NovellumAccentGlow.copy(alpha = 0.92f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 1.8.sp
            )
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .width(66.dp)
                    .height(1.dp)
                    .background(NovellumHighlight.copy(alpha = 0.50f))
            )
            Spacer(Modifier.height(38.dp))

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
            Spacer(Modifier.height(60.dp))
        }
    }
}

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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NovellumCanvas),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 390.dp)
                .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.34f), spotColor = NovellumShadow)
                .background(NovellumRaisedSoft, RoundedCornerShape(20.dp))
                
                .padding(horizontal = 34.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(NovellumInset, RoundedCornerShape(16.dp))
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
            .shadow(
                if (enabled) 4.dp else 0.dp,
                RoundedCornerShape(10.dp),
                ambientColor = Color.Black.copy(alpha = 0.26f),
                spotColor = NovellumShadow
            )
            .background(
                if (enabled) NovellumRaisedSoft else NovellumInset,
                RoundedCornerShape(10.dp)
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


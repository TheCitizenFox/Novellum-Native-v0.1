package com.example.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ViewSidebar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.ChapterEntity
import com.example.data.entity.ProjectEntity
import com.example.data.entity.SceneEntity
import com.example.ui.features.workspace.BinderSidebar
import com.example.ui.features.workspace.ConfirmDeleteDialog
import com.example.ui.features.workspace.ContextSidebar
import com.example.ui.features.workspace.EditorPane
import com.example.ui.features.workspace.EntityEditorDialog
import com.example.ui.features.workspace.ManuscriptOverviewView
import com.example.ui.features.workspace.SecondaryWorkspace
import com.example.ui.features.workspace.StructureView
import com.example.ui.features.workspace.TopWorkstationBar
import com.example.ui.features.workspace.WorkspaceTab
import com.example.ui.features.workspace.rememberEditorSessionState
import com.example.ui.features.workspace.projectDisplayTitle
import com.example.ui.features.workspace.safeDocumentName
import com.example.ui.features.workspace.wordCount
import com.example.ui.theme.ManuscriptSerif
import com.example.ui.theme.NovellumCanvas
import com.example.ui.theme.NovellumObsidian
import com.example.ui.theme.NovellumOutline
import com.example.ui.theme.NovellumOutlineVariant
import com.example.ui.theme.NovellumPrimary
import com.example.ui.theme.NovellumSurface
import com.example.ui.theme.NovellumSurfaceContainer
import com.example.ui.theme.NovellumSurfaceContainerLow
import com.example.ui.theme.UiSans
import com.example.ui.viewmodel.EditorViewModel
import kotlinx.coroutines.launch

private enum class CompactPanel { BINDER, EDITOR, CONTEXT }

@Composable
fun EditorShellScreen(viewModel: EditorViewModel) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val selectedProjectId by viewModel.selectedProjectId.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val scenes by viewModel.projectScenes.collectAsStateWithLifecycle()
    val selectedSceneId by viewModel.selectedSceneId.collectAsStateWithLifecycle()
    val currentScene by viewModel.currentScene.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val lastSavedTime by viewModel.lastSavedTime.collectAsStateWithLifecycle()
    val uiMessage by viewModel.uiMessage.collectAsStateWithLifecycle()

    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var activeTab by remember { mutableStateOf(WorkspaceTab.EDITOR) }
    var isZenMode by remember { mutableStateOf(false) }
    var binderVisible by remember { mutableStateOf(true) }
    var contextVisible by remember { mutableStateOf(true) }
    var selectedChapterId by remember { mutableStateOf<String?>(null) }
    var expandedChapterIds by remember { mutableStateOf(emptySet<String>()) }
    var knownChapterIds by remember { mutableStateOf(emptySet<String>()) }
    var searchQuery by remember { mutableStateOf("") }
    var fontSizePt by remember { mutableStateOf(17) }
    var compactPanel by remember { mutableStateOf(CompactPanel.EDITOR) }
    var editTarget by remember { mutableStateOf<EditTarget?>(null) }
    var deleteTarget by remember { mutableStateOf<EditTarget?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var localMessage by remember { mutableStateOf<String?>(null) }
    var backupPayload by remember { mutableStateOf<String?>(null) }
    var markdownPayload by remember { mutableStateOf<String?>(null) }

    val selectedProject = projects.firstOrNull { it.id == selectedProjectId }
    val visibleChapterIds = chapters.mapTo(mutableSetOf()) { it.id }
    val visibleScenes = scenes.filter { it.chapterId in visibleChapterIds }
    // A flatMapLatest Room flow can briefly retain the previous scene while a
    // new selectedSceneId is taking effect. Never let that stale object drive
    // the editor or context rail.
    val activeScene = currentScene?.takeIf { it.id == selectedSceneId }
    val currentChapter = chapters.firstOrNull {
        it.id == (activeScene?.chapterId ?: selectedChapterId)
    }
    val currentChapterPosition = currentChapter?.let { chapters.indexOfFirst { ch -> ch.id == it.id }.coerceAtLeast(0) } ?: 0
    val scenesInCurrentChapter = currentChapter?.let { chapter ->
        visibleScenes.filter { it.chapterId == chapter.id }.sortedBy { it.orderIndex }
    } ?: emptyList()
    val currentScenePosition = activeScene?.let { scene -> scenesInCurrentChapter.indexOfFirst { it.id == scene.id }.coerceAtLeast(0) } ?: 0
    val totalWords = visibleScenes.sumOf { wordCount(it.prose) }

    // This UI-only session object survives top-tab / compact-panel changes for
    // the same selected scene, while every prose mutation still goes straight
    // to EditorViewModel's existing draft/autosave machinery.
    val editorSession = rememberEditorSessionState(activeScene?.id, activeScene?.prose.orEmpty())

    LaunchedEffect(activeScene?.id) {
        activeScene?.let { scene -> viewModel.syncSceneState(scene.id, scene.prose) }
    }

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val payload = backupPayload
        if (uri != null && payload != null) localMessage = writeDocument(context, uri, payload, "Backup saved")
        backupPayload = null
    }
    val markdownLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        val payload = markdownPayload
        if (uri != null && payload != null) localMessage = writeDocument(context, uri, payload, "Markdown exported")
        markdownPayload = null
    }

    fun requestBackup() {
        val project = selectedProject ?: run {
            localMessage = "Choose a manuscript before creating a backup."
            return
        }
        scope.launch {
            val payload = viewModel.getProjectBackupJson(project.id)
            if (payload == null) localMessage = "The backup could not be prepared safely."
            else {
                backupPayload = payload
                backupLauncher.launch("${project.title.safeDocumentName("novellum_project")}_backup.json")
            }
        }
    }

    fun requestMarkdownExport() {
        val project = selectedProject ?: run {
            localMessage = "Choose a manuscript before exporting."
            return
        }
        scope.launch {
            val payload = viewModel.getProjectMarkdown(project.id)
            if (payload == null) localMessage = "The manuscript export could not be prepared safely."
            else {
                markdownPayload = payload
                markdownLauncher.launch("${project.title.safeDocumentName("novellum_manuscript")}.md")
            }
        }
    }

    fun selectChapterScope(chapterId: String) {
        selectedChapterId = chapterId
        expandedChapterIds = expandedChapterIds + chapterId
        activeTab = WorkspaceTab.EDITOR
        viewModel.clearSceneSelection()
        compactPanel = CompactPanel.EDITOR
    }

    fun selectScene(sceneId: String) {
        val target = visibleScenes.firstOrNull { it.id == sceneId }
        selectedChapterId = null
        target?.chapterId?.let { expandedChapterIds = expandedChapterIds + it }
        activeTab = WorkspaceTab.EDITOR
        viewModel.selectScene(sceneId)
        compactPanel = CompactPanel.EDITOR
    }

    LaunchedEffect(selectedProjectId) {
        selectedChapterId = null
        searchQuery = ""
        expandedChapterIds = emptySet()
        knownChapterIds = emptySet()
        activeTab = WorkspaceTab.EDITOR
    }

    LaunchedEffect(chapters.map { it.id }) {
        val currentIds = chapters.mapTo(linkedSetOf()) { it.id }
        if (knownChapterIds.isEmpty() && currentIds.isNotEmpty()) {
            expandedChapterIds = setOf(chapters.minBy { it.orderIndex }.id)
        } else {
            val newlyAdded = currentIds - knownChapterIds
            if (newlyAdded.isNotEmpty()) expandedChapterIds = expandedChapterIds + newlyAdded
        }
        knownChapterIds = currentIds
        if (selectedChapterId != null && selectedChapterId !in currentIds) selectedChapterId = null
    }

    LaunchedEffect(uiMessage) {
        val message = uiMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearUiMessage()
    }
    LaunchedEffect(localMessage) {
        val message = localMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        localMessage = null
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                viewModel.forceSaveCurrentScene()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(NovellumSurface, NovellumCanvas),
                    radius = 1500f
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val wide = maxWidth >= 980.dp

        Column(Modifier.fillMaxSize()) {
            TopWorkstationBar(
                projectTitle = selectedProject?.let(::projectDisplayTitle),
                totalWords = totalWords,
                activeTab = activeTab,
                onTabSelected = { activeTab = it },
                onBackup = ::requestBackup,
                onExport = ::requestMarkdownExport,
                onOpenSettings = { showSettings = true }
            )

            when (activeTab) {
                WorkspaceTab.EDITOR -> {
                    if (wide) {
                        Row(Modifier.weight(1f).fillMaxWidth()) {
                            AnimatedVisibility(
                                visible = !isZenMode && binderVisible,
                                enter = slideInHorizontally { -it } + fadeIn(),
                                exit = slideOutHorizontally { -it } + fadeOut()
                            ) {
                                BinderSidebar(
                                    projects = projects,
                                    selectedProject = selectedProject,
                                    chapters = chapters,
                                    scenes = visibleScenes,
                                    selectedSceneId = selectedSceneId,
                                    selectedChapterId = selectedChapterId,
                                    expandedChapterIds = expandedChapterIds,
                                    searchQuery = searchQuery,
                                    onSearchChanged = { searchQuery = it },
                                    onProjectSelected = { id ->
                                        selectedChapterId = null
                                        viewModel.selectProject(id)
                                    },
                                    onProjectScopeSelected = {
                                        selectedChapterId = null
                                        viewModel.clearSceneSelection()
                                    },
                                    onNewProject = viewModel::createNextProject,
                                    onNewChapter = viewModel::createNextChapter,
                                    onNewScene = { chapterId ->
                                        expandedChapterIds = expandedChapterIds + chapterId
                                        viewModel.createNextScene(chapterId)
                                    },
                                    onToggleChapter = { chapterId ->
                                        expandedChapterIds = if (chapterId in expandedChapterIds) expandedChapterIds - chapterId else expandedChapterIds + chapterId
                                    },
                                    onChapterSelected = ::selectChapterScope,
                                    onSceneSelected = ::selectScene,
                                    onManageProject = { editTarget = EditTarget.Project(it) },
                                    onManageChapter = { editTarget = EditTarget.Chapter(it) },
                                    onManageScene = { editTarget = EditTarget.Scene(it) }
                                )
                            }

                            if (!isZenMode) {
                                PanelRailHandle(
                                    side = PanelRailSide.LEFT,
                                    collapsed = !binderVisible,
                                    onClick = { binderVisible = !binderVisible }
                                )
                            }

                            EditorPane(
                                project = selectedProject,
                                chapter = currentChapter,
                                chapterPosition = currentChapterPosition,
                                currentScene = activeScene,
                                scenePosition = currentScenePosition,
                                scenesInChapter = scenesInCurrentChapter,
                                saveState = saveState,
                                lastSavedTime = lastSavedTime,
                                fontSizePt = fontSizePt,
                                isZenMode = isZenMode,
                                editorSession = editorSession,
                                onProseChanged = viewModel::onProseChanged,
                                onSaveNow = { viewModel.forceSaveCurrentScene() },
                                onConfirmIntentionalClear = { viewModel.forceSaveCurrentScene(isUserIntentClear = true) },
                                onCycleFontSize = { fontSizePt = when (fontSizePt) { 16 -> 17; 17 -> 18; 18 -> 20; else -> 16 } },
                                onToggleZen = { isZenMode = !isZenMode },
                                onSelectScene = ::selectScene,
                                onCreateChapter = viewModel::createNextChapter,
                                modifier = Modifier.weight(1f)
                            )

                            if (!isZenMode) {
                                PanelRailHandle(
                                    side = PanelRailSide.RIGHT,
                                    collapsed = !contextVisible,
                                    onClick = { contextVisible = !contextVisible }
                                )
                            }

                            AnimatedVisibility(
                                visible = !isZenMode && contextVisible,
                                enter = slideInHorizontally { it } + fadeIn(),
                                exit = slideOutHorizontally { it } + fadeOut()
                            ) {
                                ContextSidebar(
                                    project = selectedProject,
                                    chapter = currentChapter,
                                    scene = activeScene,
                                    chapterPosition = currentChapterPosition,
                                    scenePosition = currentScenePosition,
                                    chapters = chapters,
                                    scenes = visibleScenes,
                                    onSelectChapter = ::selectChapterScope,
                                    onSelectScene = ::selectScene
                                )
                            }
                        }
                    } else {
                        Column(Modifier.weight(1f).fillMaxWidth()) {
                            if (!isZenMode) {
                                CompactPanelSwitcher(compactPanel) { compactPanel = it }
                            }
                            Box(Modifier.weight(1f).fillMaxWidth()) {
                                when {
                                    isZenMode || compactPanel == CompactPanel.EDITOR -> EditorPane(
                                        project = selectedProject,
                                        chapter = currentChapter,
                                        chapterPosition = currentChapterPosition,
                                        currentScene = activeScene,
                                        scenePosition = currentScenePosition,
                                        scenesInChapter = scenesInCurrentChapter,
                                        saveState = saveState,
                                        lastSavedTime = lastSavedTime,
                                        fontSizePt = fontSizePt,
                                        isZenMode = isZenMode,
                                        editorSession = editorSession,
                                        onProseChanged = viewModel::onProseChanged,
                                        onSaveNow = { viewModel.forceSaveCurrentScene() },
                                        onConfirmIntentionalClear = { viewModel.forceSaveCurrentScene(isUserIntentClear = true) },
                                        onCycleFontSize = { fontSizePt = when (fontSizePt) { 16 -> 17; 17 -> 18; 18 -> 20; else -> 16 } },
                                        onToggleZen = { isZenMode = !isZenMode },
                                        onSelectScene = ::selectScene,
                                        onCreateChapter = viewModel::createNextChapter,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    compactPanel == CompactPanel.BINDER -> BinderSidebar(
                                        projects = projects,
                                        selectedProject = selectedProject,
                                        chapters = chapters,
                                        scenes = visibleScenes,
                                        selectedSceneId = selectedSceneId,
                                        selectedChapterId = selectedChapterId,
                                        expandedChapterIds = expandedChapterIds,
                                        searchQuery = searchQuery,
                                        onSearchChanged = { searchQuery = it },
                                        onProjectSelected = { id -> viewModel.selectProject(id) },
                                        onProjectScopeSelected = { selectedChapterId = null; viewModel.clearSceneSelection() },
                                        onNewProject = viewModel::createNextProject,
                                        onNewChapter = viewModel::createNextChapter,
                                        onNewScene = { id -> expandedChapterIds = expandedChapterIds + id; viewModel.createNextScene(id) },
                                        onToggleChapter = { id -> expandedChapterIds = if (id in expandedChapterIds) expandedChapterIds - id else expandedChapterIds + id },
                                        onChapterSelected = ::selectChapterScope,
                                        onSceneSelected = ::selectScene,
                                        onManageProject = { editTarget = EditTarget.Project(it) },
                                        onManageChapter = { editTarget = EditTarget.Chapter(it) },
                                        onManageScene = { editTarget = EditTarget.Scene(it) },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    else -> ContextSidebar(
                                        project = selectedProject,
                                        chapter = currentChapter,
                                        scene = activeScene,
                                        chapterPosition = currentChapterPosition,
                                        scenePosition = currentScenePosition,
                                        chapters = chapters,
                                        scenes = visibleScenes,
                                        onSelectChapter = ::selectChapterScope,
                                        onSelectScene = ::selectScene,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
                WorkspaceTab.STRUCTURE -> StructureView(
                    project = selectedProject,
                    chapters = chapters,
                    scenes = visibleScenes,
                    selectedSceneId = selectedSceneId,
                    onSelectScene = ::selectScene,
                    modifier = Modifier.weight(1f)
                )
                WorkspaceTab.MANUSCRIPT -> ManuscriptOverviewView(
                    project = selectedProject,
                    chapters = chapters,
                    scenes = visibleScenes,
                    onSelectScene = ::selectScene,
                    modifier = Modifier.weight(1f)
                )
                WorkspaceTab.VAULT, WorkspaceTab.LIBRARY -> SecondaryWorkspace(activeTab, Modifier.weight(1f))
            }
        }

        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(18.dp))
    }

    editTarget?.let { target ->
        EntityEditorDialog(
            heading = "Edit ${target.label}",
            initialTitle = target.title,
            destructiveLabel = "Remove ${target.label.lowercase()}",
            onDismiss = { editTarget = null },
            onSave = { title ->
                when (target) {
                    is EditTarget.Project -> viewModel.renameProject(target.entity.id, title)
                    is EditTarget.Chapter -> viewModel.renameChapter(target.entity.id, title)
                    is EditTarget.Scene -> viewModel.renameScene(target.entity.id, title)
                }
                editTarget = null
            },
            onRequestDelete = {
                editTarget = null
                deleteTarget = target
            }
        )
    }

    deleteTarget?.let { target ->
        ConfirmDeleteDialog(
            label = target.label.lowercase(),
            detail = deletionDetail(target, chapters, visibleScenes),
            onDismiss = { deleteTarget = null },
            onConfirm = {
                when (target) {
                    is EditTarget.Project -> viewModel.deleteProject(target.entity.id)
                    is EditTarget.Chapter -> viewModel.deleteChapter(target.entity.id)
                    is EditTarget.Scene -> viewModel.deleteScene(target.entity.id)
                }
                if (target is EditTarget.Chapter && selectedChapterId == target.entity.id) selectedChapterId = null
                deleteTarget = null
            }
        )
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            containerColor = NovellumSurfaceContainer,
            title = { Text("Workspace settings", fontFamily = ManuscriptSerif, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Text(
                    "Additional workspace preferences will appear here as the new interface is refined.",
                    fontFamily = UiSans,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = { TextButton(onClick = { showSettings = false }) { Text("Done", color = NovellumPrimary) } }
        )
    }
}

private enum class PanelRailSide { LEFT, RIGHT }

@Composable
private fun PanelRailHandle(
    side: PanelRailSide,
    collapsed: Boolean,
    onClick: () -> Unit
) {
    val description = when (side) {
        PanelRailSide.LEFT -> if (collapsed) "Show manuscript pane" else "Hide manuscript pane"
        PanelRailSide.RIGHT -> if (collapsed) "Show context pane" else "Hide context pane"
    }
    val accent = if (collapsed) NovellumPrimary.copy(alpha = 0.72f) else NovellumOutline.copy(alpha = 0.56f)

    Box(
        modifier = Modifier
            .width(12.dp)
            .fillMaxHeight()
            .background(NovellumSurface.copy(alpha = 0.34f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(10.dp)
                .height(72.dp)
                .clip(RoundedCornerShape(5.dp))
                .semantics {
                    contentDescription = description
                    role = Role.Button
                }
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(NovellumSurfaceContainerLow)
                    .border(1.dp, accent, RoundedCornerShape(2.dp))
            )
            Box(
                Modifier
                    .width(1.dp)
                    .height(22.dp)
                    .background(if (collapsed) NovellumPrimary else NovellumOutlineVariant)
            )
        }
    }
}

@Composable
private fun CompactPanelSwitcher(selected: CompactPanel, onSelect: (CompactPanel) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(NovellumSurfaceContainerLow)
            .border(1.dp, NovellumOutlineVariant.copy(alpha = 0.3f)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompactPanelButton("Binder", Icons.Default.Folder, selected == CompactPanel.BINDER) { onSelect(CompactPanel.BINDER) }
        Spacer(Modifier.width(8.dp))
        CompactPanelButton("Editor", Icons.Default.Article, selected == CompactPanel.EDITOR) { onSelect(CompactPanel.EDITOR) }
        Spacer(Modifier.width(8.dp))
        CompactPanelButton("Context", Icons.Default.ViewSidebar, selected == CompactPanel.CONTEXT) { onSelect(CompactPanel.CONTEXT) }
    }
}

@Composable
private fun CompactPanelButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (selected) NovellumSurfaceContainer else NovellumObsidian)
            .border(1.dp, if (selected) NovellumPrimary.copy(alpha = 0.55f) else NovellumOutlineVariant.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (selected) NovellumPrimary else NovellumOutline, modifier = Modifier.width(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, fontFamily = UiSans, fontSize = 10.sp, color = if (selected) NovellumPrimary else NovellumOutline)
    }
}

private sealed interface EditTarget {
    val label: String
    val title: String

    data class Project(val entity: ProjectEntity) : EditTarget {
        override val label = "Project"
        override val title = entity.title
    }

    data class Chapter(val entity: ChapterEntity) : EditTarget {
        override val label = "Chapter"
        override val title = entity.title
    }

    data class Scene(val entity: SceneEntity) : EditTarget {
        override val label = "Scene"
        override val title = entity.title
    }
}

private fun deletionDetail(target: EditTarget, chapters: List<ChapterEntity>, scenes: List<SceneEntity>): String {
    return when (target) {
        is EditTarget.Project -> {
            val projectChapters = chapters.filter { it.projectId == target.entity.id }
            val ids = projectChapters.mapTo(mutableSetOf()) { it.id }
            val projectScenes = scenes.filter { it.chapterId in ids }
            "“${target.title}” contains ${projectChapters.size} chapters, ${projectScenes.size} scenes, and %,d words currently visible to this project. Removal uses Novellum’s existing recovery-safe path.".format(projectScenes.sumOf { wordCount(it.prose) })
        }
        is EditTarget.Chapter -> {
            val chapterScenes = scenes.filter { it.chapterId == target.entity.id }
            "“${target.title}” contains ${chapterScenes.size} scenes and %,d words. Removal uses Novellum’s existing recovery-safe path.".format(chapterScenes.sumOf { wordCount(it.prose) })
        }
        is EditTarget.Scene -> "“${target.title}” contains %,d words. Removal uses Novellum’s existing recovery-safe path.".format(wordCount(target.entity.prose))
    }
}

private fun writeDocument(context: Context, uri: android.net.Uri, payload: String, successMessage: String): String {
    return try {
        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(payload) }
            ?: return "Android could not open the selected destination."
        successMessage
    } catch (error: Exception) {
        error.message ?: "The document could not be written."
    }
}

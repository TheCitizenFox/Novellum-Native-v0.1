package com.example.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.ChapterEntity
import com.example.data.entity.ProjectEntity
import com.example.data.entity.SceneEntity
import com.example.ui.features.workspace.AuxiliaryTab
import com.example.ui.features.workspace.AuxiliaryWorkspace
import com.example.ui.features.workspace.ConfirmDeleteDialog
import com.example.ui.features.workspace.EntityEditorDialog
import com.example.ui.features.workspace.ManuscriptSidebar
import com.example.ui.features.workspace.NovellumTopBar
import com.example.ui.features.workspace.WorkspaceColors
import com.example.ui.features.workspace.WorkspaceMetrics
import com.example.ui.features.workspace.WorkspaceMode
import com.example.ui.features.workspace.WritingWorkspace
import com.example.ui.features.workspace.safeDocumentName
import com.example.ui.viewmodel.EditorViewModel
import kotlinx.coroutines.launch

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

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var leftPanelOpen by remember { mutableStateOf(true) }
    var rightPanelOpen by remember { mutableStateOf(true) }
    var activeMode by remember { mutableStateOf(WorkspaceMode.Editor) }
    var auxiliaryTab by remember { mutableStateOf(AuxiliaryTab.Vault) }
    var previewChapterId by remember { mutableStateOf<String?>(null) }
    var expandedChapterIds by remember { mutableStateOf(emptySet<String>()) }
    var editTarget by remember { mutableStateOf<EditTarget?>(null) }
    var deleteTarget by remember { mutableStateOf<EditTarget?>(null) }
    var localMessage by remember { mutableStateOf<String?>(null) }
    var backupPayload by remember { mutableStateOf<String?>(null) }
    var markdownPayload by remember { mutableStateOf<String?>(null) }

    val selectedProject = projects.firstOrNull { it.id == selectedProjectId }
    val previewChapter = chapters.firstOrNull { it.id == previewChapterId }
    val currentChapter = chapters.firstOrNull {
        it.id == (currentScene?.chapterId ?: previewChapterId)
    }
    val previewScenes = scenes.filter { it.chapterId == previewChapterId }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val payload = backupPayload
        if (uri != null && payload != null) {
            localMessage = writeDocument(context, uri, payload, "Backup saved")
        }
        backupPayload = null
    }
    val markdownLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        val payload = markdownPayload
        if (uri != null && payload != null) {
            localMessage = writeDocument(context, uri, payload, "Markdown exported")
        }
        markdownPayload = null
    }

    fun unavailable(label: String) {
        localMessage = "$label is not exposed by the current workspace model."
    }

    fun requestBackup() {
        val project = selectedProject ?: run {
            localMessage = "Choose a project before creating a backup."
            return
        }
        scope.launch {
            val payload = viewModel.getProjectBackupJson(project.id)
            if (payload == null) {
                localMessage = "The backup could not be prepared safely."
            } else {
                backupPayload = payload
                backupLauncher.launch("${project.title.safeDocumentName("novellum_project")}_backup.json")
            }
        }
    }

    fun requestMarkdownExport() {
        val project = selectedProject ?: run {
            localMessage = "Choose a project before exporting."
            return
        }
        scope.launch {
            val payload = viewModel.getProjectMarkdown(project.id)
            if (payload == null) {
                localMessage = "The manuscript export could not be prepared safely."
            } else {
                markdownPayload = payload
                markdownLauncher.launch("${project.title.safeDocumentName("novellum_manuscript")}.md")
            }
        }
    }

    LaunchedEffect(selectedProjectId) {
        previewChapterId = null
        activeMode = WorkspaceMode.Editor
        expandedChapterIds = emptySet()
    }
    LaunchedEffect(chapters, selectedProjectId) {
        if (expandedChapterIds.isEmpty() && chapters.isNotEmpty()) {
            expandedChapterIds = setOf(chapters.minBy { it.orderIndex }.id)
        }
        if (previewChapterId != null && chapters.none { it.id == previewChapterId }) {
            previewChapterId = null
        }
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
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF111A1F), WorkspaceColors.Void),
                    radius = 1300f
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val wideLayout = maxWidth >= 980.dp
        val showBrandText = maxWidth >= 820.dp
        val showModeLabels = maxWidth >= 940.dp

        Column(Modifier.fillMaxSize()) {
            NovellumTopBar(
                activeMode = activeMode,
                leftPanelOpen = leftPanelOpen,
                rightPanelOpen = rightPanelOpen,
                showBrandText = showBrandText,
                showModeLabels = showModeLabels,
                onModeSelected = { mode ->
                    when (mode) {
                        WorkspaceMode.Editor -> activeMode = WorkspaceMode.Editor
                        WorkspaceMode.Manuscript -> {
                            val chapter = currentChapter ?: chapters.minByOrNull { it.orderIndex }
                            if (chapter == null) {
                                localMessage = "Create a chapter before opening manuscript preview."
                            } else {
                                previewChapterId = chapter.id
                                activeMode = WorkspaceMode.Manuscript
                                expandedChapterIds = expandedChapterIds + chapter.id
                                viewModel.clearSceneSelection()
                            }
                        }
                        WorkspaceMode.Vault -> {
                            activeMode = WorkspaceMode.Vault
                            auxiliaryTab = AuxiliaryTab.Vault
                            rightPanelOpen = true
                            if (!wideLayout) leftPanelOpen = false
                        }
                        WorkspaceMode.Library -> {
                            activeMode = WorkspaceMode.Library
                            auxiliaryTab = AuxiliaryTab.Library
                            rightPanelOpen = true
                            if (!wideLayout) leftPanelOpen = false
                        }
                        WorkspaceMode.Cards -> unavailable("Structure")
                    }
                },
                onToggleLeftPanel = {
                    leftPanelOpen = !leftPanelOpen
                    if (leftPanelOpen && !wideLayout) rightPanelOpen = false
                },
                onToggleRightPanel = {
                    rightPanelOpen = !rightPanelOpen
                    if (rightPanelOpen && !wideLayout) leftPanelOpen = false
                },
                onUnavailableUtility = ::unavailable
            )

            if (wideLayout) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            start = WorkspaceMetrics.OuterPadding,
                            end = WorkspaceMetrics.OuterPadding,
                            bottom = WorkspaceMetrics.OuterPadding
                        )
                ) {
                    AnimatedVisibility(
                        visible = leftPanelOpen,
                        enter = expandHorizontally(animationSpec = spring(stiffness = 410f, dampingRatio = .88f)) + fadeIn(),
                        exit = shrinkHorizontally(animationSpec = spring(stiffness = 470f, dampingRatio = .9f)) + fadeOut()
                    ) {
                        Row {
                            ManuscriptSidebar(
                            projects = projects,
                            selectedProject = selectedProject,
                            chapters = chapters,
                            scenes = scenes,
                            selectedSceneId = selectedSceneId,
                            selectedChapterId = previewChapterId,
                            expandedChapterIds = expandedChapterIds,
                            onProjectSelected = viewModel::selectProject,
                            onShowProjectList = viewModel::clearProjectSelection,
                            onNewProject = viewModel::createNextProject,
                            onNewChapter = viewModel::createNextChapter,
                            onNewScene = { chapterId ->
                                expandedChapterIds = expandedChapterIds + chapterId
                                viewModel.createNextScene(chapterId)
                            },
                            onChapterSelected = { chapterId ->
                                previewChapterId = chapterId
                                activeMode = WorkspaceMode.Manuscript
                                expandedChapterIds = expandedChapterIds + chapterId
                                viewModel.clearSceneSelection()
                            },
                            onSceneSelected = { sceneId ->
                                val scene = scenes.firstOrNull { it.id == sceneId }
                                if (scene != null) expandedChapterIds = expandedChapterIds + scene.chapterId
                                previewChapterId = null
                                activeMode = WorkspaceMode.Editor
                                viewModel.selectScene(sceneId)
                            },
                            onToggleChapter = { chapterId ->
                                expandedChapterIds = if (chapterId in expandedChapterIds) {
                                    expandedChapterIds - chapterId
                                } else expandedChapterIds + chapterId
                            },
                            onRenameProject = viewModel::renameProject,
                            onRenameChapter = viewModel::renameChapter,
                            onRenameScene = viewModel::renameScene,
                            onDeleteProject = viewModel::deleteProject,
                            onDeleteChapter = viewModel::deleteChapter,
                            onDeleteScene = viewModel::deleteScene,
                            onBackup = ::requestBackup,
                            onExport = ::requestMarkdownExport,
                            onUnavailableAction = ::unavailable,
                            modifier = Modifier.width(WorkspaceMetrics.LeftPanelWidth)
                            )
                            Spacer(Modifier.width(WorkspaceMetrics.PanelGap))
                        }
                    }

                    WritingWorkspace(
                        selectedProject = selectedProject,
                        currentScene = currentScene,
                        previewChapter = previewChapter,
                        previewScenes = previewScenes,
                        saveState = saveState,
                        lastSavedTime = lastSavedTime,
                        onSyncScene = viewModel::syncSceneState,
                        onProseChanged = viewModel::onProseChanged,
                        onSaveNow = { viewModel.forceSaveCurrentScene() },
                        onConfirmIntentionalClear = { viewModel.forceSaveCurrentScene(isUserIntentClear = true) },
                        onEditScene = { editTarget = EditTarget.Scene(it) },
                        onSelectScene = { sceneId ->
                            previewChapterId = null
                            activeMode = WorkspaceMode.Editor
                            viewModel.selectScene(sceneId)
                        },
                        onCreateChapter = viewModel::createNextChapter,
                        onUnavailableAction = ::unavailable,
                        modifier = Modifier.weight(1f)
                    )

                    AnimatedVisibility(
                        visible = rightPanelOpen,
                        enter = expandHorizontally(expandFrom = Alignment.End, animationSpec = spring(stiffness = 410f, dampingRatio = .88f)) + fadeIn(),
                        exit = shrinkHorizontally(shrinkTowards = Alignment.End, animationSpec = spring(stiffness = 470f, dampingRatio = .9f)) + fadeOut()
                    ) {
                        Row {
                            Spacer(Modifier.width(WorkspaceMetrics.PanelGap))
                            AuxiliaryWorkspace(
                            selectedTab = auxiliaryTab,
                            selectedProject = selectedProject,
                            selectedChapter = currentChapter,
                            selectedScene = currentScene,
                            onTabSelected = {
                                auxiliaryTab = it
                                activeMode = when (it) {
                                    AuxiliaryTab.Vault -> WorkspaceMode.Vault
                                    AuxiliaryTab.Library -> WorkspaceMode.Library
                                    AuxiliaryTab.Notes -> activeMode
                                }
                            },
                            onUnavailableAction = ::unavailable,
                            modifier = Modifier.width(WorkspaceMetrics.RightPanelWidth)
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            start = WorkspaceMetrics.OuterPadding,
                            end = WorkspaceMetrics.OuterPadding,
                            bottom = WorkspaceMetrics.OuterPadding
                        )
                ) {
                    WritingWorkspace(
                        selectedProject = selectedProject,
                        currentScene = currentScene,
                        previewChapter = previewChapter,
                        previewScenes = previewScenes,
                        saveState = saveState,
                        lastSavedTime = lastSavedTime,
                        onSyncScene = viewModel::syncSceneState,
                        onProseChanged = viewModel::onProseChanged,
                        onSaveNow = { viewModel.forceSaveCurrentScene() },
                        onConfirmIntentionalClear = { viewModel.forceSaveCurrentScene(isUserIntentClear = true) },
                        onEditScene = { editTarget = EditTarget.Scene(it) },
                        onSelectScene = { sceneId ->
                            previewChapterId = null
                            activeMode = WorkspaceMode.Editor
                            viewModel.selectScene(sceneId)
                        },
                        onCreateChapter = viewModel::createNextChapter,
                        onUnavailableAction = ::unavailable,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (leftPanelOpen || rightPanelOpen) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(WorkspaceColors.Scrim)
                        )
                    }

                    AnimatedOverlayPanel(
                        visible = leftPanelOpen,
                        fromStart = true,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        ManuscriptSidebar(
                            projects = projects,
                            selectedProject = selectedProject,
                            chapters = chapters,
                            scenes = scenes,
                            selectedSceneId = selectedSceneId,
                            selectedChapterId = previewChapterId,
                            expandedChapterIds = expandedChapterIds,
                            onProjectSelected = viewModel::selectProject,
                            onShowProjectList = viewModel::clearProjectSelection,
                            onNewProject = viewModel::createNextProject,
                            onNewChapter = viewModel::createNextChapter,
                            onNewScene = { chapterId ->
                                expandedChapterIds = expandedChapterIds + chapterId
                                viewModel.createNextScene(chapterId)
                            },
                            onChapterSelected = { chapterId ->
                                previewChapterId = chapterId
                                activeMode = WorkspaceMode.Manuscript
                                viewModel.clearSceneSelection()
                                leftPanelOpen = false
                            },
                            onSceneSelected = { sceneId ->
                                previewChapterId = null
                                activeMode = WorkspaceMode.Editor
                                viewModel.selectScene(sceneId)
                                leftPanelOpen = false
                            },
                            onToggleChapter = { chapterId ->
                                expandedChapterIds = if (chapterId in expandedChapterIds) {
                                    expandedChapterIds - chapterId
                                } else expandedChapterIds + chapterId
                            },
                            onRenameProject = viewModel::renameProject,
                            onRenameChapter = viewModel::renameChapter,
                            onRenameScene = viewModel::renameScene,
                            onDeleteProject = viewModel::deleteProject,
                            onDeleteChapter = viewModel::deleteChapter,
                            onDeleteScene = viewModel::deleteScene,
                            onBackup = ::requestBackup,
                            onExport = ::requestMarkdownExport,
                            onUnavailableAction = ::unavailable,
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(WorkspaceMetrics.OverlayPanelWidth)
                                .shadow(24.dp, RoundedCornerShape(WorkspaceMetrics.PanelRadius))
                                .zIndex(2f)
                        )
                    }

                    AnimatedOverlayPanel(
                        visible = rightPanelOpen,
                        fromStart = false,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        AuxiliaryWorkspace(
                            selectedTab = auxiliaryTab,
                            selectedProject = selectedProject,
                            selectedChapter = currentChapter,
                            selectedScene = currentScene,
                            onTabSelected = { auxiliaryTab = it },
                            onUnavailableAction = ::unavailable,
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(WorkspaceMetrics.OverlayPanelWidth)
                                .shadow(24.dp, RoundedCornerShape(WorkspaceMetrics.PanelRadius))
                                .zIndex(2f)
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(22.dp)
        )
    }

    editTarget?.let { target ->
        EntityEditorDialog(
            heading = "Edit ${target.label}",
            initialTitle = target.title,
            destructiveLabel = "Remove ${target.label.lowercase()}",
            onDismiss = { editTarget = null },
            onSave = { newTitle ->
                when (target) {
                    is EditTarget.Project -> viewModel.renameProject(target.entity.id, newTitle)
                    is EditTarget.Chapter -> viewModel.renameChapter(target.entity.id, newTitle)
                    is EditTarget.Scene -> viewModel.renameScene(target.entity.id, newTitle)
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
            detail = "“${target.title}” will use Novellum’s existing safe removal path. This UI does not bypass recovery or authored-data safeguards.",
            onDismiss = { deleteTarget = null },
            onConfirm = {
                when (target) {
                    is EditTarget.Project -> viewModel.deleteProject(target.entity.id)
                    is EditTarget.Chapter -> viewModel.deleteChapter(target.entity.id)
                    is EditTarget.Scene -> viewModel.deleteScene(target.entity.id)
                }
                deleteTarget = null
                if (target is EditTarget.Chapter && previewChapterId == target.entity.id) {
                    previewChapterId = null
                }
            }
        )
    }
}

@Composable
private fun AnimatedOverlayPanel(
    visible: Boolean,
    fromStart: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            initialOffsetX = { width -> if (fromStart) -width else width },
            animationSpec = spring(stiffness = 400f, dampingRatio = .88f)
        ) + fadeIn(),
        exit = slideOutHorizontally(
            targetOffsetX = { width -> if (fromStart) -width else width },
            animationSpec = spring(stiffness = 470f, dampingRatio = .9f)
        ) + fadeOut(),
        modifier = modifier
    ) {
        content()
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

private fun writeDocument(
    context: Context,
    uri: android.net.Uri,
    payload: String,
    successMessage: String
): String {
    return try {
        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
            writer.write(payload)
        } ?: return "Android could not open the selected destination."
        successMessage
    } catch (error: Exception) {
        error.message ?: "The document could not be written."
    }
}

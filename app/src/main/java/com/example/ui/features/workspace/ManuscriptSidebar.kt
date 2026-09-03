package com.example.ui.features.workspace

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ChapterEntity
import com.example.data.entity.ProjectEntity
import com.example.data.entity.SceneEntity

private sealed interface TreeDeleteRequest {
    val title: String

    data class Project(
        val entity: ProjectEntity,
        val chapterCount: Int,
        val sceneCount: Int,
        val words: Int
    ) : TreeDeleteRequest { override val title = entity.title }

    data class Chapter(
        val entity: ChapterEntity,
        val sceneCount: Int,
        val words: Int
    ) : TreeDeleteRequest { override val title = entity.title }

    data class Scene(val entity: SceneEntity) : TreeDeleteRequest { override val title = entity.title }
}

@Composable
internal fun ManuscriptSidebar(
    projects: List<ProjectEntity>,
    selectedProject: ProjectEntity?,
    chapters: List<ChapterEntity>,
    scenes: List<SceneEntity>,
    selectedSceneId: String?,
    selectedChapterId: String?,
    expandedChapterIds: Set<String>,
    onProjectSelected: (String) -> Unit,
    onShowProjectList: () -> Unit,
    onNewProject: () -> Unit,
    onNewChapter: () -> Unit,
    onNewScene: (String) -> Unit,
    onChapterSelected: (String) -> Unit,
    onSceneSelected: (String) -> Unit,
    onToggleChapter: (String) -> Unit,
    onRenameProject: (String, String) -> Unit,
    onRenameChapter: (String, String) -> Unit,
    onRenameScene: (String, String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onDeleteChapter: (String) -> Unit,
    onDeleteScene: (String) -> Unit,
    onBackup: () -> Unit,
    onExport: () -> Unit,
    onUnavailableAction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    PanelSurface(modifier = modifier.fillMaxHeight()) {
        if (selectedProject == null) {
            ProjectShelf(
                projects = projects,
                onProjectSelected = onProjectSelected,
                onNewProject = onNewProject,
                modifier = Modifier.fillMaxHeight()
            )
        } else {
            ManuscriptTree(
                selectedProject = selectedProject,
                chapters = chapters,
                scenes = scenes,
                selectedSceneId = selectedSceneId,
                selectedChapterId = selectedChapterId,
                expandedChapterIds = expandedChapterIds,
                onShowProjectList = onShowProjectList,
                onNewChapter = onNewChapter,
                onNewScene = onNewScene,
                onChapterSelected = onChapterSelected,
                onSceneSelected = onSceneSelected,
                onToggleChapter = onToggleChapter,
                onRenameProject = onRenameProject,
                onRenameChapter = onRenameChapter,
                onRenameScene = onRenameScene,
                onDeleteProject = onDeleteProject,
                onDeleteChapter = onDeleteChapter,
                onDeleteScene = onDeleteScene,
                onBackup = onBackup,
                onExport = onExport,
                onUnavailableAction = onUnavailableAction,
                modifier = Modifier.fillMaxHeight()
            )
        }
    }
}

@Composable
private fun ProjectShelf(
    projects: List<ProjectEntity>,
    onProjectSelected: (String) -> Unit,
    onNewProject: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("PROJECT SHELF", style = WorkspaceType.Eyebrow, modifier = Modifier.weight(1f))
            CompactIconButton(WorkspaceIcon.Add, "Create project", onNewProject)
        }
        Text("Choose a manuscript", style = WorkspaceType.PreviewTitle, modifier = Modifier.padding(top = 10.dp, bottom = 18.dp))
        Hairline()
        if (projects.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NovellumIcon(WorkspaceIcon.Project, WorkspaceColors.TextMuted, Modifier.size(38.dp))
                Text("No projects yet", style = WorkspaceType.UiStrong, modifier = Modifier.padding(top = 14.dp))
                Text("Create a project to begin a manuscript.", style = WorkspaceType.UiSmall, modifier = Modifier.padding(top = 5.dp))
                CompactTextButton("New project", onNewProject, Modifier.padding(top = 18.dp), selected = true, leadingIcon = WorkspaceIcon.Add)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(projects, key = { it.id }) { project ->
                    ProjectShelfRow(project, onClick = { onProjectSelected(project.id) })
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProjectShelfRow(project: ProjectEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WorkspaceMetrics.ControlRadius))
            .background(WorkspaceColors.Deep.copy(alpha = .64f))
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NovellumIcon(WorkspaceIcon.Project, WorkspaceColors.Accent, Modifier.size(20.dp))
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(project.title, style = WorkspaceType.UiStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (project.description.isNotBlank()) {
                Text(project.description, style = WorkspaceType.UiSmall.copy(color = WorkspaceColors.TextMuted), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ManuscriptTree(
    selectedProject: ProjectEntity,
    chapters: List<ChapterEntity>,
    scenes: List<SceneEntity>,
    selectedSceneId: String?,
    selectedChapterId: String?,
    expandedChapterIds: Set<String>,
    onShowProjectList: () -> Unit,
    onNewChapter: () -> Unit,
    onNewScene: (String) -> Unit,
    onChapterSelected: (String) -> Unit,
    onSceneSelected: (String) -> Unit,
    onToggleChapter: (String) -> Unit,
    onRenameProject: (String, String) -> Unit,
    onRenameChapter: (String, String) -> Unit,
    onRenameScene: (String, String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onDeleteChapter: (String) -> Unit,
    onDeleteScene: (String) -> Unit,
    onBackup: () -> Unit,
    onExport: () -> Unit,
    onUnavailableAction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember(selectedProject.id) { mutableStateOf("") }
    var managingKey by remember(selectedProject.id) { mutableStateOf<String?>(null) }
    var deleteRequest by remember(selectedProject.id) { mutableStateOf<TreeDeleteRequest?>(null) }

    val sortedChapters = remember(chapters) { chapters.sortedBy { it.orderIndex } }
    val sceneMatches = remember(query, scenes) {
        if (query.isBlank()) scenes.mapTo(mutableSetOf()) { it.id }
        else scenes.filter { it.title.contains(query, ignoreCase = true) }.mapTo(mutableSetOf()) { it.id }
    }
    val chapterMatches = remember(query, chapters, scenes) {
        if (query.isBlank()) chapters.mapTo(mutableSetOf()) { it.id }
        else chapters.filter { chapter ->
            chapter.title.contains(query, ignoreCase = true) || scenes.any { it.chapterId == chapter.id && it.id in sceneMatches }
        }.mapTo(mutableSetOf()) { it.id }
    }

    Column(modifier) {
        Column(Modifier.padding(start = 18.dp, end = 14.dp, top = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("MANUSCRIPT", style = WorkspaceType.Eyebrow, modifier = Modifier.weight(1f))
                CompactIconButton(WorkspaceIcon.Add, "Add chapter", onNewChapter, size = 31.dp, iconSize = 16.dp)
            }
            Text("PROJECT", style = WorkspaceType.Eyebrow, modifier = Modifier.padding(top = 10.dp))
            val projectKey = "project-${selectedProject.id}"
            if (managingKey == projectKey) {
                InlineTitleManager(
                    initialTitle = selectedProject.title,
                    accent = true,
                    onSave = {
                        onRenameProject(selectedProject.id, it)
                        managingKey = null
                    },
                    onCancel = { managingKey = null },
                    onDelete = {
                        if (chapters.isEmpty()) {
                            onDeleteProject(selectedProject.id)
                            managingKey = null
                        } else {
                            deleteRequest = TreeDeleteRequest.Project(
                                selectedProject,
                                chapters.size,
                                scenes.size,
                                scenes.sumOf { wordCount(it.prose) }
                            )
                        }
                    },
                    modifier = Modifier.padding(top = 3.dp)
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(WorkspaceMetrics.ControlRadius))
                        .combinedClickable(
                            onClick = onShowProjectList,
                            onLongClick = { managingKey = projectKey }
                        )
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        selectedProject.title,
                        style = WorkspaceType.UiStrong.copy(color = WorkspaceColors.AccentBright, fontSize = 16.sp),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    NovellumIcon(WorkspaceIcon.ChevronDown, WorkspaceColors.TextSecondary, Modifier.size(16.dp))
                }
            }
            SearchShell(
                value = query,
                placeholder = "Search manuscript…",
                onValueChange = { query = it },
                onFilter = { onUnavailableAction("Manuscript filters") },
                modifier = Modifier.padding(top = 12.dp, bottom = 10.dp)
            )
        }

        Hairline()

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 9.dp, end = 9.dp, top = 8.dp, bottom = 10.dp)
        ) {
            sortedChapters.forEachIndexed { chapterIndex, chapter ->
                if (chapter.id !in chapterMatches) return@forEachIndexed
                val chapterPosition = chapterIndex + 1
                val chapterKey = "chapter-${chapter.id}"
                item(key = chapterKey) {
                    ChapterTreeRow(
                        chapter = chapter,
                        position = chapterPosition,
                        selected = chapter.id == selectedChapterId && selectedSceneId == null,
                        expanded = chapter.id in expandedChapterIds,
                        managing = managingKey == chapterKey,
                        onToggle = { onToggleChapter(chapter.id) },
                        onSelect = { onChapterSelected(chapter.id) },
                        onAddScene = { onNewScene(chapter.id) },
                        onManage = { managingKey = chapterKey },
                        onSaveTitle = {
                            onRenameChapter(chapter.id, normalizeStoredTitle(it, "Chapter", chapterPosition))
                            managingKey = null
                        },
                        onCancelManage = { managingKey = null },
                        onDelete = {
                            val children = scenes.filter { it.chapterId == chapter.id }
                            if (children.isEmpty()) {
                                onDeleteChapter(chapter.id)
                                managingKey = null
                            } else {
                                deleteRequest = TreeDeleteRequest.Chapter(chapter, children.size, children.sumOf { wordCount(it.prose) })
                            }
                        }
                    )
                }

                val allChapterScenes = scenes.filter { it.chapterId == chapter.id }.sortedBy { it.orderIndex }
                val visibleScenes = allChapterScenes.filter { it.id in sceneMatches }
                item(key = "children-${chapter.id}") {
                    AnimatedVisibility(
                        visible = chapter.id in expandedChapterIds,
                        enter = expandVertically(animationSpec = spring(stiffness = 430f, dampingRatio = .86f)) + fadeIn(),
                        exit = shrinkVertically(animationSpec = spring(stiffness = 500f, dampingRatio = .9f)) + fadeOut()
                    ) {
                        Column {
                            visibleScenes.forEach { scene ->
                                val scenePosition = allChapterScenes.indexOfFirst { it.id == scene.id } + 1
                                val sceneKey = "scene-${scene.id}"
                                SceneTreeRow(
                                    scene = scene,
                                    position = scenePosition,
                                    selected = scene.id == selectedSceneId,
                                    managing = managingKey == sceneKey,
                                    onSelect = { onSceneSelected(scene.id) },
                                    onManage = { managingKey = sceneKey },
                                    onSaveTitle = {
                                        onRenameScene(scene.id, normalizeStoredTitle(it, "Scene", scenePosition))
                                        managingKey = null
                                    },
                                    onCancelManage = { managingKey = null },
                                    onDelete = {
                                        if (scene.prose.isBlank()) {
                                            onDeleteScene(scene.id)
                                            managingKey = null
                                        } else {
                                            deleteRequest = TreeDeleteRequest.Scene(scene)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (chapters.isEmpty()) {
                item("empty-manuscript") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        NovellumIcon(WorkspaceIcon.Folder, WorkspaceColors.TextMuted, Modifier.size(32.dp))
                        Text("This manuscript has no chapters.", style = WorkspaceType.UiSmall, modifier = Modifier.padding(top = 10.dp))
                        CompactTextButton("Add chapter", onNewChapter, Modifier.padding(top = 12.dp), selected = true, leadingIcon = WorkspaceIcon.Add)
                    }
                }
            }
        }

        ManuscriptFooter(
            chapterCount = chapters.size,
            sceneCount = scenes.size,
            words = scenes.sumOf { wordCount(it.prose) },
            onSearch = { onUnavailableAction("Expanded search") },
            onStats = { onUnavailableAction("Manuscript statistics") },
            onBackup = onBackup,
            onExport = onExport
        )
    }

    deleteRequest?.let { target ->
        when (target) {
            is TreeDeleteRequest.Scene -> {
                    HoldDeleteDialog(
                        title = displaySceneTitle(target.entity.title, 1),
                        detail = "This scene contains about ${wordCount(target.entity.prose)} words. Press and hold the delete control to move it through Novellum’s safe removal path.",
                        onDismiss = { deleteRequest = null },
                        onConfirm = {
                            onDeleteScene(target.entity.id)
                            deleteRequest = null
                            managingKey = null
                        }
                    )
            }
            is TreeDeleteRequest.Chapter -> {
                    TypeDeleteDialog(
                        heading = "Remove chapter?",
                        detail = "This chapter contains ${target.sceneCount} scene${if (target.sceneCount == 1) "" else "s"} and about ${target.words} words. Type DELETE to enable safe removal.",
                        onDismiss = { deleteRequest = null },
                        onConfirm = {
                            onDeleteChapter(target.entity.id)
                            deleteRequest = null
                            managingKey = null
                        }
                    )
            }
            is TreeDeleteRequest.Project -> {
                    TypeDeleteDialog(
                        heading = "Remove project?",
                        detail = "This project contains ${target.chapterCount} chapter${if (target.chapterCount == 1) "" else "s"}, ${target.sceneCount} scene${if (target.sceneCount == 1) "" else "s"}, and about ${target.words} words. Type DELETE to enable safe removal.",
                        onDismiss = { deleteRequest = null },
                        onConfirm = {
                            onDeleteProject(target.entity.id)
                            deleteRequest = null
                            managingKey = null
                        }
                    )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChapterTreeRow(
    chapter: ChapterEntity,
    position: Int,
    selected: Boolean,
    expanded: Boolean,
    managing: Boolean,
    onToggle: () -> Unit,
    onSelect: () -> Unit,
    onAddScene: () -> Unit,
    onManage: () -> Unit,
    onSaveTitle: (String) -> Unit,
    onCancelManage: () -> Unit,
    onDelete: () -> Unit
) {
    val selectedBg by animateColorAsState(
        if (selected) WorkspaceColors.AccentWash else Color.Transparent,
        spring(stiffness = 420f, dampingRatio = .88f),
        label = "chapterSelection"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(WorkspaceMetrics.ControlRadius))
            .background(selectedBg)
            .combinedClickable(onClick = onSelect, onLongClick = onManage)
            .padding(start = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(2.dp).height(19.dp).background(if (selected) WorkspaceColors.Accent else Color.Transparent))
        CompactIconButton(
            if (expanded) WorkspaceIcon.ChevronDown else WorkspaceIcon.ChevronRight,
            if (expanded) "Collapse ${chapter.title}" else "Expand ${chapter.title}",
            onToggle,
            size = 27.dp,
            iconSize = 13.dp
        )
        NovellumIcon(WorkspaceIcon.Folder, if (selected) WorkspaceColors.Accent else WorkspaceColors.TextSecondary, Modifier.size(17.dp))
        if (managing) {
            InlineTitleManager(
                initialTitle = editableDisplayTitle(chapter.title, "Chapter", position),
                onSave = onSaveTitle,
                onCancel = onCancelManage,
                onDelete = onDelete,
                modifier = Modifier.weight(1f).padding(start = 7.dp)
            )
        } else {
            Text(
                displayChapterTitle(chapter.title, position),
                style = WorkspaceType.UiStrong.copy(color = if (selected) WorkspaceColors.AccentBright else WorkspaceColors.TextPrimary),
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            CompactIconButton(WorkspaceIcon.Add, "Add scene to ${chapter.title}", onAddScene, size = 28.dp, iconSize = 14.dp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SceneTreeRow(
    scene: SceneEntity,
    position: Int,
    selected: Boolean,
    managing: Boolean,
    onSelect: () -> Unit,
    onManage: () -> Unit,
    onSaveTitle: (String) -> Unit,
    onCancelManage: () -> Unit,
    onDelete: () -> Unit
) {
    val selectedBg by animateColorAsState(
        if (selected) WorkspaceColors.AccentWash else Color.Transparent,
        spring(stiffness = 420f, dampingRatio = .88f),
        label = "sceneSelection"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(37.dp)
            .padding(start = 31.dp)
            .clip(RoundedCornerShape(WorkspaceMetrics.ControlRadius))
            .background(selectedBg)
            .combinedClickable(onClick = onSelect, onLongClick = onManage),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.width(2.dp).height(if (selected) 22.dp else 37.dp)
                .background(if (selected) WorkspaceColors.Accent else WorkspaceColors.Hairline.copy(alpha = .72f))
        )
        NovellumIcon(WorkspaceIcon.Document, if (selected) WorkspaceColors.Accent else WorkspaceColors.TextMuted, Modifier.padding(start = 9.dp).size(16.dp))
        if (managing) {
            InlineTitleManager(
                initialTitle = editableDisplayTitle(scene.title, "Scene", position),
                onSave = onSaveTitle,
                onCancel = onCancelManage,
                onDelete = onDelete,
                modifier = Modifier.weight(1f).padding(start = 7.dp)
            )
        } else {
            Text(
                displaySceneTitle(scene.title, position),
                style = WorkspaceType.Ui.copy(
                    color = if (selected) WorkspaceColors.AccentBright else WorkspaceColors.TextSecondary,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                ),
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (selected) {
                Box(Modifier.padding(end = 10.dp).size(6.dp).clip(RoundedCornerShape(50)).background(WorkspaceColors.Accent))
            }
        }
    }
}

@Composable
private fun InlineTitleManager(
    initialTitle: String,
    onSave: (String) -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false
) {
    var value by remember(initialTitle) { mutableStateOf(initialTitle) }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        BasicTextField(
            value = value,
            onValueChange = { value = it },
            singleLine = true,
            cursorBrush = SolidColor(WorkspaceColors.Accent),
            textStyle = WorkspaceType.UiStrong.copy(color = if (accent) WorkspaceColors.AccentBright else WorkspaceColors.TextPrimary),
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(7.dp))
                .background(WorkspaceColors.Deep.copy(alpha = .72f))
                .padding(horizontal = 8.dp, vertical = 5.dp)
        )
        CompactIconButton(WorkspaceIcon.Check, "Save title", { value.trim().takeIf { it.isNotEmpty() }?.let(onSave) }, size = 28.dp, iconSize = 14.dp, selected = true)
        CompactIconButton(WorkspaceIcon.Delete, "Delete", onDelete, size = 28.dp, iconSize = 14.dp)
        CompactIconButton(WorkspaceIcon.Close, "Cancel management", onCancel, size = 28.dp, iconSize = 14.dp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HoldDeleteDialog(
    title: String,
    detail: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WorkspaceColors.PanelRaised,
        title = { Text("Remove scene?", style = WorkspaceType.UiStrong.copy(fontSize = 16.sp)) },
        text = { Text(detail, style = WorkspaceType.Ui.copy(color = WorkspaceColors.TextSecondary)) },
        confirmButton = {
            Text(
                "PRESS & HOLD DELETE",
                style = WorkspaceType.UiSmall.copy(color = WorkspaceColors.Danger, fontWeight = FontWeight.SemiBold),
                modifier = Modifier
                    .clip(RoundedCornerShape(WorkspaceMetrics.ControlRadius))
                    .background(WorkspaceColors.Danger.copy(alpha = .07f))
                    .combinedClickable(onClick = {}, onLongClick = onConfirm)
                    .padding(horizontal = 14.dp, vertical = 11.dp)
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Keep $title", color = WorkspaceColors.TextSecondary) } }
    )
}

@Composable
private fun TypeDeleteDialog(
    heading: String,
    detail: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var typed by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WorkspaceColors.PanelRaised,
        title = { Text(heading, style = WorkspaceType.UiStrong.copy(fontSize = 16.sp)) },
        text = {
            Column {
                Text(detail, style = WorkspaceType.Ui.copy(color = WorkspaceColors.TextSecondary))
                BasicTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    singleLine = true,
                    cursorBrush = SolidColor(WorkspaceColors.Danger),
                    textStyle = WorkspaceType.UiStrong.copy(color = WorkspaceColors.TextPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .clip(RoundedCornerShape(WorkspaceMetrics.ControlRadius))
                        .background(WorkspaceColors.Deep)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }
        },
        confirmButton = {
            TextButton(enabled = typed == "DELETE", onClick = onConfirm) {
                Text("Delete", color = if (typed == "DELETE") WorkspaceColors.Danger else WorkspaceColors.TextMuted)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = WorkspaceColors.TextSecondary) } }
    )
}

private fun displayChapterTitle(raw: String, position: Int): String =
    if (isDefaultTitle(raw, "Chapter")) "Chapter $position" else "Ch $position · ${raw.trim()}"

private fun displaySceneTitle(raw: String, position: Int): String =
    if (isDefaultTitle(raw, "Scene")) "Scene $position" else "Sc $position · ${raw.trim()}"

private fun editableDisplayTitle(raw: String, kind: String, position: Int): String =
    if (isDefaultTitle(raw, kind)) "$kind $position" else raw.trim()

private fun normalizeStoredTitle(value: String, kind: String, position: Int): String =
    if (value.trim().equals("$kind $position", ignoreCase = true)) kind else value.trim()

private fun isDefaultTitle(raw: String, kind: String): Boolean =
    raw.trim().matches(Regex("(?i)^${kind}(?:\\s+\\d+)?$"))

@Composable
private fun ManuscriptFooter(
    chapterCount: Int,
    sceneCount: Int,
    words: Int,
    onSearch: () -> Unit,
    onStats: () -> Unit,
    onBackup: () -> Unit,
    onExport: () -> Unit
) {
    Hairline()
    Row(Modifier.fillMaxWidth().height(62.dp), verticalAlignment = Alignment.CenterVertically) {
        StatCell("WORDS", words.toString(), Modifier.weight(1f))
        VerticalHairline()
        StatCell("SCENES", sceneCount.toString(), Modifier.weight(1f))
        VerticalHairline()
        StatCell("CHAPTERS", chapterCount.toString(), Modifier.weight(1f))
    }
    Hairline()
    Row(
        modifier = Modifier.fillMaxWidth().height(68.dp).padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FooterAction("Search", WorkspaceIcon.Search, onSearch)
        FooterAction("Stats", WorkspaceIcon.Stats, onStats)
        FooterAction("Backup", WorkspaceIcon.Backup, onBackup)
        FooterAction("Export", WorkspaceIcon.Export, onExport)
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = WorkspaceType.Eyebrow.copy(letterSpacing = .6.sp))
        Text(value, style = WorkspaceType.UiStrong.copy(color = WorkspaceColors.Accent, fontSize = 16.sp), modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun VerticalHairline() {
    Spacer(Modifier.width(1.dp).height(34.dp).background(WorkspaceColors.Hairline.copy(alpha = .68f)))
}

@Composable
private fun FooterAction(label: String, icon: WorkspaceIcon, onClick: () -> Unit) {
    Column(Modifier.widthIn(min = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CompactIconButton(icon, label, onClick, size = 32.dp, iconSize = 18.dp)
        Text(label, style = WorkspaceType.UiSmall.copy(color = WorkspaceColors.TextMuted))
    }
}

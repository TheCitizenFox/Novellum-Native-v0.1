package com.example.ui.features.workspace

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ChapterEntity
import com.example.data.entity.ProjectEntity
import com.example.data.entity.SceneEntity

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
    onEditProject: (ProjectEntity) -> Unit,
    onEditChapter: (ChapterEntity) -> Unit,
    onEditScene: (SceneEntity) -> Unit,
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
                onEditProject = onEditProject,
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
                onEditProject = onEditProject,
                onEditChapter = onEditChapter,
                onEditScene = onEditScene,
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
    onEditProject: (ProjectEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.padding(18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("PROJECT SHELF", style = WorkspaceType.Eyebrow, modifier = Modifier.weight(1f))
            CompactIconButton(
                icon = WorkspaceIcon.Add,
                description = "Create project",
                onClick = onNewProject
            )
        }
        Text(
            text = "Choose a manuscript",
            style = WorkspaceType.PreviewTitle,
            modifier = Modifier.padding(top = 10.dp, bottom = 18.dp)
        )
        Hairline()
        if (projects.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NovellumIcon(
                    WorkspaceIcon.Project,
                    WorkspaceColors.TextMuted,
                    Modifier.size(38.dp)
                )
                Text(
                    "No projects yet",
                    style = WorkspaceType.UiStrong,
                    modifier = Modifier.padding(top = 14.dp)
                )
                Text(
                    "Create a project to begin a manuscript.",
                    style = WorkspaceType.UiSmall,
                    modifier = Modifier.padding(top = 5.dp)
                )
                CompactTextButton(
                    label = "New project",
                    leadingIcon = WorkspaceIcon.Add,
                    selected = true,
                    onClick = onNewProject,
                    modifier = Modifier.padding(top = 18.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
            ) {
                items(projects, key = { it.id }) { project ->
                    ProjectShelfRow(
                        project = project,
                        onClick = { onProjectSelected(project.id) },
                        onEdit = { onEditProject(project) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProjectShelfRow(
    project: ProjectEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WorkspaceMetrics.ControlRadius))
            .background(WorkspaceColors.Deep.copy(alpha = .64f))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onEdit
            )
            .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NovellumIcon(WorkspaceIcon.Project, WorkspaceColors.Accent, Modifier.size(20.dp))
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(
                project.title,
                style = WorkspaceType.UiStrong,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (project.description.isNotBlank()) {
                Text(
                    project.description,
                    style = WorkspaceType.UiSmall.copy(color = WorkspaceColors.TextMuted),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        CompactIconButton(
            icon = WorkspaceIcon.More,
            description = "Edit ${project.title}",
            onClick = onEdit,
            size = 30.dp,
            iconSize = 15.dp
        )
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
    onEditProject: (ProjectEntity) -> Unit,
    onEditChapter: (ChapterEntity) -> Unit,
    onEditScene: (SceneEntity) -> Unit,
    onBackup: () -> Unit,
    onExport: () -> Unit,
    onUnavailableAction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember(selectedProject.id) { mutableStateOf("") }
    val sceneMatches = remember(query, scenes) {
        if (query.isBlank()) scenes.mapTo(mutableSetOf()) { it.id }
        else scenes.filter { it.title.contains(query, ignoreCase = true) }.mapTo(mutableSetOf()) { it.id }
    }
    val chapterMatches = remember(query, chapters, scenes) {
        if (query.isBlank()) chapters.mapTo(mutableSetOf()) { it.id }
        else chapters.filter { chapter ->
            chapter.title.contains(query, ignoreCase = true) ||
                scenes.any { it.chapterId == chapter.id && it.id in sceneMatches }
        }.mapTo(mutableSetOf()) { it.id }
    }

    Column(modifier) {
        Column(Modifier.padding(start = 18.dp, end = 14.dp, top = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("MANUSCRIPT", style = WorkspaceType.Eyebrow, modifier = Modifier.weight(1f))
                CompactIconButton(
                    icon = WorkspaceIcon.Add,
                    description = "Add chapter",
                    onClick = onNewChapter,
                    size = 31.dp,
                    iconSize = 16.dp
                )
                CompactIconButton(
                    icon = WorkspaceIcon.More,
                    description = "Edit project",
                    onClick = { onEditProject(selectedProject) },
                    size = 31.dp,
                    iconSize = 16.dp
                )
            }
            Text("PROJECT", style = WorkspaceType.Eyebrow, modifier = Modifier.padding(top = 12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WorkspaceMetrics.ControlRadius))
                    .combinedClickable(
                        onClick = onShowProjectList,
                        onLongClick = { onEditProject(selectedProject) }
                    )
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    selectedProject.title,
                    style = WorkspaceType.UiStrong.copy(
                        color = WorkspaceColors.AccentBright,
                        fontSize = 17.sp
                    ),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                NovellumIcon(
                    WorkspaceIcon.ChevronDown,
                    WorkspaceColors.TextSecondary,
                    Modifier.size(16.dp)
                )
            }
            SearchShell(
                value = query,
                placeholder = "Search manuscript…",
                onValueChange = { query = it },
                onFilter = { onUnavailableAction("Manuscript filters") },
                modifier = Modifier.padding(top = 13.dp, bottom = 12.dp)
            )
        }

        Hairline()

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 10.dp,
                end = 10.dp,
                top = 10.dp,
                bottom = 12.dp
            )
        ) {
            chapters.sortedBy { it.orderIndex }
                .filter { it.id in chapterMatches }
                .forEach { chapter ->
                    item(key = "chapter-${chapter.id}") {
                        ChapterTreeRow(
                            chapter = chapter,
                            selected = chapter.id == selectedChapterId && selectedSceneId == null,
                            expanded = chapter.id in expandedChapterIds,
                            onToggle = { onToggleChapter(chapter.id) },
                            onSelect = { onChapterSelected(chapter.id) },
                            onAddScene = { onNewScene(chapter.id) },
                            onEdit = { onEditChapter(chapter) }
                        )
                    }
                    if (chapter.id in expandedChapterIds) {
                        val chapterScenes = scenes
                            .filter { it.chapterId == chapter.id && it.id in sceneMatches }
                            .sortedBy { it.orderIndex }
                        items(chapterScenes, key = { "scene-${it.id}" }) { scene ->
                            SceneTreeRow(
                                scene = scene,
                                selected = scene.id == selectedSceneId,
                                onSelect = { onSceneSelected(scene.id) },
                                onEdit = { onEditScene(scene) }
                            )
                        }
                    }
                }

            if (chapters.isEmpty()) {
                item("empty-manuscript") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        NovellumIcon(
                            WorkspaceIcon.Folder,
                            WorkspaceColors.TextMuted,
                            Modifier.size(32.dp)
                        )
                        Text(
                            "This manuscript has no chapters.",
                            style = WorkspaceType.UiSmall,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                        CompactTextButton(
                            label = "Add chapter",
                            leadingIcon = WorkspaceIcon.Add,
                            selected = true,
                            onClick = onNewChapter,
                            modifier = Modifier.padding(top = 12.dp)
                        )
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChapterTreeRow(
    chapter: ChapterEntity,
    selected: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSelect: () -> Unit,
    onAddScene: () -> Unit,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(WorkspaceMetrics.ControlRadius))
            .background(if (selected) WorkspaceColors.AccentWash else Color.Transparent)
            .combinedClickable(onClick = onSelect, onLongClick = onEdit)
            .padding(start = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(20.dp)
                .background(if (selected) WorkspaceColors.Accent else Color.Transparent)
        )
        CompactIconButton(
            icon = if (expanded) WorkspaceIcon.ChevronDown else WorkspaceIcon.ChevronRight,
            description = if (expanded) "Collapse ${chapter.title}" else "Expand ${chapter.title}",
            onClick = onToggle,
            size = 27.dp,
            iconSize = 13.dp
        )
        NovellumIcon(
            WorkspaceIcon.Folder,
            if (selected) WorkspaceColors.Accent else WorkspaceColors.TextSecondary,
            Modifier.size(18.dp)
        )
        Text(
            chapter.title,
            style = WorkspaceType.UiStrong.copy(
                color = if (selected) WorkspaceColors.AccentBright else WorkspaceColors.TextPrimary
            ),
            modifier = Modifier.weight(1f).padding(start = 9.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        CompactIconButton(
            icon = WorkspaceIcon.Add,
            description = "Add scene to ${chapter.title}",
            onClick = onAddScene,
            size = 28.dp,
            iconSize = 14.dp
        )
        CompactIconButton(
            icon = WorkspaceIcon.More,
            description = "Edit ${chapter.title}",
            onClick = onEdit,
            size = 28.dp,
            iconSize = 14.dp
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SceneTreeRow(
    scene: SceneEntity,
    selected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(39.dp)
            .padding(start = 32.dp)
            .clip(RoundedCornerShape(WorkspaceMetrics.ControlRadius))
            .background(if (selected) WorkspaceColors.AccentWash else Color.Transparent)
            .combinedClickable(onClick = onSelect, onLongClick = onEdit),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(if (selected) 23.dp else 39.dp)
                .background(
                    if (selected) WorkspaceColors.Accent
                    else WorkspaceColors.Hairline.copy(alpha = .75f)
                )
        )
        NovellumIcon(
            WorkspaceIcon.Document,
            if (selected) WorkspaceColors.Accent else WorkspaceColors.TextMuted,
            Modifier.padding(start = 10.dp).size(17.dp)
        )
        Text(
            scene.title,
            style = WorkspaceType.Ui.copy(
                color = if (selected) WorkspaceColors.AccentBright else WorkspaceColors.TextSecondary,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
            ),
            modifier = Modifier.weight(1f).padding(start = 9.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .padding(end = 7.dp)
                    .size(7.dp)
                    .clip(RoundedCornerShape(50))
                    .background(WorkspaceColors.Accent)
            )
        } else {
            CompactIconButton(
                icon = WorkspaceIcon.More,
                description = "Edit ${scene.title}",
                onClick = onEdit,
                size = 28.dp,
                iconSize = 14.dp
            )
        }
    }
}

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
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatCell("WORDS", words.toString(), Modifier.weight(1f))
        VerticalHairline()
        StatCell("SCENES", sceneCount.toString(), Modifier.weight(1f))
        VerticalHairline()
        StatCell("CHAPTERS", chapterCount.toString(), Modifier.weight(1f))
    }
    Hairline()
    Row(
        modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 6.dp),
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
        Text(
            value,
            style = WorkspaceType.UiStrong.copy(color = WorkspaceColors.Accent, fontSize = 17.sp),
            modifier = Modifier.padding(top = 3.dp)
        )
    }
}

@Composable
private fun VerticalHairline() {
    Spacer(
        Modifier
            .width(1.dp)
            .height(36.dp)
            .background(WorkspaceColors.Hairline.copy(alpha = .68f))
    )
}

@Composable
private fun FooterAction(label: String, icon: WorkspaceIcon, onClick: () -> Unit) {
    Column(
        modifier = Modifier.widthIn(min = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CompactIconButton(icon, label, onClick, size = 32.dp, iconSize = 19.dp)
        Text(label, style = WorkspaceType.UiSmall.copy(color = WorkspaceColors.TextMuted))
    }
}

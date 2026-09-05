package com.example.ui.features.workspace

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ChapterEntity
import com.example.data.entity.ProjectEntity
import com.example.data.entity.SceneEntity
import com.example.ui.theme.ManuscriptSerif
import com.example.ui.theme.NovellumObsidian
import com.example.ui.theme.NovellumOutline
import com.example.ui.theme.NovellumOutlineVariant
import com.example.ui.theme.NovellumPrimary
import com.example.ui.theme.NovellumPrimaryContainer
import com.example.ui.theme.NovellumSurfaceContainerHigh
import com.example.ui.theme.NovellumSurfaceContainerLow
import com.example.ui.theme.UiSans

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BinderSidebar(
    projects: List<ProjectEntity>,
    selectedProject: ProjectEntity?,
    chapters: List<ChapterEntity>,
    scenes: List<SceneEntity>,
    selectedSceneId: String?,
    selectedChapterId: String?,
    expandedChapterIds: Set<String>,
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    onProjectSelected: (String) -> Unit,
    onProjectScopeSelected: () -> Unit,
    onNewProject: () -> Unit,
    onNewChapter: () -> Unit,
    onNewScene: (String) -> Unit,
    onToggleChapter: (String) -> Unit,
    onChapterSelected: (String) -> Unit,
    onSceneSelected: (String) -> Unit,
    onManageProject: (ProjectEntity) -> Unit,
    onManageChapter: (ChapterEntity) -> Unit,
    onManageScene: (SceneEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var projectMenuOpen by remember { mutableStateOf(false) }
    val visibleChapterIds = chapters.mapTo(mutableSetOf()) { it.id }
    val visibleScenes = scenes.filter { it.chapterId in visibleChapterIds }

    val selectedScene = visibleScenes.firstOrNull { it.id == selectedSceneId }
    val selectedChapter = chapters.firstOrNull { it.id == selectedChapterId }
        ?: selectedScene?.let { scene -> chapters.firstOrNull { it.id == scene.chapterId } }

    val scopeStats = when {
        selectedScene != null -> {
            val words = wordCount(selectedScene.prose)
            ScopeStats("SCENE", words, "CHARS", selectedScene.prose.length, "READ", "${readingMinutes(words)}m")
        }
        selectedChapter != null -> {
            val chapterScenes = visibleScenes.filter { it.chapterId == selectedChapter.id }
            val words = chapterScenes.sumOf { wordCount(it.prose) }
            ScopeStats("CHAPTER", words, "SCENES", chapterScenes.size, "READ", "${readingMinutes(words)}m")
        }
        selectedProject != null -> {
            val words = visibleScenes.sumOf { wordCount(it.prose) }
            ScopeStats("PROJECT", words, "SCENES", visibleScenes.size, "CHAPTERS", chapters.size.toString())
        }
        else -> ScopeStats("LIBRARY", 0, "PROJECTS", projects.size, "", "")
    }

    Column(
        modifier = modifier
            .width(280.dp)
            .fillMaxHeight()
            .background(NovellumObsidian)
            .border(1.dp, NovellumOutlineVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "PROJECT",
                    fontFamily = UiSans,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 1.5.sp,
                    color = NovellumOutline
                )
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .combinedClickable(
                                onClick = { projectMenuOpen = true },
                                onLongClick = { selectedProject?.let(onManageProject) }
                            )
                            .padding(top = 2.dp, end = 8.dp)
                    ) {
                        Text(
                            selectedProject?.let(::projectDisplayTitle) ?: "Choose a manuscript",
                            fontFamily = ManuscriptSerif,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = if (selectedProject == null) MaterialTheme.colorScheme.onSurface else NovellumPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            Icons.Default.UnfoldMore,
                            "Project switcher",
                            tint = NovellumOutline,
                            modifier = Modifier.size(16.dp).padding(start = 4.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = projectMenuOpen,
                        onDismissRequest = { projectMenuOpen = false },
                        containerColor = NovellumSurfaceContainerLow
                    ) {
                        projects.forEach { project ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        projectDisplayTitle(project),
                                        fontFamily = ManuscriptSerif,
                                        fontSize = 14.sp,
                                        color = if (project.id == selectedProject?.id) NovellumPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    projectMenuOpen = false
                                    onProjectSelected(project.id)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("New manuscript", fontFamily = UiSans, fontSize = 12.sp, color = NovellumPrimary) },
                            leadingIcon = { Icon(Icons.Default.Add, null, tint = NovellumPrimary, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                projectMenuOpen = false
                                onNewProject()
                            }
                        )
                    }
                }
            }
            IconButton(
                onClick = { selectedProject?.let(onManageProject) },
                enabled = selectedProject != null,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Tune,
                    "Manuscript settings",
                    tint = if (selectedProject != null) NovellumOutline else NovellumOutline.copy(alpha = 0.3f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DividerLine()

        if (selectedProject == null) {
            Text(
                "MANUSCRIPTS",
                fontFamily = UiSans,
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.5.sp,
                color = NovellumOutline,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
            )
            LazyColumn(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                items(projects, key = { it.id }) { project ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .combinedClickable(
                                onClick = { onProjectSelected(project.id) },
                                onLongClick = { onManageProject(project) }
                            )
                            .padding(horizontal = 8.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, null, tint = NovellumPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(projectDisplayTitle(project), fontFamily = UiSans, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "MANUSCRIPT INDEX",
                    fontFamily = UiSans,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 1.5.sp,
                    color = NovellumOutline,
                    modifier = Modifier.combinedClickable(
                        onClick = onProjectScopeSelected,
                        onLongClick = { onManageProject(selectedProject) }
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { selectedChapter?.let { onNewScene(it.id) } },
                        enabled = selectedChapter != null,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            Icons.Default.PostAdd,
                            "Add scene",
                            tint = if (selectedChapter != null) NovellumOutline else NovellumOutline.copy(alpha = 0.3f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onNewChapter, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.CreateNewFolder, "Add chapter", tint = NovellumOutline, modifier = Modifier.size(16.dp))
                    }
                }
            }

            val filteredChapters = if (searchQuery.isBlank()) chapters else chapters.filter { chapter ->
                chapter.title.contains(searchQuery, ignoreCase = true) ||
                    visibleScenes.any { it.chapterId == chapter.id && it.title.contains(searchQuery, ignoreCase = true) }
            }

            LazyColumn(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                items(filteredChapters, key = { it.id }) { chapter ->
                    val chapterPosition = chapters.indexOfFirst { it.id == chapter.id }.coerceAtLeast(0)
                    val allChapterScenes = visibleScenes.filter { it.chapterId == chapter.id }.sortedBy { it.orderIndex }
                    val shownScenes = if (searchQuery.isBlank() || chapter.title.contains(searchQuery, true)) {
                        allChapterScenes
                    } else {
                        allChapterScenes.filter { it.title.contains(searchQuery, true) }
                    }
                    ChapterRow(
                        chapter = chapter,
                        chapterPosition = chapterPosition,
                        scenes = shownScenes,
                        activeSceneId = selectedSceneId,
                        isScopeSelected = selectedChapterId == chapter.id && selectedSceneId == null,
                        isExpanded = chapter.id in expandedChapterIds || searchQuery.isNotBlank(),
                        onToggle = { onToggleChapter(chapter.id) },
                        onSelectChapter = { onChapterSelected(chapter.id) },
                        onAddScene = { onNewScene(chapter.id) },
                        onManageChapter = { onManageChapter(chapter) },
                        onSelectScene = onSceneSelected,
                        onManageScene = onManageScene
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(NovellumSurfaceContainerLow)
                .border(1.dp, NovellumOutlineVariant.copy(alpha = 0.35f))
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(NovellumObsidian)
                    .border(1.dp, NovellumOutlineVariant.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = NovellumOutline, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchChanged,
                    enabled = selectedProject != null,
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = UiSans, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(NovellumPrimaryContainer),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                if (selectedProject == null) "Select a manuscript first" else "Search manuscript...",
                                fontFamily = UiSans,
                                fontSize = 11.sp,
                                color = NovellumOutline
                            )
                        }
                        inner()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChanged("") }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, "Clear search", tint = NovellumOutline, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            ScopeTelemetry(scopeStats)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChapterRow(
    chapter: ChapterEntity,
    chapterPosition: Int,
    scenes: List<SceneEntity>,
    activeSceneId: String?,
    isScopeSelected: Boolean,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onSelectChapter: () -> Unit,
    onAddScene: () -> Unit,
    onManageChapter: () -> Unit,
    onSelectScene: (String) -> Unit,
    onManageScene: (SceneEntity) -> Unit
) {
    val chapterWords = scenes.sumOf { wordCount(it.prose) }
    Column(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(if (isScopeSelected) NovellumSurfaceContainerHigh.copy(alpha = 0.72f) else NovellumObsidian)
                .padding(horizontal = 3.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggle, modifier = Modifier.size(28.dp)) {
                Icon(
                    if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                    "Expand chapter",
                    tint = NovellumOutline,
                    modifier = Modifier.size(16.dp)
                )
            }
            Icon(
                if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                null,
                tint = if (isExpanded || isScopeSelected) NovellumPrimary else NovellumOutline,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                chapterDisplayTitle(chapter, chapterPosition),
                fontFamily = UiSans,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(onClick = onSelectChapter, onLongClick = onManageChapter)
                    .padding(vertical = 7.dp)
            )
            Text(formatCompactWords(chapterWords), fontFamily = UiSans, fontSize = 10.sp, color = NovellumOutline)
            IconButton(onClick = onAddScene, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.PostAdd, "Add scene", tint = NovellumOutline, modifier = Modifier.size(15.dp))
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 2.dp, bottom = 4.dp)
                    .border(1.dp, NovellumOutlineVariant.copy(alpha = 0.2f), RoundedCornerShape(0.dp))
            ) {
                scenes.forEachIndexed { index, scene ->
                    SceneRowItem(
                        scene = scene,
                        scenePosition = index,
                        isActive = scene.id == activeSceneId,
                        onClick = { onSelectScene(scene.id) },
                        onLongClick = { onManageScene(scene) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SceneRowItem(
    scene: SceneEntity,
    scenePosition: Int,
    isActive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
            .background(if (isActive) NovellumSurfaceContainerHigh else NovellumObsidian)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(2.dp)
                .height(38.dp)
                .background(if (isActive) NovellumPrimaryContainer else NovellumObsidian)
        )
        Row(
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isActive) Icons.Default.Article else Icons.Default.Description,
                null,
                tint = if (isActive) NovellumPrimaryContainer else NovellumOutline,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                sceneDisplayTitle(scene, scenePosition),
                fontFamily = UiSans,
                fontSize = 12.sp,
                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (isActive) {
                Box(Modifier.size(5.dp).clip(CircleShape).background(NovellumPrimaryContainer))
                Spacer(Modifier.width(5.dp))
            }
            Text(
                formatCompactWords(wordCount(scene.prose)),
                fontFamily = UiSans,
                fontSize = 10.sp,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isActive) NovellumPrimary else NovellumOutline
            )
        }
    }
}

@Composable
private fun ScopeTelemetry(stats: ScopeStats) {
    Text(
        stats.scopeLabel,
        fontFamily = UiSans,
        fontSize = 10.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 1.2.sp,
        color = NovellumOutline
    )
    Spacer(Modifier.height(6.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TelemetryCell("WORDS", "%,d".format(stats.words), Modifier.weight(1f))
        TelemetryCell(stats.secondaryLabel, stats.secondaryValue.toString(), Modifier.weight(1f))
        TelemetryCell(stats.tertiaryLabel, stats.tertiaryValue, Modifier.weight(1f))
    }
    Spacer(Modifier.height(7.dp))
    Box(Modifier.fillMaxWidth().height(1.dp).background(NovellumOutlineVariant.copy(alpha = 0.3f)))
}

@Composable
private fun TelemetryCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.Start) {
        Text(label, fontFamily = UiSans, fontSize = 9.sp, letterSpacing = 0.8.sp, color = NovellumOutline)
        Text(value, fontFamily = UiSans, fontSize = 11.sp, fontWeight = FontWeight.Normal, color = NovellumPrimary)
    }
}

@Composable
private fun DividerLine() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(NovellumOutlineVariant.copy(alpha = 0.25f)))
}

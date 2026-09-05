package com.example.ui.features.workspace

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontStyle
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
import com.example.ui.theme.NovellumSurfaceContainer
import com.example.ui.theme.NovellumSurfaceContainerHigh
import com.example.ui.theme.NovellumSurfaceContainerLow
import com.example.ui.theme.UiSans

@Composable
fun ContextSidebar(
    project: ProjectEntity?,
    chapter: ChapterEntity?,
    scene: SceneEntity?,
    chapterPosition: Int,
    scenePosition: Int,
    chapters: List<ChapterEntity>,
    scenes: List<SceneEntity>,
    onSelectChapter: (String) -> Unit,
    onSelectScene: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(ContextTab.CONTEXT) }
    val visibleChapterIds = chapters.mapTo(mutableSetOf()) { it.id }
    val visibleScenes = scenes.filter { it.chapterId in visibleChapterIds }

    Column(
        modifier = modifier
            .width(300.dp)
            .fillMaxHeight()
            .background(NovellumObsidian)
            .border(1.dp, NovellumOutlineVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp).background(NovellumObsidian),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ContextTabButton("Context", selectedTab == ContextTab.CONTEXT) { selectedTab = ContextTab.CONTEXT }
            ContextTabButton("Outline", selectedTab == ContextTab.OUTLINE) { selectedTab = ContextTab.OUTLINE }
            ContextTabButton("Notes", selectedTab == ContextTab.NOTES) { selectedTab = ContextTab.NOTES }
        }
        DividerLine()

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                ContextTab.CONTEXT -> ContextContent(
                    project = project,
                    chapter = chapter,
                    scene = scene,
                    chapterPosition = chapterPosition,
                    scenePosition = scenePosition,
                    chapters = chapters,
                    scenes = visibleScenes,
                    onSelectScene = onSelectScene
                )
                ContextTab.OUTLINE -> OutlineContent(
                    chapters = chapters,
                    scenes = visibleScenes,
                    scene = scene,
                    chapter = chapter,
                    onSelectChapter = onSelectChapter,
                    onSelectScene = onSelectScene
                )
                ContextTab.NOTES -> NotesEmptyState()
            }
        }

        CurrentFocusFooter(
            project = project,
            chapter = chapter,
            scene = scene,
            chapterPosition = chapterPosition,
            scenePosition = scenePosition
        )
    }
}

@Composable
private fun ContextTabButton(label: String, selected: Boolean, onClick: () -> Unit) {
    val color by animateColorAsState(if (selected) NovellumPrimary else NovellumOutline, label = "contextTab")
    Column(
        Modifier.clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontFamily = UiSans, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal, color = color)
        Spacer(Modifier.height(7.dp))
        Box(Modifier.width(if (selected) 44.dp else 0.dp).height(2.dp).background(if (selected) NovellumPrimaryContainer else androidx.compose.ui.graphics.Color.Transparent))
    }
}

@Composable
private fun ContextContent(
    project: ProjectEntity?,
    chapter: ChapterEntity?,
    scene: SceneEntity?,
    chapterPosition: Int,
    scenePosition: Int,
    chapters: List<ChapterEntity>,
    scenes: List<SceneEntity>,
    onSelectScene: (String) -> Unit
) {
    LazyColumn(Modifier.fillMaxHeight().padding(10.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        item {
            Text(
                when {
                    scene != null -> "ACTIVE SCENE CONTEXT"
                    chapter != null -> "CHAPTER CONTEXT"
                    project != null -> "PROJECT CONTEXT"
                    else -> "WORKSPACE CONTEXT"
                },
                fontFamily = UiSans,
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.3.sp,
                color = NovellumOutline,
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp)
            )
        }

        item {
            ContextCard {
                when {
                    scene != null && chapter != null -> {
                        Text(
                            sceneDisplayTitle(scene, scenePosition),
                            fontFamily = ManuscriptSerif,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            chapterHeadingTitle(chapter, chapterPosition),
                            fontFamily = ManuscriptSerif,
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        MicroRule()
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "%,d words · %dm read".format(wordCount(scene.prose), readingMinutes(wordCount(scene.prose))),
                            fontFamily = UiSans,
                            fontSize = 11.sp,
                            color = NovellumOutline
                        )
                    }
                    chapter != null -> {
                        val chapterScenes = scenes.filter { it.chapterId == chapter.id }
                        Text(chapterHeadingTitle(chapter, chapterPosition), fontFamily = ManuscriptSerif, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(8.dp))
                        Text("${chapterScenes.size} scenes · %,d words".format(chapterScenes.sumOf { wordCount(it.prose) }), fontFamily = UiSans, fontSize = 11.sp, color = NovellumOutline)
                    }
                    project != null -> {
                        Text(projectDisplayTitle(project), fontFamily = ManuscriptSerif, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(8.dp))
                        Text("${chapters.size} chapters · ${scenes.size} scenes · %,d words".format(scenes.sumOf { wordCount(it.prose) }), fontFamily = UiSans, fontSize = 11.sp, color = NovellumOutline)
                    }
                    else -> {
                        Text("No manuscript selected", fontFamily = ManuscriptSerif, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        if (scene != null && chapter != null) {
            val chapterScenes = scenes.filter { it.chapterId == chapter.id }.sortedBy { it.orderIndex }
            val previous = chapterScenes.getOrNull(scenePosition - 1)
            val next = chapterScenes.getOrNull(scenePosition + 1)
            item {
                ContextCard {
                    Text("STRUCTURAL POSITION", fontFamily = UiSans, fontSize = 10.sp, fontWeight = FontWeight.Normal, letterSpacing = 1.sp, color = NovellumPrimary)
                    Spacer(Modifier.height(9.dp))
                    FocusRow(Icons.Default.Folder, "Chapter", "${chapterPosition + 1}")
                    FocusRow(Icons.Default.Article, "Scene", "${scenePosition + 1} of ${chapterScenes.size}")
                }
            }
            if (previous != null || next != null) {
                item {
                    ContextCard {
                        Text("NEARBY", fontFamily = UiSans, fontSize = 10.sp, fontWeight = FontWeight.Normal, letterSpacing = 1.sp, color = NovellumOutline)
                        Spacer(Modifier.height(8.dp))
                        previous?.let { NearbyScene("Previous", previous, chapterScenes.indexOf(previous), onSelectScene) }
                        next?.let { NearbyScene("Next", next, chapterScenes.indexOf(next), onSelectScene) }
                    }
                }
            }
        }
    }
}

@Composable
private fun OutlineContent(
    chapters: List<ChapterEntity>,
    scenes: List<SceneEntity>,
    scene: SceneEntity?,
    chapter: ChapterEntity?,
    onSelectChapter: (String) -> Unit,
    onSelectScene: (String) -> Unit
) {
    LazyColumn(Modifier.fillMaxHeight().padding(10.dp)) {
        items(chapters, key = { it.id }) { ch ->
            val chIndex = chapters.indexOf(ch)
            val chScenes = scenes.filter { it.chapterId == ch.id }.sortedBy { it.orderIndex }
            Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (chapter?.id == ch.id && scene == null) NovellumSurfaceContainerHigh else NovellumObsidian)
                        .clickable { onSelectChapter(ch.id) }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Folder, null, tint = if (chapter?.id == ch.id) NovellumPrimary else NovellumOutline, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(chapterDisplayTitle(ch, chIndex), fontFamily = UiSans, fontSize = 11.sp, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    Text(formatCompactWords(chScenes.sumOf { wordCount(it.prose) }), fontFamily = UiSans, fontSize = 9.sp, color = NovellumOutline)
                }
                chScenes.forEachIndexed { index, sc ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (scene?.id == sc.id) NovellumSurfaceContainerHigh else NovellumObsidian)
                            .clickable { onSelectScene(sc.id) }
                            .padding(horizontal = 8.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Description, null, tint = if (scene?.id == sc.id) NovellumPrimaryContainer else NovellumOutline, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(sceneDisplayTitle(sc, index), fontFamily = UiSans, fontSize = 10.sp, color = if (scene?.id == sc.id) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesEmptyState() {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Notes, null, tint = NovellumOutline, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(12.dp))
            Text("Notes", fontFamily = ManuscriptSerif, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(5.dp))
            Text(
                "Scene and chapter notes will appear here when that layer is enabled.",
                fontFamily = UiSans,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = NovellumOutline
            )
        }
    }
}

@Composable
private fun CurrentFocusFooter(
    project: ProjectEntity?,
    chapter: ChapterEntity?,
    scene: SceneEntity?,
    chapterPosition: Int,
    scenePosition: Int
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(NovellumSurfaceContainerLow)
            .border(1.dp, NovellumOutlineVariant.copy(alpha = 0.35f))
            .padding(10.dp)
    ) {
        Text("CURRENT FOCUS", fontFamily = UiSans, fontSize = 10.sp, fontWeight = FontWeight.Normal, letterSpacing = 1.2.sp, color = NovellumOutline)
        Spacer(Modifier.height(8.dp))
        FocusRow(Icons.Default.Route, "Project", project?.let(::projectDisplayTitle) ?: "None", project != null)
        FocusRow(Icons.Default.Folder, "Chapter", chapter?.let { chapterHeadingTitle(it, chapterPosition) } ?: "None", chapter != null)
        FocusRow(Icons.Default.Article, "Scene", scene?.let { sceneDisplayTitle(it, scenePosition) } ?: "None", scene != null)
    }
}

@Composable
private fun FocusRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    active: Boolean = true
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (active) NovellumPrimary else NovellumOutline, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(7.dp))
        Text(label, fontFamily = UiSans, fontSize = 11.sp, color = NovellumOutline, modifier = Modifier.weight(1f))
        Text(value, fontFamily = UiSans, fontSize = 10.sp, color = if (active) MaterialTheme.colorScheme.onSurfaceVariant else NovellumOutline, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(116.dp))
    }
}

@Composable
private fun NearbyScene(label: String, scene: SceneEntity, index: Int, onSelectScene: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable { onSelectScene(scene.id) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label.uppercase(), fontFamily = UiSans, fontSize = 9.sp, letterSpacing = 0.8.sp, color = NovellumOutline, modifier = Modifier.width(54.dp))
        Text(sceneDisplayTitle(scene, index), fontFamily = ManuscriptSerif, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ContextCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(NovellumSurfaceContainer)
            .border(1.dp, NovellumOutlineVariant.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
            .padding(10.dp),
        content = content
    )
}

@Composable
private fun MicroRule() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(NovellumOutlineVariant.copy(alpha = 0.3f)))
}

@Composable
private fun DividerLine() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(NovellumOutlineVariant.copy(alpha = 0.25f)))
}

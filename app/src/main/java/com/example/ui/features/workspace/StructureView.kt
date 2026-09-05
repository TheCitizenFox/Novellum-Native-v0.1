package com.example.ui.features.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.theme.NovellumSurface
import com.example.ui.theme.NovellumSurfaceContainer
import com.example.ui.theme.NovellumSurfaceContainerHigh
import com.example.ui.theme.UiSans

@Composable
fun StructureView(
    project: ProjectEntity?,
    chapters: List<ChapterEntity>,
    scenes: List<SceneEntity>,
    selectedSceneId: String?,
    onSelectScene: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleIds = chapters.mapTo(mutableSetOf()) { it.id }
    val visibleScenes = scenes.filter { it.chapterId in visibleIds }

    Column(modifier.fillMaxSize().background(NovellumSurface).padding(horizontal = 26.dp, vertical = 22.dp)) {
        Text("STRUCTURE", fontFamily = UiSans, fontSize = 10.sp, letterSpacing = 2.sp, color = NovellumPrimary)
        Spacer(Modifier.height(5.dp))
        Text(project?.let(::projectDisplayTitle) ?: "No manuscript selected", fontFamily = ManuscriptSerif, fontSize = 28.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Text(
            "Chapter and scene flow",
            fontFamily = UiSans,
            fontSize = 11.sp,
            color = NovellumOutline
        )
        Spacer(Modifier.height(18.dp))

        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(chapters, key = { it.id }) { chapter ->
                val chIndex = chapters.indexOf(chapter)
                val chScenes = visibleScenes.filter { it.chapterId == chapter.id }.sortedBy { it.orderIndex }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(NovellumObsidian)
                        .border(1.dp, NovellumOutlineVariant.copy(alpha = 0.38f), RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, null, tint = NovellumPrimary, modifier = Modifier.width(16.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(chapterDisplayTitle(chapter, chIndex), fontFamily = UiSans, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        Text("%,d wds".format(chScenes.sumOf { wordCount(it.prose) }), fontFamily = UiSans, fontSize = 10.sp, color = NovellumOutline)
                    }
                    Spacer(Modifier.height(8.dp))
                    chScenes.forEachIndexed { sceneIndex, scene ->
                        val active = scene.id == selectedSceneId
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, bottom = 5.dp)
                                .height(36.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (active) NovellumSurfaceContainerHigh else NovellumSurfaceContainer)
                                .border(1.dp, if (active) NovellumPrimaryContainer.copy(alpha = 0.7f) else NovellumOutlineVariant.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                                .clickable { onSelectScene(scene.id) }
                                .padding(horizontal = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.width(5.dp).height(5.dp).clip(CircleShape).background(if (active) NovellumPrimaryContainer else NovellumOutlineVariant))
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.Article, null, tint = if (active) NovellumPrimary else NovellumOutline, modifier = Modifier.width(14.dp))
                            Spacer(Modifier.width(7.dp))
                            Text(sceneDisplayTitle(scene, sceneIndex), fontFamily = UiSans, fontSize = 11.sp, color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                            Text(formatCompactWords(wordCount(scene.prose)), fontFamily = UiSans, fontSize = 9.sp, color = NovellumOutline)
                        }
                    }
                }
            }
        }
    }
}

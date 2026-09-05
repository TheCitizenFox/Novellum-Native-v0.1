package com.example.ui.features.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.theme.NovellumSurface
import com.example.ui.theme.NovellumSurfaceContainer
import com.example.ui.theme.UiSans

@Composable
fun ManuscriptOverviewView(
    project: ProjectEntity?,
    chapters: List<ChapterEntity>,
    scenes: List<SceneEntity>,
    onSelectScene: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleIds = chapters.mapTo(mutableSetOf()) { it.id }
    val visibleScenes = scenes.filter { it.chapterId in visibleIds }
    val totalWords = visibleScenes.sumOf { wordCount(it.prose) }

    Column(modifier.fillMaxSize().background(NovellumSurface).padding(horizontal = 34.dp, vertical = 28.dp)) {
        Text("MANUSCRIPT", fontFamily = UiSans, fontSize = 10.sp, fontWeight = FontWeight.Normal, letterSpacing = 2.sp, color = NovellumPrimary)
        Spacer(Modifier.height(6.dp))
        Text(project?.let(::projectDisplayTitle) ?: "No manuscript selected", fontFamily = ManuscriptSerif, fontSize = 30.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(12.dp))
        Text("${chapters.size} chapters · ${visibleScenes.size} scenes · %,d words".format(totalWords), fontFamily = UiSans, fontSize = 11.sp, color = NovellumOutline)
        Spacer(Modifier.height(22.dp))

        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(chapters, key = { it.id }) { chapter ->
                val chIndex = chapters.indexOf(chapter)
                val chScenes = visibleScenes.filter { it.chapterId == chapter.id }.sortedBy { it.orderIndex }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(5.dp))
                        .background(NovellumObsidian)
                        .border(1.dp, NovellumOutlineVariant.copy(alpha = 0.35f), RoundedCornerShape(5.dp))
                        .padding(12.dp)
                ) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(chapterDisplayTitle(chapter, chIndex), fontFamily = ManuscriptSerif, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        Text("%,d wds".format(chScenes.sumOf { wordCount(it.prose) }), fontFamily = UiSans, fontSize = 10.sp, color = NovellumOutline)
                    }
                    Spacer(Modifier.height(7.dp))
                    chScenes.forEachIndexed { idx, scene ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onSelectScene(scene.id) }.padding(vertical = 5.dp)
                        ) {
                            Text("%02d".format(idx + 1), fontFamily = UiSans, fontSize = 9.sp, color = NovellumPrimary, modifier = Modifier.padding(end = 10.dp))
                            Text(sceneDisplayTitle(scene, idx), fontFamily = UiSans, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                            Text(formatCompactWords(wordCount(scene.prose)), fontFamily = UiSans, fontSize = 9.sp, color = NovellumOutline)
                        }
                    }
                }
            }
        }
    }
}

package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.ChapterEntity
import com.example.data.entity.SceneEntity
import com.example.ui.viewmodel.EditorViewModel

private val NovellumBlack = Color(0xFF0B0B0C)
private val NovellumHeader = Color(0xFF111113)
private val NovellumSidebar = Color(0xFF151517)
private val NovellumCanvas = Color(0xFF0F0F11)
private val NovellumEditor = Color(0xFF121214)
private val NovellumLine = Color(0xFF2A2A2D)
private val NovellumLineSoft = Color(0xFF222225)
private val NovellumText = Color(0xFFE7E4DF)
private val NovellumTextSoft = Color(0xFFA5A29D)
private val NovellumTextDim = Color(0xFF6F6D69)
private val NovellumAccent = Color(0xFFC97942)
private val NovellumAccentSoft = Color(0xFF2B211B)
private val NovellumDanger = Color(0xFFC86D72)

@Composable
fun EditorShellScreen(viewModel: EditorViewModel) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val selectedProjectId by viewModel.selectedProjectId.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val projectScenes by viewModel.projectScenes.collectAsStateWithLifecycle()
    val selectedSceneId by viewModel.selectedSceneId.collectAsStateWithLifecycle()
    val currentScene by viewModel.currentScene.collectAsStateWithLifecycle()
    val uiMessage by viewModel.uiMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUiMessage()
        }
    }

    val selectedProject = projects.firstOrNull { it.id == selectedProjectId }

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
                hasScene = currentScene != null
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NovellumCanvas)
            ) {
                ManuscriptSidebar(
                    modifier = Modifier
                        .width(282.dp)
                        .fillMaxHeight(),
                    projectTitle = selectedProject?.title,
                    selectedProjectId = selectedProjectId,
                    selectedSceneId = selectedSceneId,
                    projects = projects.map { it.id to it.title },
                    chapters = chapters,
                    scenes = projectScenes,
                    onCreateProject = { viewModel.createProject("New Project", "") },
                    onSelectProject = viewModel::selectProject,
                    onBackToProjects = viewModel::clearProjectSelection,
                    onCreateChapter = { viewModel.createChapter("New Chapter") },
                    onCreateScene = { chapterId -> viewModel.createScene(chapterId, "New Scene") },
                    onSelectScene = viewModel::selectScene
                )

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(NovellumLine)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(NovellumCanvas)
                ) {
                    val scene = currentScene
                    if (scene == null) {
                        EmptyEditorState()
                    } else {
                        SceneEditor(
                            scene = scene,
                            onSave = { text, clearIntent ->
                                viewModel.saveSceneProse(
                                    sceneId = scene.id,
                                    newProse = text,
                                    isUserIntentClear = clearIntent
                                )
                            },
                            onDelete = { viewModel.deleteScene(scene.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NovellumTopBar(hasScene: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(NovellumHeader)
            .border(width = 1.dp, color = NovellumLineSoft)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "NOVELLUM",
            color = NovellumText,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.2.sp,
            modifier = Modifier.width(248.dp)
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopNavItem("Library", active = false)
            TopNavItem("Vault", active = false)
            TopNavItem("Editor", active = true)
            TopNavItem("Cards", active = false)
            TopNavItem("Manuscript", active = false)
        }

        Row(
            modifier = Modifier.width(248.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        color = if (hasScene) NovellumAccent else NovellumTextDim,
                        shape = RoundedCornerShape(50)
                    )
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (hasScene) "Editor ready" else "No scene selected",
                color = NovellumTextSoft,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun TopNavItem(label: String, active: Boolean) {
    Column(
        modifier = Modifier
            .height(58.dp)
            .padding(horizontal = 15.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = if (active) NovellumText else NovellumTextDim,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(9.dp))
        Box(
            modifier = Modifier
                .height(2.dp)
                .width(28.dp)
                .background(if (active) NovellumAccent else Color.Transparent)
        )
    }
}

@Composable
private fun ManuscriptSidebar(
    modifier: Modifier,
    projectTitle: String?,
    selectedProjectId: String?,
    selectedSceneId: String?,
    projects: List<Pair<String, String>>,
    chapters: List<ChapterEntity>,
    scenes: List<SceneEntity>,
    onCreateProject: () -> Unit,
    onSelectProject: (String) -> Unit,
    onBackToProjects: () -> Unit,
    onCreateChapter: () -> Unit,
    onCreateScene: (String) -> Unit,
    onSelectScene: (String) -> Unit
) {
    Column(
        modifier = modifier.background(NovellumSidebar)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MANUSCRIPT",
                color = NovellumTextDim,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.weight(1f)
            )

            if (selectedProjectId == null) {
                SmallAction("+", onClick = onCreateProject)
            } else {
                SmallAction("‹ Projects", onClick = onBackToProjects)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(NovellumLineSoft)
        )

        if (selectedProjectId == null) {
            ProjectList(
                projects = projects,
                onCreateProject = onCreateProject,
                onSelectProject = onSelectProject
            )
        } else {
            ProjectManuscript(
                projectTitle = projectTitle ?: "Untitled Project",
                chapters = chapters,
                scenes = scenes,
                selectedSceneId = selectedSceneId,
                onCreateChapter = onCreateChapter,
                onCreateScene = onCreateScene,
                onSelectScene = onSelectScene
            )
        }
    }
}

@Composable
private fun ProjectList(
    projects: List<Pair<String, String>>,
    onCreateProject: () -> Unit,
    onSelectProject: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Projects",
                color = NovellumText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            SmallAction("+ New", onClick = onCreateProject)
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            items(projects, key = { it.first }) { project ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectProject(project.first) }
                        .background(
                            color = NovellumEditor,
                            shape = RoundedCornerShape(5.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = NovellumLineSoft,
                            shape = RoundedCornerShape(5.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "▱",
                        color = NovellumAccent,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.width(9.dp))
                    Text(
                        text = project.second,
                        color = NovellumText,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectManuscript(
    projectTitle: String,
    chapters: List<ChapterEntity>,
    scenes: List<SceneEntity>,
    selectedSceneId: String?,
    onCreateChapter: () -> Unit,
    onCreateScene: (String) -> Unit,
    onSelectScene: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = projectTitle,
                color = NovellumText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "${chapters.size} chapter${if (chapters.size == 1) "" else "s"}",
                color = NovellumTextDim,
                fontSize = 11.sp
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CHAPTERS",
                color = NovellumTextDim,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                modifier = Modifier.weight(1f)
            )
            SmallAction("+ Chapter", onClick = onCreateChapter)
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 10.dp,
                end = 10.dp,
                bottom = 16.dp
            )
        ) {
            items(chapters, key = { it.id }) { chapter ->
                ChapterBlock(
                    chapter = chapter,
                    scenes = scenes.filter { it.chapterId == chapter.id },
                    selectedSceneId = selectedSceneId,
                    onCreateScene = { onCreateScene(chapter.id) },
                    onSelectScene = onSelectScene
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(NovellumLineSoft)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Backup JSON", color = NovellumTextDim, fontSize = 10.sp)
            Text("  •  ", color = NovellumLine, fontSize = 10.sp)
            Text("Export MD", color = NovellumTextDim, fontSize = 10.sp)
        }
    }
}

@Composable
private fun ChapterBlock(
    chapter: ChapterEntity,
    scenes: List<SceneEntity>,
    selectedSceneId: String?,
    onCreateScene: () -> Unit,
    onSelectScene: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 7.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⌄",
                color = NovellumTextDim,
                fontSize = 12.sp
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = chapter.title.uppercase(),
                color = NovellumTextSoft,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.7.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "+ Scene",
                color = NovellumAccent,
                fontSize = 10.sp,
                modifier = Modifier
                    .clickable(onClick = onCreateScene)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }

        scenes.forEach { scene ->
            val selected = scene.id == selectedSceneId

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, top = 1.dp, bottom = 1.dp)
                    .background(
                        color = if (selected) NovellumAccentSoft else Color.Transparent,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clickable { onSelectScene(scene.id) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(18.dp)
                        .background(
                            if (selected) NovellumAccent else Color.Transparent,
                            RoundedCornerShape(50)
                        )
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    text = scene.title,
                    color = if (selected) NovellumText else NovellumTextSoft,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SceneEditor(
    scene: SceneEntity,
    onSave: (String, Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var proseText by remember(scene.id) { mutableStateOf(scene.prose) }
    var showDeleteConfirm by remember(scene.id) { mutableStateOf(false) }
    val isDirty = proseText != scene.prose
    val isClearingExistingText = proseText.isEmpty() && scene.prose.isNotEmpty()

    val wordCount = remember(proseText) {
        if (proseText.isBlank()) 0
        else proseText.trim().split(Regex("\\s+")).size
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NovellumCanvas)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .background(NovellumEditor)
                .padding(horizontal = 28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = scene.title,
                    color = NovellumText,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (isDirty) "Unsaved changes" else "Saved",
                    color = if (isDirty) NovellumAccent else NovellumTextDim,
                    fontSize = 10.sp
                )
            }

            if (isClearingExistingText) {
                SmallAction(
                    label = "Confirm clear",
                    accent = NovellumDanger,
                    onClick = { onSave(proseText, true) }
                )
            } else {
                SmallAction(
                    label = if (isDirty) "Save" else "Saved",
                    accent = if (isDirty) NovellumAccent else NovellumTextDim,
                    enabled = isDirty,
                    onClick = { onSave(proseText, false) }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(NovellumLineSoft)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(NovellumCanvas)
                .padding(horizontal = 32.dp, vertical = 22.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 820.dp)
                    .fillMaxWidth()
                    .background(
                        color = NovellumEditor,
                        shape = RoundedCornerShape(5.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = NovellumLineSoft,
                        shape = RoundedCornerShape(5.dp)
                    )
                    .padding(horizontal = 42.dp, vertical = 34.dp)
            ) {
                BasicTextField(
                    value = proseText,
                    onValueChange = { proseText = it },
                    modifier = Modifier.fillMaxSize(),
                    textStyle = TextStyle(
                        color = NovellumText,
                        fontSize = 17.sp,
                        lineHeight = 29.sp
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(NovellumAccent),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (proseText.isEmpty()) {
                                Text(
                                    text = "Write here…",
                                    color = NovellumTextDim,
                                    fontSize = 17.sp,
                                    lineHeight = 29.sp
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
                .height(42.dp)
                .background(NovellumHeader)
                .border(width = 1.dp, color = NovellumLineSoft)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$wordCount word${if (wordCount == 1) "" else "s"}",
                color = NovellumTextDim,
                fontSize = 10.sp
            )

            Spacer(Modifier.weight(1f))

            Text(
                text = if (isDirty) "Not yet saved" else "Saved",
                color = if (isDirty) NovellumAccent else NovellumTextDim,
                fontSize = 10.sp
            )

            Spacer(Modifier.width(18.dp))

            Text(
                text = "Delete scene",
                color = NovellumDanger,
                fontSize = 10.sp,
                modifier = Modifier
                    .clickable { showDeleteConfirm = true }
                    .padding(vertical = 7.dp)
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = NovellumSidebar,
            titleContentColor = NovellumText,
            textContentColor = NovellumTextSoft,
            title = { Text("Delete Scene?") },
            text = {
                Text(
                    "This removes “${scene.title}” from the manuscript. " +
                        "This action cannot be undone from this screen."
                )
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = NovellumTextSoft)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = NovellumDanger)
                }
            }
        )
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "✎",
                color = NovellumTextDim,
                fontSize = 30.sp
            )
            Spacer(Modifier.height(13.dp))
            Text(
                text = "Select a scene to start writing.",
                color = NovellumTextSoft,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Choose a scene from the manuscript panel.",
                color = NovellumTextDim,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun SmallAction(
    label: String,
    accent: Color = NovellumAccent,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Text(
        text = label,
        color = if (enabled) accent else NovellumTextDim,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 6.dp)
    )
}

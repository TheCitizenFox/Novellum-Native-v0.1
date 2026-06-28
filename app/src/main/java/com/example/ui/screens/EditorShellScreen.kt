package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.ChapterEntity
import com.example.data.entity.ProjectEntity
import com.example.data.entity.SceneEntity
import com.example.ui.viewmodel.EditorViewModel

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Novellum") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Sidebar
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                if (selectedProjectId == null) {
                    Text("Projects", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { viewModel.createProject("New Project", "") }) {
                        Text("New Project")
                    }
                    LazyColumn {
                        items(projects) { project ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { viewModel.selectProject(project.id) }
                            ) {
                                Text(project.title, modifier = Modifier.padding(8.dp))
                            }
                        }
                    }
                } else {
                    Button(onClick = { viewModel.clearProjectSelection() }) {
                        Text("Back to Projects")
                    }
                    Text("Chapters", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
                    Button(onClick = { viewModel.createChapter("New Chapter") }) {
                        Text("New Chapter")
                    }
                    LazyColumn {
                        items(chapters) { chapter ->
                            Text(chapter.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = 4.dp))
                            
                            // Display scenes for this chapter
                            val scenesInChapter = projectScenes.filter { it.chapterId == chapter.id }
                            for (scene in scenesInChapter) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, top = 2.dp, bottom = 2.dp)
                                        .clickable { viewModel.selectScene(scene.id) }
                                ) {
                                    Text(scene.title, modifier = Modifier.padding(8.dp))
                                }
                            }
                            
                            Button(onClick = { viewModel.createScene(chapter.id, "New Scene") }, modifier = Modifier.padding(start = 16.dp)) {
                                Text("New Scene")
                            }
                        }
                    }
                }
            }

            Divider(modifier = Modifier.width(1.dp).fillMaxSize())

            // Editor Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (currentScene != null) {
                    val scene = currentScene!!
                    var proseText by remember(scene.id) { mutableStateOf(scene.prose) }

                    Column {
                        Text(scene.title, style = MaterialTheme.typography.headlineMedium)
                        
                        TextField(
                            value = proseText,
                            onValueChange = { proseText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 8.dp),
                            placeholder = { Text("Write here...") }
                        )

                        Row {
                            Button(
                                onClick = { viewModel.saveSceneProse(scene.id, proseText) },
                                enabled = !(proseText.isEmpty() && scene.prose.isNotEmpty()) // Disable ordinary save if clearing
                            ) {
                                Text("Save")
                            }
                            if (proseText.isEmpty() && scene.prose.isNotEmpty()) {
                                Button(onClick = { viewModel.saveSceneProse(scene.id, proseText, isUserIntentClear = true) }, modifier = Modifier.padding(start = 8.dp)) {
                                    Text("Confirm Clear")
                                }
                            }
                            Button(onClick = { viewModel.deleteScene(scene.id) }, modifier = Modifier.padding(start = 8.dp)) {
                                Text("Delete Scene")
                            }
                        }
                    }
                } else {
                    Text("Select a scene to edit.", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

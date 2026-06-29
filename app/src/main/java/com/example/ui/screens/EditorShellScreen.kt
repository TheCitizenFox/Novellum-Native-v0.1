package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import com.example.ui.viewmodel.SaveState

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val lastSavedTime by viewModel.lastSavedTime.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Observe App Backgrounding for Autosave
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP || event == Lifecycle.Event.ON_PAUSE) {
                viewModel.forceSaveCurrentScene()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUiMessage()
        }
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            scope.launch {
                val projectId = selectedProjectId ?: return@launch
                val json = viewModel.getProjectBackupJson(projectId) ?: return@launch
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(json.toByteArray())
                }
                snackbarHostState.showSnackbar("Backup exported.")
            }
        }
    }

    val exportMarkdownLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        uri?.let {
            scope.launch {
                val projectId = selectedProjectId ?: return@launch
                val markdown = viewModel.getProjectMarkdown(projectId) ?: return@launch
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(markdown.toByteArray())
                }
                snackbarHostState.showSnackbar("Manuscript exported.")
            }
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        val timestamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())
                        exportJsonLauncher.launch("Novellum_Backup_${timestamp}.json")
                    }) {
                        Text("Export JSON Backup")
                    }
                    Button(onClick = {
                        val timestamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())
                        exportMarkdownLauncher.launch("Novellum_Manuscript_${timestamp}.md")
                    }) {
                        Text("Export Markdown")
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

                    LaunchedEffect(scene.id) {
                        viewModel.syncSceneState(scene.prose)
                    }

                    Column {
                        Text(scene.title, style = MaterialTheme.typography.headlineMedium)
                        
                        TextField(
                            value = proseText,
                            onValueChange = { 
                                proseText = it
                                viewModel.onProseChanged(it)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 8.dp),
                            placeholder = { Text("Write here...") }
                        )

                        Row {
                            Button(
                                onClick = { viewModel.forceSaveCurrentScene() },
                                enabled = saveState == SaveState.UNSAVED || saveState == SaveState.AUTOSAVING
                            ) {
                                Text("Save")
                            }
                            if (saveState == SaveState.BLOCKED_EMPTY_CLEAR) {
                                Button(onClick = { 
                                    viewModel.forceSaveCurrentScene(isUserIntentClear = true) 
                                }, modifier = Modifier.padding(start = 8.dp)) {
                                    Text("Confirm Clear")
                                }
                            }
                            Button(onClick = { viewModel.deleteScene(scene.id) }, modifier = Modifier.padding(start = 8.dp)) {
                                Text("Delete Scene")
                            }
                        }

                        // Save Status Message
                        val statusMsg = when (saveState) {
                            SaveState.SAVED -> lastSavedTime?.let { time ->
                                val formatted = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(time))
                                "Saved at $formatted"
                            } ?: "Saved"
                            SaveState.UNSAVED -> "Unsaved changes"
                            SaveState.AUTOSAVING -> "Autosaving..."
                            SaveState.BLOCKED_EMPTY_CLEAR -> "Autosave blocked: empty clear requires confirmation"
                        }
                        Text(statusMsg, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                    }
                } else {
                    Text("Select a scene to edit.", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

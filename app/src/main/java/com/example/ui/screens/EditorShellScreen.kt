package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
            TopAppBar(
                title = { Text("Novellum") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isLandscape = maxWidth > 600.dp
            
            val sidebarContent: @Composable () -> Unit = {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        if (selectedProjectId == null) {
                            Text("Projects", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.createProject("New Project", "") },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("New Project", color = MaterialTheme.colorScheme.onPrimary)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyColumn {
                                items(projects) { project ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable { viewModel.selectProject(project.id) },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Text(project.title, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        } else {
                            Button(
                                onClick = { viewModel.clearProjectSelection() },
                                colors = ButtonDefaults.textButtonColors(),
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                Text("Back to Projects", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            
                            Row {
                                Button(
                                    onClick = {
                                        val timestamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())
                                        exportJsonLauncher.launch("Novellum_Backup_${timestamp}.json")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Text("JSON", color = MaterialTheme.colorScheme.onSurface)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val timestamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())
                                        exportMarkdownLauncher.launch("Novellum_Manuscript_${timestamp}.md")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Text("Markdown", color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Chapters", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Button(
                                onClick = { viewModel.createChapter("New Chapter") },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("New Chapter", color = MaterialTheme.colorScheme.onPrimary)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            LazyColumn {
                                items(chapters) { chapter ->
                                    Text(
                                        text = chapter.title, 
                                        style = MaterialTheme.typography.titleMedium, 
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                    )
                                    
                                    val scenesInChapter = projectScenes.filter { it.chapterId == chapter.id }
                                    for (scene in scenesInChapter) {
                                        val isSelected = currentScene?.id == scene.id
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
                                                .clickable { viewModel.selectScene(scene.id) },
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                            )
                                        ) {
                                            Text(
                                                text = scene.title, 
                                                modifier = Modifier.padding(12.dp),
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    
                                    Button(
                                        onClick = { viewModel.createScene(chapter.id, "New Scene") }, 
                                        colors = ButtonDefaults.textButtonColors(),
                                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                                    ) {
                                        Text("+ Scene", color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            val editorContent: @Composable () -> Unit = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 24.dp)
                ) {
                    if (currentScene != null) {
                        val scene = currentScene!!
                        var proseText by remember(scene.id) { mutableStateOf(scene.prose) }

                        LaunchedEffect(scene.id) {
                            viewModel.syncSceneState(scene.prose)
                        }

                        Column(modifier = Modifier.fillMaxSize()) {
                            if (!isLandscape) {
                                IconButton(onClick = { viewModel.clearProjectSelection() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            
                            Text(scene.title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                            
                            TextField(
                                value = proseText,
                                onValueChange = { 
                                    proseText = it
                                    viewModel.onProseChanged(it)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(vertical = 16.dp),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                                placeholder = { Text("Write here...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                                )
                            )

                            Row {
                                Button(
                                    onClick = { viewModel.forceSaveCurrentScene() },
                                    enabled = saveState == SaveState.UNSAVED,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        contentColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text("Save")
                                }
                                if (saveState == SaveState.BLOCKED_EMPTY_CLEAR) {
                                    Button(
                                        onClick = { viewModel.forceSaveCurrentScene(isUserIntentClear = true) }, 
                                        modifier = Modifier.padding(start = 8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("Confirm Clear")
                                    }
                                }
                                Button(
                                    onClick = { viewModel.deleteScene(scene.id) }, 
                                    modifier = Modifier.padding(start = 8.dp),
                                    colors = ButtonDefaults.textButtonColors()
                                ) {
                                    Text("Delete Scene", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            // Save Status Message
                            val statusMsg = when (saveState) {
                                SaveState.SAVED -> lastSavedTime?.let { time ->
                                    val formatted = SimpleDateFormat("HH:mm", Locale.US).format(Date(time))
                                    "Saved at $formatted"
                                } ?: "Saved"
                                SaveState.UNSAVED -> "Unsaved changes"
                                SaveState.AUTOSAVING -> "Autosaving..."
                                SaveState.BLOCKED_EMPTY_CLEAR -> "Autosave blocked: empty clear requires confirmation"
                            }
                            Text(statusMsg, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp))
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            Text("Select a scene to edit.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.width(320.dp)) {
                        sidebarContent()
                    }
                    Divider(modifier = Modifier.width(1.dp).fillMaxSize(), color = MaterialTheme.colorScheme.outline)
                    Box(modifier = Modifier.weight(1f)) {
                        editorContent()
                    }
                }
            } else {
                if (currentScene == null) {
                    sidebarContent()
                } else {
                    editorContent()
                }
            }
        }
    }
}

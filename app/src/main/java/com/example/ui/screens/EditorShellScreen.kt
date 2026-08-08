package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.ChapterEntity
import com.example.data.entity.ProjectEntity
import com.example.data.entity.SceneEntity
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CharcoalBackground
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.DividerGray
import com.example.ui.theme.IgniteOrange
import com.example.ui.theme.IgniteOrangeLight
import com.example.ui.theme.MutedGrayPanel
import com.example.ui.theme.ScrimDark
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.EditorViewModel
import com.example.ui.viewmodel.SaveState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Dialog state management
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showNewChapterDialog by remember { mutableStateOf(false) }
    var showNewSceneChapterId by remember { mutableStateOf<String?>(null) }
    var sceneToDelete by remember { mutableStateOf<SceneEntity?>(null) }

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

    var isLeftPanelOpen by remember { mutableStateOf(currentScene == null) }
    var isRightPanelOpen by remember { mutableStateOf(false) }

    LaunchedEffect(currentScene) {
        if (currentScene == null) {
            isLeftPanelOpen = true
        }
    }

    Scaffold(
        topBar = {
            // Refined Top Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                color = CharcoalSurface
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left Brand & Toggle Area
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { 
                                    isLeftPanelOpen = !isLeftPanelOpen 
                                    if (!isLandscape && isLeftPanelOpen) {
                                        isRightPanelOpen = false
                                    }
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = "Toggle Left Panel",
                                    tint = if (isLeftPanelOpen) IgniteOrange else TextPrimary
                                )
                            }

                            // Novellum Logo Mark
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(IgniteOrange)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "N",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                            }

                            Text(
                                text = "NOVELLUM",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                letterSpacing = 2.sp
                            )
                        }

                        // Center Mode Navigation Pills
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Library",
                                color = TextMuted,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Text(
                                text = "Vault",
                                color = TextMuted,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            // Active Editor Mode
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(IgniteOrange)
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Editor",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "Cards",
                                color = TextMuted,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Text(
                                text = "Manuscript",
                                color = TextMuted,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }

                        // Right Status & Support Panel Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Subtle Save Status Badge
                            val (statusColor, statusText) = when (saveState) {
                                SaveState.SAVED -> StatusGreen to (lastSavedTime?.let {
                                    "Saved ${SimpleDateFormat("HH:mm", Locale.US).format(Date(it))}"
                                } ?: "Saved")
                                SaveState.UNSAVED -> IgniteOrangeLight to "Unsaved"
                                SaveState.AUTOSAVING -> IgniteOrange to "Saving..."
                                SaveState.BLOCKED_EMPTY_CLEAR -> MaterialTheme.colorScheme.error to "Clear pending"
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MutedGrayPanel)
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(statusColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = statusText,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            IconButton(
                                onClick = { 
                                    isRightPanelOpen = !isRightPanelOpen 
                                    if (!isLandscape && isRightPanelOpen) {
                                        isLeftPanelOpen = false
                                    }
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Toggle Right Panel",
                                    tint = if (isRightPanelOpen) IgniteOrange else TextPrimary
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = DividerGray, thickness = 1.dp)
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = CharcoalBackground
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Left Manuscript Sidebar Content
            val sidebarContent: @Composable () -> Unit = {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CharcoalSurface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MANUSCRIPT",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextMuted,
                                letterSpacing = 1.5.sp
                            )

                            if (selectedProjectId != null) {
                                IconButton(
                                    onClick = { viewModel.clearProjectSelection() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Switch Projects",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (selectedProjectId == null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Projects",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary
                                )
                                IconButton(
                                    onClick = { showNewProjectDialog = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "New Project", tint = IgniteOrange)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(projects) { project ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.selectProject(project.id) },
                                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                project.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = TextPrimary
                                            )
                                            if (project.description.isNotBlank()) {
                                                Text(
                                                    project.description,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = TextSecondary,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            val selectedProject = projects.find { it.id == selectedProjectId }
                            Text(
                                text = selectedProject?.title ?: "Manuscript",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CHAPTERS",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextMuted,
                                    letterSpacing = 1.2.sp
                                )

                                OutlinedButton(
                                    onClick = { showNewChapterDialog = true },
                                    modifier = Modifier.height(32.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = IgniteOrange),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, IgniteOrange.copy(alpha = 0.5f)),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Chapter", fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(chapters) { chapter ->
                                    Column {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = chapter.title.uppercase(),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = TextSecondary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            TextButton(
                                                onClick = { showNewSceneChapterId = chapter.id },
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                                modifier = Modifier.height(26.dp)
                                            ) {
                                                Text("+ Scene", color = IgniteOrange, fontSize = 12.sp)
                                            }
                                        }

                                        val scenesInChapter = projectScenes.filter { it.chapterId == chapter.id }
                                        for (scene in scenesInChapter) {
                                            val isSelected = currentScene?.id == scene.id
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(start = 12.dp, top = 3.dp, bottom = 3.dp)
                                                    .clickable { viewModel.selectScene(scene.id) },
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) IgniteOrange else CardSurface
                                                ),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        Icons.Outlined.Edit,
                                                        contentDescription = null,
                                                        tint = if (isSelected) Color.White else TextMuted,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = scene.title,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) Color.White else TextPrimary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = DividerGray, modifier = Modifier.padding(vertical = 12.dp))

                            // Quiet Secondary Export Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                TextButton(
                                    onClick = {
                                        val timestamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())
                                        exportJsonLauncher.launch("Novellum_Backup_${timestamp}.json")
                                    }
                                ) {
                                    Text("Backup JSON", color = TextSecondary, fontSize = 12.sp)
                                }
                                TextButton(
                                    onClick = {
                                        val timestamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())
                                        exportMarkdownLauncher.launch("Novellum_Manuscript_${timestamp}.md")
                                    }
                                ) {
                                    Text("Export MD", color = TextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Center Writing Editor Content
            val editorContent: @Composable () -> Unit = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CharcoalBackground),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (currentScene != null) {
                        val scene = currentScene!!
                        var proseText by remember(scene.id) { mutableStateOf(scene.prose) }
                        var showHeaderMenu by remember { mutableStateOf(false) }

                        LaunchedEffect(scene.id) {
                            viewModel.syncSceneState(scene.prose)
                        }

                        // Centered Writing Column
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 840.dp)
                                .padding(horizontal = 32.dp, vertical = 20.dp)
                        ) {
                            // Scene Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = scene.title,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = TextPrimary
                                    )
                                    val parentChapter = chapters.find { it.id == scene.chapterId }
                                    if (parentChapter != null) {
                                        Text(
                                            text = parentChapter.title,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextMuted
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Save Button
                                    if (saveState == SaveState.UNSAVED) {
                                        Button(
                                            onClick = { viewModel.forceSaveCurrentScene() },
                                            colors = ButtonDefaults.buttonColors(containerColor = IgniteOrange),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Text("Save", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else if (saveState == SaveState.BLOCKED_EMPTY_CLEAR) {
                                        Button(
                                            onClick = { viewModel.forceSaveCurrentScene(isUserIntentClear = true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Text("Confirm Clear", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Secondary Actions Dropdown
                                    Box {
                                        IconButton(onClick = { showHeaderMenu = true }) {
                                            Icon(Icons.Default.MoreVert, contentDescription = "Scene Actions", tint = TextSecondary)
                                        }
                                        DropdownMenu(
                                            expanded = showHeaderMenu,
                                            onDismissRequest = { showHeaderMenu = false },
                                            modifier = Modifier.background(CardSurface)
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Export Backup (JSON)", color = TextPrimary) },
                                                onClick = {
                                                    showHeaderMenu = false
                                                    val timestamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())
                                                    exportJsonLauncher.launch("Novellum_Backup_${timestamp}.json")
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Export Manuscript (MD)", color = TextPrimary) },
                                                onClick = {
                                                    showHeaderMenu = false
                                                    val timestamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())
                                                    exportMarkdownLauncher.launch("Novellum_Manuscript_${timestamp}.md")
                                                }
                                            )
                                            HorizontalDivider(color = DividerGray)
                                            DropdownMenuItem(
                                                text = { Text("Delete Scene", color = MaterialTheme.colorScheme.error) },
                                                onClick = {
                                                    showHeaderMenu = false
                                                    sceneToDelete = scene
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = DividerGray, modifier = Modifier.padding(vertical = 12.dp))

                            // Writing Canvas
                            TextField(
                                value = proseText,
                                onValueChange = {
                                    proseText = it
                                    viewModel.onProseChanged(it)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                                placeholder = {
                                    Text("Begin writing your prose here...", color = TextMuted, style = MaterialTheme.typography.bodyLarge)
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )

                            // Status Footer
                            val wordCount = proseText.split(Regex("\\s+")).filter { it.isNotBlank() }.size
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$wordCount words",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextMuted
                                )
                                Text(
                                    text = when (saveState) {
                                        SaveState.SAVED -> "Autosaved"
                                        SaveState.UNSAVED -> "Unsaved draft"
                                        SaveState.AUTOSAVING -> "Saving..."
                                        SaveState.BLOCKED_EMPTY_CLEAR -> "Empty clear blocked"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextMuted
                                )
                            }
                        }
                    } else {
                        // Calm Placeholder Surface
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Select a scene to start writing.",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Right Support Panel Content
            val rightPanelContent: @Composable () -> Unit = {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CharcoalSurface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SUPPORT PANEL",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextMuted,
                                letterSpacing = 1.5.sp
                            )
                            IconButton(
                                onClick = { isRightPanelOpen = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close Right Panel", tint = TextSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Segmented Control Tabs
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MutedGrayPanel)
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(IgniteOrange)
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Notes", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Vault", color = TextMuted, fontSize = 12.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Library", color = TextMuted, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Placeholder Card Body
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CardSurface),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Scene Notes & References",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MutedGrayPanel)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Preview Shell", color = TextMuted, fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Character sheets, plot outlines, and worldbuilding notes associated with this scene will appear here.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Screen Layout Rendering according to orientation
            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Panel with smooth horizontal animation
                    AnimatedVisibility(
                        visible = isLeftPanelOpen,
                        enter = expandHorizontally(),
                        exit = shrinkHorizontally()
                    ) {
                        Row {
                            Box(modifier = Modifier.width(280.dp)) {
                                sidebarContent()
                            }
                            VerticalDivider(color = DividerGray, thickness = 1.dp)
                        }
                    }

                    // Main Editor Center
                    Box(modifier = Modifier.weight(1f)) {
                        editorContent()
                    }

                    // Right Panel with smooth horizontal animation
                    AnimatedVisibility(
                        visible = isRightPanelOpen,
                        enter = expandHorizontally(expandFrom = Alignment.End),
                        exit = shrinkHorizontally(shrinkTowards = Alignment.End)
                    ) {
                        Row {
                            VerticalDivider(color = DividerGray, thickness = 1.dp)
                            Box(modifier = Modifier.width(280.dp)) {
                                rightPanelContent()
                            }
                        }
                    }
                }
            } else {
                // Portrait Tablet Layout: Overlays rather than multi-column splitting
                Box(modifier = Modifier.fillMaxSize()) {
                    editorContent()

                    // Left Panel Drawer Overlay
                    if (isLeftPanelOpen) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(ScrimDark)
                                .clickable { isLeftPanelOpen = false }
                        )
                        AnimatedVisibility(
                            visible = isLeftPanelOpen,
                            enter = expandHorizontally() + fadeIn(),
                            exit = shrinkHorizontally() + fadeOut(),
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(300.dp)
                                    .fillMaxHeight()
                            ) {
                                sidebarContent()
                            }
                        }
                    }

                    // Right Panel Drawer Overlay
                    if (isRightPanelOpen) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(ScrimDark)
                                .clickable { isRightPanelOpen = false }
                        )
                        AnimatedVisibility(
                            visible = isRightPanelOpen,
                            enter = expandHorizontally(expandFrom = Alignment.End) + fadeIn(),
                            exit = shrinkHorizontally(shrinkTowards = Alignment.End) + fadeOut(),
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(300.dp)
                                    .fillMaxHeight()
                            ) {
                                rightPanelContent()
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Create New Project
    if (showNewProjectDialog) {
        var projectTitle by remember { mutableStateOf("") }
        var projectDesc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewProjectDialog = false },
            title = { Text("Create Project", color = TextPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = projectTitle,
                        onValueChange = { projectTitle = it },
                        label = { Text("Project Title") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IgniteOrange,
                            unfocusedBorderColor = DividerGray
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = projectDesc,
                        onValueChange = { projectDesc = it },
                        label = { Text("Description (Optional)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IgniteOrange,
                            unfocusedBorderColor = DividerGray
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (projectTitle.isNotBlank()) {
                            viewModel.createProject(projectTitle.trim(), projectDesc.trim())
                            showNewProjectDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IgniteOrange)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewProjectDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CharcoalSurface
        )
    }

    // Dialog: Create New Chapter
    if (showNewChapterDialog) {
        var chapterTitle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewChapterDialog = false },
            title = { Text("New Chapter", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = chapterTitle,
                    onValueChange = { chapterTitle = it },
                    label = { Text("Chapter Title") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IgniteOrange,
                        unfocusedBorderColor = DividerGray
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (chapterTitle.isNotBlank()) {
                            viewModel.createChapter(chapterTitle.trim())
                            showNewChapterDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IgniteOrange)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewChapterDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CharcoalSurface
        )
    }

    // Dialog: Create New Scene
    showNewSceneChapterId?.let { chapterId ->
        var sceneTitle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewSceneChapterId = null },
            title = { Text("New Scene", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = sceneTitle,
                    onValueChange = { sceneTitle = it },
                    label = { Text("Scene Title") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IgniteOrange,
                        unfocusedBorderColor = DividerGray
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (sceneTitle.isNotBlank()) {
                            viewModel.createScene(chapterId, sceneTitle.trim())
                            showNewSceneChapterId = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IgniteOrange)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewSceneChapterId = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CharcoalSurface
        )
    }

    // Dialog: Delete Scene Confirmation
    sceneToDelete?.let { scene ->
        AlertDialog(
            onDismissRequest = { sceneToDelete = null },
            title = { Text("Delete Scene?", color = TextPrimary) },
            text = {
                Text(
                    "Are you sure you want to delete '${scene.title}'? A recovery checkpoint will be created before deletion. Restore functionality is not yet available in this version.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteScene(scene.id)
                        sceneToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { sceneToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CharcoalSurface
        )
    }
}

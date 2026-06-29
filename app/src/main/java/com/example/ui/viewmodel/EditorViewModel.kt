package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.entity.ChapterEntity
import com.example.data.entity.ProjectEntity
import com.example.data.entity.SceneEntity
import com.example.repository.ManuscriptRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SaveState {
    SAVED, UNSAVED, AUTOSAVING, BLOCKED_EMPTY_CLEAR
}

class EditorViewModel(
    private val repository: ManuscriptRepository
) : ViewModel() {

    val projects = repository.getProjects().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedProjectId = MutableStateFlow<String?>(null)
    val selectedProjectId = _selectedProjectId.asStateFlow()
    
    // In a real app we might flatten chapters and scenes into a tree.
    // For Milestone 1, we can just observe chapters based on selected project.
    val chapters = _selectedProjectId.flatMapLatest { projectId ->
        if (projectId != null) {
            repository.getChapters(projectId)
        } else {
            MutableStateFlow(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projectScenes = _selectedProjectId.flatMapLatest { projectId ->
        if (projectId != null) {
            repository.getScenesForProject(projectId)
        } else {
            MutableStateFlow(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedSceneId = MutableStateFlow<String?>(null)
    val selectedSceneId = _selectedSceneId.asStateFlow()

    val currentScene = _selectedSceneId.flatMapLatest { sceneId ->
        if (sceneId != null) {
            repository.getSceneFlow(sceneId)
        } else {
            MutableStateFlow(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage = _uiMessage.asStateFlow()

    private val _saveState = MutableStateFlow(SaveState.SAVED)
    val saveState = _saveState.asStateFlow()

    private val _lastSavedTime = MutableStateFlow<Long?>(null)
    val lastSavedTime = _lastSavedTime.asStateFlow()

    private var autosaveJob: Job? = null
    private var draftProse: String = ""
    private var savedProse: String = ""
    private var activeSceneId: String? = null

    fun selectProject(projectId: String) {
        _selectedProjectId.value = projectId
        _selectedSceneId.value = null
    }

    fun clearProjectSelection() {
        _selectedProjectId.value = null
        _selectedSceneId.value = null
    }

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    fun selectScene(sceneId: String) {
        forceSaveCurrentScene() // Ensure previous is saved if switching
        _selectedSceneId.value = sceneId
        activeSceneId = sceneId
        autosaveJob?.cancel()
        
        // Reset state for new scene (wait for flow to emit, but prep state)
        // In a real app we might want to observe the currentScene synchronously for the switch
        _saveState.value = SaveState.SAVED
    }
    
    // Called by UI when scene data loads to sync our tracking state
    fun syncSceneState(prose: String) {
        draftProse = prose
        savedProse = prose
        _saveState.value = SaveState.SAVED
    }

    fun createProject(title: String, description: String) {
        viewModelScope.launch {
            repository.createProject(title, description)
        }
    }

    fun createChapter(title: String) {
        val pid = _selectedProjectId.value ?: return
        viewModelScope.launch {
            repository.createChapter(pid, title, 0) // Simplified order
        }
    }
    
    fun createScene(chapterId: String, title: String) {
        viewModelScope.launch {
            repository.createScene(chapterId, title, 0) // Simplified order
        }
    }

    fun onProseChanged(newProse: String) {
        draftProse = newProse
        autosaveJob?.cancel()
        
        if (draftProse != savedProse) {
            if (draftProse.isEmpty() && savedProse.isNotEmpty()) {
                _saveState.value = SaveState.BLOCKED_EMPTY_CLEAR
            } else {
                _saveState.value = SaveState.UNSAVED
                autosaveJob = viewModelScope.launch {
                    delay(3000) // 3 seconds delay for autosave
                    forceSaveCurrentScene()
                }
            }
        } else {
            _saveState.value = SaveState.SAVED
        }
    }

    fun forceSaveCurrentScene(isUserIntentClear: Boolean = false) {
        val sceneId = activeSceneId ?: return
        if (draftProse == savedProse) return
        if (draftProse.isEmpty() && savedProse.isNotEmpty() && !isUserIntentClear) return
        if (_saveState.value == SaveState.AUTOSAVING) return

        val proseToSave = draftProse
        _saveState.value = SaveState.AUTOSAVING
        autosaveJob?.cancel()

        viewModelScope.launch {
            try {
                repository.updateSceneProse(sceneId, proseToSave, isUserIntentClear)
                savedProse = proseToSave
                
                if (draftProse == savedProse) {
                    _saveState.value = SaveState.SAVED
                } else if (draftProse.isEmpty() && savedProse.isNotEmpty()) {
                    _saveState.value = SaveState.BLOCKED_EMPTY_CLEAR
                } else {
                    _saveState.value = SaveState.UNSAVED
                }
                _lastSavedTime.value = System.currentTimeMillis()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
                _uiMessage.value = e.message ?: "Failed to save scene: Safety rejection."
                
                if (draftProse.isEmpty() && savedProse.isNotEmpty()) {
                    _saveState.value = SaveState.BLOCKED_EMPTY_CLEAR
                } else {
                    _saveState.value = SaveState.UNSAVED
                }
            }
        }
    }

    fun deleteScene(sceneId: String) {
        viewModelScope.launch {
            repository.deleteSceneSoft(sceneId)
            if (_selectedSceneId.value == sceneId) {
                _selectedSceneId.value = null
                activeSceneId = null
            }
        }
    }

    suspend fun getProjectBackupJson(projectId: String): String? {
        return repository.getFullProjectJson(projectId)
    }

    suspend fun getProjectMarkdown(projectId: String): String? {
        return repository.getFullProjectMarkdown(projectId)
    }
}

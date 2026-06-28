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
import kotlinx.coroutines.launch

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
        _selectedSceneId.value = sceneId
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

    // Debouncing happens in UI layer or flow for simplicity, here we just expose save
    fun saveSceneProse(sceneId: String, newProse: String, isUserIntentClear: Boolean = false) {
        viewModelScope.launch {
            try {
                repository.updateSceneProse(sceneId, newProse, isUserIntentClear)
            } catch (e: IllegalStateException) {
                // Handle programmatic empty overwrite rejection
                e.printStackTrace()
                _uiMessage.value = e.message ?: "Failed to save scene: Safety rejection."
            }
        }
    }

    fun deleteScene(sceneId: String) {
        viewModelScope.launch {
            repository.deleteSceneSoft(sceneId)
            if (_selectedSceneId.value == sceneId) {
                _selectedSceneId.value = null
            }
        }
    }
}

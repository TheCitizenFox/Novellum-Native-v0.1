package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.repository.ManuscriptRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
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

    val chapters = _selectedProjectId
        .flatMapLatest { projectId ->
            if (projectId != null) {
                repository.getChapters(projectId)
            } else {
                MutableStateFlow(emptyList())
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val projectScenes = _selectedProjectId
        .flatMapLatest { projectId ->
            if (projectId != null) {
                repository.getScenesForProject(projectId)
            } else {
                MutableStateFlow(emptyList())
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    private val _selectedSceneId = MutableStateFlow<String?>(null)
    val selectedSceneId = _selectedSceneId.asStateFlow()

    val currentScene = _selectedSceneId
        .flatMapLatest { sceneId ->
            if (sceneId != null) {
                repository.getSceneFlow(sceneId)
            } else {
                MutableStateFlow(null)
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )

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
        val projectId = _selectedProjectId.value ?: return

        viewModelScope.launch {
            repository.createChapter(projectId, title, 0)
        }
    }

    fun createScene(chapterId: String, title: String) {
        viewModelScope.launch {
            repository.createScene(chapterId, title, 0)
        }
    }

    fun saveSceneProse(
        sceneId: String,
        newProse: String,
        isUserIntentClear: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                repository.updateSceneProse(
                    sceneId = sceneId,
                    newProse = newProse,
                    isUserIntentClear = isUserIntentClear
                )
            } catch (e: IllegalStateException) {
                e.printStackTrace()
                _uiMessage.value =
                    e.message ?: "Failed to save scene: safety rejection."
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

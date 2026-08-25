package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.repository.ManuscriptRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class SaveState {
    SAVED,
    UNSAVED,
    AUTOSAVING,
    BLOCKED_EMPTY_CLEAR
}

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
            if (projectId != null) repository.getChapters(projectId)
            else MutableStateFlow(emptyList())
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val projectScenes = _selectedProjectId
        .flatMapLatest { projectId ->
            if (projectId != null) repository.getScenesForProject(projectId)
            else MutableStateFlow(emptyList())
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
            if (sceneId != null) repository.getSceneFlow(sceneId)
            else MutableStateFlow(null)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )

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
    private var saveWriteJob: Job? = null
    private val saveMutex = Mutex()

    fun selectProject(projectId: String) {
        if (_selectedProjectId.value == projectId && _selectedSceneId.value == null) return
        viewModelScope.launch {
            if (!prepareToLeaveCurrentScene()) return@launch
            autosaveJob?.cancel()
            autosaveJob = null
            _selectedProjectId.value = projectId
            _selectedSceneId.value = null
            activeSceneId = null
            draftProse = ""
            savedProse = ""
            _saveState.value = SaveState.SAVED
        }
    }

    fun clearProjectSelection() {
        viewModelScope.launch {
            if (!prepareToLeaveCurrentScene()) return@launch
            autosaveJob?.cancel()
            autosaveJob = null
            _selectedProjectId.value = null
            _selectedSceneId.value = null
            activeSceneId = null
            draftProse = ""
            savedProse = ""
            _saveState.value = SaveState.SAVED
        }
    }

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    fun selectScene(sceneId: String) {
        if (_selectedSceneId.value == sceneId) return
        viewModelScope.launch {
            if (!prepareToLeaveCurrentScene()) return@launch
            autosaveJob?.cancel()
            autosaveJob = null
            _selectedSceneId.value = sceneId
            activeSceneId = sceneId
            draftProse = ""
            savedProse = ""
            _saveState.value = SaveState.SAVED
        }
    }

    /**
     * Called once when a newly selected scene is loaded into the live editor.
     * This establishes the baseline used by autosave without reacting to later
     * Room emissions from the save that this editor itself initiated.
     */
    fun syncSceneState(sceneId: String, prose: String) {
        if (activeSceneId != sceneId) {
            activeSceneId = sceneId
        }
        draftProse = prose
        savedProse = prose
        autosaveJob?.cancel()
        autosaveJob = null
        _saveState.value = SaveState.SAVED
    }

    fun createNextProject() {
        viewModelScope.launch {
            repository.createProject("Project", "")
        }
    }

    fun createNextChapter() {
        val projectId = _selectedProjectId.value ?: return
        val current = chapters.value
        val nextOrderIndex = maxOf(
            current.size,
            (current.maxOfOrNull { it.orderIndex } ?: -1) + 1
        )
        viewModelScope.launch {
            repository.createChapter(projectId, "Chapter", nextOrderIndex)
        }
    }

    fun createNextScene(chapterId: String) {
        val current = projectScenes.value.filter { it.chapterId == chapterId }
        val nextOrderIndex = maxOf(
            current.size,
            (current.maxOfOrNull { it.orderIndex } ?: -1) + 1
        )
        viewModelScope.launch {
            repository.createScene(chapterId, "Scene", nextOrderIndex)
        }
    }

    fun renameProject(projectId: String, title: String) {
        viewModelScope.launch { repository.renameProject(projectId, title) }
    }

    fun renameChapter(chapterId: String, title: String) {
        viewModelScope.launch { repository.renameChapter(chapterId, title) }
    }

    fun renameScene(sceneId: String, title: String) {
        viewModelScope.launch { repository.renameScene(sceneId, title) }
    }

    fun onProseChanged(newProse: String) {
        draftProse = newProse
        autosaveJob?.cancel()
        autosaveJob = null

        when {
            draftProse == savedProse -> _saveState.value = SaveState.SAVED
            draftProse.isBlank() && savedProse.isNotBlank() -> {
                _saveState.value = SaveState.BLOCKED_EMPTY_CLEAR
            }
            else -> {
                _saveState.value = SaveState.UNSAVED
                scheduleAutosave()
            }
        }
    }

    private fun scheduleAutosave() {
        autosaveJob?.cancel()
        val sceneId = activeSceneId ?: return
        autosaveJob = viewModelScope.launch {
            delay(3000)
            autosaveJob = null
            if (activeSceneId == sceneId) {
                forceSaveCurrentScene()
            }
        }
    }

    fun forceSaveCurrentScene(isUserIntentClear: Boolean = false) {
        val sceneId = activeSceneId ?: return
        val proseToSave = draftProse
        val savedAtStart = savedProse

        if (proseToSave == savedAtStart) {
            if (sceneId == activeSceneId) _saveState.value = SaveState.SAVED
            return
        }

        if (proseToSave.isBlank() && savedAtStart.isNotBlank() && !isUserIntentClear) {
            if (sceneId == activeSceneId) _saveState.value = SaveState.BLOCKED_EMPTY_CLEAR
            return
        }

        autosaveJob?.cancel()
        autosaveJob = null

        if (saveWriteJob?.isActive == true) {
            val inFlight = saveWriteJob
            if (isUserIntentClear) {
                viewModelScope.launch {
                    inFlight?.join()
                    if (sceneId == activeSceneId && draftProse == proseToSave) {
                        forceSaveCurrentScene(isUserIntentClear = true)
                    }
                }
            } else if (sceneId == activeSceneId) {
                _saveState.value = SaveState.UNSAVED
                scheduleAutosave()
            }
            return
        }

        if (sceneId == activeSceneId) {
            _saveState.value = SaveState.AUTOSAVING
        }

        saveWriteJob = viewModelScope.launch {
            try {
                saveMutex.withLock {
                    repository.updateSceneProse(
                        sceneId = sceneId,
                        newProse = proseToSave,
                        isUserIntentClear = isUserIntentClear
                    )
                }

                if (sceneId == activeSceneId) {
                    savedProse = proseToSave
                    _lastSavedTime.value = System.currentTimeMillis()

                    when {
                        draftProse == savedProse -> _saveState.value = SaveState.SAVED
                        draftProse.isBlank() && savedProse.isNotBlank() -> {
                            _saveState.value = SaveState.BLOCKED_EMPTY_CLEAR
                        }
                        else -> {
                            _saveState.value = SaveState.UNSAVED
                            scheduleAutosave()
                        }
                    }
                }
            } catch (e: Exception) {
                _uiMessage.value = e.message ?: "Failed to save scene."
                if (sceneId == activeSceneId) {
                    _saveState.value = if (draftProse.isBlank() && savedProse.isNotBlank()) {
                        SaveState.BLOCKED_EMPTY_CLEAR
                    } else {
                        SaveState.UNSAVED
                    }
                }
            }
        }
    }

    private suspend fun prepareToLeaveCurrentScene(): Boolean {
        autosaveJob?.cancel()
        autosaveJob = null
        saveWriteJob?.join()

        val sceneId = activeSceneId ?: return true
        if (draftProse == savedProse) return true

        if (draftProse.isBlank() && savedProse.isNotBlank()) {
            _saveState.value = SaveState.BLOCKED_EMPTY_CLEAR
            _uiMessage.value = "The scene is blank but its last saved prose is not. Confirm clear before leaving this scene."
            return false
        }

        return try {
            val proseToSave = draftProse
            saveMutex.withLock {
                repository.updateSceneProse(sceneId, proseToSave, isUserIntentClear = false)
            }
            if (sceneId == activeSceneId) {
                savedProse = proseToSave
                _lastSavedTime.value = System.currentTimeMillis()
                _saveState.value = SaveState.SAVED
            }
            true
        } catch (e: Exception) {
            _uiMessage.value = e.message ?: "Could not safely save the current scene, so navigation was cancelled."
            _saveState.value = SaveState.UNSAVED
            false
        }
    }

    /**
     * Preserve any newer non-blank live draft before an operation that reads or
     * removes stored manuscript data. A blank live draft is intentionally not
     * persisted here because empty-overwrite protection should retain the last
     * saved prose unless the user explicitly confirms a clear.
     */
    private suspend fun persistLiveDraftIfSafe() {
        autosaveJob?.cancel()
        autosaveJob = null
        saveWriteJob?.join()

        val sceneId = activeSceneId ?: return
        if (draftProse != savedProse && draftProse.isNotBlank()) {
            val proseToSave = draftProse
            saveMutex.withLock {
                repository.updateSceneProse(sceneId, proseToSave, isUserIntentClear = false)
            }
            if (sceneId == activeSceneId) {
                savedProse = proseToSave
                _lastSavedTime.value = System.currentTimeMillis()
                _saveState.value = if (draftProse == savedProse) SaveState.SAVED else SaveState.UNSAVED
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            try {
                if (_selectedProjectId.value == projectId) {
                    persistLiveDraftIfSafe()
                }
                repository.deleteProjectSoft(projectId)
                if (_selectedProjectId.value == projectId) {
                    autosaveJob?.cancel()
                    autosaveJob = null
                    _selectedProjectId.value = null
                    _selectedSceneId.value = null
                    activeSceneId = null
                    draftProse = ""
                    savedProse = ""
                    _saveState.value = SaveState.SAVED
                }
            } catch (e: Exception) {
                _uiMessage.value = e.message ?: "Project deletion was stopped because the live draft could not be preserved."
            }
        }
    }

    fun deleteChapter(chapterId: String) {
        viewModelScope.launch {
            try {
                if (currentScene.value?.chapterId == chapterId) {
                    persistLiveDraftIfSafe()
                }
                repository.deleteChapterSoft(chapterId)
                if (currentScene.value?.chapterId == chapterId) {
                    autosaveJob?.cancel()
                    autosaveJob = null
                    _selectedSceneId.value = null
                    activeSceneId = null
                    draftProse = ""
                    savedProse = ""
                    _saveState.value = SaveState.SAVED
                }
            } catch (e: Exception) {
                _uiMessage.value = e.message ?: "Chapter deletion was stopped because the live draft could not be preserved."
            }
        }
    }

    fun deleteScene(sceneId: String) {
        viewModelScope.launch {
            try {
                if (_selectedSceneId.value == sceneId) {
                    persistLiveDraftIfSafe()
                }
                repository.deleteSceneSoft(sceneId)
                if (_selectedSceneId.value == sceneId) {
                    autosaveJob?.cancel()
                    autosaveJob = null
                    _selectedSceneId.value = null
                    activeSceneId = null
                    draftProse = ""
                    savedProse = ""
                    _saveState.value = SaveState.SAVED
                }
            } catch (e: Exception) {
                _uiMessage.value = e.message ?: "Scene deletion was stopped because the live draft could not be preserved."
            }
        }
    }

    suspend fun getProjectBackupJson(projectId: String): String? {
        if (_selectedProjectId.value == projectId) {
            persistLiveDraftIfSafe()
        }
        return repository.getFullProjectJson(projectId)
    }

    suspend fun getProjectMarkdown(projectId: String): String? {
        if (_selectedProjectId.value == projectId) {
            persistLiveDraftIfSafe()
        }
        return repository.getFullProjectMarkdown(projectId)
    }
}

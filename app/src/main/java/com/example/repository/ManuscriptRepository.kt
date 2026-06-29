package com.example.repository

import androidx.room.withTransaction
import com.example.data.AppDatabase
import com.example.data.dao.ManuscriptDao
import com.example.data.entity.BeatEntity
import com.example.data.entity.ChapterEntity
import com.example.data.entity.CheckpointEntity
import com.example.data.entity.ProjectEntity
import com.example.data.entity.RevisionEntity
import com.example.data.entity.SceneEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.UUID

class ManuscriptRepository(
    private val database: AppDatabase
) {
    private val dao: ManuscriptDao = database.manuscriptDao()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // --- Utility ---
    private fun generateId(): String = UUID.randomUUID().toString()
    
    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // --- Read operations ---
    fun getProjects(): Flow<List<ProjectEntity>> = dao.getProjects()
    fun getChapters(projectId: String): Flow<List<ChapterEntity>> = dao.getChaptersForProject(projectId)
    fun getScenesForProject(projectId: String): Flow<List<SceneEntity>> = dao.getScenesForProject(projectId)
    fun getScenes(chapterId: String): Flow<List<SceneEntity>> = dao.getScenesForChapter(chapterId)
    fun getBeats(sceneId: String): Flow<List<BeatEntity>> = dao.getBeatsForScene(sceneId)
    fun getSceneFlow(sceneId: String): Flow<SceneEntity?> = dao.getSceneFlow(sceneId)
    
    suspend fun getProjectById(projectId: String): ProjectEntity? = dao.getProjectById(projectId)

    // --- Export ---
    suspend fun getFullProjectJson(projectId: String): String? {
        val project = dao.getProjectById(projectId) ?: return null
        val chapters = dao.getChaptersForProjectSync(projectId)
        val scenes = dao.getScenesForProjectSync(projectId)
        val beats = dao.getBeatsForProjectSync(projectId)
        val snippets = dao.getSnippetsForProjectSync(projectId)
        val stagingItems = dao.getStagingItemsForProjectSync(projectId)

        val backup = com.example.data.model.ProjectBackup(
            schemaVersion = 1,
            exportedAt = System.currentTimeMillis(),
            appName = "Novellum",
            project = project,
            chapters = chapters,
            scenes = scenes,
            beats = beats,
            snippets = snippets,
            stagingItems = stagingItems
        )
        return json.encodeToString(backup)
    }

    suspend fun getFullProjectMarkdown(projectId: String): String? {
        val project = dao.getProjectById(projectId) ?: return null
        val chapters = dao.getChaptersForProjectSync(projectId)
        val scenes = dao.getScenesForProjectSync(projectId)
        val beats = dao.getBeatsForProjectSync(projectId)

        val sb = StringBuilder()
        sb.append("# ${project.title}\n\n")
        
        for (chapter in chapters) {
            sb.append("## ${chapter.title}\n\n")
            val chapterScenes = scenes.filter { it.chapterId == chapter.id }
            for (scene in chapterScenes) {
                sb.append("### ${scene.title}\n\n")
                if (scene.prose.isNotEmpty()) {
                    sb.append(scene.prose).append("\n\n")
                }
                
                val sceneBeats = beats.filter { it.sceneId == scene.id }
                if (sceneBeats.isNotEmpty()) {
                    sb.append("Beats\n\n")
                    for (beat in sceneBeats) {
                        if (beat.prose.isNotEmpty()) {
                            sb.append("- ${beat.prose}\n")
                        } else {
                            sb.append("- ${beat.title}\n") // fallback to title if prose is empty
                        }
                    }
                    sb.append("\n")
                }
            }
        }
        
        return sb.toString()
    }

    // --- Writes ---
    suspend fun createProject(title: String, description: String) {
        val project = ProjectEntity(
            id = generateId(),
            title = title,
            description = description,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        database.withTransaction {
            dao.insertProject(project)
        }
    }

    suspend fun createChapter(projectId: String, title: String, orderIndex: Int) {
        val chapter = ChapterEntity(
            id = generateId(),
            projectId = projectId,
            title = title,
            orderIndex = orderIndex,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        database.withTransaction {
            dao.insertChapter(chapter)
        }
    }

    suspend fun createScene(chapterId: String, title: String, orderIndex: Int) {
        val scene = SceneEntity(
            id = generateId(),
            chapterId = chapterId,
            title = title,
            prose = "",
            orderIndex = orderIndex,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        database.withTransaction {
            dao.insertScene(scene)
        }
    }

    suspend fun updateSceneProse(sceneId: String, newProse: String, isUserIntentClear: Boolean = false) {
        database.withTransaction {
            val scene = dao.getSceneById(sceneId) ?: return@withTransaction
            val chapter = dao.getChapterById(scene.chapterId) ?: return@withTransaction
            val realProjectId = chapter.projectId
            
            // Empty overwrite protection:
            val isClearing = scene.prose.isNotEmpty() && newProse.isEmpty()
            
            if (isClearing && !isUserIntentClear) {
                throw IllegalStateException("Programmatic empty overwrite rejected. Explicit user intent required to clear scene prose.")
            }

            val beforeJsonStr = json.encodeToString(scene)
            
            val updatedScene = scene.copy(
                prose = newProse,
                updatedAt = System.currentTimeMillis()
            )
            val afterJsonStr = json.encodeToString(updatedScene)

            if (isClearing) {
                // Destructive operation -> Checkpoint and Revision
                val checkpoint = CheckpointEntity(
                    id = generateId(),
                    projectId = realProjectId,
                    affectedEntityType = "SCENE",
                    affectedEntityId = scene.id,
                    reason = "User intentionally cleared scene",
                    humanLabel = "Before clear",
                    schemaVersion = 1,
                    payloadJson = beforeJsonStr,
                    payloadHash = hashString(beforeJsonStr),
                    createdAt = System.currentTimeMillis()
                )
                dao.insertCheckpoint(checkpoint)

                val revision = RevisionEntity(
                    id = generateId(),
                    projectId = realProjectId,
                    entityType = "SCENE",
                    entityId = scene.id,
                    operationType = "UPDATE_PROSE_CLEAR",
                    beforeJson = beforeJsonStr,
                    afterJson = afterJsonStr,
                    createdAt = System.currentTimeMillis(),
                    reason = "Intentional clear",
                    groupId = null
                )
                dao.insertRevision(revision)
            } else if (scene.prose != newProse) {
                // Normal revision tracking (In real app, debounce/coalesce logic happens in ViewModel, here we save what VM sends)
                val revision = RevisionEntity(
                    id = generateId(),
                    projectId = realProjectId,
                    entityType = "SCENE",
                    entityId = scene.id,
                    operationType = "UPDATE_PROSE",
                    beforeJson = beforeJsonStr,
                    afterJson = afterJsonStr,
                    createdAt = System.currentTimeMillis(),
                    reason = "Auto-save",
                    groupId = null
                )
                dao.insertRevision(revision)
            }
            
            dao.updateScene(updatedScene)
        }
    }

    suspend fun deleteSceneSoft(sceneId: String) {
        database.withTransaction {
            val scene = dao.getSceneById(sceneId) ?: return@withTransaction
            val chapter = dao.getChapterById(scene.chapterId) ?: return@withTransaction
            val realProjectId = chapter.projectId
            
            val beforeJsonStr = json.encodeToString(scene)
            
            // Destructive action -> Checkpoint
            val checkpoint = CheckpointEntity(
                id = generateId(),
                projectId = realProjectId,
                affectedEntityType = "SCENE",
                affectedEntityId = scene.id,
                reason = "User deleted scene",
                humanLabel = "Before deletion",
                schemaVersion = 1,
                payloadJson = beforeJsonStr,
                payloadHash = hashString(beforeJsonStr),
                createdAt = System.currentTimeMillis()
            )
            dao.insertCheckpoint(checkpoint)
            
            val updatedScene = scene.copy(
                isDeleted = true,
                deletedAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            dao.updateScene(updatedScene)
            
            val revision = RevisionEntity(
                id = generateId(),
                projectId = realProjectId,
                entityType = "SCENE",
                entityId = scene.id,
                operationType = "DELETE",
                beforeJson = beforeJsonStr,
                afterJson = json.encodeToString(updatedScene),
                createdAt = System.currentTimeMillis(),
                reason = "Soft delete",
                groupId = null
            )
            dao.insertRevision(revision)
        }
    }
}

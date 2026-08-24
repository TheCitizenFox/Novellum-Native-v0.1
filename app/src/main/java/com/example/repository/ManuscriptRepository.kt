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

    private fun generateId(): String = UUID.randomUUID().toString()

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun getProjects(): Flow<List<ProjectEntity>> = dao.getProjects()
    fun getChapters(projectId: String): Flow<List<ChapterEntity>> = dao.getChaptersForProject(projectId)
    fun getScenesForProject(projectId: String): Flow<List<SceneEntity>> = dao.getScenesForProject(projectId)
    fun getScenes(chapterId: String): Flow<List<SceneEntity>> = dao.getScenesForChapter(chapterId)
    fun getBeats(sceneId: String): Flow<List<BeatEntity>> = dao.getBeatsForScene(sceneId)
    fun getSceneFlow(sceneId: String): Flow<SceneEntity?> = dao.getSceneFlow(sceneId)
    suspend fun getProjectById(projectId: String): ProjectEntity? = dao.getProjectById(projectId)

    suspend fun createProject(title: String, description: String) {
        val project = ProjectEntity(
            id = generateId(),
            title = title,
            description = description,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        database.withTransaction { dao.insertProject(project) }
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
        database.withTransaction { dao.insertChapter(chapter) }
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
        database.withTransaction { dao.insertScene(scene) }
    }

    suspend fun renameProject(projectId: String, newTitle: String) {
        val cleanTitle = newTitle.trim()
        if (cleanTitle.isEmpty()) return

        database.withTransaction {
            val project = dao.getProjectById(projectId) ?: return@withTransaction
            if (project.title == cleanTitle) return@withTransaction
            dao.updateProject(
                project.copy(
                    title = cleanTitle,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun renameChapter(chapterId: String, newTitle: String) {
        val cleanTitle = newTitle.trim()
        if (cleanTitle.isEmpty()) return

        database.withTransaction {
            val chapter = dao.getChapterById(chapterId) ?: return@withTransaction
            if (chapter.title == cleanTitle) return@withTransaction
            dao.updateChapter(
                chapter.copy(
                    title = cleanTitle,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun renameScene(sceneId: String, newTitle: String) {
        val cleanTitle = newTitle.trim()
        if (cleanTitle.isEmpty()) return

        database.withTransaction {
            val scene = dao.getSceneById(sceneId) ?: return@withTransaction
            if (scene.title == cleanTitle) return@withTransaction
            dao.updateScene(
                scene.copy(
                    title = cleanTitle,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun updateSceneProse(
        sceneId: String,
        newProse: String,
        isUserIntentClear: Boolean = false
    ) {
        database.withTransaction {
            val scene = dao.getSceneById(sceneId) ?: return@withTransaction
            val isClearing = scene.prose.isNotEmpty() && newProse.isEmpty()

            if (isClearing && !isUserIntentClear) {
                throw IllegalStateException(
                    "Programmatic empty overwrite rejected. Explicit user intent required to clear scene prose."
                )
            }

            val beforeJsonStr = json.encodeToString(scene)
            val updatedScene = scene.copy(
                prose = newProse,
                updatedAt = System.currentTimeMillis()
            )
            val afterJsonStr = json.encodeToString(updatedScene)

            if (isClearing) {
                val checkpoint = CheckpointEntity(
                    id = generateId(),
                    projectId = scene.chapterId,
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
            } else if (scene.prose != newProse) {
                val revision = RevisionEntity(
                    id = generateId(),
                    projectId = scene.chapterId,
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
            val beforeJsonStr = json.encodeToString(scene)

            val checkpoint = CheckpointEntity(
                id = generateId(),
                projectId = scene.chapterId,
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
                projectId = scene.chapterId,
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

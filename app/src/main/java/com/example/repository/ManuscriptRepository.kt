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
                        val beatText = if (beat.prose.isNotEmpty()) beat.prose else beat.title
                        sb.append("- ").append(beatText).append("\n")
                    }
                    sb.append("\n")
                }
            }
        }

        return sb.toString()
    }

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
            val chapter = dao.getChapterById(scene.chapterId) ?: return@withTransaction
            val realProjectId = chapter.projectId
            val isClearing = scene.prose.isNotBlank() && newProse.isBlank()

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

                dao.insertRevision(
                    RevisionEntity(
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
                )
            } else if (scene.prose != newProse) {
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

    suspend fun deleteProjectSoft(projectId: String) {
        database.withTransaction {
            val project = dao.getProjectById(projectId) ?: return@withTransaction
            val beforeJsonStr = json.encodeToString(project)
            val now = System.currentTimeMillis()

            val checkpoint = CheckpointEntity(
                id = generateId(),
                projectId = project.id,
                affectedEntityType = "PROJECT",
                affectedEntityId = project.id,
                reason = "User deleted project",
                humanLabel = "Before deletion",
                schemaVersion = 1,
                payloadJson = beforeJsonStr,
                payloadHash = hashString(beforeJsonStr),
                createdAt = now
            )
            dao.insertCheckpoint(checkpoint)

            val updatedProject = project.copy(
                isDeleted = true,
                deletedAt = now,
                updatedAt = now
            )
            dao.updateProject(updatedProject)

            dao.insertRevision(
                RevisionEntity(
                    id = generateId(),
                    projectId = project.id,
                    entityType = "PROJECT",
                    entityId = project.id,
                    operationType = "DELETE",
                    beforeJson = beforeJsonStr,
                    afterJson = json.encodeToString(updatedProject),
                    createdAt = now,
                    reason = "Soft delete",
                    groupId = null
                )
            )
        }
    }

    suspend fun deleteChapterSoft(chapterId: String) {
        database.withTransaction {
            val chapter = dao.getChapterById(chapterId) ?: return@withTransaction
            val beforeJsonStr = json.encodeToString(chapter)
            val now = System.currentTimeMillis()

            val checkpoint = CheckpointEntity(
                id = generateId(),
                projectId = chapter.projectId,
                affectedEntityType = "CHAPTER",
                affectedEntityId = chapter.id,
                reason = "User deleted chapter",
                humanLabel = "Before deletion",
                schemaVersion = 1,
                payloadJson = beforeJsonStr,
                payloadHash = hashString(beforeJsonStr),
                createdAt = now
            )
            dao.insertCheckpoint(checkpoint)

            val updatedChapter = chapter.copy(
                isDeleted = true,
                deletedAt = now,
                updatedAt = now
            )
            dao.updateChapter(updatedChapter)

            dao.insertRevision(
                RevisionEntity(
                    id = generateId(),
                    projectId = chapter.projectId,
                    entityType = "CHAPTER",
                    entityId = chapter.id,
                    operationType = "DELETE",
                    beforeJson = beforeJsonStr,
                    afterJson = json.encodeToString(updatedChapter),
                    createdAt = now,
                    reason = "Soft delete",
                    groupId = null
                )
            )
        }
    }

    suspend fun deleteSceneSoft(sceneId: String) {
        database.withTransaction {
            val scene = dao.getSceneById(sceneId) ?: return@withTransaction
            val chapter = dao.getChapterById(scene.chapterId) ?: return@withTransaction
            val realProjectId = chapter.projectId
            val beforeJsonStr = json.encodeToString(scene)

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

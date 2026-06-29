package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.entity.BeatEntity
import com.example.data.entity.ChapterEntity
import com.example.data.entity.CheckpointEntity
import com.example.data.entity.ProjectEntity
import com.example.data.entity.RevisionEntity
import com.example.data.entity.SceneEntity
import com.example.data.entity.SnippetEntity
import com.example.data.entity.StagingItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ManuscriptDao {

    // --- Projects ---
    @Query("SELECT * FROM projects WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun getProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id AND isDeleted = 0")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    // --- Chapters ---
    @Query("SELECT * FROM chapters WHERE projectId = :projectId AND isDeleted = 0 ORDER BY orderIndex ASC")
    fun getChaptersForProject(projectId: String): Flow<List<ChapterEntity>>
    
    @Query("SELECT * FROM chapters WHERE projectId = :projectId AND isDeleted = 0 ORDER BY orderIndex ASC")
    suspend fun getChaptersForProjectSync(projectId: String): List<ChapterEntity>
    
    @Query("SELECT * FROM chapters WHERE id = :id")
    suspend fun getChapterById(id: String): ChapterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity)

    @Update
    suspend fun updateChapter(chapter: ChapterEntity)

    // --- Scenes ---
    @Query("SELECT scenes.* FROM scenes INNER JOIN chapters ON scenes.chapterId = chapters.id WHERE chapters.projectId = :projectId AND scenes.isDeleted = 0 ORDER BY chapters.orderIndex ASC, scenes.orderIndex ASC")
    fun getScenesForProject(projectId: String): Flow<List<SceneEntity>>

    @Query("SELECT scenes.* FROM scenes INNER JOIN chapters ON scenes.chapterId = chapters.id WHERE chapters.projectId = :projectId AND scenes.isDeleted = 0 ORDER BY chapters.orderIndex ASC, scenes.orderIndex ASC")
    suspend fun getScenesForProjectSync(projectId: String): List<SceneEntity>

    @Query("SELECT * FROM scenes WHERE chapterId = :chapterId AND isDeleted = 0 ORDER BY orderIndex ASC")
    fun getScenesForChapter(chapterId: String): Flow<List<SceneEntity>>

    @Query("SELECT * FROM scenes WHERE id = :id AND isDeleted = 0")
    fun getSceneFlow(id: String): Flow<SceneEntity?>

    @Query("SELECT * FROM scenes WHERE id = :id")
    suspend fun getSceneById(id: String): SceneEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScene(scene: SceneEntity)

    @Update
    suspend fun updateScene(scene: SceneEntity)

    // --- Beats ---
    @Query("SELECT * FROM beats WHERE sceneId = :sceneId AND isDeleted = 0 ORDER BY orderIndex ASC")
    fun getBeatsForScene(sceneId: String): Flow<List<BeatEntity>>

    @Query("SELECT beats.* FROM beats INNER JOIN scenes ON beats.sceneId = scenes.id INNER JOIN chapters ON scenes.chapterId = chapters.id WHERE chapters.projectId = :projectId AND beats.isDeleted = 0 ORDER BY chapters.orderIndex ASC, scenes.orderIndex ASC, beats.orderIndex ASC")
    suspend fun getBeatsForProjectSync(projectId: String): List<BeatEntity>

    @Query("SELECT * FROM beats WHERE id = :id")
    suspend fun getBeatById(id: String): BeatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeat(beat: BeatEntity)

    @Update
    suspend fun updateBeat(beat: BeatEntity)

    // --- Snippets ---
    @Query("SELECT * FROM snippets WHERE projectId = :projectId AND isDeleted = 0")
    fun getSnippetsForProject(projectId: String): Flow<List<SnippetEntity>>
    
    @Query("SELECT * FROM snippets WHERE projectId = :projectId AND isDeleted = 0")
    suspend fun getSnippetsForProjectSync(projectId: String): List<SnippetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnippet(snippet: SnippetEntity)

    @Update
    suspend fun updateSnippet(snippet: SnippetEntity)

    // --- Staging Items ---
    @Query("SELECT * FROM staging_items WHERE projectId = :projectId AND isDeleted = 0")
    fun getStagingItemsForProject(projectId: String): Flow<List<StagingItemEntity>>
    
    @Query("SELECT * FROM staging_items WHERE projectId = :projectId AND isDeleted = 0")
    suspend fun getStagingItemsForProjectSync(projectId: String): List<StagingItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStagingItem(item: StagingItemEntity)

    @Update
    suspend fun updateStagingItem(item: StagingItemEntity)

    // --- Revisions ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRevision(revision: RevisionEntity)

    @Query("SELECT * FROM revisions WHERE entityId = :entityId ORDER BY createdAt DESC")
    fun getRevisionsForEntity(entityId: String): Flow<List<RevisionEntity>>

    // --- Checkpoints ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckpoint(checkpoint: CheckpointEntity)

    @Query("SELECT * FROM checkpoints WHERE affectedEntityId = :entityId ORDER BY createdAt DESC")
    fun getCheckpointsForEntity(entityId: String): Flow<List<CheckpointEntity>>
}

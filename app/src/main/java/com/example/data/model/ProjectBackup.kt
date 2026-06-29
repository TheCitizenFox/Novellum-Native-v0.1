package com.example.data.model

import com.example.data.entity.BeatEntity
import com.example.data.entity.ChapterEntity
import com.example.data.entity.ProjectEntity
import com.example.data.entity.SceneEntity
import com.example.data.entity.SnippetEntity
import com.example.data.entity.StagingItemEntity
import kotlinx.serialization.Serializable

@Serializable
data class ProjectBackup(
    val schemaVersion: Int,
    val exportedAt: Long,
    val appName: String,
    val project: ProjectEntity,
    val chapters: List<ChapterEntity>,
    val scenes: List<SceneEntity>,
    val beats: List<BeatEntity>,
    val snippets: List<SnippetEntity>,
    val stagingItems: List<StagingItemEntity>
)

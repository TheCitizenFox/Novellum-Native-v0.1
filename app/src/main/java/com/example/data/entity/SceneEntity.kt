package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "scenes",
    foreignKeys = [
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"]
        )
    ],
    indices = [Index("chapterId")] // CASCADE delete removed for authored content
)
data class SceneEntity(
    @PrimaryKey val id: String,
    val chapterId: String,
    val title: String,
    val prose: String,
    val orderIndex: Int,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

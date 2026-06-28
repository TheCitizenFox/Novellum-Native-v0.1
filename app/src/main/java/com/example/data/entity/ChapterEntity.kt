package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"]
        )
    ],
    indices = [Index("projectId")]
)
data class ChapterEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val title: String,
    val orderIndex: Int,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

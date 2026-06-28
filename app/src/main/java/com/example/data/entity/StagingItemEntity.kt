package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "staging_items",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"]
        )
    ],
    indices = [Index("projectId")]
)
data class StagingItemEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val content: String,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

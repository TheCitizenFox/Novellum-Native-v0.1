package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "revisions",
    indices = [
        Index("projectId"),
        Index("entityId")
    ]
)
data class RevisionEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val entityType: String, // e.g. "SCENE", "BEAT", "CHAPTER"
    val entityId: String,
    val operationType: String, // e.g. "CREATE", "UPDATE", "DELETE", "REORDER"
    val beforeJson: String?,
    val afterJson: String?,
    val createdAt: Long,
    val reason: String?,
    val groupId: String?
)

package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "checkpoints",
    indices = [
        Index("projectId"),
        Index("affectedEntityId")
    ]
)
data class CheckpointEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val affectedEntityType: String,
    val affectedEntityId: String,
    val reason: String,
    val humanLabel: String,
    val schemaVersion: Int,
    val payloadJson: String,
    val payloadHash: String,
    val createdAt: Long
)

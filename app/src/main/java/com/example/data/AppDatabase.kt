package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.dao.ManuscriptDao
import com.example.data.entity.BeatEntity
import com.example.data.entity.ChapterEntity
import com.example.data.entity.CheckpointEntity
import com.example.data.entity.ProjectEntity
import com.example.data.entity.RevisionEntity
import com.example.data.entity.SceneEntity
import com.example.data.entity.SnippetEntity
import com.example.data.entity.StagingItemEntity

@Database(
    entities = [
        ProjectEntity::class,
        ChapterEntity::class,
        SceneEntity::class,
        BeatEntity::class,
        SnippetEntity::class,
        StagingItemEntity::class,
        RevisionEntity::class,
        CheckpointEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun manuscriptDao(): ManuscriptDao
}

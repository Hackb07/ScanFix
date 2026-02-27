package com.example.tfliteapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface DetectionDao {
    @Insert
    suspend fun insertAll(detections: List<DetectionEntity>)

    @Query("SELECT * FROM detections ORDER BY timestamp DESC")
    suspend fun getAllDetections(): List<DetectionEntity>

    @Query("SELECT * FROM detections ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentDetections(limit: Int): List<DetectionEntity>

    @Query("SELECT COUNT(*) FROM detections")
    suspend fun getTotalCount(): Int

    @Query("SELECT label, COUNT(*) as count FROM detections GROUP BY label ORDER BY count DESC")
    suspend fun getClassDistribution(): List<ClassCount>

    @Query("DELETE FROM detections")
    suspend fun clearAll()
}

data class ClassCount(
    val label: String,
    val count: Int
)

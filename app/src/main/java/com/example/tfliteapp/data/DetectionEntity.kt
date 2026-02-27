package com.example.tfliteapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "detections")
data class DetectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis()
)

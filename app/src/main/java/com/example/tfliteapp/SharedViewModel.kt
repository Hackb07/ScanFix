package com.example.tfliteapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.tensorflow.lite.task.vision.detector.Detection

class SharedViewModel : ViewModel() {
    private val _threshold = MutableLiveData<Float>(0.5f)
    val threshold: LiveData<Float> = _threshold

    // Live data for dashboard
    private val _totalDetections = MutableLiveData<Int>(0)
    val totalDetections: LiveData<Int> = _totalDetections

    private val _averageConfidence = MutableLiveData<Float>(0f)
    val averageConfidence: LiveData<Float> = _averageConfidence
    
    // Store recent detections as a list of strings "Label (Score)"
    private val _recentDetections = MutableLiveData<List<String>>(emptyList())
    val recentDetections: LiveData<List<String>> = _recentDetections

    // Internal state to calculate average
    private var totalConferenceSum = 0f
    private var detectionCountInternal = 0

    fun setThreshold(value: Float) {
        _threshold.value = value
    }

    fun updateDetectionStats(results: List<Detection>) {
        if (results.isEmpty()) return

        detectionCountInternal += results.size
        _totalDetections.postValue(detectionCountInternal)

        var batchSum = 0f
        val newDetections = mutableListOf<String>()

        for (detection in results) {
            val category = detection.categories.firstOrNull() ?: continue
            batchSum += category.score
            newDetections.add("${category.label} (${String.format("%.2f", category.score)})")
        }

        totalConferenceSum += batchSum
        if (detectionCountInternal > 0) {
            _averageConfidence.postValue(totalConferenceSum / detectionCountInternal)
        }

        val currentList = _recentDetections.value?.toMutableList() ?: mutableListOf()
        currentList.addAll(0, newDetections)
        // Keep only top 10
        val cappedList = if (currentList.size > 10) currentList.subList(0, 10) else currentList
        _recentDetections.postValue(cappedList)
    }
}

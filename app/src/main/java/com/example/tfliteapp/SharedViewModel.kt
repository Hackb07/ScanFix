package com.example.tfliteapp

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tfliteapp.data.AppDatabase
import com.example.tfliteapp.data.DetectionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.task.vision.detector.Detection
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Context
import androidx.core.content.FileProvider

class SharedViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val detectionDao = database.detectionDao()

    private val _threshold = MutableLiveData<Float>(0.5f)
    val threshold: LiveData<Float> = _threshold

    // Optional target class to beep on. Empty means beep on anything.
    private val _targetClass = MutableLiveData<String>("")
    val targetClass: LiveData<String> = _targetClass

    // Live data for dashboard
    private val _totalDetections = MutableLiveData<Int>(0)
    val totalDetections: LiveData<Int> = _totalDetections

    private val _averageConfidence = MutableLiveData<Float>(0f)
    val averageConfidence: LiveData<Float> = _averageConfidence
    
    // Store recent detections as a list of strings "Label (Score)"
    private val _recentDetections = MutableLiveData<List<String>>(emptyList())
    val recentDetections: LiveData<List<String>> = _recentDetections

    // Selected custom TFLite model URI (null = use bundled default model)
    private val _modelUri = MutableLiveData<Uri?>(null)
    val modelUri: LiveData<Uri?> = _modelUri

    // Class distribution map: Label -> Count
    private val _classDistribution = MutableLiveData<Map<String, Int>>(emptyMap())
    val classDistribution: LiveData<Map<String, Int>> = _classDistribution

    // Internal state to calculate average
    private var totalConferenceSum = 0f
    private var detectionCountInternal = 0
    private val classCountsInternal = mutableMapOf<String, Int>()

    init {
        loadHistoricalData()
    }

    private fun loadHistoricalData() {
        viewModelScope.launch {
            try {
                // Background operations
                val total = withContext(Dispatchers.IO) { detectionDao.getTotalCount() }
                if (total == 0) return@launch // Nothing to load
                
                val allDetections = withContext(Dispatchers.IO) { detectionDao.getAllDetections() }
                val distributions = withContext(Dispatchers.IO) { detectionDao.getClassDistribution() }
                
                // Update internal counters and live data
                detectionCountInternal = total
                _totalDetections.postValue(total)
                
                var globalSum = 0f
                val recent = mutableListOf<String>()
                allDetections.forEachIndexed { index, det ->
                    globalSum += det.confidence
                    if (index < 10) {
                        recent.add("${det.label} (${String.format("%.2f", det.confidence)})")
                    }
                }
                
                totalConferenceSum = globalSum
                _averageConfidence.postValue(if(total > 0) globalSum / total else 0f)
                _recentDetections.postValue(recent)
                
                distributions.forEach { classCountsInternal[it.label] = it.count }
                _classDistribution.postValue(classCountsInternal.toMap())
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    fun setThreshold(value: Float) {
        _threshold.value = value
    }

    fun setTargetClass(target: String) {
        _targetClass.value = target
    }

    fun setModelUri(uri: Uri?) {
        _modelUri.value = uri
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
        
        for (detection in results) {
            val category = detection.categories.firstOrNull() ?: continue
            classCountsInternal[category.label] = (classCountsInternal[category.label] ?: 0) + 1
        }
        _classDistribution.postValue(classCountsInternal.toMap())

        val currentList = _recentDetections.value?.toMutableList() ?: mutableListOf()
        currentList.addAll(0, newDetections)
        // Keep only top 10
        val cappedList = if (currentList.size > 10) currentList.subList(0, 10) else currentList
        _recentDetections.postValue(cappedList)
        
        // Save to Database asynchronously
        val newEntities = results.mapNotNull {
            val category = it.categories.firstOrNull()
            if (category != null) {
                DetectionEntity(label = category.label, confidence = category.score)
            } else null
        }
        if (newEntities.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    detectionDao.insertAll(newEntities)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun clearDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                detectionDao.clearAll()
                
                withContext(Dispatchers.Main) {
                    // Reset UI
                    totalConferenceSum = 0f
                    detectionCountInternal = 0
                    classCountsInternal.clear()
                    
                    _totalDetections.value = 0
                    _averageConfidence.value = 0f
                    _classDistribution.value = emptyMap()
                    _recentDetections.value = emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun exportDatabaseToCsv(outputStream: OutputStream, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allDetections = detectionDao.getAllDetections()
                if (allDetections.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        onComplete(false, "No data to export.")
                    }
                    return@launch
                }

                val writer = OutputStreamWriter(outputStream)
                writer.write("ID,Label,Confidence,Timestamp\n")

                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                for (detection in allDetections) {
                    val dateStr = sdf.format(Date(detection.timestamp))
                    val confidenceStr = String.format("%.2f", detection.confidence)
                    val escapedLabel = if (detection.label.contains(",")) "\"${detection.label}\"" else detection.label
                    writer.write("${detection.id},$escapedLabel,$confidenceStr,$dateStr\n")
                }

                writer.flush()
                writer.close()

                withContext(Dispatchers.Main) {
                    onComplete(true, "Data exported successfully.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onComplete(false, "Export failed: ${e.localizedMessage}")
                }
            }
        }
    }

    fun exportDatabaseToCacheAndShare(context: Context, onComplete: (Boolean, Uri?, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allDetections = detectionDao.getAllDetections()
                if (allDetections.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        onComplete(false, null, "No data to export.")
                    }
                    return@launch
                }

                // Create exports directory in cache
                val exportsDir = File(context.cacheDir, "exports")
                if (!exportsDir.exists()) {
                    exportsDir.mkdirs()
                }

                val sdfTime = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val fileName = "ScanFix_Export_${sdfTime.format(Date())}.csv"
                val file = File(exportsDir, fileName)

                val writer = OutputStreamWriter(FileOutputStream(file))
                writer.write("ID,Label,Confidence,Timestamp\n")

                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                for (detection in allDetections) {
                    val dateStr = sdf.format(Date(detection.timestamp))
                    val confidenceStr = String.format("%.2f", detection.confidence)
                    val escapedLabel = if (detection.label.contains(",")) "\"${detection.label}\"" else detection.label
                    writer.write("${detection.id},$escapedLabel,$confidenceStr,$dateStr\n")
                }

                writer.flush()
                writer.close()

                // Generate FileProvider URI
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                withContext(Dispatchers.Main) {
                    onComplete(true, uri, "Data exported successfully.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onComplete(false, null, "Export failed: ${e.localizedMessage}")
                }
            }
        }
    }
}

package com.example.tfliteapp

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.io.File
import java.io.FileOutputStream

class ObjectDetectorHelper(
    var threshold: Float = 0.5f,
    var numThreads: Int = 2,
    var maxResults: Int = 3,
    var currentDelegate: Int = 0,
    var currentModel: Int = 0,
    val context: Context,
    val objectDetectorListener: DetectorListener?,
    var modelUri: Uri? = null
) {

    private var objectDetector: ObjectDetector? = null

    init {
        setupObjectDetector()
    }

    fun clearObjectDetector() {
        objectDetector = null
    }

    fun setupObjectDetector() {
        val optionsBuilder =
            ObjectDetector.ObjectDetectorOptions.builder()
                .setScoreThreshold(threshold)
                .setMaxResults(maxResults)

        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(numThreads)
        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        try {
            if (modelUri != null) {
                // Load model from user-selected file URI
                val modelFile = copyUriToLocalFile(modelUri!!)
                if (modelFile != null) {
                    // Use the file path string with Context overload
                    objectDetector =
                        ObjectDetector.createFromFileAndOptions(
                            context,
                            modelFile.absolutePath,
                            optionsBuilder.build()
                        )
                    Log.d("ObjectDetectorHelper", "Loaded custom model from: ${modelFile.absolutePath}")
                } else {
                    objectDetectorListener?.onError("Failed to load the selected model file.")
                }
            } else {
                // Fallback: load bundled default model from assets
                val modelName = "mobilenet_v1_1_0_224_quant.tflite"
                objectDetector =
                    ObjectDetector.createFromFileAndOptions(context, modelName, optionsBuilder.build())
            }
        } catch (e: Exception) {
            objectDetectorListener?.onError(
                "Object detector failed to initialize. See error logs for details"
            )
            Log.e("ObjectDetectorHelper", "TFLite failed to load model with error: " + e.message)
        }
    }

    /**
     * Copies the content from a URI to a local file in the app's cache directory
     * so TFLite can read it via an absolute path.
     */
    private fun copyUriToLocalFile(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File(context.filesDir, "custom_model.tflite")
            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            tempFile
        } catch (e: Exception) {
            Log.e("ObjectDetectorHelper", "Error copying model file: ${e.message}")
            null
        }
    }

    fun detect(image: Bitmap, imageRotation: Int) {
        if (objectDetector == null) {
            setupObjectDetector()
        }

        var inferenceTime = SystemClock.uptimeMillis()

        val imageProcessor =
            ImageProcessor.Builder()
                .build()

        val tensorImage = TensorImage.fromBitmap(image)
        val processedImage = imageProcessor.process(tensorImage)

        val results = objectDetector?.detect(processedImage)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime
        objectDetectorListener?.onResults(
            results,
            inferenceTime,
            processedImage.height,
            processedImage.width
        )
    }

    interface DetectorListener {
        fun onError(error: String)
        fun onResults(
            results: MutableList<Detection>?,
            inferenceTime: Long,
            imageHeight: Int,
            imageWidth: Int
        )
    }
}

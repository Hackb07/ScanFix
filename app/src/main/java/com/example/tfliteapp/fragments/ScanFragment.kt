package com.example.tfliteapp.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.tfliteapp.ObjectDetectorHelper
import com.example.tfliteapp.ObjectDetectorHelper.DetectorListener
import com.example.tfliteapp.OverlayView
import com.example.tfliteapp.R
import com.example.tfliteapp.SharedViewModel
import org.tensorflow.lite.task.vision.detector.Detection
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanFragment : Fragment(), DetectorListener {

    private lateinit var overlay: OverlayView
    private lateinit var viewFinder: PreviewView
    private lateinit var objectDetectorHelper: ObjectDetectorHelper
    private lateinit var cameraExecutor: ExecutorService
    
    private val sharedViewModel: SharedViewModel by activityViewModels()

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(context, getString(R.string.permission_denied), Toast.LENGTH_LONG).show()
            }
        }

    private var isScanning = false
    private lateinit var detectButton: com.google.android.material.button.MaterialButton
    private lateinit var statusBadge: android.widget.TextView
    
    // Audio feedback
    private var toneGenerator: ToneGenerator? = null
    private var lastBeepTime = 0L
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_scan, container, false)
        overlay = root.findViewById(R.id.overlay)
        viewFinder = root.findViewById(R.id.viewFinder)
        detectButton = root.findViewById(R.id.detect_button)
        statusBadge = root.findViewById(R.id.status_badge)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        objectDetectorHelper = ObjectDetectorHelper(
            context = requireContext(),
            objectDetectorListener = this,
            modelUri = sharedViewModel.modelUri.value
        )

        // Initialize ToneGenerator for max volume on the notification stream
        toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, ToneGenerator.MAX_VOLUME)

        // Observe threshold changes
        sharedViewModel.threshold.observe(viewLifecycleOwner) {
            objectDetectorHelper.threshold = it
            objectDetectorHelper.clearObjectDetector()
            objectDetectorHelper.setupObjectDetector()
        }

        // Observe model URI changes (user uploaded a new .tflite model)
        sharedViewModel.modelUri.observe(viewLifecycleOwner) { uri ->
            objectDetectorHelper.modelUri = uri
            objectDetectorHelper.clearObjectDetector()
            objectDetectorHelper.setupObjectDetector()
        }
        
        detectButton.setOnClickListener {
            isScanning = !isScanning
            if (isScanning) {
                detectButton.text = getString(R.string.stop_scanning)
                statusBadge.text = getString(R.string.scanning)
                detectButton.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.accent_red)
                )
            } else {
                detectButton.text = getString(R.string.start_industrial_scan)
                statusBadge.text = getString(R.string.ready_to_scan)
                detectButton.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.button_mint)
                )
                overlay.clear()
            }
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            activityResultLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            if (!isAdded) return@addListener
            
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            // ImageAnalysis
            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { image ->
                        if (!isScanning) {
                            image.close()
                            return@setAnalyzer
                        }
                        
                        // Safely create a bitmap (avoiding rowStride crash)
                        val sourceBitmap = image.toBitmap()
                        val matrix = android.graphics.Matrix().apply {
                            postRotate(image.imageInfo.rotationDegrees.toFloat())
                        }
                        val rotatedBitmap = Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.width, sourceBitmap.height, matrix, true)
                        
                        // Pass upright image
                        objectDetectorHelper.detect(rotatedBitmap, 0)
                        
                        image.close()
                    }
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner, cameraSelector, preview, imageAnalyzer
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    override fun onError(error: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResults(
        results: MutableList<Detection>?,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int
    ) {
        val safeResults = results ?: mutableListOf()
        
        // Only accept detections that intersect with the invisible scan area
        val scanArea = overlay.getScanAreaInImageCoordinates()
        val filteredResults = mutableListOf<Detection>()
        
        for (detection in safeResults) {
            val box = detection.boundingBox
            // Check if the center of the bounding box is inside the scan area
            val cx = box.centerX()
            val cy = box.centerY()
            
            if (scanArea.contains(cx.toInt(), cy.toInt())) {
                filteredResults.add(detection)
            }
        }
        
        if (filteredResults.isNotEmpty()) {
             sharedViewModel.updateDetectionStats(filteredResults)
             
             // Check if we should beep for this specific target class
             val targetClass = sharedViewModel.targetClass.value ?: ""
             val shouldBeep = if (targetClass.isEmpty()) {
                 true
             } else {
                 filteredResults.any { 
                     val label = it.categories.firstOrNull()?.label ?: ""
                     label.equals(targetClass, ignoreCase = true)
                 }
             }
             
             if (shouldBeep) {
                 // Play a loud beep to notify the user, throttled to 1 beep per second
                 val currentTime = System.currentTimeMillis()
                 if (currentTime - lastBeepTime > 1000) {
                     toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                     lastBeepTime = currentTime
                 }
             }
        }
        
        activity?.runOnUiThread {
            overlay.setResults(
                filteredResults,
                imageHeight,
                imageWidth
            )
            // Force redraw
            overlay.invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        toneGenerator?.release()
        toneGenerator = null
    }

    companion object {
        private const val TAG = "ScanFragment"
    }
}

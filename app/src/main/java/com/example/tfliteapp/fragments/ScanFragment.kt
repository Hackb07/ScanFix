package com.example.tfliteapp.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
                Toast.makeText(context, "Permission Request Denied", Toast.LENGTH_LONG).show()
            }
        }

    private var isScanning = false
    private lateinit var detectButton: com.google.android.material.button.MaterialButton
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_scan, container, false)
        overlay = root.findViewById(R.id.overlay)
        viewFinder = root.findViewById(R.id.viewFinder)
        detectButton = root.findViewById(R.id.detect_button)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        objectDetectorHelper = ObjectDetectorHelper(
            context = requireContext(),
            objectDetectorListener = this
        )

        // Observe threshold changes
        sharedViewModel.threshold.observe(viewLifecycleOwner) {
            objectDetectorHelper.threshold = it
            objectDetectorHelper.clearObjectDetector()
            objectDetectorHelper.setupObjectDetector()
        }
        
        detectButton.setOnClickListener {
            isScanning = !isScanning
            if (isScanning) {
                detectButton.text = "Stop Scanning"
                detectButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.accent_red))
            } else {
                detectButton.text = "Detect Objects"
                detectButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.button_mint))
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
                        
                        // Per instructions: "Remove the bounding box and the respected region"
                        // Interpreting this as "Removing the crop (ROI) and scanning the full image again"
                        // but also "Remove the bounding box" from the UI (handled in OverlayView).
                        
                        val bitmapBuffer = Bitmap.createBitmap(
                            image.width,
                            image.height,
                            Bitmap.Config.ARGB_8888
                        )
                        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }
                        
                        // Pass full image, no cropping
                        val rotation = image.imageInfo.rotationDegrees
                        objectDetectorHelper.detect(bitmapBuffer, rotation)
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
        if (safeResults.isNotEmpty()) {
             sharedViewModel.updateDetectionStats(safeResults)
        }
        
        activity?.runOnUiThread {
            // No offset calc needed as we aren't cropping anymore
            overlay.setResults(
                safeResults,
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
    }

    companion object {
        private const val TAG = "ScanFragment"
    }
}

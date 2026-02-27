package com.example.tfliteapp.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.tfliteapp.R
import com.example.tfliteapp.SharedViewModel
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsFragment : Fragment() {

    private val sharedViewModel: SharedViewModel by activityViewModels()

    private lateinit var modelNameText: TextView
    private lateinit var uploadModelButton: MaterialButton
    private lateinit var removeModelButton: MaterialButton

    // File picker launcher — only accepts .tflite files
    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri: Uri? = result.data?.data
                if (uri != null) {
                    val fileName = getFileNameFromUri(uri)
                    if (fileName != null) {
                        // Take persistable URI permission so we can access it later
                        try {
                            requireContext().contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (_: Exception) {
                            // Some providers don't support persistable permissions
                        }

                        sharedViewModel.setModelUri(uri)
                        modelNameText.text = fileName
                        removeModelButton.visibility = View.VISIBLE
                        Toast.makeText(context, "Model loaded: $fileName", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            context,
                            "Invalid file selected.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Threshold controls
        val seekBar = view.findViewById<SeekBar>(R.id.threshold_seekbar)
        val valueText = view.findViewById<TextView>(R.id.threshold_label)

        sharedViewModel.threshold.observe(viewLifecycleOwner) {
            val progress = (it * 100).toInt()
            seekBar.progress = progress
            valueText.text = "Confidence Threshold: ${progress}%"
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val value = progress / 100f
                    valueText.text = "Confidence Threshold: ${progress}%"
                    sharedViewModel.setThreshold(value)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Target class controls
        val targetClassInput = view.findViewById<AutoCompleteTextView>(R.id.target_class_input)
        
        // Define some common COCO dataset labels that might be in the default MobileNet model
        val commonClasses = arrayOf("person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat", "traffic light", "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog", "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella", "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard", "sports ball", "kite", "baseball bat", "baseball glove", "skateboard", "surfboard", "tennis racket", "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple", "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch", "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote", "keyboard", "cell phone", "microwave", "oven", "toaster", "sink", "refrigerator", "book", "clock", "vase", "scissors", "teddy bear", "hair drier", "toothbrush")
        
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, commonClasses)
        targetClassInput.setAdapter(adapter)

        sharedViewModel.targetClass.observe(viewLifecycleOwner) {
            val currentText = targetClassInput.text.toString()
            if (currentText != it) {
                targetClassInput.setText(it, false) // false prevents popup from showing up again when observing state
            }
        }
        
        targetClassInput.addTextChangedListener {
            sharedViewModel.setTargetClass(it.toString().trim())
        }

        // Model upload controls
        modelNameText = view.findViewById(R.id.model_name_text)
        uploadModelButton = view.findViewById(R.id.upload_model_button)
        removeModelButton = view.findViewById(R.id.remove_model_button)

        // Observe current model URI
        sharedViewModel.modelUri.observe(viewLifecycleOwner) { uri ->
            if (uri != null) {
                val name = getFileNameFromUri(uri) ?: "Custom model"
                modelNameText.text = name
                removeModelButton.visibility = View.VISIBLE
            } else {
                modelNameText.text = "Default model (MobileNet V1)"
                removeModelButton.visibility = View.GONE
            }
        }

        uploadModelButton.setOnClickListener {
            openFilePicker()
        }

        removeModelButton.setOnClickListener {
            sharedViewModel.setModelUri(null)
            Toast.makeText(context, "Switched back to default model", Toast.LENGTH_SHORT).show()
        }

        // Clear Database
        val clearDbButton = view.findViewById<android.widget.LinearLayout>(R.id.clear_database_btn)
        clearDbButton.setOnClickListener {
            sharedViewModel.clearDatabase()
            Toast.makeText(context, "Database cleared successfully", Toast.LENGTH_SHORT).show()
        }

        // Export Data via Share Intent
        val exportButton = view.findViewById<android.widget.LinearLayout>(R.id.export_data_btn)
        exportButton.setOnClickListener {
            sharedViewModel.exportDatabaseToCacheAndShare(requireContext()) { success, uri, message ->
                if (success && uri != null) {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        // This flag is what gives the target app permission to read the file provider URI
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(shareIntent, "Share Export Data"))
                } else {
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            // Filter to .tflite files where supported
            putExtra(
                Intent.EXTRA_MIME_TYPES,
                arrayOf("application/octet-stream", "*/*")
            )
        }
        filePickerLauncher.launch(intent)
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var name: String? = null
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    name = it.getString(index)
                }
            }
        }
        if (name == null) {
            // Fallback: extract from URI path
            name = uri.lastPathSegment
        }
        return name
    }
}

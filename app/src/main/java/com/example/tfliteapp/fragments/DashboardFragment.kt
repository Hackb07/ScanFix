package com.example.tfliteapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.tfliteapp.R
import com.example.tfliteapp.SharedViewModel

class DashboardFragment : Fragment() {

    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }
    

    private lateinit var totalText: TextView
    private lateinit var avgText: TextView
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        totalText = view.findViewById(R.id.total_detections_text)
        avgText = view.findViewById(R.id.avg_confidence_text)
        
        sharedViewModel.totalDetections.observe(viewLifecycleOwner) {
            totalText.text = it.toString()
        }
        
        sharedViewModel.averageConfidence.observe(viewLifecycleOwner) { avgConfidence ->
             // Update text
            val percent = (avgConfidence * 100).toInt()
            avgText.text = "$percent%"
        }
    }
}

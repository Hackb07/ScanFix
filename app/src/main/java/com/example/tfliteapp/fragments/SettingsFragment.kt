package com.example.tfliteapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.tfliteapp.R
import com.example.tfliteapp.SharedViewModel

class SettingsFragment : Fragment() {

    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
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
    }
}

package com.example.tfliteapp.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.tfliteapp.R
import com.example.tfliteapp.SharedViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.AdapterView

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
    private lateinit var barChart: BarChart
    private lateinit var pieChart: PieChart
    private lateinit var chartTypeGroup: RadioGroup
    private lateinit var filterSpinner: Spinner
    private lateinit var classCountContainer: android.widget.LinearLayout
    
    // State variables for filtering
    private var currentFilterLimit: Int = 5 // Default Top 5
    private var currentDistribution: Map<String, Int> = emptyMap()
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        totalText = view.findViewById(R.id.total_detections_text)
        avgText = view.findViewById(R.id.avg_confidence_text)
        barChart = view.findViewById(R.id.class_distribution_chart)
        pieChart = view.findViewById(R.id.class_distribution_pie_chart)
        chartTypeGroup = view.findViewById(R.id.chart_type_group)
        filterSpinner = view.findViewById(R.id.filter_spinner)
        classCountContainer = view.findViewById(R.id.class_count_list_container)
        
        setupBarChart()
        setupPieChart()
        setupControls()
        
        sharedViewModel.totalDetections.observe(viewLifecycleOwner) {
            totalText.text = it.toString()
        }
        
        sharedViewModel.averageConfidence.observe(viewLifecycleOwner) { avgConfidence ->
            val percent = (avgConfidence * 100).toInt()
            avgText.text = "$percent%"
        }

        sharedViewModel.classDistribution.observe(viewLifecycleOwner) { distribution ->
            currentDistribution = distribution
            updateClassDistributionUI()
        }
    }

    private fun setupControls() {
        chartTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.radio_bar_chart) {
                barChart.visibility = View.VISIBLE
                pieChart.visibility = View.GONE
            } else {
                barChart.visibility = View.GONE
                pieChart.visibility = View.VISIBLE
            }
        }

        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentFilterLimit = when (position) {
                    0 -> 5    // Top 5
                    1 -> 10   // Top 10
                    else -> Int.MAX_VALUE // All
                }
                updateClassDistributionUI()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupBarChart() {
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.setDrawGridBackground(false)
        barChart.setDrawBorders(false)

        // X Axis setup
        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textColor = Color.WHITE
        xAxis.textSize = 12f

        // Y Axis Setup
        val leftAxis = barChart.axisLeft
        leftAxis.textColor = Color.WHITE
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = Color.DKGRAY
        leftAxis.axisMinimum = 0f
        leftAxis.granularity = 1f
        
        barChart.axisRight.isEnabled = false
    }

    private fun setupPieChart() {
        pieChart.description.isEnabled = false
        pieChart.isDrawHoleEnabled = true
        pieChart.holeRadius = 40f
        pieChart.transparentCircleRadius = 45f
        pieChart.setHoleColor(Color.TRANSPARENT)
        pieChart.setDrawEntryLabels(false)
        
        val legend = pieChart.legend
        legend.textColor = Color.WHITE
        legend.textSize = 12f
        legend.isWordWrapEnabled = true
    }

    private fun updateClassDistributionUI() {
        if (currentDistribution.isEmpty()) {
            barChart.clear()
            barChart.setNoDataText("No detections yet")
            barChart.setNoDataTextColor(Color.GRAY)
            
            pieChart.clear()
            pieChart.setNoDataText("No detections yet")
            pieChart.setNoDataTextColor(Color.GRAY)
            
            classCountContainer.removeAllViews()
            return
        }

        // Sort and apply filter
        val sortedDistribution = currentDistribution.entries
            .sortedByDescending { it.value }
            .take(currentFilterLimit)

        updateBarChartData(sortedDistribution)
        updatePieChartData(sortedDistribution)
        updateClassCountList(sortedDistribution)
    }

    private fun updateClassCountList(sortedDistribution: List<Map.Entry<String, Int>>) {
        classCountContainer.removeAllViews()
        
        val context = requireContext()
        val paramMargin = android.widget.LinearLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 0, 0, 16)
        }
        
        for (entry in sortedDistribution) {
            val row = android.widget.LinearLayout(context).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                layoutParams = paramMargin
            }
            
            val classNameText = TextView(context).apply {
                text = entry.key
                textSize = 16f
                setTextColor(ContextCompat.getColor(context, R.color.text_white))
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    0,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }
            
            val countText = TextView(context).apply {
                text = entry.value.toString()
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(ContextCompat.getColor(context, R.color.accent_blue))
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            
            row.addView(classNameText)
            row.addView(countText)
            classCountContainer.addView(row)
        }
    }

    private fun updateBarChartData(sortedDistribution: List<Map.Entry<String, Int>>) {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        sortedDistribution.forEachIndexed { index, entry ->
            entries.add(BarEntry(index.toFloat(), entry.value.toFloat()))
            labels.add(entry.key)
        }

        val dataSet = BarDataSet(entries, "Classes")
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.accent_blue)
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 12f
        
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return value.toInt().toString()
            }
        }

        val barData = BarData(dataSet)
        barData.barWidth = 0.5f

        barChart.data = barData
        
        val xAxis = barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.labelCount = labels.size
        
        xAxis.axisMinimum = -0.5f
        xAxis.axisMaximum = labels.size - 0.5f

        barChart.animateY(500)
        barChart.invalidate()
    }
    
    private fun updatePieChartData(sortedDistribution: List<Map.Entry<String, Int>>) {
        val entries = ArrayList<PieEntry>()
        
        sortedDistribution.forEach { entry ->
            entries.add(PieEntry(entry.value.toFloat(), entry.key))
        }

        val dataSet = PieDataSet(entries, "")
        
        // Setup some nice colors
        val colors = arrayListOf(
            ContextCompat.getColor(requireContext(), R.color.accent_blue),
            ContextCompat.getColor(requireContext(), R.color.accent_orange),
            ContextCompat.getColor(requireContext(), R.color.accent_green),
            ContextCompat.getColor(requireContext(), R.color.accent_mint),
            ContextCompat.getColor(requireContext(), R.color.accent_red)
        )
        
        // Pad with standard colors if list is large
        if (entries.size > colors.size) {
            colors.addAll(com.github.mikephil.charting.utils.ColorTemplate.MATERIAL_COLORS.toList())
        }
        
        dataSet.colors = colors
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 14f
        
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return value.toInt().toString()
            }
        }

        val pieData = PieData(dataSet)
        pieChart.data = pieData
        
        pieChart.animateY(500)
        pieChart.invalidate()
    }
}
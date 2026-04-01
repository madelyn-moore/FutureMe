package com.example.futureme

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.futureme.databinding.FragmentAnalyticsBinding
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class AnalyticsFragment : Fragment() {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: TasksViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using view binding
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)

        // Get the DAO from the Room database
        val application = requireNotNull(activity).application
        val dao = TaskDatabase.getInstance(application).taskDao

        // Build the ViewModel using the factory
        val viewModelFactory = TasksViewModelFactory(dao)
        viewModel = ViewModelProvider(this, viewModelFactory)[TasksViewModel::class.java]

        // Attach ViewModel to XML for data binding
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        // Set up tap-to-expand sections
        setupExpandableSections()

        // Observe analytics data and update charts
        observeCharts()

        return binding.root
    }

    private fun setupExpandableSections() {
        // Toggle weekly delay section
        binding.weeklyHeader.setOnClickListener {
            toggleSection(binding.weeklyContent)
        }

        // Toggle task behavior donut chart section
        binding.statusHeader.setOnClickListener {
            toggleSection(binding.statusContent)
        }

        // Toggle overdue-by-tag bar chart section
        binding.tagHeader.setOnClickListener {
            toggleSection(binding.tagContent)
        }
    }

    private fun toggleSection(view: View) {
        // Show the section if hidden, hide it if visible
        view.visibility = if (view.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    private fun observeCharts() {
        // Observe weekly average delay data
        viewModel.weeklyAverageDelay.observe(viewLifecycleOwner) { values ->
            updateWeeklyLineChart(values)
        }

        // Observe procrastination behavior breakdown
        viewModel.procrastinationBreakdown.observe(viewLifecycleOwner) { breakdown ->
            updateDonutChart(breakdown)
        }

        // Observe overdue tasks grouped by tag
        viewModel.overdueByTag.observe(viewLifecycleOwner) { tagMap ->
            updateTagBarChart(tagMap)
        }
    }

    private fun updateWeeklyLineChart(values: List<Float>) {
        val labels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

        // Create chart entries from weekly values
        val entries = values.mapIndexed { index, value ->
            Entry(index.toFloat(), value)
        }

        val dataSet = LineDataSet(entries, "Average Delay by Day")

        // Purple/blue project color palette
        dataSet.color = Color.parseColor("#7E6AA2")
        dataSet.circleColors = listOf(Color.parseColor("#B8A4D4"))
        dataSet.lineWidth = 3f
        dataSet.circleRadius = 5f
        dataSet.valueTextSize = 11f
        dataSet.valueTextColor = Color.parseColor("#1D1230")
        dataSet.setDrawValues(true)

        // Fill area under the line with a soft lavender
        dataSet.setDrawFilled(true)
        dataSet.fillColor = Color.parseColor("#DCD2F0")

        val lineData = LineData(dataSet)

        binding.weeklyLineChart.data = lineData
        binding.weeklyLineChart.setBackgroundColor(Color.parseColor("#F4F1ED"))
        binding.weeklyLineChart.description.isEnabled = false
        binding.weeklyLineChart.axisRight.isEnabled = false
        binding.weeklyLineChart.setTouchEnabled(false)
        binding.weeklyLineChart.setPinchZoom(false)
        binding.weeklyLineChart.setDrawGridBackground(false)
        binding.weeklyLineChart.animateX(1000, Easing.EaseInOutQuart)

        // Style X axis
        binding.weeklyLineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        binding.weeklyLineChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        binding.weeklyLineChart.xAxis.granularity = 1f
        binding.weeklyLineChart.xAxis.setDrawGridLines(false)
        binding.weeklyLineChart.xAxis.textSize = 11f
        binding.weeklyLineChart.xAxis.textColor = Color.parseColor("#1D1230")

        // Style left axis
        binding.weeklyLineChart.axisLeft.axisMinimum = 0f
        binding.weeklyLineChart.axisLeft.textSize = 11f
        binding.weeklyLineChart.axisLeft.textColor = Color.parseColor("#1D1230")

        // Legend styling
        binding.weeklyLineChart.legend.form = Legend.LegendForm.LINE
        binding.weeklyLineChart.legend.textSize = 12f
        binding.weeklyLineChart.legend.textColor = Color.parseColor("#1D1230")

        binding.weeklyLineChart.invalidate()
    }

    private fun updateDonutChart(breakdown: Map<String, Int>) {
        val entries = breakdown.mapNotNull { (label, value) ->
            if (value > 0) PieEntry(value.toFloat(), label) else null
        }

        // If no data exists, show a message instead
        if (entries.isEmpty()) {
            binding.statusDonutChart.clear()
            binding.statusDonutChart.setNoDataText("No task data yet")
            binding.statusDonutChart.invalidate()
            return
        }

        val dataSet = PieDataSet(entries, "Task Behavior")

        // Use your purple/blue palette
        dataSet.colors = listOf(
            Color.parseColor("#B8A4D4"), // soft purple
            Color.parseColor("#7E6AA2"), // lavender
            Color.parseColor("#1D1230"), // deep plum
            Color.parseColor("#80A6BB")  // sky blue
        )

        dataSet.sliceSpace = 4f
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.WHITE

        val pieData = PieData(dataSet)

        binding.statusDonutChart.data = pieData
        binding.statusDonutChart.setBackgroundColor(Color.parseColor("#F4F1ED"))
        binding.statusDonutChart.description.isEnabled = false
        binding.statusDonutChart.setUsePercentValues(false)
        binding.statusDonutChart.isDrawHoleEnabled = true
        binding.statusDonutChart.holeRadius = 55f
        binding.statusDonutChart.transparentCircleRadius = 60f
        binding.statusDonutChart.setHoleColor(Color.parseColor("#F4F1ED"))
        binding.statusDonutChart.centerText = "Task\nBehavior"
        binding.statusDonutChart.setCenterTextSize(16f)
        binding.statusDonutChart.setCenterTextColor(Color.parseColor("#1D1230"))
        binding.statusDonutChart.setEntryLabelColor(Color.parseColor("#1D1230"))
        binding.statusDonutChart.setEntryLabelTextSize(11f)
        binding.statusDonutChart.legend.textSize = 12f
        binding.statusDonutChart.legend.textColor = Color.parseColor("#1D1230")
        binding.statusDonutChart.animateY(1000, Easing.EaseInOutQuad)

        binding.statusDonutChart.invalidate()
    }

    private fun updateTagBarChart(tagMap: Map<String, Int>) {
        // If there is no data, show message
        if (tagMap.isEmpty()) {
            binding.tagBarChart.clear()
            binding.tagBarChart.setNoDataText("No overdue tag data yet")
            binding.tagBarChart.invalidate()
            return
        }

        val labels = tagMap.keys.toList()

        // Build entries for each tag
        val entries = labels.mapIndexed { index, label ->
            BarEntry(index.toFloat(), (tagMap[label] ?: 0).toFloat())
        }

        val dataSet = BarDataSet(entries, "Overdue Tasks by Tag")

        // Purple/blue mixed color palette
        dataSet.colors = listOf(
            Color.parseColor("#7E6AA2"),
            Color.parseColor("#B8A4D4"),
            Color.parseColor("#80A6BB"),
            Color.parseColor("#AEC9D2"),
            Color.parseColor("#DCD2F0"),
            Color.parseColor("#1D1230")
        )

        dataSet.valueTextSize = 11f
        dataSet.valueTextColor = Color.parseColor("#1D1230")

        val data = BarData(dataSet)
        data.barWidth = 0.55f

        binding.tagBarChart.data = data
        binding.tagBarChart.setBackgroundColor(Color.parseColor("#F4F1ED"))
        binding.tagBarChart.description.isEnabled = false
        binding.tagBarChart.axisRight.isEnabled = false
        binding.tagBarChart.setTouchEnabled(false)
        binding.tagBarChart.setPinchZoom(false)
        binding.tagBarChart.setFitBars(true)
        binding.tagBarChart.animateY(1000)

        // Style X axis
        binding.tagBarChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        binding.tagBarChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        binding.tagBarChart.xAxis.granularity = 1f
        binding.tagBarChart.xAxis.labelRotationAngle = -20f
        binding.tagBarChart.xAxis.setDrawGridLines(false)
        binding.tagBarChart.xAxis.textSize = 11f
        binding.tagBarChart.xAxis.textColor = Color.parseColor("#1D1230")

        // Style left axis
        binding.tagBarChart.axisLeft.axisMinimum = 0f
        binding.tagBarChart.axisLeft.textSize = 11f
        binding.tagBarChart.axisLeft.textColor = Color.parseColor("#1D1230")

        binding.tagBarChart.legend.textSize = 12f
        binding.tagBarChart.legend.textColor = Color.parseColor("#1D1230")

        binding.tagBarChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
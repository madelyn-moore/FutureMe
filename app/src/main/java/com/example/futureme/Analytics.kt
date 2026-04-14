package com.example.futureme

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)

        val application = requireNotNull(activity).application
        val database = TaskDatabase.getInstance(application)
        val viewModelFactory = TasksViewModelFactory(database.taskDao, database.tagDao)
        viewModel = ViewModelProvider(this, viewModelFactory)[TasksViewModel::class.java]

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        setupExpandableSections()
        observeCharts()

        return binding.root
    }

    private fun setupExpandableSections() {
        binding.weeklyHeader.setOnClickListener {
            toggleSection(binding.weeklyContent)
        }

        binding.statusHeader.setOnClickListener {
            toggleSection(binding.statusContent)
        }

        binding.tagHeader.setOnClickListener {
            toggleSection(binding.tagContent)
        }
    }

    private fun toggleSection(view: View) {
        view.visibility = if (view.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    private fun observeCharts() {
        viewModel.weeklyAverageDelay.observe(viewLifecycleOwner) { values ->
            updateWeeklyLineChart(values)
        }

        viewModel.procrastinationBreakdown.observe(viewLifecycleOwner) { breakdown ->
            updateDonutChart(breakdown)
        }

        viewModel.overdueByTag.observe(viewLifecycleOwner) { tagMap ->
            updateTagBarChart(tagMap)
        }
    }

    private fun updateWeeklyLineChart(values: List<Float>) {
        val textColor = ContextCompat.getColor(requireContext(), R.color.text_dark)
        val cardColor = Color.parseColor("#F4F1ED")

        if (values.isEmpty() || values.all { it == 0f }) {
            binding.weeklyLineChart.clear()
            binding.weeklyLineChart.setNoDataText("No weekly delay data yet")
            binding.weeklyLineChart.setNoDataTextColor(textColor)
            binding.weeklyLineChart.setBackgroundColor(cardColor)
            binding.weeklyLineChart.invalidate()
            return
        }

        val labels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val entries = values.mapIndexed { index, value ->
            Entry(index.toFloat(), value)
        }

        val dataSet = LineDataSet(entries, "Average Delay by Day")
        dataSet.color = Color.parseColor("#7E6AA2")
        dataSet.circleColors = listOf(Color.parseColor("#B8A4D4"))
        dataSet.lineWidth = 3f
        dataSet.circleRadius = 5f
        dataSet.valueTextSize = 11f
        dataSet.valueTextColor = textColor
        dataSet.setDrawValues(true)
        dataSet.setDrawFilled(true)
        dataSet.fillColor = Color.parseColor("#DCD2F0")

        val lineData = LineData(dataSet)

        binding.weeklyLineChart.data = lineData
        binding.weeklyLineChart.setBackgroundColor(cardColor)
        binding.weeklyLineChart.description.isEnabled = false
        binding.weeklyLineChart.axisRight.isEnabled = false
        binding.weeklyLineChart.setTouchEnabled(false)
        binding.weeklyLineChart.setPinchZoom(false)
        binding.weeklyLineChart.setDrawGridBackground(false)
        binding.weeklyLineChart.legend.form = Legend.LegendForm.LINE
        binding.weeklyLineChart.legend.textSize = 12f
        binding.weeklyLineChart.legend.textColor = textColor
        binding.weeklyLineChart.animateX(1000, Easing.EaseInOutQuart)

        binding.weeklyLineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        binding.weeklyLineChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        binding.weeklyLineChart.xAxis.granularity = 1f
        binding.weeklyLineChart.xAxis.setDrawGridLines(false)
        binding.weeklyLineChart.xAxis.textSize = 11f
        binding.weeklyLineChart.xAxis.textColor = textColor

        binding.weeklyLineChart.axisLeft.axisMinimum = 0f
        binding.weeklyLineChart.axisLeft.textSize = 11f
        binding.weeklyLineChart.axisLeft.textColor = textColor

        binding.weeklyLineChart.invalidate()
    }

    private fun updateDonutChart(breakdown: Map<String, Int>) {
        val textColor = ContextCompat.getColor(requireContext(), R.color.text_dark)
        val cardColor = Color.parseColor("#F4F1ED")

        val entries = breakdown.mapNotNull { (label, value) ->
            if (value > 0) PieEntry(value.toFloat(), label) else null
        }

        if (entries.isEmpty()) {
            binding.statusDonutChart.clear()
            binding.statusDonutChart.setNoDataText("No task data yet")
            binding.statusDonutChart.setNoDataTextColor(textColor)
            binding.statusDonutChart.setBackgroundColor(cardColor)
            binding.statusDonutChart.invalidate()
            return
        }

        val dataSet = PieDataSet(entries, "Task Behavior")
        dataSet.colors = listOf(
            Color.parseColor("#B8A4D4"),
            Color.parseColor("#7E6AA2"),
            Color.parseColor("#1D1230"),
            Color.parseColor("#80A6BB")
        )
        dataSet.sliceSpace = 4f
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.WHITE

        val pieData = PieData(dataSet)

        binding.statusDonutChart.data = pieData
        binding.statusDonutChart.setBackgroundColor(cardColor)
        binding.statusDonutChart.description.isEnabled = false
        binding.statusDonutChart.setUsePercentValues(false)
        binding.statusDonutChart.isDrawHoleEnabled = true
        binding.statusDonutChart.holeRadius = 55f
        binding.statusDonutChart.transparentCircleRadius = 60f
        binding.statusDonutChart.setHoleColor(cardColor)
        binding.statusDonutChart.centerText = "Task\nBehavior"
        binding.statusDonutChart.setCenterTextSize(16f)
        binding.statusDonutChart.setCenterTextColor(textColor)
        binding.statusDonutChart.setEntryLabelColor(textColor)
        binding.statusDonutChart.setEntryLabelTextSize(11f)
        binding.statusDonutChart.legend.textSize = 12f
        binding.statusDonutChart.legend.textColor = textColor
        binding.statusDonutChart.animateY(1000, Easing.EaseInOutQuad)

        binding.statusDonutChart.invalidate()
    }

    private fun updateTagBarChart(tagMap: Map<String, Int>) {
        val textColor = ContextCompat.getColor(requireContext(), R.color.text_dark)
        val cardColor = Color.parseColor("#F4F1ED")

        if (tagMap.isEmpty()) {
            binding.tagBarChart.clear()
            binding.tagBarChart.setNoDataText("No overdue tag data yet")
            binding.tagBarChart.setNoDataTextColor(textColor)
            binding.tagBarChart.setBackgroundColor(cardColor)
            binding.tagBarChart.invalidate()
            return
        }

        val labels = tagMap.keys.toList()
        val entries = labels.mapIndexed { index, label ->
            BarEntry(index.toFloat(), (tagMap[label] ?: 0).toFloat())
        }

        val dataSet = BarDataSet(entries, "Overdue Tasks by Tag")
        dataSet.colors = listOf(
            Color.parseColor("#7E6AA2"),
            Color.parseColor("#B8A4D4"),
            Color.parseColor("#80A6BB"),
            Color.parseColor("#AEC9D2"),
            Color.parseColor("#DCD2F0"),
            Color.parseColor("#1D1230")
        )
        dataSet.valueTextSize = 11f
        dataSet.valueTextColor = textColor

        val data = BarData(dataSet)
        data.barWidth = 0.55f

        binding.tagBarChart.data = data
        binding.tagBarChart.setBackgroundColor(cardColor)
        binding.tagBarChart.description.isEnabled = false
        binding.tagBarChart.axisRight.isEnabled = false
        binding.tagBarChart.setTouchEnabled(false)
        binding.tagBarChart.setPinchZoom(false)
        binding.tagBarChart.setFitBars(true)
        binding.tagBarChart.animateY(1000)

        binding.tagBarChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        binding.tagBarChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        binding.tagBarChart.xAxis.granularity = 1f
        binding.tagBarChart.xAxis.labelRotationAngle = -20f
        binding.tagBarChart.xAxis.setDrawGridLines(false)
        binding.tagBarChart.xAxis.textSize = 11f
        binding.tagBarChart.xAxis.textColor = textColor

        binding.tagBarChart.axisLeft.axisMinimum = 0f
        binding.tagBarChart.axisLeft.textSize = 11f
        binding.tagBarChart.axisLeft.textColor = textColor

        binding.tagBarChart.legend.textSize = 12f
        binding.tagBarChart.legend.textColor = textColor

        binding.tagBarChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
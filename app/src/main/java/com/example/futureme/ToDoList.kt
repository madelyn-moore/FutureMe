package com.example.futureme

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.futureme.databinding.FragmentToDoListBinding

class ToDoListFragment : Fragment() {

    private var _binding: FragmentToDoListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: TasksViewModel
    private lateinit var adapter: TaskAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentToDoListBinding.inflate(inflater, container, false)

        val application = requireNotNull(activity).application
        val dao = TaskDatabase.getInstance(application).taskDao
        val factory = TasksViewModelFactory(dao)
        viewModel = ViewModelProvider(this, factory)[TasksViewModel::class.java]

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        setupRecyclerView()
        setupSearch()
        setupFilters()

        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = TaskAdapter()
        binding.taskRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.taskRecyclerView.adapter = adapter

        viewModel.filteredTasks.observe(viewLifecycleOwner) { tasks ->
            adapter.submitList(tasks)
        }
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.updateSearchQuery(s?.toString() ?: "")
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })
    }

    private fun setupFilters() {
        val statusOptions = listOf("All", "Overdue", "Completed", "Incomplete")
        val sortOptions = listOf("Due Date ↑", "Due Date ↓", "Name A-Z", "Tag A-Z")

        val statusAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            statusOptions
        )
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.statusSpinner.adapter = statusAdapter

        val sortAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            sortOptions
        )
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.sortSpinner.adapter = sortAdapter

        binding.statusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val filter = when (position) {
                    1 -> TaskStatusFilter.OVERDUE
                    2 -> TaskStatusFilter.COMPLETED
                    3 -> TaskStatusFilter.INCOMPLETE
                    else -> TaskStatusFilter.ALL
                }
                viewModel.updateStatusFilter(filter)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        binding.sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val sort = when (position) {
                    1 -> TaskSortOption.DUE_DATE_DESC
                    2 -> TaskSortOption.NAME_AZ
                    3 -> TaskSortOption.TAG_AZ
                    else -> TaskSortOption.DUE_DATE_ASC
                }
                viewModel.updateSortOption(sort)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
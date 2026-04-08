package com.example.futureme

import android.R
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.futureme.databinding.FragmentAddEditTaskBinding

class AddEditTaskFragment : Fragment() {

    private var _binding: FragmentAddEditTaskBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: TasksViewModel
    private var editingTask: Task? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditTaskBinding.inflate(inflater, container, false)

        val application = requireNotNull(activity).application
        val database = TaskDatabase.getInstance(application)
        val dao = TaskDatabase.getInstance(application).taskDao
        val tagDao = TaskDatabase.getInstance(application).tagDao
        val viewModelFactory = TasksViewModelFactory(database.taskDao, database.tagDao)
        viewModel = ViewModelProvider(this, viewModelFactory)[TasksViewModel::class.java]

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        setupDateRecalculation()
        loadTaskFromArgsIfNeeded()
        setupButtons()
        setUpSpinner()


        return binding.root
    }

    private fun setupDateRecalculation() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.recalculateTaskStats()
            }

            override fun afterTextChanged(s: Editable?) {}
        }

        binding.editNewDueDate.addTextChangedListener(watcher)
        binding.dateCompleted.addTextChangedListener(watcher)
        binding.datePostponed.addTextChangedListener(watcher)
    }

    private fun loadTaskFromArgsIfNeeded() {
        val args = arguments ?: return
        val taskId = args.getLong("taskId", -1L)
        if (taskId == -1L) return

        viewModel.allTasks.observe(viewLifecycleOwner) { tasks ->
            val task = tasks.firstOrNull { it.taskId == taskId } ?: return@observe
            if (editingTask == null) {
                editingTask = task
                viewModel.loadTask(task)
            }
        }
    }

    private fun setupButtons() {
        binding.saveButton.setOnClickListener {
            val dueDate = viewModel.newTaskDueDate.value?.trim().orEmpty()
            val dateCompleted = viewModel.newTaskDateCompleted.value?.trim().orEmpty()
            val datePostponed = viewModel.newTaskDatePostponed.value?.trim().orEmpty()

            if (dueDate.isBlank() || !isValidOrBlankDate(dueDate)) {
                Toast.makeText(requireContext(), "Enter due date as MM/dd/yyyy", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            if (!isValidOrBlankDate(dateCompleted)) {
                Toast.makeText(
                    requireContext(),
                    "Enter completed date as MM/dd/yyyy",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (!isValidOrBlankDate(datePostponed)) {
                Toast.makeText(
                    requireContext(),
                    "Enter postponed date as MM/dd/yyyy",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (editingTask == null) {
                viewModel.addTask()
                Toast.makeText(requireContext(), "Task added", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.updateTask()
                Toast.makeText(requireContext(), "Task updated", Toast.LENGTH_SHORT).show()
            }

            findNavController().navigateUp()
        }

        binding.deleteButton.setOnClickListener {
            val task = editingTask
            if (task != null) {
                viewModel.deleteTask(task)
                Toast.makeText(requireContext(), "Task deleted", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } else {
                Toast.makeText(requireContext(), "No task to delete", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isValidOrBlankDate(date: String): Boolean {
        if (date.isBlank()) return true
        val regex = Regex("""^(0?[1-9]|1[0-2])/(0?[1-9]|[12]\d|3[01])/\d{4}$""")
        return regex.matches(date)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // set up spinner
    private fun setUpSpinner() {
        viewModel.allTags.observe(viewLifecycleOwner)
        { tags ->
            val adapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_item, tags)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.tagSpinner.adapter = adapter
        }
        binding.tagSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedTag = parent?.getItemAtPosition(position) as? Tag
                viewModel.selectedTag.value = selectedTag
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                viewModel.selectedTag.value = null
            }
        }
    }
}
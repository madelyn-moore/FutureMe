package com.example.futureme

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.futureme.databinding.FragmentAddEditTaskBinding
import kotlinx.coroutines.launch

class AddEditTaskFragment : Fragment() {

    companion object {
        private const val NO_TAG_OPTION = "No Tag"
    }

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
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                syncFieldsToViewModel()
                viewModel.recalculateTaskStats()
            }

            override fun afterTextChanged(s: Editable?) {
            }
        }

        binding.addEditTaskName.addTextChangedListener(watcher)
        binding.editNewDueDate.addTextChangedListener(watcher)
        binding.dateCompleted.addTextChangedListener(watcher)
        binding.datePostponed.addTextChangedListener(watcher)
        binding.addEditDescription.addTextChangedListener(watcher)
        binding.addEditWhyTaskPushedOff.addTextChangedListener(watcher)
        binding.addEditPenalties.addTextChangedListener(watcher)
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
            syncFieldsToViewModel()

            if (!validateTaskForm()) return@setOnClickListener

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
            val taskId = arguments?.getLong("taskId", -1L) ?: -1L

            // New task has not been saved yet, so there is nothing in the DB to delete.
            if (taskId <= 0L) {
                Toast.makeText(requireContext(), "No task to delete", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val application = requireNotNull(activity).application
            val database = TaskDatabase.getInstance(application)

            viewLifecycleOwner.lifecycleScope.launch {
                val task = database.taskDao.getTaskById(taskId)

                if (task != null) {
                    viewModel.deleteTask(task)
                    showDeletedDialog()
                } else {
                    Toast.makeText(requireContext(), "No task to delete", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun syncFieldsToViewModel() {
        viewModel.newTaskName.value = binding.addEditTaskName.text?.toString()?.trim().orEmpty()
        viewModel.newTaskDueDate.value = binding.editNewDueDate.text?.toString()?.trim().orEmpty()
        viewModel.newTaskDateCompleted.value = binding.dateCompleted.text?.toString()?.trim().orEmpty()
        viewModel.newTaskDatePostponed.value = binding.datePostponed.text?.toString()?.trim().orEmpty()
        viewModel.newTaskDescription.value = binding.addEditDescription.text?.toString()?.trim().orEmpty()
        viewModel.newWhyTaskPushedOff.value = binding.addEditWhyTaskPushedOff.text?.toString()?.trim().orEmpty()
        viewModel.newTaskPenalties.value = binding.addEditPenalties.text?.toString()?.trim().orEmpty()
    }

    private fun validateTaskForm(): Boolean {
        val taskName = binding.addEditTaskName.text?.toString()?.trim().orEmpty()
        val dueDate = binding.editNewDueDate.text?.toString()?.trim().orEmpty()
        val dateCompleted = binding.dateCompleted.text?.toString()?.trim().orEmpty()
        val datePostponed = binding.datePostponed.text?.toString()?.trim().orEmpty()

        if (taskName.isBlank()) {
            Toast.makeText(requireContext(), "Enter a task name", Toast.LENGTH_SHORT).show()
            return false
        }

        if (dueDate.isBlank()) {
            Toast.makeText(requireContext(), "Enter due date as MM/dd/yyyy", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!isValidOrBlankDate(dueDate)) {
            Toast.makeText(requireContext(), "Enter due date as MM/dd/yyyy", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!isValidOrBlankDate(dateCompleted)) {
            Toast.makeText(requireContext(), "Enter completed date as MM/dd/yyyy", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!isValidOrBlankDate(datePostponed)) {
            Toast.makeText(requireContext(), "Enter postponed date as MM/dd/yyyy", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun showDeletedDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_task_delete, null)

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("OK") { _, _ ->
                Toast.makeText(requireContext(), "Task deleted", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
            .show()
    }

    private fun isValidOrBlankDate(date: String): Boolean {
        if (date.isBlank()) return true
        val regex = Regex("""^(0?[1-9]|1[0-2])/(0?[1-9]|[12]\d|3[01])/\d{4}$""")
        return regex.matches(date)
    }

    private fun setUpSpinner() {
        viewModel.allTags.observe(viewLifecycleOwner) { tags ->
            val spinnerItems = mutableListOf(Tag(NO_TAG_OPTION))
            spinnerItems.addAll(tags.filterNot { it.tagName == NO_TAG_OPTION })

            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                spinnerItems
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.tagSpinner.adapter = adapter

            val currentTagName = editingTask?.tags.orEmpty()
            val selectedIndex = if (currentTagName.isBlank()) {
                0
            } else {
                spinnerItems.indexOfFirst { it.tagName == currentTagName }.coerceAtLeast(0)
            }

            binding.tagSpinner.setSelection(selectedIndex, false)
        }

        binding.tagSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedTag = parent?.getItemAtPosition(position) as? Tag
                viewModel.selectedTag.value =
                    if (selectedTag?.tagName == NO_TAG_OPTION) null else selectedTag
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                viewModel.selectedTag.value = null
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
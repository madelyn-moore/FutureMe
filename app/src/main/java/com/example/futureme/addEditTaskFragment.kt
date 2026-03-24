package com.example.futureme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.futureme.databinding.FragmentAddEditTaskBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

class AddEditTaskFragment : Fragment() {

    private var _binding: FragmentAddEditTaskBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: TasksViewModel
    private var currentTaskId: Long = 0L

    // Strict formatter so invalid dates like 02/30/2026 are rejected
    private val dateFormatter = DateTimeFormatter.ofPattern("MM/dd/uuuu")
        .withResolverStyle(ResolverStyle.STRICT)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditTaskBinding.inflate(inflater, container, false)

        val application = requireNotNull(activity).application
        val dao = TaskDatabase.getInstance(application).taskDao
        val viewModelFactory = TasksViewModelFactory(dao)
        viewModel = ViewModelProvider(this, viewModelFactory)[TasksViewModel::class.java]

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        currentTaskId = arguments?.getLong("taskId", 0L) ?: 0L

        if (currentTaskId != 0L) {
            viewModel.loadTask(currentTaskId)
        }

        binding.addEditNewDueDate.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateSingleDateField(binding.addEditNewDueDate, required = false)
                viewModel.recalculateTaskStats()
            }
        }

        binding.datePostponed.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateSingleDateField(binding.datePostponed, required = false)
                viewModel.recalculateTaskStats()
            }
        }

        binding.dateCompleted.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateSingleDateField(binding.dateCompleted, required = false)
            }
        }

        binding.saveButton.setOnClickListener {
            saveTask()
        }

        binding.deleteButton.setOnClickListener {
            deleteCurrentTask()
        }

        return binding.root
    }

    private fun saveTask() {
        val taskName = binding.addEditTaskName.text.toString().trim()

        if (taskName.isEmpty()) {
            binding.addEditTaskName.error = "Please enter a task name"
            return
        }

        val dueDateValid = validateSingleDateField(binding.addEditNewDueDate, required = false)
        val postponedDateValid = validateSingleDateField(binding.datePostponed, required = false)
        val completedDateValid = validateSingleDateField(binding.dateCompleted, required = false)

        if (!dueDateValid || !postponedDateValid || !completedDateValid) {
            Toast.makeText(
                requireContext(),
                "Please use MM/dd/yyyy for all date fields",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        viewModel.recalculateTaskStats()

        if (currentTaskId == 0L) {
            viewModel.addTask()
            Toast.makeText(requireContext(), "Task saved", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.updateTask()
            Toast.makeText(requireContext(), "Task updated", Toast.LENGTH_SHORT).show()
        }

        findNavController().navigateUp()
    }

    private fun deleteCurrentTask() {
        if (currentTaskId == 0L) {
            Toast.makeText(requireContext(), "No saved task to delete", Toast.LENGTH_SHORT).show()
            return
        }

        val taskToDelete = Task(
            taskId = currentTaskId,
            name = binding.addEditTaskName.text.toString().trim(),
            tags = binding.addEditTags.text.toString().trim(),
            dueDate = binding.addEditNewDueDate.text.toString().trim(),
            datePostponed = binding.datePostponed.text.toString().trim(),
            description = binding.addEditDescription.text.toString().trim(),
            whyTaskPushedOff = binding.addEditWhyTaskPushedOff.text.toString().trim(),
            penalties = binding.addEditPenalties.text.toString().trim(),
            numDaysDelayed = binding.numDaysDelayed.text.toString().trim(),
            overdueStatus = binding.overdueStatus.isChecked,
            dateCompleted = binding.dateCompleted.text.toString().trim()
        )

        viewModel.deleteTask(taskToDelete)
        Toast.makeText(requireContext(), "Task deleted", Toast.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }

    // Validates one EditText as a date in MM/dd/yyyy format
    // If field is blank and not required, it is considered valid
    private fun validateSingleDateField(
        editText: android.widget.EditText,
        required: Boolean
    ): Boolean {
        val value = editText.text.toString().trim()

        if (value.isEmpty()) {
            if (required) {
                editText.error = "Date is required in MM/dd/yyyy format"
                return false
            }
            editText.error = null
            return true
        }

        return if (isValidDate(value)) {
            editText.error = null
            true
        } else {
            editText.error = "Use MM/dd/yyyy"
            false
        }
    }

    // Returns true only if the date is real and matches MM/dd/yyyy
    private fun isValidDate(value: String): Boolean {
        return try {
            LocalDate.parse(value, dateFormatter)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
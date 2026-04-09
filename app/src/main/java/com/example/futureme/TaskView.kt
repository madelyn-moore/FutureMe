package com.example.futureme

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.futureme.databinding.FragmentTaskViewBinding

// TaskViewFragment
class TaskViewFragment : Fragment() {
    // variables
    private var _binding: FragmentTaskViewBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: TasksViewModel
    private var currentTaskId: Long = -1L

    private var taskView: Task? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskViewBinding.inflate(inflater, container, false)

        val application = requireNotNull(activity).application
        val database = TaskDatabase.getInstance(application)
        val viewModelFactory = TasksViewModelFactory(database.taskDao, database.tagDao)
        viewModel = ViewModelProvider(this, viewModelFactory)[TasksViewModel::class.java]

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        // Get taskId from navigation arguments
        currentTaskId = arguments?.getLong("taskId", -1L) ?: -1L

        if (currentTaskId != -1L) {

            database.taskDao.get(currentTaskId).observe(viewLifecycleOwner) { task ->
                task?.let {
                    viewModel.loadTask(it)
                }
            }
        }

        setupButtons()
        loadTaskFromArgsIfNeeded()

        return binding.root
    }

    // setup buttons
    private fun setupButtons() {
        binding.editButton.setOnClickListener {
            if (currentTaskId != -1L) {
                val bundle = bundleOf("taskId" to currentTaskId)
                findNavController().navigate(R.id.action_taskView_to_addEditTask, bundle)
            }
        }

        binding.deleteButton.setOnClickListener {
            val database = TaskDatabase.getInstance(requireContext())
            database.taskDao.get(currentTaskId).observe(viewLifecycleOwner) { task ->
                if (task != null) {
                    viewModel.deleteTask(task)
                    Toast.makeText(requireContext(), "Task deleted", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                } else {
                    Toast.makeText(requireContext(), "No task to delete", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // load task from args if needed
    private fun loadTaskFromArgsIfNeeded() {
        val args = arguments ?: return
        val taskId = args.getLong("taskId", -1L)
        if (taskId == -1L) return

        viewModel.allTasks.observe(viewLifecycleOwner) { tasks ->
            val task = tasks.firstOrNull { it.taskId == taskId } ?: return@observe
            if (taskView == null) {
                taskView = task
                viewModel.loadTask(task)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.example.futureme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
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
        val viewModelFactory = TasksViewModelFactory(dao)
        viewModel = ViewModelProvider(this, viewModelFactory)[TasksViewModel::class.java]

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        adapter = TaskAdapter(

            onEditClick = { selectedTask ->
                val bundle = Bundle().apply {
                    putLong("taskId", selectedTask.taskId)
                }
                findNavController().navigate(R.id.addEditTask, bundle)
            },

            onDeleteClick = { selectedTask ->
                viewModel.deleteTask(selectedTask)
            }
        )

        binding.taskRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.taskRecyclerView.adapter = adapter

        viewModel.tasks.observe(viewLifecycleOwner) { taskList ->
            adapter.submitList(taskList)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
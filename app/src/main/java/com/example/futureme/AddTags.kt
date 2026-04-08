package com.example.futureme

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.futureme.databinding.FragmentAddTagsBinding

class AddTags : Fragment() {

    private var _binding: FragmentAddTagsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: TasksViewModel
    private lateinit var adapter: TagsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTagsBinding.inflate(inflater, container, false)

        val application = requireNotNull(activity).application
        val database = TaskDatabase.getInstance(application)
        val factory = TasksViewModelFactory(database.taskDao, database.tagDao)
        viewModel = ViewModelProvider(this, factory)[TasksViewModel::class.java]

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        setUpTagRecyclerView()

        return binding.root
    }

    private fun setUpTagRecyclerView() {
        adapter = TagsAdapter { tagToDelete ->
            viewModel.deleteTag(tagToDelete)
        }
        binding.tagsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.tagsRecyclerView.adapter = adapter

        viewModel.allTags.observe(viewLifecycleOwner) { tags ->
            adapter.submitList(tags)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

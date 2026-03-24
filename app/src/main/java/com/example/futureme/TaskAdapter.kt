package com.example.futureme

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.futureme.databinding.ItemTaskBinding

// Adapter for showing tasks in a RecyclerView
class TaskAdapter(
    private val onEditClick: (Task) -> Unit,
    private val onDeleteClick: (Task) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    // ViewHolder holds one row/item of the RecyclerView
    class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            task: Task,
            onEditClick: (Task) -> Unit,
            onDeleteClick: (Task) -> Unit
        ) {
            binding.taskTitle.text = task.name
            binding.taskTag.text = "Tag: ${task.tags}"
            binding.taskDueDate.text = "Due: ${task.dueDate}"

            binding.editButton.setOnClickListener {
                onEditClick(task)
            }

            binding.deleteButton.setOnClickListener {
                onDeleteClick(task)
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val currentTask = getItem(position)
        holder.bind(currentTask, onEditClick, onDeleteClick)
    }
}

// Helps RecyclerView know what changed
class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
    override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
        return oldItem.taskId == newItem.taskId
    }

    override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
        return oldItem == newItem
    }
}
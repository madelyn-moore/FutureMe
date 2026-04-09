package com.example.futureme

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.futureme.databinding.ItemTaskBinding

// Takes the val on task clicked and takes a long aka task id and returns a unit aka nothing
class TaskAdapter(private val onTaskClicked: (Long) -> Unit, private val onTaskChecked: (Task, Boolean) -> Unit) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    // ViewHolder for tasks
    class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task, onTaskClicked: (Long) -> Unit, onTaskChecked: (Task, Boolean) -> Unit) {
            binding.taskNameText.text = task.name
            binding.taskTagText.text = if (task.tags.isNullOrBlank()) "Tag: None" else "Tag: ${task.tags}"
            binding.taskDueDateText.text = "Due: ${task.dueDate}"

            // set the checkboxes up
            binding.taskCheckBox.setOnCheckedChangeListener(null)
            binding.taskCheckBox.isChecked = task.isCompleted

            binding.taskCheckBox.setOnCheckedChangeListener { _, isChecked ->
                onTaskChecked(task, isChecked)
            }

            
            // Set the click listener on the entire card on the to do list
            binding.root.setOnClickListener {
                onTaskClicked(task.taskId)
            }
        }
    }

    // Create view holder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TaskViewHolder(binding)
    }

    // Bind view holder
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position), onTaskClicked, onTaskChecked)
    }
}

// Diff callback for tasks
class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
    override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
        return oldItem.taskId == newItem.taskId
    }

    override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
        return oldItem == newItem
    }
}

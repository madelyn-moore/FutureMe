package com.example.futureme

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.futureme.databinding.ItemTagsBinding

// adapter for tags
// The on delete click is a lambda that takes a tag and returns nothing
class TagsAdapter(private val onDeleteClick: (Tag) -> Unit) : ListAdapter<Tag, TagsAdapter.TagsViewHolder>(TagsDiffCallback()) {

    // ViewHolder for tags
    class TagsViewHolder(private val binding: ItemTagsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(tag: Tag, onDeleteClick: (Tag) -> Unit) {
            binding.tagNameText.text = tag.tagName
            binding.tagsDeleteButton.setOnClickListener {
                onDeleteClick(tag)
            }
        }
    }

    // Create view holder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagsViewHolder {
        val binding = ItemTagsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TagsViewHolder(binding)
    }

    // Bind view holder
    override fun onBindViewHolder(holder: TagsViewHolder, position: Int) {
        holder.bind(getItem(position), onDeleteClick)
    }
}

// Diff callback for tags
class TagsDiffCallback : DiffUtil.ItemCallback<Tag>() {
    override fun areItemsTheSame(oldItem: Tag, newItem: Tag): Boolean {
        return oldItem.tagName == newItem.tagName
    }

    override fun areContentsTheSame(oldItem: Tag, newItem: Tag): Boolean {
        return oldItem == newItem
    }
}

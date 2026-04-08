package com.example.futureme

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.futureme.databinding.ItemTagsBinding

class TagsAdapter(private val onDeleteClick: (Tag) -> Unit) : ListAdapter<Tag, TagsAdapter.TagsViewHolder>(TagsDiffCallback()) {

    class TagsViewHolder(private val binding: ItemTagsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(tag: Tag, onDeleteClick: (Tag) -> Unit) {
            binding.tagNameText.text = tag.tagName
            binding.tagsDeleteButton.setOnClickListener {
                onDeleteClick(tag)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagsViewHolder {
        val binding = ItemTagsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TagsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TagsViewHolder, position: Int) {
        holder.bind(getItem(position), onDeleteClick)
    }
}

class TagsDiffCallback : DiffUtil.ItemCallback<Tag>() {
    override fun areItemsTheSame(oldItem: Tag, newItem: Tag): Boolean {
        return oldItem.tagName == newItem.tagName
    }

    override fun areContentsTheSame(oldItem: Tag, newItem: Tag): Boolean {
        return oldItem == newItem
    }
}

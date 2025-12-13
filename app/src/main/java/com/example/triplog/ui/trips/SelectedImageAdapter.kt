package com.example.triplog.ui.trips

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.triplog.R

class SelectedImageAdapter(
    private val onRemoveClick: (String) -> Unit,
    private val onAddClick: () -> Unit
) : ListAdapter<String, RecyclerView.ViewHolder>(ImagePathDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_ADD = 0
        private const val VIEW_TYPE_IMAGE = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_ADD else VIEW_TYPE_IMAGE
    }

    override fun getItemCount(): Int {
        return super.getItemCount() + 1 // +1 dla placeholdera "Dodaj"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_ADD -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_add_image_placeholder, parent, false)
                AddImageViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_selected_image, parent, false)
                SelectedImageViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is AddImageViewHolder -> holder.bind()
            is SelectedImageViewHolder -> holder.bind(getItem(position - 1)) // -1 bo pierwszy to placeholder
        }
    }

    inner class AddImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind() {
            itemView.setOnClickListener {
                onAddClick()
            }
        }
    }

    inner class SelectedImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageViewSelected)
        private val buttonRemove: ImageButton = itemView.findViewById(R.id.buttonRemoveImage)

        fun bind(imagePath: String) {
            imageView.setImageURI(Uri.parse(imagePath))
            buttonRemove.setOnClickListener {
                onRemoveClick(imagePath)
            }
        }
    }

    class ImagePathDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}


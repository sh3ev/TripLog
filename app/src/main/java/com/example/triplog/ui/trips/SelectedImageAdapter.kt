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
    private val onRemoveClick: (String) -> Unit
) : ListAdapter<String, SelectedImageAdapter.SelectedImageViewHolder>(ImagePathDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectedImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selected_image, parent, false)
        return SelectedImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: SelectedImageViewHolder, position: Int) {
        holder.bind(getItem(position))
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


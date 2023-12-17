package com.firstapp.androidchatapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.firstapp.androidchatapp.R
import com.firstapp.androidchatapp.models.SlideItem

class SlideItemAdapter(
    private val items: List<SlideItem>
) : RecyclerView.Adapter<SlideItemAdapter.ViewHolder>() {
    class ViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.ivImage)
        val titleView: TextView = itemView.findViewById(R.id.tvTitle)
        val descView: TextView = itemView.findViewById(R.id.tvDesc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.view_slide_item, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.imageView.setImageResource(item.image)
        holder.titleView.text = item.title
        holder.descView.text = item.desc
    }
}
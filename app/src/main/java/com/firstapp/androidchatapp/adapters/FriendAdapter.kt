package com.firstapp.androidchatapp.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firstapp.androidchatapp.R
import com.firstapp.androidchatapp.models.Friend

class FriendAdapter(
    private val friends: List<Friend>
) : RecyclerView.Adapter<FriendAdapter.ViewHolder>() {

    private lateinit var context: Context

    class ViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        val friendAvatar: ImageView = itemView.findViewById(R.id.ivFriendAvatar)
        val friendName: TextView = itemView.findViewById(R.id.tvFriendName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.view_online_friend, parent, false
            )
        )
    }

    override fun getItemCount(): Int = friends.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val friend = friends[position]
        Glide.with(context).load(friend.avatarURI).into(holder.friendAvatar)
        holder.friendName.text = friend.name
    }
}
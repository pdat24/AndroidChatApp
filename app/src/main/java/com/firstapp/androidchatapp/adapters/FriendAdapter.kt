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
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModel
import com.google.android.material.button.MaterialButton

class FriendAdapter(
    private val dbViewModel: DatabaseViewModel,
    private val friends: List<Friend>
) : RecyclerView.Adapter<FriendAdapter.ViewHolder>() {

    private lateinit var context: Context

    class ViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        val avatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        val name: TextView = itemView.findViewById(R.id.tvName)
        val uid: TextView = itemView.findViewById(R.id.tvID)
        val btnUnfriend: MaterialButton = itemView.findViewById(R.id.btnUnfriend)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        return ViewHolder(
            LayoutInflater.from(context).inflate(R.layout.view_friend, parent, false)
        )
    }

    override fun getItemCount(): Int = friends.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = friends[position]
        Glide.with(context).load(user.avatarURI).into(holder.avatar)
        holder.name.text = user.name
        holder.uid.text = user.uid
        holder.btnUnfriend.setOnClickListener {
            handleUnfriend(user)
        }
    }

    private fun handleUnfriend(friend: Friend) {
        // TODO: Unfriend
    }
}
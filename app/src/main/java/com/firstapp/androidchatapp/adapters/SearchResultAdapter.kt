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
import com.firstapp.androidchatapp.models.FriendRequest
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModel
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SearchResultAdapter(
    private val dbViewModel: DatabaseViewModel,
    private val results: List<FriendRequest>
) : RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {

    private lateinit var context: Context
    private val currentUserID = FirebaseAuth.getInstance().currentUser!!.uid

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        val name: TextView = itemView.findViewById(R.id.tvName)
        val uid: TextView = itemView.findViewById(R.id.tvID)
        val btnSendReq: MaterialButton = itemView.findViewById(R.id.btnSendRequest)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.view_search_result, parent, false)
        )
    }

    override fun getItemCount(): Int = results.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = results[position]
        Glide.with(context).load(user.avatarURI).into(holder.avatar)
        holder.name.text = user.name
        holder.uid.text = user.uid
        holder.btnSendReq.setOnClickListener {
            sendFriendRequest(user)
        }
    }

    private fun sendFriendRequest(request: FriendRequest) {
        CoroutineScope(Dispatchers.IO).launch {
            dbViewModel.addSentRequest(request)
            dbViewModel.addReceivedRequest(request.uid)
            sendNotification()
        }
    }

    private fun sendNotification() {
        // TODO: Handle send notification
    }
}
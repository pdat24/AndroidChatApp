package com.firstapp.androidchatapp.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firstapp.androidchatapp.R
import com.firstapp.androidchatapp.models.Friend
import com.firstapp.androidchatapp.models.FriendRequest
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModel
import com.firstapp.androidchatapp.utils.Functions
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class SearchResultAdapter(
    private val itemMutationFlow: MutableStateFlow<Boolean>,
    private val friends: List<Friend>,
    private val sentRequests: List<FriendRequest>,
    private val receivedRequests: List<FriendRequest>,
    private val dbViewModel: DatabaseViewModel,
    private val results: List<FriendRequest>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private lateinit var context: Context

    class NormalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        val name: TextView = itemView.findViewById(R.id.tvName)
        val uid: TextView = itemView.findViewById(R.id.tvID)
        val btnSendReq: MaterialButton = itemView.findViewById(R.id.btnSendRequest)
    }

    class IsFriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        val name: TextView = itemView.findViewById(R.id.tvName)
        val uid: TextView = itemView.findViewById(R.id.tvID)
        val btnUnfriend: MaterialButton = itemView.findViewById(R.id.btnUnfriend)
    }

    class SentRequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        val name: TextView = itemView.findViewById(R.id.tvName)
        val uid: TextView = itemView.findViewById(R.id.tvID)
        val btnRecall: MaterialButton = itemView.findViewById(R.id.btnRecall)
    }

    class ReceivedRequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        val name: TextView = itemView.findViewById(R.id.tvName)
        val uid: TextView = itemView.findViewById(R.id.tvID)
        val btnAccept: MaterialButton = itemView.findViewById(R.id.btnAccept)
        val btnReject: MaterialButton = itemView.findViewById(R.id.btnReject)
    }

    override fun getItemViewType(position: Int): Int {
        /**
         * @return
         *
         * 0: the result is a friend of current user
         *
         * 1: current user sent friend request to that user
         *
         * 2: current user received friend request from that user
         *
         * -1: normal result (all other users except the above cases)
         */
        val result = results[position]
        return if (friends.map { it.uid }.contains(result.uid))
            0
        else if (sentRequests.map { it.uid }.contains(result.uid))
            1
        else if (receivedRequests.map { it.uid }.contains(result.uid))
            2
        else -1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        context = parent.context
        var viewHolder: RecyclerView.ViewHolder = NormalViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.view_search_result, parent, false)
        )
        when (viewType) {
            0 -> viewHolder = IsFriendViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.view_friend, parent, false)
            )

            1 -> viewHolder = SentRequestViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.view_sent_request, parent, false)
            )

            2 -> viewHolder = ReceivedRequestViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.view_received_request, parent, false)
            )
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val result = results[position]
        val func = Functions(dbViewModel)
        when (holder.itemViewType) {
            -1 -> {
                val view = holder as NormalViewHolder
                Glide.with(context).load(result.avatarURI).into(view.avatar)
                view.name.text = result.name
                view.uid.text = result.uid
                view.btnSendReq.setOnClickListener {
                    CoroutineScope(Dispatchers.IO).launch {
                        view.btnSendReq.setOnClickListener(null)
                        sendFriendRequest(result).join()
                        notifyItemMutation()
                    }
                }
            }

            0 -> {
                val view = holder as IsFriendViewHolder
                Glide.with(context).load(result.avatarURI).into(view.avatar)
                view.name.text = result.name
                view.uid.text = result.uid
                view.btnUnfriend.setOnClickListener {
                    CoroutineScope(Dispatchers.IO).launch {
                        unfriend(result.uid, result.name).join()
                        notifyItemMutation()
                    }
                }
            }

            1 -> {
                val view = holder as SentRequestViewHolder
                Glide.with(context).load(result.avatarURI).into(view.avatar)
                view.name.text = result.name
                view.uid.text = result.uid
                view.btnRecall.setOnClickListener {
                    CoroutineScope(Dispatchers.IO).launch {
                        view.btnRecall.setOnClickListener(null)
                        func.removeRequests(result).join()
                        notifyItemMutation()
                    }
                }
            }

            2 -> {
                val view = holder as ReceivedRequestViewHolder
                Glide.with(context).load(result.avatarURI).into(view.avatar)
                view.name.text = result.name
                view.uid.text = result.uid
                view.btnReject.setOnClickListener {
                    CoroutineScope(Dispatchers.IO).launch {
                        view.btnReject.setOnClickListener(null)
                        func.removeRequests(result).join()
                        notifyItemMutation()
                    }
                }
                view.btnAccept.setOnClickListener {
                    CoroutineScope(Dispatchers.IO).launch {
                        view.btnAccept.setOnClickListener(null)
                        Functions(dbViewModel).acceptRequest(result).join()
                        notifyItemMutation()
                    }
                }
            }
        }
    }

    private suspend fun notifyItemMutation() {
        itemMutationFlow.emit(!itemMutationFlow.value)
    }

    override fun getItemCount(): Int = results.size

    private fun sendFriendRequest(result: FriendRequest) =
        CoroutineScope(Dispatchers.IO).launch {
            dbViewModel.addSentRequest(result)
            dbViewModel.addReceivedRequest(result.uid)
        }

    private fun unfriend(friendId: String, name: String) = CoroutineScope(Dispatchers.Main).launch {
        dbViewModel.removeFriend(friendId)
        Toast.makeText(context, "Unfriended with $name", Toast.LENGTH_SHORT).show()
    }
}
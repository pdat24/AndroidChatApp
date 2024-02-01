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
import com.firstapp.androidchatapp.utils.Functions
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SentRequestAdapter(
    private val dbViewModel: DatabaseViewModel,
    private val requests: List<FriendRequest>,
) : RecyclerView.Adapter<SentRequestAdapter.ViewHolder>() {

    private lateinit var context: Context

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        val name: TextView = itemView.findViewById(R.id.tvName)
        val id: TextView = itemView.findViewById(R.id.tvID)
        val btnRecall: MaterialButton = itemView.findViewById(R.id.btnRecall)
        val btnSend: MaterialButton = itemView.findViewById(R.id.btnSend)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        return ViewHolder(
            LayoutInflater.from(context).inflate(R.layout.view_sent_request, parent, false)
        )
    }

    override fun getItemCount(): Int = requests.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val req = requests[position]
        Glide.with(context).load(req.avatarURI).into(holder.avatar)
        holder.name.text = req.name
        holder.id.text = req.uid
        holder.btnRecall.setOnClickListener {
            if (Functions.isInternetConnected(context)) {
                CoroutineScope(Dispatchers.Main).launch {
                    Functions(dbViewModel).removeRequests(req)
                    holder.btnRecall.visibility = View.GONE
                    // add send event
                    holder.btnSend.setOnClickListener {
                        CoroutineScope(Dispatchers.Main).launch {
                            sendFriendRequest(req).join()
                            holder.btnSend.visibility = View.GONE
                            holder.btnRecall.visibility = View.VISIBLE
                        }

                    }
                    holder.btnSend.visibility = View.VISIBLE
                }
            } else
                Functions.showNoInternetNotification(context)
        }
    }

    private fun sendFriendRequest(req: FriendRequest): Job =
        CoroutineScope(Dispatchers.IO).launch {
            CoroutineScope(Dispatchers.IO).launch {
                dbViewModel.addSentRequest(req)
            }
            CoroutineScope(Dispatchers.IO).launch {
                dbViewModel.addReceivedRequest(req.uid)
            }
        }
}
package com.firstapp.androidchatapp.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firstapp.androidchatapp.R
import com.firstapp.androidchatapp.models.FriendRequest
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModel
import com.firstapp.androidchatapp.utils.Functions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReceivedRequestAdapter(
    private val dbViewModel: DatabaseViewModel,
    private val requests: List<FriendRequest>
) : RecyclerView.Adapter<ReceivedRequestAdapter.ViewHolder>() {

    private lateinit var context: Context

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val container: LinearLayout = itemView.findViewById(R.id.container)
        val avatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        val name: TextView = itemView.findViewById(R.id.tvName)
        val id: TextView = itemView.findViewById(R.id.tvID)
        val btnReject: TextView = itemView.findViewById(R.id.btnReject)
        val btnAccept: TextView = itemView.findViewById(R.id.btnAccept)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        return ViewHolder(
            LayoutInflater.from(context)
                .inflate(R.layout.view_received_request, parent, false)
        )
    }

    override fun getItemCount(): Int = requests.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val req = requests[position]
        Glide.with(context).load(req.avatarURI).into(holder.avatar)
        holder.name.text = req.name
        holder.id.text = req.uid
        holder.btnAccept.setOnClickListener {
            if (isInternetConnected()) {
                CoroutineScope(Dispatchers.Main).launch {
                    Functions(dbViewModel).acceptRequest(req).join()
                    holder.container.visibility = View.GONE
                    Toast.makeText(context, "Accepted Request", Toast.LENGTH_SHORT).show()
                }
            } else
                Functions.showNoInternetNotification(context)
        }

        holder.btnReject.setOnClickListener {
            if (isInternetConnected()) {
                CoroutineScope(Dispatchers.Main).launch {
                    Functions(dbViewModel).removeRequests(req)
                    holder.container.visibility = View.GONE
                    Toast.makeText(context, "Rejected Request", Toast.LENGTH_SHORT).show()
                }
            } else
                Functions.showNoInternetNotification(context)
        }
    }

    private fun isInternetConnected(): Boolean =
        Functions.isInternetConnected(context)
}
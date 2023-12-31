package com.firstapp.androidchatapp.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firstapp.androidchatapp.R
import com.firstapp.androidchatapp.models.MessageBox
import com.firstapp.androidchatapp.ui.activities.ChatActivity
import com.firstapp.androidchatapp.utils.Constants.Companion.CONVERSATION_ID

class MessageBoxAdapter(
    private val messageBoxes: List<MessageBox>
) : RecyclerView.Adapter<MessageBoxAdapter.ViewHolder>() {

    private lateinit var context: Context

    class ViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        val container: ConstraintLayout = itemView.findViewById(R.id.container)
        val avatarView: ImageView = itemView.findViewById(R.id.imvAvatar)
        val nameView: TextView = itemView.findViewById(R.id.tvName)
        val previewMsgView: TextView = itemView.findViewById(R.id.tvPreviewMessage)
        val timView: TextView = itemView.findViewById(R.id.tvTime)
        val msgNumberView: TextView = itemView.findViewById(R.id.tvMessageNumber)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        return ViewHolder(
            LayoutInflater.from(context).inflate(R.layout.view_message_box, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return messageBoxes.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val messageBox = messageBoxes[position]
        holder.container.setOnClickListener {
            toChatActivity(messageBox.conversationID)
        }
        Glide.with(context).load(messageBox.avatarUri).into(holder.avatarView)
        holder.nameView.text = messageBox.name
        holder.previewMsgView.text = messageBox.previewMessage
        holder.timView.text = parseSendingTime(messageBox.time)
        holder.msgNumberView.text = messageBox.unreadMessages.toString()
    }

    private fun toChatActivity(conversationID: String) {
        val intent = Intent(context, ChatActivity::class.java)
        intent.putExtra(CONVERSATION_ID, conversationID)
        context.startActivity(intent)
    }

    private fun parseSendingTime(time: Long): String {
        return "14 mins"
    }
}
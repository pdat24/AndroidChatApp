package com.firstapp.androidchatapp.adapters

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firstapp.androidchatapp.R
import com.firstapp.androidchatapp.models.MessageBox
import com.firstapp.androidchatapp.models.MessageBoxesList
import com.firstapp.androidchatapp.ui.activities.ChatActivity
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModel
import com.firstapp.androidchatapp.utils.Constants.Companion.AVATAR_URI
import com.firstapp.androidchatapp.utils.Constants.Companion.CONVERSATION_ID
import com.firstapp.androidchatapp.utils.Constants.Companion.FRIEND_UID
import com.firstapp.androidchatapp.utils.Constants.Companion.IS_FRIEND
import com.firstapp.androidchatapp.utils.Constants.Companion.NAME
import com.firstapp.androidchatapp.utils.Functions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class MessageBoxAdapter(
    private val dbViewModel: DatabaseViewModel,
    private val messageBoxes: List<MessageBox>
) : RecyclerView.Adapter<MessageBoxAdapter.ViewHolder>() {

    private lateinit var context: Context
    private var friendsID: List<String>? = null

    class ViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        val container: ConstraintLayout = itemView.findViewById(R.id.container)
        val avatarView: ImageView = itemView.findViewById(R.id.imvAvatar)
        val nameView: TextView = itemView.findViewById(R.id.tvName)
        val previewMsgView: TextView = itemView.findViewById(R.id.tvPreviewMessage)
        val btnDelete: RelativeLayout = itemView.findViewById(R.id.btnDelete)
        val timView: TextView = itemView.findViewById(R.id.tvTime)
        val msgNumberView: TextView = itemView.findViewById(R.id.tvMessageNumber)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        return ViewHolder(
            LayoutInflater.from(context).inflate(R.layout.view_message_box, parent, false)
        )
    }

    override fun getItemCount(): Int = messageBoxes.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val messageBox = messageBoxes[position]
        holder.container.setOnClickListener {
            toChatActivity(messageBox)
            changeStateToRead(messageBox)
        }
        holder.btnDelete.setOnClickListener {
            Functions.observeLiveValueOneTime(dbViewModel.getCachedMessageBoxes()) {
                CoroutineScope(Dispatchers.IO).launch {
                    val newMessageBoxes = dbViewModel.getMessageBoxes(
                        dbViewModel.getMessageBoxList()
                    ).filter { it.conversationID != messageBox.conversationID }
                    dbViewModel.updateMessageBoxList(MessageBoxesList(newMessageBoxes))
                    dbViewModel.cacheMessageBoxes(newMessageBoxes)
                }
            }
        }
        Glide.with(context).load(messageBox.avatarURI).into(holder.avatarView)
        holder.nameView.text = messageBox.name
        holder.previewMsgView.text = messageBox.previewMessage
        holder.timView.text = parseSendingTime(messageBox.time)
        if (!messageBox.read) {
            holder.msgNumberView.text = messageBox.unreadMessages.toString()
            // set style for preview message when the user unread
            holder.previewMsgView.setTextColor(context.getColor(R.color.indicator))
            holder.previewMsgView.typeface = Typeface.DEFAULT_BOLD
        } else
            holder.msgNumberView.visibility = View.GONE
    }

    private fun changeStateToRead(messageBox: MessageBox) =
        CoroutineScope(Dispatchers.Main).launch {
            if (isInternetConnected())
                dbViewModel.updateMsgBoxReadState(messageBox.conversationID, true)
        }

    private fun toChatActivity(msgBox: MessageBox) {
        val intent = Intent(context, ChatActivity::class.java)
        intent.putExtra(CONVERSATION_ID, msgBox.conversationID)
        intent.putExtra(FRIEND_UID, msgBox.friendUID)
        intent.putExtra(AVATAR_URI, msgBox.avatarURI)
        intent.putExtra(NAME, msgBox.name)
        CoroutineScope(Dispatchers.Main).launch {
            if (friendsID == null)
                friendsID = dbViewModel.getFriends().map {
                    it.uid
                }
            intent.putExtra(IS_FRIEND, friendsID!!.contains(msgBox.friendUID))
            context.startActivity(intent)
        }
    }

    private fun parseSendingTime(time: Long): String {
        val tmp = LocalDateTime.ofEpochSecond(time, 0, ZoneOffset.UTC)
        return if (tmp.toLocalDate() == LocalDate.now())
            tmp.format(DateTimeFormatter.ofPattern("HH:mm"))
        else
            tmp.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }

    private fun isInternetConnected(): Boolean =
        Functions.isInternetConnected(context)
}
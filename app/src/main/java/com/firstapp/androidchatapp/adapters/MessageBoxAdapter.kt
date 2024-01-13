package com.firstapp.androidchatapp.adapters

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
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
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModel
import com.firstapp.androidchatapp.utils.Constants.Companion.AVATAR_URI
import com.firstapp.androidchatapp.utils.Constants.Companion.CONVERSATIONS_COLLECTION_PATH
import com.firstapp.androidchatapp.utils.Constants.Companion.CONVERSATION_ID
import com.firstapp.androidchatapp.utils.Constants.Companion.FRIEND_UID
import com.firstapp.androidchatapp.utils.Constants.Companion.MESSAGE_BOXES
import com.firstapp.androidchatapp.utils.Constants.Companion.MESSAGE_BOX_INDEX
import com.firstapp.androidchatapp.utils.Constants.Companion.NAME
import com.firstapp.androidchatapp.utils.Constants.Companion.PREVIEW_MESSAGE
import com.firstapp.androidchatapp.utils.Constants.Companion.TIME
import com.firstapp.androidchatapp.utils.Constants.Companion.UNREAD_MESSAGES
import com.firstapp.androidchatapp.utils.Functions
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class MessageBoxAdapter(
    private val dbViewModel: DatabaseViewModel,
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

    override fun getItemCount(): Int = messageBoxes.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val messageBox = messageBoxes[position]
        holder.container.setOnClickListener {
            toChatActivity(messageBox)
            changeStateToRead(position)
        }
        observeMessageBoxChanges(messageBox, holder)
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

    private fun observeMessageBoxChanges(messageBox: MessageBox, holder: ViewHolder) {
        FirebaseFirestore.getInstance().collection(CONVERSATIONS_COLLECTION_PATH).document(
            messageBox.conversationID
        ).addSnapshotListener { value, _ ->
            value?.let {
                CoroutineScope(Dispatchers.IO).launch {
                    // TODO: can cause exceptions, let's test it
                    val tmp = dbViewModel.getMessageBoxList()[MESSAGE_BOXES] as List<*>
                    val m = tmp[messageBox.index] as HashMap<*, *>
                    withContext(Dispatchers.Main) {
                        val unreadMsg = m[UNREAD_MESSAGES] as Long
                        if (unreadMsg != 0L) {
                            holder.msgNumberView.visibility = View.VISIBLE
                            holder.msgNumberView.text = unreadMsg.toString()
                            holder.previewMsgView.text = value[PREVIEW_MESSAGE] as String
                            holder.timView.text = parseSendingTime(value[TIME] as Long)
                        }
                    }
                }
            }
        }
    }

    private fun changeStateToRead(index: Int) =
        CoroutineScope(Dispatchers.Main).launch {
            if (isInternetConnected())
                dbViewModel.updateMsgBoxReadState(index, true)
        }

    private fun toChatActivity(msgBox: MessageBox) {
        val intent = Intent(context, ChatActivity::class.java)
        intent.putExtra(CONVERSATION_ID, msgBox.conversationID)
        intent.putExtra(FRIEND_UID, msgBox.friendUID)
        intent.putExtra(AVATAR_URI, msgBox.avatarURI)
        intent.putExtra(NAME, msgBox.name)
        intent.putExtra(MESSAGE_BOX_INDEX, msgBox.index)
        context.startActivity(intent)
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
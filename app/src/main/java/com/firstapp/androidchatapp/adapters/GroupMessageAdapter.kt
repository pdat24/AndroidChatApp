package com.firstapp.androidchatapp.adapters

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firstapp.androidchatapp.R
import com.firstapp.androidchatapp.models.GroupMessage
import com.firstapp.androidchatapp.utils.Constants.Companion.FILE
import com.firstapp.androidchatapp.utils.Constants.Companion.ICON
import com.firstapp.androidchatapp.utils.Constants.Companion.IMAGE
import com.firstapp.androidchatapp.utils.Constants.Companion.TEXT
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class GroupMessageAdapter(
    private val friendAvatarURI: String,
    private val groupMessages: List<GroupMessage>
) : RecyclerView.Adapter<GroupMessageAdapter.ViewHolder>() {

    private lateinit var context: Context
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private var lastSend = 0L

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val group: LinearLayout = itemView.findViewById(R.id.groupMessage)
        val friendAvatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        val sendDay: TextView = itemView.findViewById(R.id.tvSendDay)
        val sendingStatus: TextView = itemView.findViewById(R.id.tvSendingStatus)
        val messagesContainer: LinearLayout = itemView.findViewById(R.id.messageContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.view_group_message, parent, false
            )
        )
    }

    override fun getItemCount(): Int = groupMessages.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val groupMessage = groupMessages[position]

        Glide.with(context).load(friendAvatarURI).into(holder.friendAvatar)
        // handle show send time
        if (
            convertLongToDateTime(groupMessage.sendTime).toLocalDate() !=
            convertLongToDateTime(lastSend).toLocalDate()
        ) {
            holder.sendDay.text = formatSendTime(groupMessage.sendTime)
            holder.sendDay.visibility = View.VISIBLE
            lastSend = groupMessage.sendTime
        }
        // change group message side
        var side = GroupMessageSide.LEFT
        if (groupMessage.senderID == currentUser?.uid) {
            holder.group.gravity = Gravity.END
            holder.messagesContainer.gravity = Gravity.END
            holder.friendAvatar.visibility = View.GONE
            side = GroupMessageSide.RIGHT
        }
        val messages = groupMessage.messages
        val msgNumber = messages.size
        val container = holder.messagesContainer
        // handle render messages
        for (i in 0 until msgNumber) {
            val msg = messages[i]
            when (msg.type) {
                TEXT -> renderTextMessage(
                    container = container,
                    side = side,
                    index = i,
                    msgNumber = msgNumber,
                    content = msg.content
                )

                IMAGE -> renderImageMessage(holder.messagesContainer, msg.content)

                ICON -> renderIconMessage(holder.messagesContainer, msg.content)

                FILE -> renderFileMessage(holder.messagesContainer)
            }
        }
    }

    /**
     * format send time to HH:mm dd/MM/yyyy
     * @param time epoch time
     * @return formatted time
     */
    private fun formatSendTime(time: Long): String {
        val sendTime = convertLongToDateTime(time)
        return if (sendTime.toLocalDate() == LocalDate.now())
            "${formatTime(sendTime)}  Today"
        else if (sendTime.toLocalDate() == LocalDate.now().minusDays(1))
            "${formatTime(sendTime)}  Yesterday"
        else
            "${formatTime(sendTime)}  ${sendTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}"
    }

    /**
     * Format time to HH:mm
     */
    private fun formatTime(sendTime: LocalDateTime): String {
        return sendTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    }

    private fun convertLongToDateTime(time: Long): LocalDateTime {
        return LocalDateTime.ofEpochSecond(time, 0, ZoneOffset.UTC)
    }

    private fun setMessageText(msgView: View, text: String) {
        msgView.findViewById<TextView>(R.id.tvContent).text = text
    }

    private fun createMessageView(position: MessagePosition): View {
        val layout =
            when (position) {
                MessagePosition.TOP_LEFT -> R.layout.view_top_left_message
                MessagePosition.MIDDLE_LEFT -> R.layout.view_middle_left_message
                MessagePosition.BOTTOM_LEFT -> R.layout.view_bottom_left_message
                MessagePosition.TOP_RIGHT -> R.layout.view_top_right_message
                MessagePosition.MIDDLE_RIGHT -> R.layout.view_middle_right_message
                MessagePosition.BOTTOM_RIGHT -> R.layout.view_bottom_right_message
            }
        return LayoutInflater.from(context).inflate(layout, LinearLayout(context), false)
    }

    /**
     * Show text message on screen
     * @param container The view group directly contain the message
     * @param side message in the left or right
     * @param content the content of the message
     * @param index position of the message in group message (start is 0)
     * @param msgNumber the number of messages in group
     */
    private fun renderTextMessage(
        container: ViewGroup,
        side: GroupMessageSide,
        content: String,
        index: Int,
        msgNumber: Int
    ) {
        if (side == GroupMessageSide.LEFT) {
            if (index == 0) {
                val topLeftMessage = createMessageView(MessagePosition.TOP_LEFT)
                setMessageText(topLeftMessage, content)
                container.addView(topLeftMessage)
            } else if (index == msgNumber - 1) {
                val bottomLeftMessage = createMessageView(MessagePosition.BOTTOM_LEFT)
                setMessageText(bottomLeftMessage, content)
                container.addView(bottomLeftMessage)
            } else {
                val middleLeftMessage = createMessageView(MessagePosition.MIDDLE_LEFT)
                setMessageText(middleLeftMessage, content)
                container.addView(middleLeftMessage)
            }
        } else if (side == GroupMessageSide.RIGHT) {
            if (index == 0) {
                val topRightMessage = createMessageView(MessagePosition.TOP_RIGHT)
                setMessageText(topRightMessage, content)
                container.addView(topRightMessage)
            } else if (index == msgNumber - 1) {
                val bottomRightMessage = createMessageView(MessagePosition.BOTTOM_RIGHT)
                setMessageText(bottomRightMessage, content)
                container.addView(bottomRightMessage)
            } else {
                val middleRightMessage = createMessageView(MessagePosition.MIDDLE_RIGHT)
                setMessageText(middleRightMessage, content)
                container.addView(middleRightMessage)
            }
        }
    }

    private fun renderIconMessage(container: ViewGroup, iconUri: String) {
        val iconMessage = LayoutInflater.from(context).inflate(
            R.layout.view_icon_message, container, false
        )
        Glide.with(context).load(iconUri).into(iconMessage.findViewById<ImageView>(R.id.ivIcon))
        container.addView(iconMessage)
    }

    private fun renderFileMessage(container: ViewGroup) {
        TODO("Not yet implemented")
    }

    private fun renderImageMessage(container: ViewGroup, imgURI: String) {
        val imgMessage = LayoutInflater.from(context).inflate(
            R.layout.view_image_message, MaterialCardView(context), false
        )
        Glide.with(context).load(imgURI).into(imgMessage.findViewById<ImageView>(R.id.ivImage))
        container.addView(imgMessage)
    }

    enum class GroupMessageSide {
        LEFT, RIGHT
    }

    enum class MessagePosition {
        TOP_LEFT, TOP_RIGHT, MIDDLE_LEFT,
        MIDDLE_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }
}
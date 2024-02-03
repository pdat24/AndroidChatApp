package com.firstapp.androidchatapp.adapters

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
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
import com.firstapp.androidchatapp.ui.activities.ChatActivity
import com.firstapp.androidchatapp.utils.Constants.Companion.FILE
import com.firstapp.androidchatapp.utils.Constants.Companion.ICON
import com.firstapp.androidchatapp.utils.Constants.Companion.IMAGE
import com.firstapp.androidchatapp.utils.Constants.Companion.TEXT
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class GroupMessageAdapter(
    private val sentStatusFlow: MutableStateFlow<Boolean>,
    private val activity: ChatActivity,
    private val friendAvatarURI: String,
    private val groupMessages: List<GroupMessage>
) : RecyclerView.Adapter<GroupMessageAdapter.ViewHolder>() {

    private lateinit var context: Context
    private val currentUser = FirebaseAuth.getInstance().currentUser

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val group: LinearLayout = itemView.findViewById(R.id.groupMessage)
        val friendAvatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        val sendDay: TextView = itemView.findViewById(R.id.tvSendDay)
        val sendingStatus: TextView = itemView.findViewById(R.id.tvSendingStatus)
        val sentStatus: TextView = itemView.findViewById(R.id.tvSentStatus)
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
        resetViews(holder)
        val groupMessage = groupMessages[position]

        Glide.with(context).load(friendAvatarURI).into(holder.friendAvatar)
        handleShowSendTime(holder, groupMessage, position)

        val side = putGroupOnLeftOrRight(holder, groupMessage)
        val messages = groupMessage.messages
        val msgNumber = messages.size
        // handle render messages
        for (i in 0 until msgNumber) {
            val msg = messages[i]
            when (msg.type) {
                TEXT -> renderTextMessage(
                    container = holder.messagesContainer,
                    side = side,
                    index = i,
                    msgNumber = msgNumber,
                    content = msg.content
                )

                IMAGE -> renderImageMessage(holder.messagesContainer, msg.content)

                ICON -> renderIconMessage(holder.messagesContainer, msg.content)

                FILE -> renderFileMessage(holder.messagesContainer, msg.content)
            }
        }
        if (position == groupMessages.size - 1 && groupMessage.senderID == currentUser!!.uid) {
            holder.sentStatus.visibility = View.VISIBLE
            holder.sendingStatus.visibility = View.GONE
//            CoroutineScope(Dispatchers.Main).launch {
//                sentStatusFlow.collectLatest { sentStatus ->
//                    if (sentStatus) {
//                        holder.sentStatus.visibility = View.VISIBLE
//                        holder.sendingStatus.visibility = View.GONE
//                    } else {
//                        holder.sentStatus.visibility = View.GONE
//                        holder.sendingStatus.visibility = View.VISIBLE
//                    }
//                }
//            }
        }
    }

    /**
     * Set this views as default to prevent cause exceptions when render due to recycling
     */
    private fun resetViews(holder: ViewHolder) {
        holder.messagesContainer.removeAllViews()
        holder.sendDay.visibility = View.GONE
        holder.group.gravity = Gravity.START
        holder.messagesContainer.gravity = Gravity.START
        holder.friendAvatar.visibility = View.VISIBLE
    }

    /**
     * Change group message side.
     */
    private fun putGroupOnLeftOrRight(holder: ViewHolder, group: GroupMessage): GroupMessageSide {
        var side = GroupMessageSide.LEFT
        if (group.senderID == currentUser?.uid) {
            holder.group.gravity = Gravity.END
            holder.messagesContainer.gravity = Gravity.END
            holder.friendAvatar.visibility = View.GONE
            side = GroupMessageSide.RIGHT
        }
        return side
    }

    /**
     * handle whether show the send time of group message or not
     */
    private fun handleShowSendTime(holder: ViewHolder, group: GroupMessage, groupPos: Int) {
        if (
            groupPos == 0 || (
                    convertLongToDateTime(group.sendTime).toLocalDate() !=
                            convertLongToDateTime(groupMessages[groupPos - 1].sendTime).toLocalDate()
                    )
        ) {
            holder.sendDay.text = formatSendTime(group.sendTime)
            holder.sendDay.visibility = View.VISIBLE
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

    private fun getFileSize(bytes: Long): String {
        var result = ""
        if (bytes < 1000)
            result = "$bytes B"
        else if (bytes in 1000..999_999)
            result = "${bytes / 1000} KB"
        else if (bytes in 1_000_000..999_999_999)
            result = "${bytes / 1_000_000} MB"
        else if (bytes in 1_000_000_000..999_999_999_999)
            result = "${bytes / 1_000_000_000} GB"
        return result
    }

    @SuppressLint("SetTextI18n")
    private fun renderFileMessage(container: ViewGroup, fileURI: String) {
        val view = LayoutInflater.from(context).inflate(
            R.layout.view_file_message, container, false
        )
        val file = FirebaseStorage.getInstance().getReferenceFromUrl(fileURI)
        file.metadata.addOnCompleteListener {
            if (it.isSuccessful) {
                // set file name
                view.findViewById<TextView>(R.id.tvFileName).text =
                    it.result.name!!.split('|').first()
                view.findViewById<TextView>(R.id.tvFileCapacity).text =
                    getFileSize(it.result.sizeBytes)
            }
        }
        // download file when clicked
        view.setOnClickListener {
            val downloadManager =
                activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(fileURI))
            request.setAllowedNetworkTypes(
                DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI
            )
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            downloadManager.enqueue(request)
        }
        container.addView(view)
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
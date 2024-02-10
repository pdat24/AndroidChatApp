package com.firstapp.androidchatapp.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.firstapp.androidchatapp.models.Message
import com.firstapp.androidchatapp.repositories.ConversationManager
import com.firstapp.androidchatapp.utils.Constants.Companion.CONVERSATION_ID
import com.firstapp.androidchatapp.utils.Constants.Companion.NOTIFICATION_ID
import com.firstapp.androidchatapp.utils.Constants.Companion.REPLY_RESULT_KEY
import com.firstapp.androidchatapp.utils.Constants.Companion.TEXT
import com.firstapp.androidchatapp.utils.Functions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset

class ReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        val inputContent = remoteInput!!.getCharSequence(REPLY_RESULT_KEY)
        intent.extras?.let { extras ->
            CoroutineScope(Dispatchers.IO).launch {
                val message = Message(inputContent.toString(), TEXT)
                // send message
                ConversationManager().addMessage(
                    extras.getString(CONVERSATION_ID, ""),
                    message,
                    Functions.getPreviewMessage(context, message),
                    LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
                )
                // cancel notification
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(extras.getInt(NOTIFICATION_ID, 0))
            }
        }
    }
}


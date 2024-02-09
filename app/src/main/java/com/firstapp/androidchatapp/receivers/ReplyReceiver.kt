package com.firstapp.androidchatapp.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
//        intent?.let {
//            val remoteInput = RemoteInput.getResultsFromIntent(it)
//            val inputContent = remoteInput!!.getCharSequence(REPLY_RESULT_KEY)
//            it.extras?.let { extras ->
//                CoroutineScope(Dispatchers.IO).launch {
//                    // send message
//                    ConversationManager().addMessage(
//                        extras.getString(CONVERSATION_ID, ""),
//                        Message(inputContent.toString(), TEXT)
//                    )
//                    // cancel notification
////                    context?.let { context ->
////                        val notificationManager =
////                            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
////                        notificationManager.cancel(extras.getInt(NOTIFICATION_ID, 0))
////                    }
//                }
//            }
//        }
    }
}
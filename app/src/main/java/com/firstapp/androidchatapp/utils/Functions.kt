package com.firstapp.androidchatapp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.view.View
import com.firstapp.androidchatapp.models.GroupMessage
import com.firstapp.androidchatapp.models.Message
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModel
import com.firstapp.androidchatapp.utils.Constants.Companion.NOT_LOGIN_ERROR_CODE
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Contain functions that is used in many places on project
 */
class Functions {

    companion object {

        fun throwUserNotLoginError() {
            throw FirebaseAuthException(NOT_LOGIN_ERROR_CODE, "user is not already login!")
        }

        /**
         * get group messages in a conversation
         * @param conversation the conversation that is returned from
         * function [DatabaseViewModel.getConversation]
         * @return list of groups message
         * @see DatabaseViewModel
         */
        fun getGroupMessagesInConversation(conversation: DocumentSnapshot): List<GroupMessage> {
            val result = mutableListOf<GroupMessage>()
            val groups = conversation.get(Constants.GROUP_MESSAGES) as List<*>
            for (g in groups) {
                val group = g as HashMap<*, *>
                val messages = mutableListOf<Message>()
                val tmp = group[Constants.MESSAGES] as List<*>
                for (i in tmp) {
                    val tmp2 = i as HashMap<*, *>
                    messages.add(
                        Message(tmp2[Constants.CONTENT] as String, tmp2[Constants.TYPE] as String)
                    )
                }
                result.add(
                    GroupMessage(
                        group[Constants.SENDER_ID] as String,
                        messages,
                        group[Constants.SEND_TIME] as Long
                    )
                )
            }
            return result
        }

        fun createUniqueString(): String {
            val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
            return (1..10).map { allowedChars.random() }.joinToString("") +
                    "${LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)}"
        }

        fun scaleDownUpAnimation(view: View) {
            val duration = 150L
            view.animate().scaleX(0.7f).scaleY(0.7f).duration = duration
            CoroutineScope(Dispatchers.Default).launch {
                delay(duration)
                withContext(Dispatchers.Main) {
                    view.animate().scaleX(1f).scaleY(1f).duration = duration
                }
            }
        }

        fun showNoInternetNotification() {
            // TODO: show no internet notification
        }

        fun isInternetConnected(context: Context): Boolean {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return cm.activeNetworkInfo?.isConnected == true
        }

        fun search(list: List<String>, input: String): List<String> {
            return list.filter {
                it.startsWith(input, true) || it.split(" ").contains(input)
            }
        }
    }

}
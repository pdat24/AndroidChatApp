package com.firstapp.androidchatapp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import com.firstapp.androidchatapp.R
import com.firstapp.androidchatapp.localdb.entities.UserInfo
import com.firstapp.androidchatapp.models.Friend
import com.firstapp.androidchatapp.models.FriendRequest
import com.firstapp.androidchatapp.models.GroupMessage
import com.firstapp.androidchatapp.models.Message
import com.firstapp.androidchatapp.models.MessageBox
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModel
import com.firstapp.androidchatapp.utils.Constants.Companion.NOT_LOGIN_ERROR_CODE
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
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
class Functions(
    private val dbViewModel: DatabaseViewModel
) {

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

        fun showNoInternetNotification(context: Context) {
            val view = LayoutInflater.from(context)
                .inflate(R.layout.view_no_connection, LinearLayout(context), false)
            val dialog = MaterialAlertDialogBuilder(context)
                .setView(view)
                .create()
            view.findViewById<ImageView>(R.id.btnClose).setOnClickListener {
                dialog.dismiss()
            }
            dialog.show()
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

    /**
     * Create new message box for user and friend
     */
    private fun createMessageBoxes(req: FriendRequest, conID: String) =
        CoroutineScope(Dispatchers.IO).launch {
            // create message box for current user
            createMsgBoxForCurrentUser(req, conID)
            // create message box for user is sent this request
            createMsgBoxForUserSentRequest(req, conID)
        }

    private suspend fun createMsgBoxForCurrentUser(req: FriendRequest, conID: String) {
        val newBox = MessageBox(
            index = dbViewModel.getCachedMessageBoxNumber(),
            friendUID = req.uid,
            avatarURI = req.avatarURI,
            name = req.name,
            conversationID = conID,
            previewMessage = Constants.DEFAULT_PREVIEW_MESSAGE,
            time = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        )
        dbViewModel.createMessageBox(
            msgBoxListId = dbViewModel.getMessageBoxListId(
                dbViewModel.firebaseAuth.currentUser!!.uid
            ),
            msgBox = newBox
        )
        // add new message box to cache
        dbViewModel.cacheMessageBox(newBox)
    }

    private suspend fun createMsgBoxForUserSentRequest(req: FriendRequest, conID: String) {
        val getUserInfo: (UserInfo) -> Unit = {
            CoroutineScope(Dispatchers.IO).launch {
                val msgBoxNumber =
                    dbViewModel.getMessageBoxList(req.uid)[Constants.MESSAGE_BOXES] as List<*>
                dbViewModel.createMessageBox(
                    msgBoxListId = dbViewModel.getMessageBoxListId(req.uid),
                    msgBox = MessageBox(
                        index = msgBoxNumber.size,
                        friendUID = FirebaseAuth.getInstance().currentUser!!.uid,
                        avatarURI = it.avatarURI,
                        name = it.name,
                        conversationID = conID,
                        previewMessage = Constants.DEFAULT_PREVIEW_MESSAGE,
                        time = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
                    ),
                )
            }
        }
        CoroutineScope(Dispatchers.Main).launch {
            dbViewModel.getCachedUserInfo().observeForever(getUserInfo)
            dbViewModel.getCachedUserInfo().removeObserver(getUserInfo)
        }
    }

    /**
     * Add friend for user and friend
     * @param conID the id of conversation
     */
    private fun addFriend(req: FriendRequest, conID: String) =
        CoroutineScope(Dispatchers.IO).launch {
            dbViewModel.addFriend(
                Friend(
                    uid = req.uid,
                    name = req.name,
                    avatarURI = req.avatarURI,
                    conversationID = conID
                )
            )
        }

    /**
     * Remove the sent request of new friend and the received request of current user
     */
    fun removeRequests(req: FriendRequest) = CoroutineScope(Dispatchers.IO).launch {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null)
            throwUserNotLoginError()
        CoroutineScope(Dispatchers.IO).launch {
            dbViewModel.removeSentRequest(senderId = currentUser!!.uid, receiverId = req.uid)
            dbViewModel.removeReceivedRequest(senderId = currentUser.uid, receiverId = req.uid)
        }

        CoroutineScope(Dispatchers.IO).launch {
            dbViewModel.removeReceivedRequest(senderId = req.uid, receiverId = currentUser!!.uid)
            dbViewModel.removeSentRequest(senderId = req.uid, receiverId = currentUser.uid)
        }
    }

    fun acceptRequest(req: FriendRequest) =
        CoroutineScope(Dispatchers.IO).launch {
            val func = Functions(dbViewModel)
            // create new conversation
            var conID: String? = null
            var msgBoxIsExisted = false
            dbViewModel.getMessageBoxes(
                dbViewModel.getMessageBoxList()
            ).forEach {
                if (it.friendUID == req.uid) {
                    msgBoxIsExisted = true
                    conID = it.conversationID
                }
            }
            if (!msgBoxIsExisted) {
                conID = dbViewModel.createEmptyConversation()
                func.createMessageBoxes(req, conID!!)
            }
            func.addFriend(req, conID!!).join()
            func.removeRequests(req)
        }
}
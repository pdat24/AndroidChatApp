package com.firstapp.androidchatapp.utils

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.lifecycle.LiveData
import com.firstapp.androidchatapp.MainApp
import com.firstapp.androidchatapp.R
import com.firstapp.androidchatapp.localdb.entities.UserInfo
import com.firstapp.androidchatapp.models.Friend
import com.firstapp.androidchatapp.models.FriendRequest
import com.firstapp.androidchatapp.models.GroupMessage
import com.firstapp.androidchatapp.models.Message
import com.firstapp.androidchatapp.models.MessageBox
import com.firstapp.androidchatapp.repositories.MessageBoxManager
import com.firstapp.androidchatapp.repositories.UserManager
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModel
import com.firstapp.androidchatapp.utils.Constants.Companion.CONVERSATIONS_COLLECTION_PATH
import com.firstapp.androidchatapp.utils.Constants.Companion.MAIN_SHARED_PREFERENCE
import com.firstapp.androidchatapp.utils.Constants.Companion.NOT_LOGIN_ERROR_CODE
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Locale

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

        fun changeLanguage(activity: Activity, languageCode: String) {
            activity.getSharedPreferences(MAIN_SHARED_PREFERENCE, Context.MODE_PRIVATE).edit()
                .putString(Constants.LANGUAGE, languageCode).apply()
            MainApp.locale = Locale(languageCode)
            activity.finish()
            activity.overridePendingTransition(0, 0)
            activity.startActivity(activity.intent)
        }

        fun getMessageBoxes(msgBoxesList: DocumentSnapshot): List<MessageBox> {
            val msgBoxes = msgBoxesList[Constants.MESSAGE_BOXES] as List<*>
            val result = mutableListOf<MessageBox>()
            for (i in msgBoxes) {
                val box = i as HashMap<*, *>
                result.add(
                    MessageBox(
                        friendUID = box[Constants.FRIEND_UID] as String,
                        avatarURI = box[Constants.AVATAR_URI] as String,
                        conversationID = box[Constants.CONVERSATION_ID] as String,
                        name = box[Constants.NAME] as String,
                        read = box[Constants.READ] as Boolean,
                        unreadMessages = (box[Constants.UNREAD_MESSAGES] as Long).toInt(),
                        previewMessage = box[Constants.PREVIEW_MESSAGE] as String,
                        time = box[Constants.TIME] as Long
                    )
                )
            }
            return result
        }

        fun observeConversationsChanges(
            observer: (documentChanges: List<DocumentSnapshot>) -> Unit
        ) {
            FirebaseFirestore.getInstance()
                .collection(CONVERSATIONS_COLLECTION_PATH)
                .addSnapshotListener { value, _ ->
                    value?.let {
                        CoroutineScope(Dispatchers.IO).launch {
                            // take conversations the current user owns
                            val conversationIdList = getMessageBoxes(
                                MessageBoxManager().getMessageBoxList(
                                    UserManager().getUserById(
                                        FirebaseAuth.getInstance().currentUser!!.uid
                                    )[Constants.MESSAGE_BOX_LIST_ID] as String
                                )
                            ).map {
                                it.conversationID
                            }
                            val docChangesOfUser = mutableListOf<DocumentSnapshot>()
                            for (con in it.documentChanges) {
                                // trigger observer when a conversation user owns updates
                                if (conversationIdList.contains(con.document.id)) {
                                    docChangesOfUser.add(con.document)
                                }
                            }
                            observer(docChangesOfUser)
                        }
                    }
                }
        }

        fun createRandomInteger(): Int {
            return (-999_999..999_999).random()
        }

        fun getPreviewMessage(context: Context, message: Message): String {
            return when (message.type) {
                Constants.TEXT -> message.content
                Constants.IMAGE -> context.getString(R.string.sent_an_image)
                Constants.FILE -> context.getString(R.string.sent_a_file)
                Constants.ICON_LIKE -> context.getString(R.string.sent_icon)
                else -> ""
            }
        }

        fun isServiceRunning(context: Context, service: Class<*>): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (s in manager.getRunningServices(10)) {
                if (s.service.className == service.name)
                    return true
            }
            return false
        }

        fun <T> observeLiveValueOneTime(livedata: LiveData<T>, observer: (T) -> Job?) =
            CoroutineScope(Dispatchers.Main).launch {
                val isFinished = MutableStateFlow(false)
                val tmp: (T) -> Unit = {
                    CoroutineScope(Dispatchers.IO).launch {
                        observer(it)?.join()
                        isFinished.emit(false)
                    }
                }
                livedata.observeForever(tmp)
                isFinished.collectLatest {
                    if (it) livedata.removeObserver(tmp)
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
            friendUID = req.uid,
            avatarURI = req.avatarURI,
            name = req.name,
            conversationID = conID,
            previewMessage = Constants.DEFAULT_PREVIEW_MESSAGE,
            time = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        )
        dbViewModel.createMessageBoxOnTop(
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
                dbViewModel.createMessageBoxOnTop(
                    msgBoxListId = dbViewModel.getMessageBoxListId(req.uid),
                    msgBox = MessageBox(
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
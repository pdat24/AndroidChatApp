package com.firstapp.androidchatapp.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.firstapp.androidchatapp.localdb.entities.UserInfo
import com.firstapp.androidchatapp.models.Conversation
import com.firstapp.androidchatapp.models.Friend
import com.firstapp.androidchatapp.models.FriendRequest
import com.firstapp.androidchatapp.models.Message
import com.firstapp.androidchatapp.models.MessageBox
import com.firstapp.androidchatapp.models.MessageBoxesList
import com.firstapp.androidchatapp.models.User
import com.firstapp.androidchatapp.repositories.ConversationManager
import com.firstapp.androidchatapp.repositories.LocalRepository
import com.firstapp.androidchatapp.repositories.MessageBoxManager
import com.firstapp.androidchatapp.repositories.UserManager
import com.firstapp.androidchatapp.utils.Constants.Companion.AVATAR_URI
import com.firstapp.androidchatapp.utils.Constants.Companion.CONVERSATION_ID
import com.firstapp.androidchatapp.utils.Constants.Companion.MESSAGE_BOXES
import com.firstapp.androidchatapp.utils.Constants.Companion.MESSAGE_BOX_LIST_ID
import com.firstapp.androidchatapp.utils.Constants.Companion.NAME
import com.firstapp.androidchatapp.utils.Constants.Companion.PREVIEW_MESSAGE
import com.firstapp.androidchatapp.utils.Constants.Companion.READ
import com.firstapp.androidchatapp.utils.Constants.Companion.TIME
import com.firstapp.androidchatapp.utils.Constants.Companion.UNREAD_MESSAGES
import com.firstapp.androidchatapp.utils.Functions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DatabaseViewModel(
    context: Context
) : ViewModel() {

    private val localRepository = LocalRepository(context)
    private val conversationManager = ConversationManager()
    private val userManager = UserManager(this)
    private val msgBoxListManager = MessageBoxManager(this)
    val firebaseAuth = FirebaseAuth.getInstance()


    // apis to interact with firestore database
    /**
     * @return the ID of conversation
     */
    suspend fun createEmptyConversation(): String {
        return conversationManager.create(Conversation(emptyList()))
    }

    suspend fun getConversation(id: String): DocumentSnapshot {
        return conversationManager.getConversation(id)
    }


    /**
     * add new message to conversation
     * @param id the id of conversation
     */
    suspend fun addMessage(id: String, message: Message) {
        conversationManager.addMessage(id, message)
    }

    /**
     * @param id user ID
     * @return the ID of user
     * @see UserManager
     */
    suspend fun createUser(user: User, id: String? = null): String {
        return userManager.create(user, id)
    }

    suspend fun changeUserName(name: String) {
        val user = firebaseAuth.currentUser
        if (user == null)
            Functions.throwUserNotLoginError()
        userManager.updateName(user!!.uid, name)
    }

    suspend fun changeUserAvatar(avatarUri: String) {
        val user = firebaseAuth.currentUser
        if (user == null)
            Functions.throwUserNotLoginError()
        userManager.updateAvatar(user!!.uid, avatarUri)
    }

    suspend fun getUserById(userID: String): DocumentSnapshot {
        return userManager.getUserById(userID)
    }

    suspend fun getUsersByName(name: String): List<DocumentSnapshot> {
        return userManager.getUsersByName(name)
    }

    suspend fun getUserRequests(
        type: UserManager.RequestType,
        filter: ((String) -> Boolean)? = null
    ): List<FriendRequest> {
        return userManager.getUserRequests(firebaseAuth.currentUser!!.uid, type, filter)
    }

    suspend fun addSentRequest(req: FriendRequest) {
        userManager.addSentRequest(firebaseAuth.currentUser!!.uid, req)
    }

    suspend fun addReceivedRequest(id: String) {
        userManager.addReceivedRequest(id)
    }

    suspend fun removeSentRequest(receiverId: String) {
        userManager.removeSentRequest(firebaseAuth.currentUser!!.uid, receiverId)
    }

    suspend fun removeReceivedRequest(senderId: String) {
        userManager.removeReceivedRequest(firebaseAuth.currentUser!!.uid, senderId)
    }

    suspend fun addFriend(friend: Friend) {
        userManager.addFriend(firebaseAuth.currentUser!!.uid, friend)
        val getUserInfo: (UserInfo) -> Unit = {
            CoroutineScope(Dispatchers.IO).launch {
                userManager.addFriend(
                    friend.uid, Friend(
                        uid = firebaseAuth.currentUser!!.uid,
                        name = it.name,
                        avatarURI = it.avatarURI,
                        conversationID = friend.conversationID
                    )
                )
            }
        }
        CoroutineScope(Dispatchers.Main).launch {
            getCachedUserInfo().observeForever(getUserInfo)
            getCachedUserInfo().removeObserver(getUserInfo)
        }
    }

    /**
     * @return the ID of message boxes list
     */
    suspend fun createMsgBoxesList(msgBoxesList: MessageBoxesList): String {
        return msgBoxListManager.createMessageBoxList(msgBoxesList)
    }

    private suspend fun getMessageBoxList(): DocumentSnapshot {
        return msgBoxListManager.getMessageBoxList(getMessageBoxListId(firebaseAuth.currentUser!!.uid))
    }

    suspend fun getMessageBoxes(): List<MessageBox> {
        val tmp = getMessageBoxList()[MESSAGE_BOXES] as List<*>
        val result = mutableListOf<MessageBox>()
        for (i in tmp) {
            val t = i as HashMap<*, *>
            result.add(
                MessageBox(
                    avatarURI = t[AVATAR_URI] as String,
                    conversationID = t[CONVERSATION_ID] as String,
                    name = t[NAME] as String,
                    previewMessage = t[PREVIEW_MESSAGE] as String,
                    read = t[READ] as Boolean,
                    time = t[TIME] as Long,
                    unreadMessages = (t[UNREAD_MESSAGES] as Long).toInt()
                )
            )
        }
        return result
    }

    /**
     * Create new message box at end of message box list have id is [msgBoxListId]
     * @param msgBoxListId the id of message box list, the default value is of signed in user
     */
    suspend fun createMessageBox(msgBoxListId: String, msgBox: MessageBox) {
        msgBoxListManager.createMessageBox(msgBoxListId, msgBox)
    }

    suspend fun getMessageBoxListId(userID: String): String {
        return getUserById(userID)[MESSAGE_BOX_LIST_ID] as String
    }

    // apis to interact with local database
    suspend fun cacheUser(user: UserInfo) {
        localRepository.upsertInfo(user)
    }

    suspend fun changeCachedName(name: String) =
        localRepository.updateName(name)

    suspend fun changeCachedAvatar(avatarURI: String) =
        localRepository.updateAvatar(avatarURI)

    suspend fun removeCachedUser() {
        localRepository.removeCachedUser()
    }

    fun getCachedUserInfo(): LiveData<UserInfo> =
        localRepository.getUserInfo()

    // firebase storage
    /**
     * Upload avatar to storage
     * @param name image name on storage
     * @param uri image uri
     * @return the downloadURI of avatar on storage. Example: http(s)://.., etc
     */
    suspend fun uploadFileByUri(name: String, uri: Uri): String {
        return localRepository.uploadFileByUri(name, uri)
    }

    /**
     * Upload image to storage
     * @param name image name on storage
     * @param bytes image after converted to bytearray
     * @param ext image extension
     * @return the downloadURI of image on storage. Example: http(s)://.., etc
     */
    suspend fun uploadImageByBytes(name: String, bytes: ByteArray, ext: String = "jpg"): String {
        return localRepository.uploadImageByBytes(name, bytes, ext)
    }
}
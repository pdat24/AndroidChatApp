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
import com.firstapp.androidchatapp.utils.Constants.Companion.DEFAULT_PREVIEW_MESSAGE
import com.firstapp.androidchatapp.utils.Constants.Companion.FRIEND_UID
import com.firstapp.androidchatapp.utils.Constants.Companion.INDEX
import com.firstapp.androidchatapp.utils.Constants.Companion.MAIN_SHARED_PREFERENCE
import com.firstapp.androidchatapp.utils.Constants.Companion.MESSAGE_BOXES
import com.firstapp.androidchatapp.utils.Constants.Companion.MESSAGE_BOX_LIST_ID
import com.firstapp.androidchatapp.utils.Constants.Companion.NAME
import com.firstapp.androidchatapp.utils.Constants.Companion.PREVIEW_MESSAGE
import com.firstapp.androidchatapp.utils.Constants.Companion.READ
import com.firstapp.androidchatapp.utils.Constants.Companion.SP_MESSAGE_BOX_NUMBER
import com.firstapp.androidchatapp.utils.Constants.Companion.TIME
import com.firstapp.androidchatapp.utils.Constants.Companion.UNREAD_MESSAGES
import com.firstapp.androidchatapp.utils.Functions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset

class DatabaseViewModel(
    context: Context
) : ViewModel() {

    private val localRepository = LocalRepository(context)
    private val conversationManager = ConversationManager()
    private val userManager = UserManager()
    private val sharedPreferences =
        context.getSharedPreferences(MAIN_SHARED_PREFERENCE, Context.MODE_PRIVATE)
    private val msgBoxListManager = MessageBoxManager(this)
    val firebaseAuth = FirebaseAuth.getInstance()


    // apis to interact with firestore database
    /**
     * @return the ID of conversation
     */
    suspend fun createEmptyConversation(): String {
        return conversationManager.create(
            Conversation(
                emptyList(),
                DEFAULT_PREVIEW_MESSAGE,
                LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
            )
        )
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

    suspend fun updatePreviewMessage(conversationID: String, content: String) {
        conversationManager.updatePreviewMessage(conversationID, content)
    }

    suspend fun updateLastSendTime(conversationID: String, time: Long) {
        conversationManager.updateLastSendTime(conversationID, time)
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

    suspend fun getOnlineFriends(): List<Friend> {
        return userManager.getOnlineFriends(getUserById(firebaseAuth.currentUser!!.uid))
    }

    fun getOnlineFriends(user: DocumentSnapshot): List<Friend> =
        userManager.getOnlineFriends(user)

    suspend fun getFriends(): List<Friend> =
        userManager.getFriends(firebaseAuth.currentUser!!.uid)

    suspend fun updateOnlineState(state: Boolean) {
        return userManager.updateOnlineState(firebaseAuth.currentUser!!.uid, state)
    }

    suspend fun addSentRequest(req: FriendRequest) {
        userManager.addSentRequest(firebaseAuth.currentUser!!.uid, req)
    }

    suspend fun addReceivedRequest(senderId: String) {
        userManager.addReceivedRequest(senderId)
    }

    suspend fun removeSentRequest(senderId: String) {
        userManager.removeSentRequest(senderId, firebaseAuth.currentUser!!.uid)
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

    suspend fun removeFriend(friendId: String) {
        userManager.removeFriend(firebaseAuth.currentUser!!.uid, friendId)
        userManager.removeFriend(friendId, firebaseAuth.currentUser!!.uid)
    }

    /**
     * @return the ID of message boxes list
     */
    suspend fun createMsgBoxesList(msgBoxesList: MessageBoxesList): String {
        return msgBoxListManager.createMessageBoxList(msgBoxesList)
    }

    suspend fun getMessageBoxList(userID: String? = null): DocumentSnapshot {
        return msgBoxListManager.getMessageBoxList(
            getMessageBoxListId(userID ?: firebaseAuth.currentUser!!.uid)
        )
    }

    fun getMessageBoxes(msgBoxesDocument: DocumentSnapshot): List<MessageBox> {
        val msgBoxes = msgBoxesDocument[MESSAGE_BOXES] as List<*>
        val result = mutableListOf<MessageBox>()
        for (i in msgBoxes) {
            val box = i as HashMap<*, *>
            result.add(
                MessageBox(
                    index = (box[INDEX] as Long).toInt(),
                    friendUID = box[FRIEND_UID] as String,
                    avatarURI = box[AVATAR_URI] as String,
                    conversationID = box[CONVERSATION_ID] as String,
                    name = box[NAME] as String,
                    read = box[READ] as Boolean,
                    unreadMessages = (box[UNREAD_MESSAGES] as Long).toInt(),
                    previewMessage = box[PREVIEW_MESSAGE] as String,
                    time = box[TIME] as Long
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

    suspend fun updateMessageBoxList(msgBox: MessageBoxesList) {
        msgBoxListManager.updateMessageBoxList(
            getMessageBoxListId(firebaseAuth.currentUser!!.uid),
            msgBox
        )
    }

    suspend fun getMessageBoxListId(userID: String): String {
        return getUserById(userID)[MESSAGE_BOX_LIST_ID] as String
    }

    suspend fun updateMsgBoxReadState(msgBoxIndex: Int, state: Boolean) {
        msgBoxListManager.updateReadState(
            getMessageBoxListId(firebaseAuth.currentUser!!.uid), msgBoxIndex, state
        )
    }

    suspend fun updateUnreadMsgNumber(msgBoxIndex: Int, n: Int) {
        msgBoxListManager.updateUnreadMsgNumber(
            getMessageBoxListId(firebaseAuth.currentUser!!.uid), msgBoxIndex, n
        )
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

    suspend fun removeCachedMessageBoxes() {
        localRepository.removeMessageBoxes()
    }

    fun cacheMessageBoxNumber(n: Int) =
        sharedPreferences.edit().putInt(SP_MESSAGE_BOX_NUMBER, n).apply()

    fun getCachedMessageBoxNumber(): Int =
        sharedPreferences.getInt(SP_MESSAGE_BOX_NUMBER, -1)

    fun getCachedUserInfo(): LiveData<UserInfo> =
        localRepository.getUserInfo()

    suspend fun cacheMessageBoxes(boxes: List<MessageBox>) {
        for (box in boxes) {
            localRepository.addMessageBox(box)
        }
    }

    suspend fun cacheMessageBox(box: MessageBox) {
        localRepository.addMessageBox(box)
    }

    fun getCachedMessageBoxes(): LiveData<List<MessageBox>> =
        localRepository.getMessageBoxes()

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
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
import com.firstapp.androidchatapp.utils.Constants.Companion.DEFAULT_PREVIEW_MESSAGE
import com.firstapp.androidchatapp.utils.Constants.Companion.MAIN_SHARED_PREFERENCE
import com.firstapp.androidchatapp.utils.Constants.Companion.MESSAGE_BOX_LIST_ID
import com.firstapp.androidchatapp.utils.Constants.Companion.SP_MESSAGE_BOX_NUMBER
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
    private val msgBoxesManager = MessageBoxManager()
    val firebaseAuth = FirebaseAuth.getInstance()
    private val currentUserUID by lazy {
        FirebaseAuth.getInstance().currentUser!!.uid
    }


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
    suspend fun addMessage(id: String, message: Message, previewMsg: String, sendTime: Long) {
        conversationManager.addMessage(id, message, previewMsg, sendTime)
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
        return userManager.getUserRequests(currentUserUID, type, filter)
    }

    suspend fun getOnlineFriends(user: DocumentSnapshot? = null): List<Friend> {
        return userManager.getOnlineFriends(user ?: getUserById(currentUserUID))
    }

    suspend fun getFriends(): List<Friend> =
        userManager.getFriends(currentUserUID)

    suspend fun updateOnlineState(state: Boolean) {
        return userManager.updateOnlineState(currentUserUID, state)
    }

    suspend fun addSentRequest(req: FriendRequest) {
        userManager.addSentRequest(currentUserUID, req)
    }

    suspend fun addReceivedRequest(senderId: String) {
        userManager.addReceivedRequest(senderId)
    }

    suspend fun removeSentRequest(senderId: String, receiverId: String) {
        userManager.removeSentRequest(senderId, receiverId)
    }

    suspend fun removeReceivedRequest(senderId: String, receiverId: String) {
        userManager.removeReceivedRequest(senderId, receiverId)
    }

    suspend fun addFriend(friend: Friend) {
        userManager.addFriend(currentUserUID, friend)
        val getUserInfo: (UserInfo) -> Unit = {
            CoroutineScope(Dispatchers.IO).launch {
                userManager.addFriend(
                    friend.uid, Friend(
                        uid = currentUserUID,
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
        userManager.removeFriend(currentUserUID, friendId)
        userManager.removeFriend(friendId, currentUserUID)
    }

    /**
     * @return the ID of message boxes list
     */
    suspend fun createMsgBoxesList(msgBoxesList: MessageBoxesList): String {
        return msgBoxesManager.createMessageBoxList(msgBoxesList)
    }

    suspend fun getMessageBoxList(userID: String? = null): DocumentSnapshot {
        return msgBoxesManager.getMessageBoxList(
            getMessageBoxListId(userID ?: currentUserUID)
        )
    }

    /**
     * @param msgBoxesList The message box list
     * @return The list of message box that is sorted ascending by index
     */
    fun getMessageBoxes(msgBoxesList: DocumentSnapshot): List<MessageBox> {
        return Functions.getMessageBoxes(msgBoxesList)
    }

    suspend fun putMessageBoxOnTop(msgBoxListId: String, conversationID: String) {
        msgBoxesManager.putMessageBoxOnTop(msgBoxListId, conversationID)
    }

    fun updateActiveStatus(on: Boolean) {
        userManager.updateActiveStatus(currentUserUID, on)
    }

    /**
     * Create new message box at end of message box list have id is [msgBoxListId]
     * @param msgBoxListId the id of message box list, the default value is of signed in user
     */
    suspend fun createMessageBoxOnTop(msgBoxListId: String, msgBox: MessageBox) {
        msgBoxesManager.createMessageBoxOnTop(msgBoxListId, msgBox)
    }

    suspend fun updateMessageBoxList(msgBox: MessageBoxesList) {
        msgBoxesManager.updateMessageBoxList(
            getMessageBoxListId(currentUserUID),
            msgBox
        )
    }

    suspend fun getMessageBoxListId(userID: String): String {
        return getUserById(userID)[MESSAGE_BOX_LIST_ID] as String
    }

    suspend fun updateMsgBoxReadState(conversationID: String, state: Boolean) {
        msgBoxesManager.updateReadState(
            getMessageBoxListId(currentUserUID), conversationID, state
        )
    }

    suspend fun updateUnreadMsgNumber(conversationID: String, n: Int) {
        msgBoxesManager.updateUnreadMsgNumber(
            getMessageBoxListId(currentUserUID), conversationID, n
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
        localRepository.addMessageBoxes(boxes)
    }

    suspend fun cacheMessageBox(box: MessageBox) {
        localRepository.addMessageBox(box)
    }

    fun getCachedMessageBoxes(): LiveData<List<MessageBox>> =
        localRepository.getMessageBoxes()

    suspend fun cacheReceivedFriendRequest(req: FriendRequest) =
        localRepository.addReceivedFriendRequest(req)

    suspend fun removeCachedReceivedFriendRequest(senderId: String) =
        localRepository.removeReceivedFriendRequest(senderId)

    suspend fun clearReceivedFriendRequests() =
        localRepository.clearReceivedFriendRequests()

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
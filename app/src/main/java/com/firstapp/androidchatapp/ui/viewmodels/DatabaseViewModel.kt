package com.firstapp.androidchatapp.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.firstapp.androidchatapp.localdb.entities.UserInfo
import com.firstapp.androidchatapp.models.Conversation
import com.firstapp.androidchatapp.models.Message
import com.firstapp.androidchatapp.models.MessageBoxesList
import com.firstapp.androidchatapp.models.User
import com.firstapp.androidchatapp.repositories.ConversationManager
import com.firstapp.androidchatapp.repositories.LocalRepository
import com.firstapp.androidchatapp.repositories.MessageBoxListManager
import com.firstapp.androidchatapp.repositories.UserManager
import com.firstapp.androidchatapp.utils.Functions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot

class DatabaseViewModel(
    context: Context
) : ViewModel() {

    private val localRepository = LocalRepository(context)
    private val conversationManager = ConversationManager()
    private val userManager = UserManager()
    private val msgBoxesListManager = MessageBoxListManager()
    private val firebaseAuth = FirebaseAuth.getInstance()


    // apis to interact with firestore database
    /**
     * @return the ID of conversation
     */
    suspend fun createConversation(conversation: Conversation): String {
        return conversationManager.create(conversation)
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

    suspend fun getUser(userID: String): DocumentSnapshot {
        return userManager.getUser(userID)
    }

    /**
     * @return the ID of message boxes list
     */
    suspend fun createMsgBoxesList(msgBoxesList: MessageBoxesList): String {
        return msgBoxesListManager.create(msgBoxesList)
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
    suspend fun uploadImageMessage(name: String, uri: Uri): String {
        return localRepository.uploadImageMessage(name, uri)
    }


    /**
     * Upload avatar to storage
     * @param name file name on storag
     * @param uri file uri
     * @return the downloadURI of avatar on storage. Example: http(s)://.., etc
     */
    suspend fun uploadFileMessage(name: String, uri: Uri): String {
        return localRepository.uploadFileMessage(name, uri)
    }
}
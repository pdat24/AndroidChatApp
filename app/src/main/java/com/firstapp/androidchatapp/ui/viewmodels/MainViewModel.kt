package com.firstapp.androidchatapp.ui.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.firstapp.androidchatapp.localdb.entities.UserInfo
import com.firstapp.androidchatapp.models.Conversation
import com.firstapp.androidchatapp.models.MessageBoxesList
import com.firstapp.androidchatapp.models.User
import com.firstapp.androidchatapp.repositories.ConversationManager
import com.firstapp.androidchatapp.repositories.LocalRepository
import com.firstapp.androidchatapp.repositories.MessageBoxListManager
import com.firstapp.androidchatapp.repositories.UserManager
import com.google.firebase.firestore.DocumentSnapshot

class MainViewModel(
    context: Context
) : ViewModel() {

    private val localRepository = LocalRepository(context)
    private val conversationManager = ConversationManager()
    private val userManager = UserManager()
    private val msgBoxesListManager = MessageBoxListManager()

    /**
     * @return the ID of conversation
     */
    suspend fun createConversation(conversation: Conversation): String {
        return conversationManager.create(conversation)
    }


    /**
     * @param id user ID
     * @return the ID of user
     * @see UserManager
     */
    suspend fun createUser(user: User, id: String? = null): String {
        return userManager.create(user, id)
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


    suspend fun cacheUser(user: UserInfo) {
        localRepository.upsertInfo(user)
    }

    suspend fun removeCachedUser() {
        localRepository.removeCachedUser()
    }

    fun getLocalUserInfo(): LiveData<UserInfo> =
        localRepository.getUserInfo()
}
package com.firstapp.androidchatapp.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.firstapp.androidchatapp.models.Conversation
import com.firstapp.androidchatapp.models.MessageBoxesList
import com.firstapp.androidchatapp.models.User
import com.firstapp.androidchatapp.repositories.ConversationManager
import com.firstapp.androidchatapp.repositories.MessageBoxListManager
import com.firstapp.androidchatapp.repositories.UserManager

class MainViewModel : ViewModel() {
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
     * @return the ID of user
     */
    suspend fun createUser(user: User): String {
        return userManager.create(user)
    }

    /**
     * @return the ID of message boxes list
     */
    suspend fun createMsgBoxesList(msgBoxesList: MessageBoxesList): String {
        return msgBoxesListManager.create(msgBoxesList)
    }
}
package com.firstapp.androidchatapp.repositories

import android.util.Log
import com.firstapp.androidchatapp.models.Conversation
import com.firstapp.androidchatapp.models.Message
import com.firstapp.androidchatapp.utils.Constants.Companion.CONVERSATIONS_COLLECTION_PATH
import com.firstapp.androidchatapp.utils.Constants.Companion.MESSAGES
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ConversationManager {

    private val LOG_TAG = ConversationManager::class.simpleName
    private val conversationDB =
        FirebaseFirestore.getInstance().collection(CONVERSATIONS_COLLECTION_PATH)

    /**
     * Get conversation data on firestore
     * @param conversationID ID of required conversation
     * @return conversation data
     */
    suspend fun getConversation(conversationID: String): DocumentSnapshot? {
        return conversationDB.document(conversationID).get().await()
    }

    /**
     * Add new conversation to firestore
     * @param conversation the conversation to save
     * @return conversation ID on firestore
     */
    suspend fun create(conversation: Conversation): String {
        return conversationDB.add(conversation).await().id
    }

    /**
     * Add new message on firestore
     * @param conversationID The ID of user
     * @param message new message
     */
    suspend fun addMessage(conversationID: String, message: Message) {
        val conversation = conversationDB.document(conversationID)
        val messages = conversation.get().await().get(MESSAGES) as? MutableList<Message>
        if (messages != null) {
            messages.add(message)
            conversation.update(MESSAGES, messages).await()
        } else {
            Log.e(LOG_TAG, "Can't cast messages to type MutableList<Message>")
        }
    }
}
package com.firstapp.androidchatapp.repositories

import com.firstapp.androidchatapp.models.Conversation
import com.firstapp.androidchatapp.models.GroupMessage
import com.firstapp.androidchatapp.models.Message
import com.firstapp.androidchatapp.utils.Constants.Companion.CONVERSATIONS_COLLECTION_PATH
import com.firstapp.androidchatapp.utils.Constants.Companion.GROUP_MESSAGES
import com.firstapp.androidchatapp.utils.Constants.Companion.PREVIEW_MESSAGE
import com.firstapp.androidchatapp.utils.Constants.Companion.TIME
import com.firstapp.androidchatapp.utils.Functions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class ConversationManager {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val conversationDB =
        FirebaseFirestore.getInstance().collection(CONVERSATIONS_COLLECTION_PATH)

    /**
     * Get conversation data on firestore
     * @param conversationID ID of required conversation
     * @return conversation data
     */
    suspend fun getConversation(conversationID: String): DocumentSnapshot {
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
        val uid = firebaseAuth.currentUser!!.uid
        val groups = Functions.getGroupMessagesInConversation(getConversation(conversationID))
        if (
            groups.isEmpty() ||
            groups.first().senderID != uid ||
            !isSentToday(groups.first())
        ) {
            createNewGroupMsg(conversationID)
        }
        appendMessageToLatestGroup(conversationID, message)
    }

    private fun isSentToday(g1: GroupMessage): Boolean {
        return LocalDateTime.ofEpochSecond(
            g1.sendTime,
            0,
            ZoneOffset.UTC
        ).toLocalDate() == LocalDate.now()
    }

    /**
     * @param id the id of conversation
     */
    private suspend fun createNewGroupMsg(id: String) {
        val uid = firebaseAuth.currentUser!!.uid
        val con = conversationDB.document(id)
        val groups = Functions.getGroupMessagesInConversation(getConversation(id))
        con.update(
            GROUP_MESSAGES,
            groups.toMutableList().apply {
                add(
                    0,
                    GroupMessage(
                        uid,
                        emptyList(),
                        LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
                    )
                )
            }
        ).await()
    }

    private suspend fun appendMessageToLatestGroup(id: String, msg: Message) {
        val con = conversationDB.document(id)
        val groups = Functions.getGroupMessagesInConversation(getConversation(id))
        con.update(
            GROUP_MESSAGES,
            groups.apply {
                first().apply {
                    messages = messages.toMutableList().apply {
                        add(msg)
                    }
                }
            }
        ).await()
    }

    fun updatePreviewMessage(conversationID: String, content: String): Task<Void> =
        conversationDB.document(conversationID).update(PREVIEW_MESSAGE, content)

    fun updateLastSendTime(conversationID: String, time: Long): Task<Void> =
        conversationDB.document(conversationID).update(TIME, time)
}
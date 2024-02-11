package com.firstapp.androidchatapp.repositories

import com.firstapp.androidchatapp.models.MessageBox
import com.firstapp.androidchatapp.models.MessageBoxesList
import com.firstapp.androidchatapp.utils.Constants.Companion.AVATAR_URI
import com.firstapp.androidchatapp.utils.Constants.Companion.CONVERSATION_ID
import com.firstapp.androidchatapp.utils.Constants.Companion.DEFAULT_PREVIEW_MESSAGE
import com.firstapp.androidchatapp.utils.Constants.Companion.FRIEND_UID
import com.firstapp.androidchatapp.utils.Constants.Companion.INDEX
import com.firstapp.androidchatapp.utils.Constants.Companion.MESSAGE_BOXES
import com.firstapp.androidchatapp.utils.Constants.Companion.MESSAGE_BOXES_COLLECTION_PATH
import com.firstapp.androidchatapp.utils.Constants.Companion.NAME
import com.firstapp.androidchatapp.utils.Functions
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime
import java.time.ZoneOffset

class MessageBoxManager() {

    private val messageBoxDB =
        FirebaseFirestore.getInstance().collection(MESSAGE_BOXES_COLLECTION_PATH)

    /**
     * Get message box list
     * @param id the id of the message box list
     * @return the list of message boxes of an user
     */
    suspend fun getMessageBoxList(id: String): DocumentSnapshot {
        return messageBoxDB.document(id).get().await()
    }

    suspend fun createMessageBoxList(msgBoxList: MessageBoxesList): String {
        return messageBoxDB.add(msgBoxList).await().id
    }

    fun updateMessageBoxList(id: String, msgBoxList: MessageBoxesList) {
        messageBoxDB.document(id).set(msgBoxList)
    }

    /**
     * Add message box to the message box list have the id is [id]
     * @param id the id of the message box list
     */
    suspend fun createMessageBoxOnTop(id: String, msgBox: MessageBox) {
        val msgBoxList = getMessageBoxList(id)[MESSAGE_BOXES] as List<*>
        val list = mutableListOf<MessageBox>()
        for (i in msgBoxList) {
            val box = i as HashMap<*, *>
            list.add(
                MessageBox(
                    index = (box[INDEX] as Long).toInt() + 1,
                    friendUID = box[FRIEND_UID] as String,
                    avatarURI = box[AVATAR_URI] as String,
                    name = box[NAME] as String,
                    conversationID = box[CONVERSATION_ID] as String,
                    previewMessage = DEFAULT_PREVIEW_MESSAGE,
                    time = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
                )
            )
        }
        list.add(0, msgBox)
        messageBoxDB.document(id).update(MESSAGE_BOXES, list)
    }

    suspend fun updateUnreadMsgNumber(id: String, conversationID: String, number: Int) {
        val msgBoxes = Functions.getMessageBoxes(getMessageBoxList(id)).toMutableList()
        msgBoxes.forEach {
            if (it.conversationID == conversationID)
                it.unreadMessages = number
        }
        messageBoxDB.document(id).update(MESSAGE_BOXES, msgBoxes)
    }

    suspend fun updateReadState(id: String, conversationID: String, state: Boolean) {
        val msgBoxes = Functions.getMessageBoxes(getMessageBoxList(id)).toMutableList()
        msgBoxes.forEach {
            if (it.conversationID == conversationID)
                it.read = state
        }
        messageBoxDB.document(id).update(MESSAGE_BOXES, msgBoxes)
    }

    /**
     * Put message box contains conversation id is [conversationID] on top
     * @param id the id of message box list
     * @param conversationID conversation id of tha message box want to put on top
     */
    suspend fun putMessageBoxOnTop(id: String, conversationID: String) {
        val msgBoxes = Functions.getMessageBoxes(getMessageBoxList(id)).toMutableList()
        // increase index of all front message boxes by 1
        for (i in msgBoxes) {
            if (i.conversationID == conversationID) {
                i.index = 0
                break
            } else i.index++
        }
        messageBoxDB.document(id).update(MESSAGE_BOXES, msgBoxes)
    }

    suspend fun removeMessageBox(id: String, conversationID: String) {
        val msgBoxes = Functions.getMessageBoxes(getMessageBoxList(id)).toMutableList()
        messageBoxDB.document(id).update(MESSAGE_BOXES, msgBoxes.filter {
            it.conversationID != conversationID
        })
    }
}
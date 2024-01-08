package com.firstapp.androidchatapp.repositories

import com.firstapp.androidchatapp.models.MessageBox
import com.firstapp.androidchatapp.models.MessageBoxesList
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModel
import com.firstapp.androidchatapp.utils.Constants.Companion.AVATAR_URI
import com.firstapp.androidchatapp.utils.Constants.Companion.CONVERSATION_ID
import com.firstapp.androidchatapp.utils.Constants.Companion.MESSAGE_BOXES
import com.firstapp.androidchatapp.utils.Constants.Companion.MESSAGE_BOXES_COLLECTION_PATH
import com.firstapp.androidchatapp.utils.Constants.Companion.NAME
import com.firstapp.androidchatapp.utils.Constants.Companion.TIME
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MessageBoxManager(
    private val dbViewModel: DatabaseViewModel
) {

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

    /**
     * Add message box to the message box list have the id is [id]
     * @param id the id of the message box list
     */
    suspend fun createMessageBox(id: String, msgBox: MessageBox) {
        val msgBoxList = getMessageBoxList(id)[MESSAGE_BOXES] as List<*>
        val list = mutableListOf<MessageBox>()
        for (i in msgBoxList) {
            val box = i as HashMap<*, *>
            list.add(
                MessageBox(
                    avatarURI = box[AVATAR_URI] as String,
                    name = box[NAME] as String,
                    time = box[TIME] as Long,
                    conversationID = box[CONVERSATION_ID] as String
                )
            )
        }
        list.add(msgBox)
        messageBoxDB.document(id).update(MESSAGE_BOXES, list).await()
    }

    suspend fun updatePreviewMessage(id: String, msgBoxIndex: Int, content: String) {
        val msgBoxes = dbViewModel.getMessageBoxes(getMessageBoxList(id)).toMutableList()
        msgBoxes.forEachIndexed { index, messageBox ->
            if (msgBoxIndex == index)
                messageBox.previewMessage = content
        }
        messageBoxDB.document(id).update(MESSAGE_BOXES, msgBoxes).await()
    }

    suspend fun updateUnreadMsgNumber(id: String, msgBoxIndex: Int, number: Int) {
        val msgBoxes = dbViewModel.getMessageBoxes(getMessageBoxList(id)).toMutableList()
        msgBoxes.forEachIndexed { index, messageBox ->
            if (msgBoxIndex == index)
                messageBox.unreadMessages = number
        }
        messageBoxDB.document(id).update(MESSAGE_BOXES, msgBoxes).await()
    }

    suspend fun updateLastSendTime(id: String, msgBoxIndex: Int, time: Long) {
        val msgBoxes = dbViewModel.getMessageBoxes(getMessageBoxList(id)).toMutableList()
        msgBoxes.forEachIndexed { index, messageBox ->
            if (msgBoxIndex == index)
                messageBox.time = time
        }
        messageBoxDB.document(id).update(MESSAGE_BOXES, msgBoxes).await()
    }

    suspend fun updateReadState(id: String, msgBoxIndex: Int, state: Boolean) {
        val msgBoxes = dbViewModel.getMessageBoxes(getMessageBoxList(id)).toMutableList()
        msgBoxes.forEachIndexed { index, messageBox ->
            if (msgBoxIndex == index)
                messageBox.read = state
        }
        messageBoxDB.document(id).update(MESSAGE_BOXES, msgBoxes).await()
    }
}
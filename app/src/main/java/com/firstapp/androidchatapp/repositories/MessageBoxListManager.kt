package com.firstapp.androidchatapp.repositories

import android.util.Log
import com.firstapp.androidchatapp.models.MessageBox
import com.firstapp.androidchatapp.models.MessageBoxesList
import com.firstapp.androidchatapp.utils.Constants.Companion.MESSAGE_BOXES
import com.firstapp.androidchatapp.utils.Constants.Companion.MESSAGE_BOXES_COLLECTION_PATH
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MessageBoxListManager {

    private val LOG_TAG = MessageBoxListManager::class.simpleName
    private val msgBoxListDB: CollectionReference =
        FirebaseFirestore.getInstance().collection(MESSAGE_BOXES_COLLECTION_PATH)

    /**
     * Get the list of message boxes on firestore
     * @param messageBoxListID ID of required message box list
     * @return message boxes data
     */
    suspend fun getMessageBoxes(messageBoxListID: String): List<MessageBox>? {
        return msgBoxListDB.document(messageBoxListID).get().await()
            .get(MESSAGE_BOXES) as? List<MessageBox>
    }

    /**
     * Add new user to firestore
     * @param list the new message boxes list
     * @return ID of the list of message boxes on firestore
     */
    suspend fun create(list: MessageBoxesList): String {
        return msgBoxListDB.add(list).await().id
    }

    /**
     * Add new message box to message box list  on firestore
     * @param msgBoxListID The ID of the list of message boxes
     * @param msgBox new message box
     */
    suspend fun addNewMessageBox(msgBoxListID: String, msgBox: MessageBox) {
        val user = msgBoxListDB.document(msgBoxListID)
        val msgBoxes = user.get().await().get(MESSAGE_BOXES) as? MutableList<MessageBox>
        if (msgBoxes != null) {
            msgBoxes.add(msgBox)
            user.update(MESSAGE_BOXES, msgBoxes).await()
        } else {
            Log.e(LOG_TAG, "Can't cast msgBoxes to type MutableList<MessageBox>")
        }
    }
}
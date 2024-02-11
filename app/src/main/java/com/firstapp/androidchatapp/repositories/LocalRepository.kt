package com.firstapp.androidchatapp.repositories

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import com.firstapp.androidchatapp.localdb.SQLiteDB
import com.firstapp.androidchatapp.localdb.entities.UserInfo
import com.firstapp.androidchatapp.models.FriendRequest
import com.firstapp.androidchatapp.models.MessageBox
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class LocalRepository(
    context: Context,
) {

    private val storageRef = FirebaseStorage.getInstance().reference
    private val userDao = SQLiteDB.getInstance(context).getUserDao()
    private val msgBoxDao = SQLiteDB.getInstance(context).getMessageBoxDao()
    private val receivedFriendRequestDao = SQLiteDB.getInstance(context).getReceivedFriendRequestDao()

    suspend fun upsertInfo(user: UserInfo) =
        userDao.upsertInfo(user)

    suspend fun updateName(name: String) =
        userDao.updateName(name)

    suspend fun updateAvatar(avatarURI: String) =
        userDao.updateAvatar(avatarURI)

    suspend fun removeCachedUser() =
        userDao.clear()

    fun getUserInfo(): LiveData<UserInfo> =
        userDao.getUserInfo()

    suspend fun addMessageBox(box: MessageBox) =
        msgBoxDao.addMessageBox(box)

    fun getMessageBoxes(): LiveData<List<MessageBox>> =
        msgBoxDao.getMessageBoxes()

    suspend fun removeMessageBoxes() =
        msgBoxDao.clear()

    suspend fun addReceivedFriendRequest(req: FriendRequest) =
        receivedFriendRequestDao.addRequest(req)

    fun getReceivedFriendRequests(): LiveData<List<FriendRequest>> =
        receivedFriendRequestDao.getRequests()

    suspend fun removeReceivedFriendRequest(senderID: String) =
        receivedFriendRequestDao.removeRequest(senderID)

    suspend fun clearReceivedFriendRequests() =
        receivedFriendRequestDao.clear()

    /**
     * Upload avatar to storage
     * @param name file name on storage
     * @param uri file uri
     * @return the downloadURI of file on storage. Example: http(s)://.., etc
     */
    suspend fun uploadFileByUri(name: String, uri: Uri): String {
        return storageRef.child(name).putFile(uri).await()
            .storage.downloadUrl.await()
            .toString()
    }

    /**
     * Upload image to storage by it's bytes
     * @param name image name on storage
     * @param bytes image after converted to bytearray
     * @param ext image extension
     * @return the downloadURI of image on storage. Example: http(s)://.., etc
     */
    suspend fun uploadImageByBytes(
        name: String,
        bytes: ByteArray,
        ext: String = "jpg"
    ): String {
        return storageRef.child("$$name.$ext").putBytes(bytes).await()
            .storage.downloadUrl.await()
            .toString()
    }
}
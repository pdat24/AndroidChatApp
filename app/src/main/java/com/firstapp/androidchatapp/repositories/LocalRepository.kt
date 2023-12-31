package com.firstapp.androidchatapp.repositories

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import com.firstapp.androidchatapp.localdb.SQLiteDB
import com.firstapp.androidchatapp.localdb.entities.UserInfo
import com.firstapp.androidchatapp.utils.Constants.Companion.FILE_STORAGE_PATH
import com.firstapp.androidchatapp.utils.Constants.Companion.IMAGE_STORAGE_PATH
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class LocalRepository(
    context: Context,
) {
    private val imagesRef = FirebaseStorage.getInstance().getReference(IMAGE_STORAGE_PATH)
    private val filesRef = FirebaseStorage.getInstance().getReference(FILE_STORAGE_PATH)

    private val userDao = SQLiteDB.getInstance(context).getUserDao()

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


    /**
     * Upload avatar to storage
     * @param name image name on storage
     * @param uri image uri
     * @return the downloadURI of avatar on storage. Example: http(s)://.., etc
     */
    suspend fun uploadImageMessage(name: String, uri: Uri): String {
        return imagesRef.child(name).putFile(uri).await()
            .storage.downloadUrl.await()
            .toString()
    }

    /**
     * Upload avatar to storage
     * @param name file name on storage
     * @param uri file uri
     * @return the downloadURI of avatar on storage. Example: http(s)://.., etc
     */
    suspend fun uploadFileMessage(name: String, uri: Uri): String {
        return filesRef.child(name).putFile(uri).await()
            .storage.downloadUrl.await()
            .toString()
    }
}
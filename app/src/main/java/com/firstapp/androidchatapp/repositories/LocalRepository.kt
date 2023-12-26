package com.firstapp.androidchatapp.repositories

import android.content.Context
import androidx.lifecycle.LiveData
import com.firstapp.androidchatapp.localdb.SQLiteDB
import com.firstapp.androidchatapp.localdb.entities.UserInfo

class LocalRepository(
    private val context: Context,
) {
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
}
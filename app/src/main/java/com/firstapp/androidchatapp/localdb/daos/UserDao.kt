package com.firstapp.androidchatapp.localdb.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.firstapp.androidchatapp.localdb.entities.UserInfo

@Dao
interface UserDao {

    @Upsert
    suspend fun upsertInfo(user: UserInfo)

    @Query("UPDATE UserInfo SET name = :name")
    suspend fun updateName(name: String)

    @Query("UPDATE UserInfo SET avatarURI = :avatarURI")
    suspend fun updateAvatar(avatarURI: String)

    @Query("DELETE FROM UserInfo")
    suspend fun clear()

    @Query("SELECT * FROM UserInfo LIMIT 1")
    fun getUserInfo(): LiveData<UserInfo>
}
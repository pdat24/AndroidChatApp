package com.firstapp.androidchatapp.localdb.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.firstapp.androidchatapp.models.FriendRequest

@Dao
interface ReceivedFriendRequestDao {
    @Query("SELECT * FROM ReceivedFriendsRequests")
    fun getRequests(): LiveData<List<FriendRequest>>

    @Query("DELETE FROM ReceivedFriendsRequests WHERE uid = :senderID")
    suspend fun removeRequest(senderID: String)

    @Upsert
    suspend fun addRequest(request: FriendRequest)

    @Query("DELETE FROM ReceivedFriendsRequests")
    suspend fun clear()
}
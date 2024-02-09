package com.firstapp.androidchatapp.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ReceivedFriendsRequests")
data class FriendRequest(
    @PrimaryKey
    var uid: String,
    var name: String,
    var avatarURI: String,
)
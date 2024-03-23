package com.firstapp.androidchatapp.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "MessageBoxes")
class MessageBox(
    @PrimaryKey
    var friendUID: String,
    var avatarURI: String,
    var name: String,
    var conversationID: String,
    var read: Boolean = false,
    var previewMessage: String,
    var time: Long,
    var unreadMessages: Int = 1
)

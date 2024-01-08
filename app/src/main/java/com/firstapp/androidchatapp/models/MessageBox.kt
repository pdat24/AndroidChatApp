package com.firstapp.androidchatapp.models

import com.firstapp.androidchatapp.utils.Constants.Companion.DEFAULT_PREVIEW_MESSAGE

data class MessageBox(
    var avatarURI: String,
    var name: String,
    var time: Long,
    var conversationID: String,
    var read: Boolean = false,
    var unreadMessages: Int = 1,
    var previewMessage: String = DEFAULT_PREVIEW_MESSAGE
)

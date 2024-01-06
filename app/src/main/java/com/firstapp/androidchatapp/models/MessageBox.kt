package com.firstapp.androidchatapp.models

import com.firstapp.androidchatapp.utils.Constants.Companion.DEFAULT_PREVIEW_MESSAGE

data class MessageBox(
    val avatarURI: String,
    val name: String,
    val time: Long,
    val conversationID: String,
    val read: Boolean = false,
    val unreadMessages: Int = 1,
    val previewMessage: String = DEFAULT_PREVIEW_MESSAGE
)

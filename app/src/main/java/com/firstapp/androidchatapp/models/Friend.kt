package com.firstapp.androidchatapp.models

data class Friend(
    val uid: String,
    val name: String,
    val avatarURI: String,
    val conversationID: String
)
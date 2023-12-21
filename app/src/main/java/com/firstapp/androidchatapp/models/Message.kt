package com.firstapp.androidchatapp.models

data class Message(
    val content: String,
    val type: String,
    val senderID: String,
)

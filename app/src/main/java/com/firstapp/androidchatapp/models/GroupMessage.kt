package com.firstapp.androidchatapp.models

data class GroupMessage(
    val senderID: String,
    var messages: List<Message>,
    val sendTime: Long,
)

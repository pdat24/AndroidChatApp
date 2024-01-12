package com.firstapp.androidchatapp.models

data class Conversation(
    val groupMessages: List<GroupMessage>,
    var previewMessage: String,
    val time: Long
)
package com.firstapp.androidchatapp.models

data class User(
    val name: String,
    val avatarURI: String,
    val messageBoxListId: String,
    val notificationID: Int,
    val activeStatusOn: Boolean = true,
    val friends: List<Friend> = listOf(),
    val sentRequests: List<FriendRequest> = listOf(),
    val receivedRequests: List<FriendRequest> = listOf(),
    val onlineFriends: List<String> = listOf(),
)
package com.firstapp.androidchatapp.utils

class Constants {
    companion object {
        val DEFAULT_AVATAR_URIS = listOf(
            "https://firebasestorage.googleapis.com/v0/b/androidchatapp-6df26.appspot.com/o/avatars%2Favatar_10.jpg?alt=media&token=28899241-e0c9-4e7e-b344-afa9dd6e2548",
            "https://firebasestorage.googleapis.com/v0/b/androidchatapp-6df26.appspot.com/o/avatars%2Favatar_9.jpg?alt=media&token=5eba4a29-5792-4ebc-92cc-9f6715edbb0c",
            "https://firebasestorage.googleapis.com/v0/b/androidchatapp-6df26.appspot.com/o/avatars%2Favatar_8.jpg?alt=media&token=3793243c-8302-46b9-bf93-1fe72179d6da",
            "https://firebasestorage.googleapis.com/v0/b/androidchatapp-6df26.appspot.com/o/avatars%2Favatar_7.jpg?alt=media&token=b60bf882-ff9e-41ba-a0d2-f521889a6b78",
            "https://firebasestorage.googleapis.com/v0/b/androidchatapp-6df26.appspot.com/o/avatars%2Favatar_6.jpg?alt=media&token=3dfb78bd-f61b-40f1-9471-b52d6a4802cc",
            "https://firebasestorage.googleapis.com/v0/b/androidchatapp-6df26.appspot.com/o/avatars%2Favatar_5.jpg?alt=media&token=831e0bd5-14de-4533-9daf-d908e571e7db",
            "https://firebasestorage.googleapis.com/v0/b/androidchatapp-6df26.appspot.com/o/avatars%2Favatar_4.jpg?alt=media&token=2f630629-be82-41c6-86e6-6df69d350ff5",
            "https://firebasestorage.googleapis.com/v0/b/androidchatapp-6df26.appspot.com/o/avatars%2Favatar_3.jpg?alt=media&token=4982fb7a-cad2-4733-bb58-d7b53a653263",
            "https://firebasestorage.googleapis.com/v0/b/androidchatapp-6df26.appspot.com/o/avatars%2Favatar_5.jpg?alt=media&token=bc0acaa2-4a4b-4fa0-8f3b-68d4c63061ee",
            "https://firebasestorage.googleapis.com/v0/b/androidchatapp-6df26.appspot.com/o/avatars%2Favatar_1.jpg?alt=media&token=f7eec50b-a91c-4e76-a799-1880d8faba74",
        )
        const val USERS_COLLECTION_PATH = "users"
        const val CONVERSATIONS_COLLECTION_PATH = "conversations"
        const val MESSAGE_BOXES_COLLECTION_PATH = "messageBoxList"
        const val AVATAR_STORAGE_PATH = "avatars"
        const val IMAGE_STORAGE_PATH = "image_in_conversations"
        const val FILE_STORAGE_PATH = "file_in_conversations"
        const val DEFAULT_PREVIEW_MESSAGE = "You have become friends!"
        const val CONVERSATION_ID = "conversationID"
        const val SENDER_ID = "senderID"
        const val SEND_TIME = "sendTime"
        const val AVATAR_URI = "avatarURI"
        const val NAME = "name"
        const val TIME = "time"
        const val MESSAGES = "messages"
        const val CONTENT = "content"
        const val TYPE = "type"
        const val GROUP_MESSAGES = "groupMessages"
        const val MESSAGE_BOXES = "messageBoxes"
        const val NOT_LOGIN_ERROR_CODE = "ERROR_NOT_LOGIN"
        const val MESSAGE_BOX_LIST_ID = "messageBoxListId"
        const val MESSAGE_BOXES_LIST = "messageBoxesList"
        const val PREVIEW_MESSAGE = "previewMessage"
        const val UNREAD_MESSAGES = "unreadMessages"
        const val READ = "read"
        const val TEXT = "text"
        const val IMAGE = "image"
        const val FILE = "file"
        const val ICON = "icon"
        const val UID = "uid"
        const val FRIENDS = "friends"
        const val SENT_REQUESTS = "sentRequests"
        const val RECEIVED_REQUESTS = "receivedRequests"
        const val MESSAGE_BOX_INDEX = "messageBoxIndex"
    }
}
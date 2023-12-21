package com.firstapp.androidchatapp.localdb.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "UserInfo")
data class UserInfo(
    val name: String,
    val avatarURI: String,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
)
package com.firstapp.androidchatapp.localdb

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.firstapp.androidchatapp.localdb.daos.UserDao
import com.firstapp.androidchatapp.localdb.entities.UserInfo

@Database(entities = [UserInfo::class], version = 1, exportSchema = false)
abstract class SQLiteDB : RoomDatabase() {
    abstract fun getUserDao(): UserDao
    companion object {
        @Volatile
        var INSTANCE: SQLiteDB? = null

        fun getInstance(context: Context): SQLiteDB {
            if (INSTANCE != null)
                return INSTANCE as SQLiteDB
            INSTANCE = synchronized(this) {
                Room.databaseBuilder(
                    context,
                    SQLiteDB::class.java,
                    "chat_with_me.db"
                ).build()
            }
            return INSTANCE as SQLiteDB
        }
    }
}
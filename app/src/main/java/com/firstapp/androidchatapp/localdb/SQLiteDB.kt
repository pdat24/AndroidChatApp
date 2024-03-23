package com.firstapp.androidchatapp.localdb

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.firstapp.androidchatapp.localdb.daos.MessageBoxDao
import com.firstapp.androidchatapp.localdb.daos.ReceivedFriendRequestDao
import com.firstapp.androidchatapp.localdb.daos.UserDao
import com.firstapp.androidchatapp.localdb.entities.UserInfo
import com.firstapp.androidchatapp.models.FriendRequest
import com.firstapp.androidchatapp.models.MessageBox

@Database(
    entities = [UserInfo::class, MessageBox::class, FriendRequest::class],
    version = 5,
    exportSchema = false
)
abstract class SQLiteDB : RoomDatabase() {
    abstract fun getUserDao(): UserDao
    abstract fun getMessageBoxDao(): MessageBoxDao
    abstract fun getReceivedFriendRequestDao(): ReceivedFriendRequestDao

    companion object {
        @Volatile
        private var INSTANCE: SQLiteDB? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS MessageBoxes")
                db.execSQL(
                    """
                    CREATE TABLE MessageBoxes(
                        [index] INTEGER NOT NULL,
                        friendUID TEXT NOT NULL PRIMARY KEY,
                        avatarURI TEXT NOT NULL,
                        name TEXT NOT NULL,
                        conversationID TEXT NOT NULL,
                        read INTEGER NOT NULL,
                        unreadMessages INTEGER NOT NULL DEFAULT 1
                    )
                """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS MessageBoxes")
                db.execSQL(
                    """
                    CREATE TABLE MessageBoxes(
                        [index] INTEGER NOT NULL,
                        friendUID TEXT NOT NULL PRIMARY KEY,
                        avatarURI TEXT NOT NULL,
                        name TEXT NOT NULL,
                        conversationID TEXT NOT NULL,
                        read INTEGER NOT NULL,
                        previewMessage TEXT NOT NULL,
                        time INTEGER NOT NULL,
                        unreadMessages INTEGER NOT NULL DEFAULT 1
                    )
                """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS ReceivedFriendsRequests")
                db.execSQL(
                    """
                    CREATE TABLE ReceivedFriendsRequests(
                        uid TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        avatarURI TEXT NOT NULL
                    )
                """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS MessageBoxes")
                db.execSQL(
                    """
                    CREATE TABLE MessageBoxes(
                        friendUID TEXT NOT NULL PRIMARY KEY,
                        avatarURI TEXT NOT NULL,
                        name TEXT NOT NULL,
                        conversationID TEXT NOT NULL,
                        read INTEGER NOT NULL,
                        previewMessage TEXT NOT NULL,
                        time INTEGER NOT NULL,
                        unreadMessages INTEGER NOT NULL DEFAULT 1
                    )
                """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): SQLiteDB {
            if (INSTANCE != null)
                return INSTANCE as SQLiteDB
            INSTANCE = synchronized(this) {
                Room.databaseBuilder(
                    context,
                    SQLiteDB::class.java,
                    "chat_with_me.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addMigrations(MIGRATION_2_3)
                    .addMigrations(MIGRATION_3_4)
                    .addMigrations(MIGRATION_4_5)
                    .build()
            }
            return INSTANCE as SQLiteDB
        }
    }
}
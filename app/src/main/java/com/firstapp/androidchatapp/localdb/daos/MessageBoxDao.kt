package com.firstapp.androidchatapp.localdb.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.firstapp.androidchatapp.models.MessageBox

@Dao
interface MessageBoxDao {

    @Upsert
    suspend fun addMessageBox(box: MessageBox)

    @Query("SELECT * FROM MessageBoxes")
    fun getMessageBoxes(): LiveData<List<MessageBox>>

    @Query("DELETE FROM MessageBoxes")
    suspend fun removeMessageBoxes()
}
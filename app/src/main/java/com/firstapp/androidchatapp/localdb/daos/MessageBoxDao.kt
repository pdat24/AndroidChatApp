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

    @Upsert
    suspend fun addMessageBoxes(boxes: List<MessageBox>)

    @Query("SELECT * FROM MessageBoxes ORDER BY time DESC")
    fun getMessageBoxes(): LiveData<List<MessageBox>>

    @Query("DELETE FROM MessageBoxes")
    suspend fun clear()
}
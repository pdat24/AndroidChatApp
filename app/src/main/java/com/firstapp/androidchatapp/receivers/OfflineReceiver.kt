package com.firstapp.androidchatapp.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.firstapp.androidchatapp.repositories.UserManager
import com.firstapp.androidchatapp.ui.activities.AddFriendActivity
import com.firstapp.androidchatapp.ui.activities.ChatActivity
import com.firstapp.androidchatapp.ui.activities.FriendRequestsActivity
import com.firstapp.androidchatapp.ui.activities.FriendsActivity
import com.firstapp.androidchatapp.ui.activities.MainActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OfflineReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val appIsActive = MainActivity.active ||
                AddFriendActivity.active ||
                ChatActivity.active ||
                FriendsActivity.active ||
                FriendRequestsActivity.active
        if (!appIsActive)
            CoroutineScope(Dispatchers.IO).launch {
                val userManager = UserManager()
                val currentUserUid = FirebaseAuth.getInstance().currentUser!!.uid
                userManager.updateOnlineState(currentUserUid, false)
            }
    }
}
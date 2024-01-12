package com.firstapp.androidchatapp.services

import android.content.Intent
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.firstapp.androidchatapp.repositories.UserManager
import com.firstapp.androidchatapp.utils.Constants.Companion.ACTION_ONLINE
import com.firstapp.androidchatapp.utils.Constants.Companion.ACTION_PREPARE_OFFLINE
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OnlineService : LifecycleService() {

    private lateinit var userManager: UserManager
    private var currentUserUid: String? = null

    override fun onCreate() {
        super.onCreate()
        userManager = UserManager()
        currentUserUid = FirebaseAuth.getInstance().currentUser!!.uid
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleScope.launch {
            if (intent?.action == ACTION_ONLINE)
                userManager.updateOnlineState(currentUserUid!!, true)
            else if (intent?.action == ACTION_PREPARE_OFFLINE) {
                // 3 minute
                delay(1000 * 5)
                userManager.updateOnlineState(currentUserUid!!, false)
                stopSelf()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }
}
package com.firstapp.androidchatapp

import android.app.Activity
import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import com.firstapp.androidchatapp.receivers.OfflineReceiver
import com.firstapp.androidchatapp.repositories.UserManager
import com.firstapp.androidchatapp.ui.activities.AddFriendActivity
import com.firstapp.androidchatapp.ui.activities.ChatActivity
import com.firstapp.androidchatapp.ui.activities.FriendRequestsActivity
import com.firstapp.androidchatapp.ui.activities.FriendsActivity
import com.firstapp.androidchatapp.ui.activities.MainActivity
import com.firstapp.androidchatapp.ui.activities.SettingsActivity
import com.firstapp.androidchatapp.utils.Constants.Companion.ACTIVE_STATUS_ON
import com.firstapp.androidchatapp.utils.Constants.Companion.LANGUAGE
import com.firstapp.androidchatapp.utils.Constants.Companion.MAIN_SHARED_PREFERENCE
import com.firstapp.androidchatapp.utils.Constants.Companion.NIGHT_MODE_ON
import com.firstapp.androidchatapp.utils.Constants.Companion.USER_CHANGED_NIGHT_MODE
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class MainApp : Application() {

    private val sharedPreferences by lazy {
        getSharedPreferences(MAIN_SHARED_PREFERENCE, MODE_PRIVATE)
    }

    companion object {

        @Volatile
        private var offlineIntent: PendingIntent? = null
        var locale: Locale? = null
        var nightModeIsOn = false
        val isRunning = MainActivity.active ||
                AddFriendActivity.active ||
                ChatActivity.active ||
                FriendsActivity.active ||
                FriendRequestsActivity.active ||
                SettingsActivity.active

        private fun getOfflinePendingIntent(context: Context): PendingIntent {
            return if (offlineIntent != null)
                offlineIntent as PendingIntent
            else {
                offlineIntent = synchronized(this) {
                    PendingIntent.getBroadcast(
                        context,
                        0,
                        Intent(context, OfflineReceiver::class.java),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                }
                offlineIntent as PendingIntent
            }
        }

        fun startOnlineStatus(context: Context) {
            if (context
                    .getSharedPreferences(MAIN_SHARED_PREFERENCE, MODE_PRIVATE)
                    .getBoolean(ACTIVE_STATUS_ON, true)
            )
                CoroutineScope(Dispatchers.IO).launch {
                    UserManager().updateOnlineState(
                        FirebaseAuth.getInstance().currentUser!!.uid, true
                    )
                }
        }

        fun prepareOffline(activity: Activity, delayInMinutes: Int = 3) {
            val delay = delayInMinutes * 1000 * 60
            (activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager).setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + if (delay >= 0) delay else 0,
                getOfflinePendingIntent(activity)
            )
        }

        fun cancelPrepareOfflineJob(activity: Activity) {
            (activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(
                getOfflinePendingIntent(activity)
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        toggleNightMode()
        locale = Locale(sharedPreferences.getString(LANGUAGE, "en")!!)
    }

    private fun toggleNightMode() {
        val mode: Int
        // get night mode state after the user set
        if (sharedPreferences.getBoolean(USER_CHANGED_NIGHT_MODE, false)) {
            nightModeIsOn = sharedPreferences.getBoolean(NIGHT_MODE_ON, false)
            mode = if (nightModeIsOn)
                AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        } else {
            // get night mode follow the system
            nightModeIsOn =
                when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                    Configuration.UI_MODE_NIGHT_YES -> {
                        mode = AppCompatDelegate.MODE_NIGHT_YES
                        true
                    }

                    else -> {
                        mode = AppCompatDelegate.MODE_NIGHT_NO
                        false
                    }
                }
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
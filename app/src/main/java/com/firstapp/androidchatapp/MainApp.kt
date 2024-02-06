package com.firstapp.androidchatapp

import android.app.Activity
import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import com.firstapp.androidchatapp.receivers.OfflineReceiver
import com.firstapp.androidchatapp.repositories.UserManager
import com.firstapp.androidchatapp.utils.Constants.Companion.ACTIVE_STATUS_ON
import com.firstapp.androidchatapp.utils.Constants.Companion.LANGUAGE
import com.firstapp.androidchatapp.utils.Constants.Companion.MAIN_SHARED_PREFERENCE
import com.firstapp.androidchatapp.utils.Constants.Companion.NIGHT_MODE_FOLLOW_SYSTEM
import com.firstapp.androidchatapp.utils.Constants.Companion.NIGHT_MODE_ON
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

        private fun getOfflinePendingIntent(context: Context): PendingIntent {
            return if (offlineIntent != null)
                offlineIntent as PendingIntent
            else {
                offlineIntent = synchronized(this) {
                    PendingIntent.getBroadcast(
                        context,
                        0,
                        Intent(context, OfflineReceiver::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT
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
        var mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        if (!sharedPreferences.getBoolean(NIGHT_MODE_FOLLOW_SYSTEM, true)) {
            mode = AppCompatDelegate.MODE_NIGHT_NO
            if (sharedPreferences.getBoolean(NIGHT_MODE_ON, false))
                mode = AppCompatDelegate.MODE_NIGHT_YES
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
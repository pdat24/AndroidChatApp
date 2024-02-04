package com.firstapp.androidchatapp.ui.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.firstapp.androidchatapp.MainApp
import com.firstapp.androidchatapp.R
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModel
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModelFactory
import com.firstapp.androidchatapp.ui.viewmodels.MainViewModel
import com.firstapp.androidchatapp.utils.Constants.Companion.ACTIVE_STATUS_ON
import com.firstapp.androidchatapp.utils.Constants.Companion.MAIN_SHARED_PREFERENCE
import com.firstapp.androidchatapp.utils.Constants.Companion.NIGHT_MODE_ON
import com.firstapp.androidchatapp.utils.Constants.Companion.NOTIFICATION_ON
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {
    companion object {
        var active = false
    }

    private lateinit var nightModeSwitcher: MaterialSwitch
    private lateinit var notificationSwitcher: MaterialSwitch
    private lateinit var activeStatusSwitcher: MaterialSwitch
    private lateinit var dbViewModel: DatabaseViewModel
    private lateinit var mainViewModel: MainViewModel
    private lateinit var inputManager: InputMethodManager
    private val firebaseAuth = FirebaseAuth.getInstance()
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        sharedPreferences = getSharedPreferences(MAIN_SHARED_PREFERENCE, MODE_PRIVATE)
        val nightModeIsOn = sharedPreferences.getBoolean(NIGHT_MODE_ON, false)
        // change status bar color
        window.statusBarColor = getColor(R.color.bg_settings_activity)
        ViewCompat.getWindowInsetsController(window.decorView)?.isAppearanceLightStatusBars = !nightModeIsOn
        inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        // get views
        nightModeSwitcher = findViewById(R.id.modeSwitcher)
        notificationSwitcher = findViewById(R.id.notificationSwitcher)
        activeStatusSwitcher = findViewById(R.id.activeStatusSwitcher)
        // update switchers status
        nightModeSwitcher.isChecked = nightModeIsOn
        activeStatusSwitcher.isChecked = sharedPreferences.getBoolean(ACTIVE_STATUS_ON, false)
        notificationSwitcher.isChecked = sharedPreferences.getBoolean(NOTIFICATION_ON, false)
        // add event listeners
        activeStatusSwitcher.setOnCheckedChangeListener { view, isChecked ->
            changeSwitcherThumbColor(view as MaterialSwitch)
            toggleActiveStatus(isChecked)
        }
        nightModeSwitcher.setOnCheckedChangeListener { view, isChecked ->
            changeSwitcherThumbColor(view as MaterialSwitch)
            toggleNightMode(isChecked)
        }
        notificationSwitcher.setOnCheckedChangeListener { view, isChecked ->
            changeSwitcherThumbColor(view as MaterialSwitch)
            toggleNotification(isChecked)
        }
        dbViewModel = ViewModelProvider(
            this,
            DatabaseViewModelFactory(this)
        )[DatabaseViewModel::class.java]
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
    }

    override fun onStart() {
        super.onStart()
        changeSwitcherThumbColor(activeStatusSwitcher)
        changeSwitcherThumbColor(nightModeSwitcher)
        changeSwitcherThumbColor(notificationSwitcher)
    }

    override fun onResume() {
        super.onResume()
        active = true
        MainApp.cancelPrepareOfflineJob(this)
        MainApp.startOnlineStatus()
    }

    override fun onStop() {
        super.onStop()
        active = false
        MainApp.prepareOffline(this)
    }

    private fun changeSwitcherThumbColor(switcher: MaterialSwitch) {
        switcher.thumbTintList = ColorStateList.valueOf(
            if (switcher.isChecked)
                getColor(R.color.white)
            else
                getColor(R.color.switcher_thumb)
        )
    }

    private fun toggleNightMode(isChecked: Boolean) {
        sharedPreferences.edit().putBoolean(NIGHT_MODE_ON, isChecked).apply()
        AppCompatDelegate.setDefaultNightMode(
            if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun toggleNotification(isChecked: Boolean) {
        sharedPreferences.edit().putBoolean(NOTIFICATION_ON, isChecked).apply()
    }

    private fun toggleActiveStatus(isChecked: Boolean) {
        sharedPreferences.edit().putBoolean(ACTIVE_STATUS_ON, isChecked).apply()
    }

    /**
     * Show a dialog to confirm user decision,
     * sign out if agree else cancel
     * */
    fun signOut(view: View) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Sign out")
            .setBackground(
                AppCompatResources.getDrawable(this, R.drawable.bg_sign_out_dialog)
            )
            .setMessage("Are you sure want to sign out?")
            .setPositiveButton("Confirm") { _, _ ->
                // sign out
                onAgreeSignOut()
            }.setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }.show()
    }

    private fun onAgreeSignOut() {
        lifecycleScope.launch {
            // remove cached user on sqlite database
            dbViewModel.removeCachedUser()
            dbViewModel.removeCachedMessageBoxes()
            dbViewModel.cacheMessageBoxNumber(-1)
            dbViewModel.updateOnlineState(false)
            // sign out
            withContext(Dispatchers.Main) {
                firebaseAuth.signOut()
                startActivity(
                    Intent(this@SettingsActivity, IntroduceActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                )
                finish()
            }
        }
    }

    fun back(view: View) = finish()
}
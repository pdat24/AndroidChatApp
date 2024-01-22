package com.firstapp.androidchatapp.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.firstapp.androidchatapp.MainApp
import com.firstapp.androidchatapp.R
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModel
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModelFactory
import com.firstapp.androidchatapp.ui.viewmodels.MainViewModel
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

    private lateinit var modeSwitcher: MaterialSwitch
    private lateinit var notificationSwitcher: MaterialSwitch
    private lateinit var dbViewModel: DatabaseViewModel
    private lateinit var mainViewModel: MainViewModel
    private lateinit var inputManager: InputMethodManager
    private val firebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        // change status bar color
        window.statusBarColor = getColor(R.color.light_black)
        inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        // get views
        modeSwitcher = findViewById(R.id.modeSwitcher)
        notificationSwitcher = findViewById(R.id.notificationSwitcher)
        // add event listeners
        modeSwitcher.setOnCheckedChangeListener { _, isChecked -> toggleNightMode(isChecked) }
        notificationSwitcher.setOnCheckedChangeListener { _, isChecked ->
            toggleNotification(isChecked)
        }
        dbViewModel = ViewModelProvider(
            this,
            DatabaseViewModelFactory(this)
        )[DatabaseViewModel::class.java]
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
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

    private fun toggleNightMode(isChecked: Boolean) {

    }

    private fun toggleNotification(isChecked: Boolean) {

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
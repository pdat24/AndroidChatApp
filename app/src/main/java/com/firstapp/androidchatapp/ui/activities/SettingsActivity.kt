package com.firstapp.androidchatapp.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.firstapp.androidchatapp.R
import com.firstapp.androidchatapp.ui.viewmodels.MainViewModel
import com.firstapp.androidchatapp.ui.viewmodels.MainViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {

    private lateinit var modeSwitcher: MaterialSwitch
    private lateinit var notificationSwitcher: MaterialSwitch
    private lateinit var mainViewModel: MainViewModel
    private lateinit var avatarView: ImageView
    private lateinit var nameView: TextView
    private val firebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        // change status bar color
        window.statusBarColor = getColor(R.color.light_black)
        // get views
        modeSwitcher = findViewById(R.id.modeSwitcher)
        notificationSwitcher = findViewById(R.id.notificationSwitcher)
        avatarView = findViewById(R.id.ivAvatar)
        nameView = findViewById(R.id.tvName)
        // add event listeners for switchers
        modeSwitcher.setOnCheckedChangeListener { _, isChecked -> toggleNightMode(isChecked) }
        notificationSwitcher.setOnCheckedChangeListener { _, isChecked ->
            toggleNotification(isChecked)
        }
        // view model
        mainViewModel = ViewModelProvider(
            this,
            MainViewModelFactory(this)
        )[MainViewModel::class.java]

        // update UI base on user
        mainViewModel.getLocalUserInfo().observe(this) { userInfo ->
            if (userInfo?.name != null) {
                Glide.with(this).load(userInfo.avatarURI).into(avatarView)
                nameView.text = userInfo.name
            }
        }
    }

    fun changeAvatar(view: View) {

    }

    fun editName(view: View) {

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
            .setTitle("Confirm")
            .setMessage("Are you sure want to sign out?")
            .setPositiveButton("Sign out") { _, _ ->
                // sign out
                onAgreeSignOut()
            }.setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }.show()
    }

    private fun onAgreeSignOut() {
        lifecycleScope.launch {
            // remove cached user on sqlite database
            mainViewModel.removeCachedUser()
            // sign out
            withContext(Dispatchers.Main) {
                firebaseAuth.signOut()
                startActivity(
                    Intent(this@SettingsActivity, IntroduceActivity::class.java)
                )
                finish()
            }
        }
    }

    fun back(view: View) = finish()
}
package com.firstapp.androidchatapp.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.firstapp.androidchatapp.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {

    private lateinit var modeSwitcher: MaterialSwitch
    private lateinit var notificationSwitcher: MaterialSwitch
    private val firebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        // change status bar color
        window.statusBarColor = getColor(R.color.light_black)
        // get views
        modeSwitcher = findViewById(R.id.modeSwitcher)
        notificationSwitcher = findViewById(R.id.notificationSwitcher)
        // add event listeners for switchers
        modeSwitcher.setOnCheckedChangeListener { _, isChecked -> toggleNightMode(isChecked) }
        notificationSwitcher.setOnCheckedChangeListener { _, isChecked ->
            toggleNotification(isChecked)
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
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Confirm")
            .setMessage("Are you sure want to sign out?")
            .setPositiveButton("Sign out") { _, _ ->
                // sign out
                firebaseAuth.signOut()
                startActivity(
                    Intent(this, IntroduceActivity::class.java)
                )
                finish()
            }.setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }.create()
        dialog.show()
    }

    fun back(view: View) = finish()
}
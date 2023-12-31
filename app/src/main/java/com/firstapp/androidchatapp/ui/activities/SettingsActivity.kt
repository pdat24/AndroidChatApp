package com.firstapp.androidchatapp.ui.activities

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.firstapp.androidchatapp.R
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModel
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModelFactory
import com.firstapp.androidchatapp.ui.viewmodels.MainViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {

    private lateinit var modeSwitcher: MaterialSwitch
    private lateinit var notificationSwitcher: MaterialSwitch
    private lateinit var dbViewModel: DatabaseViewModel
    private lateinit var mainViewModel: MainViewModel
    private lateinit var avatarView: ImageView
    private lateinit var nameView: TextView
    private lateinit var nameInputContainer: TextInputLayout
    private lateinit var nameInput: TextInputEditText
    private lateinit var editNameBtn: ImageView
    private lateinit var confirmChangeNameBtn: ImageView
    private lateinit var changeAvatarBtn: RelativeLayout
    private lateinit var inputManager: InputMethodManager
    private lateinit var imgPickerFragment: FragmentContainerView
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
        avatarView = findViewById(R.id.ivAvatar)
        changeAvatarBtn = findViewById(R.id.changeAvatarBtn)
        nameView = findViewById(R.id.tvName)
        editNameBtn = findViewById(R.id.ivEditName)
        nameInputContainer = findViewById(R.id.editNameLayout)
        confirmChangeNameBtn = findViewById(R.id.ivConfirmChangeName)
        nameInput = findViewById(R.id.editNameInput)
        imgPickerFragment = findViewById(R.id.frmImagePicker)
        // add event listeners
        modeSwitcher.setOnCheckedChangeListener { _, isChecked -> toggleNightMode(isChecked) }
        notificationSwitcher.setOnCheckedChangeListener { _, isChecked ->
            toggleNotification(isChecked)
        }
        editNameBtn.setOnClickListener {
            if (internetIsConnected()) {
                nameInput.setText(nameView.text)
                openEditNameInput()
            } else showNoInternetNotification()
        }
        changeAvatarBtn.setOnClickListener {
            if (internetIsConnected()) {
                showImgPickerDialog()
            } else showNoInternetNotification()
        }
        confirmChangeNameBtn.setOnClickListener {
            val newName = nameInput.text.toString()
            if (newName.isNotEmpty()) {
                lifecycleScope.launch {
                    changeName(newName)
                    withContext(Dispatchers.Main) {
                        closeEditNameInput()
                    }
                }
            }
        }
//        imgPickerFragment.visibility =
//            if (mainViewModel.imgPickerIsOpened) View.VISIBLE else View.GONE
        // view model
        dbViewModel = ViewModelProvider(
            this,
            DatabaseViewModelFactory(this)
        )[DatabaseViewModel::class.java]
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        // update UI base on user
        dbViewModel.getCachedUserInfo().observe(this) { userInfo ->
            if (userInfo?.name != null) {
                Glide.with(this).load(userInfo.avatarURI).into(avatarView)
                nameView.text = userInfo.name
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                // collect image picker state
                mainViewModel.imgPickerState.collectLatest { openImgPicker ->
                    imgPickerFragment.visibility =
                        if (openImgPicker) View.VISIBLE else View.GONE
                }

                // collect avatar state
                mainViewModel.avatarState.collectLatest { avatarURI ->
                    if (avatarURI.isNotEmpty()) {
                        Glide.with(this@SettingsActivity).load(avatarURI).into(avatarView)
                    }
                }
            }
        }
    }

    private fun openEditNameInput() {
        // toggle views
        nameView.visibility = View.INVISIBLE
        editNameBtn.visibility = View.INVISIBLE
        nameInputContainer.visibility = View.VISIBLE
        confirmChangeNameBtn.visibility = View.VISIBLE

        nameInput.requestFocus()
        inputManager.showSoftInput(nameInput, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun closeEditNameInput() {
        nameInputContainer.visibility = View.INVISIBLE
        confirmChangeNameBtn.visibility = View.INVISIBLE
        nameView.visibility = View.VISIBLE
        editNameBtn.visibility = View.VISIBLE
        nameInput.text?.clear()
    }

    private fun showImgPickerDialog() {
        imgPickerFragment.visibility = View.VISIBLE
        lifecycleScope.launch {
            mainViewModel.imgPickerState.emit(true)
        }
    }

    private fun internetIsConnected(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo?.isConnected == true
    }

    private fun showNoInternetNotification() {
        startActivityIfNeeded(
            Intent.createChooser(
                Intent(Intent.ACTION_PICK), ""
            ), 0
        )
    }

    private suspend fun changeName(name: String) {
        dbViewModel.changeUserName(name)
        dbViewModel.changeCachedName(name)
    }

    private fun toggleNightMode(isChecked: Boolean) {

    }

    private fun toggleNotification(isChecked: Boolean) {

    }

    private fun cancelChangeName() {
        inputManager.hideSoftInputFromWindow(
            nameInput.windowToken,
            InputMethodManager.RESULT_UNCHANGED_HIDDEN
        )
        closeEditNameInput()
        Toast.makeText(this, "The change isn't applied", Toast.LENGTH_SHORT).show()
    }

    /**
     * Show a dialog to confirm user decision,
     * sign out if agree else cancel
     * */
    fun signOut(view: View) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Confirm")
            .setBackground(
                AppCompatResources.getDrawable(this, R.drawable.bg_sign_out_dialog)
            )
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
            dbViewModel.removeCachedUser()
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

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            // close name input when click on outside of it
            // and check button
            val v = currentFocus
            if (v is EditText) {
                val nameInputRect = Rect()
                val okChangeBtnRect = Rect()
                v.getGlobalVisibleRect(nameInputRect)
                confirmChangeNameBtn.getGlobalVisibleRect(okChangeBtnRect)
                if (
                    !nameInputRect.contains(ev.rawX.toInt(), ev.rawY.toInt()) &&
                    !okChangeBtnRect.contains(ev.rawX.toInt(), ev.rawY.toInt())
                ) {
                    cancelChangeName()
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}
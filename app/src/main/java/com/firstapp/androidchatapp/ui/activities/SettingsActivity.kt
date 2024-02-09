package com.firstapp.androidchatapp.ui.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.ListPopupWindow
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.firstapp.androidchatapp.MainApp
import com.firstapp.androidchatapp.R
import com.firstapp.androidchatapp.adapters.LanguageMenuAdapter
import com.firstapp.androidchatapp.databinding.ActivitySettingsBinding
import com.firstapp.androidchatapp.services.NotificationsService
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModel
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModelFactory
import com.firstapp.androidchatapp.ui.viewmodels.MainViewModel
import com.firstapp.androidchatapp.utils.Constants.Companion.ACTIVE_STATUS_ON
import com.firstapp.androidchatapp.utils.Constants.Companion.MAIN_SHARED_PREFERENCE
import com.firstapp.androidchatapp.utils.Constants.Companion.NIGHT_MODE_FOLLOW_SYSTEM
import com.firstapp.androidchatapp.utils.Constants.Companion.NIGHT_MODE_ON
import com.firstapp.androidchatapp.utils.Constants.Companion.NOTIFICATION_ON
import com.firstapp.androidchatapp.utils.Constants.Companion.USER_CHANGED_NIGHT_MODE
import com.firstapp.androidchatapp.utils.Functions
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

    init {
        this.applyOverrideConfiguration(Configuration().apply {
            setLocale(MainApp.locale)
        })
    }

    private lateinit var nightModeSwitcher: MaterialSwitch
    private lateinit var notificationSwitcher: MaterialSwitch
    private lateinit var activeStatusSwitcher: MaterialSwitch
    private lateinit var dbViewModel: DatabaseViewModel
    private lateinit var mainViewModel: MainViewModel
    private lateinit var inputManager: InputMethodManager
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val languagePopup: ListPopupWindow by lazy {
        ListPopupWindow(this)
    }
    private lateinit var sharedPreferences: SharedPreferences
    private val activityScheme: MutableLiveData<Scheme> by lazy {
        MutableLiveData<Scheme>(
            if (MainApp.nightModeIsOn) Scheme.DarkScheme() else Scheme.LightScheme()
        )
    }
    private val languagePicker: ConstraintLayout by lazy {
        findViewById(R.id.languagePicker)
    }
    private val btnBack: ImageView by lazy {
        findViewById(R.id.btnBack)
    }
    private val iconArrowRight: ImageView by lazy {
        findViewById(R.id.iconArrowRight)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivitySettingsBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_settings)
        sharedPreferences = getSharedPreferences(MAIN_SHARED_PREFERENCE, MODE_PRIVATE)
        MainApp.nightModeIsOn = sharedPreferences.getBoolean(NIGHT_MODE_ON, false)
        // bind color scheme
        activityScheme.postValue(
            if (MainApp.nightModeIsOn) Scheme.DarkScheme() else Scheme.LightScheme()
        )
        observeSchemeChange(binding)
        inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        // get views
        nightModeSwitcher = findViewById(R.id.modeSwitcher)
        notificationSwitcher = findViewById(R.id.notificationSwitcher)
        activeStatusSwitcher = findViewById(R.id.activeStatusSwitcher)

        // update switchers status
        nightModeSwitcher.isChecked = MainApp.nightModeIsOn
        activeStatusSwitcher.isChecked = sharedPreferences.getBoolean(ACTIVE_STATUS_ON, true)
        notificationSwitcher.isChecked = sharedPreferences.getBoolean(NOTIFICATION_ON, true)
        // add event listeners
        activeStatusSwitcher.setOnCheckedChangeListener { _, isChecked ->
            toggleActiveStatus(isChecked)
        }
        nightModeSwitcher.setOnCheckedChangeListener { _, isChecked ->
            toggleNightMode(isChecked)
        }
        notificationSwitcher.setOnCheckedChangeListener { _, isChecked ->
            toggleNotification(isChecked)
        }
        languagePicker.setOnClickListener(::openLanguagePickerPopup)
        dbViewModel = ViewModelProvider(
            this,
            DatabaseViewModelFactory(this)
        )[DatabaseViewModel::class.java]
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
    }

    override fun onStart() {
        super.onStart()
        changeSwitchTrackTint(activeStatusSwitcher)
        changeSwitchTrackTint(nightModeSwitcher)
        changeSwitchTrackTint(notificationSwitcher)
    }

    override fun onResume() {
        super.onResume()
        active = true
        MainApp.cancelPrepareOfflineJob(this)
        MainApp.startOnlineStatus(this)
    }

    override fun onStop() {
        super.onStop()
        active = false
        MainApp.prepareOffline(this)
    }

    private fun observeSchemeChange(binding: ActivitySettingsBinding) =
        activityScheme.observe(this) { scheme ->
            binding.textSettingsOptionsColor = scheme.textSettingsOptions
            binding.bgSettingsOptions = scheme.bgSettingsOptions
            binding.bgSettingsActivity = scheme.bgSettingsActivity
            binding.signOutBtn = scheme.signOutBtn
            binding.title = scheme.activityTitle
            binding.divider = scheme.divider
            window.statusBarColor = scheme.bgSettingsActivity
            ViewCompat.getWindowInsetsController(window.decorView)?.isAppearanceLightStatusBars =
                !MainApp.nightModeIsOn
            if (scheme is Scheme.LightScheme) {
                btnBack.setBackgroundResource(R.drawable.bg_light_back_button)
                iconArrowRight.setImageResource(R.drawable.light_baseline_navigate_next_24)
            } else if (scheme is Scheme.DarkScheme) {
                btnBack.setBackgroundResource(R.drawable.bg_dark_back_button)
                iconArrowRight.setImageResource(R.drawable.baseline_navigate_next_24)
            }
        }

    private fun changeToDarkScheme() {
        activityScheme.postValue(Scheme.DarkScheme())
    }

    private fun changeToLightScheme() {
        activityScheme.postValue(Scheme.LightScheme())
    }

    private fun openLanguagePickerPopup(view: View) {
        languagePopup.anchorView = view
        languagePopup.setBackgroundDrawable(
            AppCompatResources.getDrawable(
                this,
                if (MainApp.nightModeIsOn)
                    R.drawable.bg_dark_sign_out_dialog
                else R.drawable.bg_light_sign_out_dialog
            )
        )
        languagePopup.setDropDownGravity(Gravity.START)
        languagePopup.setAdapter(
            LanguageMenuAdapter(
                this,
                R.layout.view_language_menu,
                listOf(getString(R.string.english), getString(R.string.vietnamese)),
            )
        )
        languagePopup.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> Functions.changeLanguage(this, "en")

                1 -> Functions.changeLanguage(this, "vi")
            }
            languagePopup.dismiss()
        }
        languagePopup.show()
    }

    private fun toggleNightMode(isChecked: Boolean) {
        sharedPreferences.edit()
            .putBoolean(NIGHT_MODE_ON, isChecked)
            .putBoolean(USER_CHANGED_NIGHT_MODE, true)
            .apply()
        MainApp.nightModeIsOn = isChecked
        if (isChecked)
            changeToDarkScheme()
        else changeToLightScheme()
        changeSwitchTrackTint(nightModeSwitcher)
    }

    private fun toggleNotification(isChecked: Boolean) {
        sharedPreferences.edit().putBoolean(NOTIFICATION_ON, isChecked).apply()
        changeSwitchTrackTint(notificationSwitcher)
        if (isChecked)
            turnOnNotifications()
        else
            turnOffNotifications()
    }

    private fun toggleActiveStatus(isChecked: Boolean) {
        dbViewModel.updateActiveStatus(isChecked)
        sharedPreferences.edit().putBoolean(ACTIVE_STATUS_ON, isChecked).apply()
        changeSwitchTrackTint(activeStatusSwitcher)
    }

    private fun changeSwitchTrackTint(switch: MaterialSwitch) {
        switch.trackTintList = ColorStateList.valueOf(
            getColor(
                if (switch.isChecked) R.color.blue else R.color.switch_track_color
            )
        )
    }

    /**
     * Show a dialog to confirm user decision,
     * sign out if agree else cancel
     * */
    fun signOut(view: View) {
        MaterialAlertDialogBuilder(
            this,
            if (MainApp.nightModeIsOn)
                R.style.DarkDialog
            else R.style.LightDialog
        )
            .setTitle(getString(R.string.sign_out))
            .setMessage(getString(R.string.sign_out_promt))
            .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                // sign out
                onAgreeSignOut()
            }.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
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
            turnOffNotifications()
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

    private fun turnOffNotifications() {
        stopService(Intent(this, NotificationsService::class.java))
    }

    private fun turnOnNotifications() {
        startService(Intent(this, NotificationsService::class.java))
    }

    fun back(view: View) = finish()

    sealed class Scheme(
        val bgSettingsActivity: Int,
        val bgSettingsOptions: Int,
        val textSettingsOptions: Int,
        val signOutBtn: Int,
        val activityTitle: Int,
        val divider: Int,
    ) {
        class LightScheme(
            bgSettingsActivity: Int = Color.parseColor("#DCDCDC"),
            bgSettingsOptions: Int = Color.parseColor("#AEB1B1"),
            textSettingsOptions: Int = Color.parseColor("#605C5C"),
            signOutBtn: Int = Color.parseColor("#4194D8"),
            activityTitle: Int = Color.parseColor("#333333"),
            divider: Int = Color.parseColor("#33706969"),
        ) : Scheme(
            bgSettingsActivity,
            bgSettingsOptions,
            textSettingsOptions,
            signOutBtn,
            activityTitle,
            divider,
        )

        class DarkScheme(
            bgSettingsActivity: Int = Color.parseColor("#282A36"),
            bgSettingsOptions: Int = Color.parseColor("#44475a"),
            textSettingsOptions: Int = Color.parseColor("#C3C1C1"),
            signOutBtn: Int = Color.parseColor("#194166"),
            activityTitle: Int = Color.parseColor("#ffffff"),
            divider: Int = Color.parseColor("#33B1AEAE"),
        ) : Scheme(
            bgSettingsActivity,
            bgSettingsOptions,
            textSettingsOptions,
            signOutBtn,
            activityTitle,
            divider,
        )
    }
}
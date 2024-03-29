package com.firstapp.androidchatapp.ui.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firstapp.androidchatapp.MainApp
import com.firstapp.androidchatapp.R
import com.firstapp.androidchatapp.adapters.FriendAdapter
import com.firstapp.androidchatapp.models.Friend
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModel
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModelFactory
import com.firstapp.androidchatapp.utils.Constants.Companion.MAIN_SHARED_PREFERENCE
import com.firstapp.androidchatapp.utils.Constants.Companion.NIGHT_MODE_ON
import com.firstapp.androidchatapp.utils.Functions
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FriendsActivity : AppCompatActivity() {
    companion object {
        var active = false
    }

    init {
        this.applyOverrideConfiguration(Configuration().apply {
            setLocale(MainApp.locale)
        })
    }

    private lateinit var rcvFriends: RecyclerView
    private lateinit var dbViewModel: DatabaseViewModel
    private lateinit var loading: CircularProgressIndicator
    private lateinit var searchInput: TextInputEditText
    private lateinit var tvNoFriend: FlexboxLayout
    private lateinit var tvNoResult: TextView
    private var friends: List<Friend>? = null
    private var flow = MutableStateFlow<String?>(null)
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend)
        sharedPreferences = getSharedPreferences(MAIN_SHARED_PREFERENCE, MODE_PRIVATE)
        window.statusBarColor = getColor(R.color.bg_main_activity)
        ViewCompat.getWindowInsetsController(window.decorView)
            ?.isAppearanceLightStatusBars = !MainApp.nightModeIsOn

        //get views
        rcvFriends = findViewById(R.id.rcvFriends)
        loading = findViewById(R.id.loading)
        searchInput = findViewById(R.id.searchInput)
        tvNoFriend = findViewById(R.id.tvNoMessageBox)
        tvNoResult = findViewById(R.id.tvNoResult)
        rcvFriends.layoutManager = LinearLayoutManager(this)

        dbViewModel = ViewModelProvider(
            this,
            DatabaseViewModelFactory(this)
        )[DatabaseViewModel::class.java]

        CoroutineScope(Dispatchers.Main).launch {
            loading.visibility = View.VISIBLE
            friends = dbViewModel.getFriends()
            if (friends!!.isEmpty()) {
                tvNoFriend.visibility = View.VISIBLE
            }
            rcvFriends.adapter = FriendAdapter(flow, dbViewModel, friends!!)
            loading.visibility = View.GONE
        }

        searchInput.addTextChangedListener { text ->
            if (text != null) {
                friends?.let {
                    if (it.isNotEmpty()) {
                        val resultSet = it.filter { friend ->
                            Functions.search(
                                it.map { f -> f.name }, text.toString()
                            ).contains(friend.name)
                        }
                        if (resultSet.isEmpty()) {
                            tvNoResult.visibility = View.VISIBLE
                            tvNoFriend.visibility = View.GONE
                        } else {
                            tvNoResult.visibility = View.GONE
                        }
                        rcvFriends.adapter = FriendAdapter(flow, dbViewModel, resultSet)
                    }
                }
            }
        }

        // listen when a friend are removed
        lifecycleScope.launch {
            flow.collectLatest {
                it?.let { friendId ->
                    val newFriendList = friends!!.filter { f ->
                        f.uid != friendId
                    }
                    if (newFriendList.isEmpty())
                        tvNoFriend.visibility = View.VISIBLE
                    rcvFriends.adapter = FriendAdapter(flow, dbViewModel, newFriendList)
                }
            }
        }
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

    fun back(view: View) = finish()

    fun toAddFriendActivity(view: View) {
        startActivity(Intent(this, AddFriendActivity::class.java))
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            val view = currentFocus
            if (view is TextInputEditText) {
                val msgInputBox = Rect()
                view.getGlobalVisibleRect(msgInputBox)
                if (
                    !msgInputBox.contains(ev.rawX.toInt(), ev.rawY.toInt())
                ) {
                    searchInput.clearFocus()
                    (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                        .hideSoftInputFromWindow(
                            searchInput.windowToken,
                            InputMethodManager.RESULT_UNCHANGED_SHOWN
                        )
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}
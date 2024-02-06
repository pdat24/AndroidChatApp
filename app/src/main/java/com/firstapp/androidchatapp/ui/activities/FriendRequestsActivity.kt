package com.firstapp.androidchatapp.ui.activities

import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firstapp.androidchatapp.MainApp
import com.firstapp.androidchatapp.R
import com.firstapp.androidchatapp.adapters.ReceivedRequestAdapter
import com.firstapp.androidchatapp.adapters.SentRequestAdapter
import com.firstapp.androidchatapp.models.FriendRequest
import com.firstapp.androidchatapp.repositories.UserManager
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModel
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModelFactory
import com.firstapp.androidchatapp.ui.viewmodels.MainViewModel
import com.firstapp.androidchatapp.utils.Constants.Companion.MAIN_SHARED_PREFERENCE
import com.firstapp.androidchatapp.utils.Constants.Companion.NIGHT_MODE_ON
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FriendRequestsActivity : AppCompatActivity() {
    companion object {
        var active = false
    }

    init {
        this.applyOverrideConfiguration(Configuration().apply {
            setLocale(MainApp.locale)
        })
    }

    private lateinit var noRequest: TextView
    private lateinit var rcvRequests: RecyclerView
    private lateinit var dbViewModel: DatabaseViewModel
    private lateinit var mainViewModel: MainViewModel
    private lateinit var loading: CircularProgressIndicator
    private lateinit var tabLayout: TabLayout
    private var sentRequests: List<FriendRequest>? = null
    private var receivedRequests: List<FriendRequest>? = null
    private var openInFirstTime = true
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend_requests)
        sharedPreferences = getSharedPreferences(MAIN_SHARED_PREFERENCE, MODE_PRIVATE)
        window.statusBarColor = getColor(R.color.bg_main_activity)
        ViewCompat.getWindowInsetsController(window.decorView)
            ?.isAppearanceLightStatusBars = !sharedPreferences.getBoolean(NIGHT_MODE_ON, false)

        // get views
        noRequest = findViewById(R.id.tvNoRequest)
        rcvRequests = findViewById(R.id.rcvRequests)
        loading = findViewById(R.id.loading)
        tabLayout = findViewById(R.id.tabLayout)

        rcvRequests.layoutManager = LinearLayoutManager(this)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                CoroutineScope(Dispatchers.Main).launch {
                    loading.visibility = View.VISIBLE
                    if (tab?.position == 0) {
                        showReceivedRequests()
                        mainViewModel.tabPosition = 0
                    } else if (tab?.position == 1) {
                        showSentRequests()
                        mainViewModel.tabPosition = 1
                    }
                    loading.visibility = View.GONE
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab?) {}

        })

        // view models
        dbViewModel = ViewModelProvider(
            this,
            DatabaseViewModelFactory(this)
        )[DatabaseViewModel::class.java]
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
    }

    override fun onStart() {
        super.onStart()
        CoroutineScope(Dispatchers.Main).launch {
            if (openInFirstTime) {
                showReceivedRequests()
                openInFirstTime = false
            }
        }
        tabLayout.selectTab(tabLayout.getTabAt(mainViewModel.tabPosition))
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

    private suspend fun showSentRequests() {
        sentRequests = sentRequests ?: dbViewModel.getUserRequests(UserManager.RequestType.SENT)
        sentRequests?.let {
            if (it.isEmpty())
                noRequest.visibility = View.VISIBLE
            else
                noRequest.visibility = View.GONE
            rcvRequests.adapter = SentRequestAdapter(dbViewModel, it)
        }
    }

    private suspend fun showReceivedRequests() {
        receivedRequests =
            receivedRequests ?: dbViewModel.getUserRequests(UserManager.RequestType.RECEIVED)
        receivedRequests?.let {
            if (it.isEmpty())
                noRequest.visibility = View.VISIBLE
            else
                noRequest.visibility = View.GONE
            rcvRequests.adapter = ReceivedRequestAdapter(dbViewModel, it)
        }
    }

    fun back(view: View) = finish()
}
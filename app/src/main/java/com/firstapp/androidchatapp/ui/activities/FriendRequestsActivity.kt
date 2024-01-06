package com.firstapp.androidchatapp.ui.activities

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firstapp.androidchatapp.R
import com.firstapp.androidchatapp.adapters.ReceivedRequestAdapter
import com.firstapp.androidchatapp.adapters.SentRequestAdapter
import com.firstapp.androidchatapp.repositories.UserManager
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModel
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModelFactory
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FriendRequestsActivity : AppCompatActivity() {

    private lateinit var noSentReqView: TextView
    private lateinit var noReceivedReqView: TextView
    private lateinit var rcvSentRequests: RecyclerView
    private lateinit var rcvReceivedRequests: RecyclerView
    private lateinit var dbViewModel: DatabaseViewModel
    private lateinit var receivedReqLoading: CircularProgressIndicator
    private lateinit var sentReqLoading: CircularProgressIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend_requests)
        // get views
        noSentReqView = findViewById(R.id.tvNoSentReq)
        noReceivedReqView = findViewById(R.id.tvNoReceivedReq)
        rcvSentRequests = findViewById(R.id.rcvSentRequests)
        rcvReceivedRequests = findViewById(R.id.rcvReceivedRequests)
        sentReqLoading = findViewById(R.id.sentReqLoading)
        receivedReqLoading = findViewById(R.id.receivedReqLoading)

        dbViewModel = ViewModelProvider(
            this,
            DatabaseViewModelFactory(this)
        )[DatabaseViewModel::class.java]

        showSentRequests()
        showReceivedRequests()
    }

    private fun showSentRequests() {
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                sentReqLoading.visibility = View.VISIBLE
                val sentRequests = dbViewModel.getUserRequests(UserManager.RequestType.SENT)
                if (sentRequests.isEmpty())
                    noSentReqView.visibility = View.VISIBLE
                else
                    noSentReqView.visibility = View.GONE
                rcvSentRequests.adapter = SentRequestAdapter(dbViewModel, sentRequests)
                rcvSentRequests.layoutManager = LinearLayoutManager(this@FriendRequestsActivity)
                sentReqLoading.visibility = View.GONE
            }
        }
    }

    private fun showReceivedRequests() {
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                receivedReqLoading.visibility = View.VISIBLE
                val receivedRequests = dbViewModel.getUserRequests(UserManager.RequestType.RECEIVED)
                if (receivedRequests.isEmpty())
                    noReceivedReqView.visibility = View.GONE
                else
                    noReceivedReqView.visibility = View.VISIBLE
                rcvReceivedRequests.adapter = ReceivedRequestAdapter(dbViewModel, receivedRequests)
                rcvReceivedRequests.layoutManager = LinearLayoutManager(this@FriendRequestsActivity)
                receivedReqLoading.visibility = View.GONE
            }
        }
    }

    fun back(view: View) = finish()
}
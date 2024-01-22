package com.firstapp.androidchatapp.ui.activities

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firstapp.androidchatapp.MainApp
import com.firstapp.androidchatapp.R
import com.firstapp.androidchatapp.adapters.SearchResultAdapter
import com.firstapp.androidchatapp.models.Friend
import com.firstapp.androidchatapp.models.FriendRequest
import com.firstapp.androidchatapp.repositories.UserManager
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModel
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModelFactory
import com.firstapp.androidchatapp.utils.Constants.Companion.AVATAR_URI
import com.firstapp.androidchatapp.utils.Constants.Companion.NAME
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AddFriendActivity : AppCompatActivity() {
    companion object {
        var active = false
    }

    private val itemMutationFlow = MutableStateFlow(false)
    private lateinit var dbViewModel: DatabaseViewModel
    private lateinit var input: TextInputEditText
    private lateinit var searchByGroup: RadioGroup
    private lateinit var noResultView: TextView
    private lateinit var rcvSearchResult: RecyclerView
    private lateinit var loading: CircularProgressIndicator
    private var searchResults = MutableLiveData<List<FriendRequest>>()
    private var searchJob: Job? = null
    private var filterJob: Job? = null
    private var searchFilter = MutableLiveData(SearchBy.NAME)
    private var searchContent = ""
    @Volatile
    private var friends: List<Friend>? = null
    private var sentRequests: List<FriendRequest>? = null
    private var receivedRequests: List<FriendRequest>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_friend)
        window.statusBarColor = getColor(R.color.dialog_bg)

        // get views
        input = findViewById(R.id.input)
        searchByGroup = findViewById(R.id.rgSearchBy)
        noResultView = findViewById(R.id.tvNoResult)
        loading = findViewById(R.id.loadingIndicator)
        rcvSearchResult = findViewById(R.id.rcvSearchResults)
        searchByGroup.check(R.id.rbName)

        rcvSearchResult.layoutManager = LinearLayoutManager(this)

        dbViewModel = ViewModelProvider(
            this,
            DatabaseViewModelFactory(this)
        )[DatabaseViewModel::class.java]

        // handle search
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s != null) {
                    searchJob?.cancel()
                    searchJob = lifecycleScope.launch {
                        searchResults.postValue(emptyList())
                        val text = s.toString()
                        if (searchFilter.value == SearchBy.NAME)
                            searchUsersByName(text)
                        else
                            searchUserByID(text)
                        searchContent = text
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        searchByGroup.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == R.id.rbName)
                searchFilter.postValue(SearchBy.NAME)
            else if (checkedId == R.id.rbID)
                searchFilter.postValue(SearchBy.ID)
        }
    }

    override fun onStart() {
        super.onStart()
        CoroutineScope(Dispatchers.Main).launch {
            itemMutationFlow.collectLatest {
                refreshCollections().join()
                println("Called")
                rcvSearchResult.adapter = SearchResultAdapter(
                    itemMutationFlow,
                    friends = friends ?: emptyList(),
                    sentRequests = sentRequests ?: emptyList(),
                    receivedRequests = receivedRequests ?: emptyList(),
                    dbViewModel = dbViewModel,
                    results = searchResults.value ?: emptyList(),
                )
            }
        }

        // observe search results change
        searchResults.observe(this) {
            rcvSearchResult.adapter = SearchResultAdapter(
                itemMutationFlow,
                friends = friends ?: emptyList(),
                sentRequests = sentRequests ?: emptyList(),
                receivedRequests = receivedRequests ?: emptyList(),
                dbViewModel = dbViewModel,
                results = it
            )
        }

        // observe search filter change
        searchFilter.observe(this) {
            filterJob?.cancel()
            filterJob = lifecycleScope.launch {
                searchResults.postValue(emptyList())
                if (it == SearchBy.NAME)
                    searchUsersByName(searchContent)
                else
                    searchUserByID(searchContent)
            }
        }
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

    private fun refreshCollections() = lifecycleScope.launch {
        friends = dbViewModel.getFriends()
        sentRequests = dbViewModel.getUserRequests(UserManager.RequestType.SENT)
        receivedRequests = dbViewModel.getUserRequests(UserManager.RequestType.RECEIVED)
    }

    private suspend fun searchUsersByName(name: String) {
        noResultView.visibility = View.GONE
        if (name.isEmpty())
            searchResults.postValue(emptyList())
        else {
            loading.visibility = View.VISIBLE
            val res = mutableListOf<FriendRequest>()
            val users = dbViewModel.getUsersByName(name)
            refreshCollections().join()
            for (i in users) {
                res.add(
                    FriendRequest(
                        uid = i.id,
                        name = i[NAME] as String,
                        avatarURI = i[AVATAR_URI] as String
                    )
                )
            }
            if (res.isNotEmpty())
                searchResults.postValue(res)
            else
                noResultView.visibility = View.VISIBLE
        }
        loading.visibility = View.GONE
    }

    private suspend fun searchUserByID(id: String) {
        noResultView.visibility = View.GONE
        if (id.isEmpty())
            searchResults.postValue(emptyList())
        else {
            loading.visibility = View.VISIBLE
            try {
                val user = dbViewModel.getUserById(id)
                refreshCollections().join()
                searchResults.postValue(
                    listOf(
                        FriendRequest(
                            uid = user.id,
                            name = user[NAME] as String,
                            avatarURI = user[AVATAR_URI] as String
                        )
                    )
                )
            } catch (e: Exception) {
                noResultView.visibility = View.VISIBLE
            }
            loading.visibility = View.GONE
        }
    }

    fun back(view: View) = finish()

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            val view = currentFocus
            if (view is TextInputEditText) {
                val msgInputBox = Rect()
                view.getGlobalVisibleRect(msgInputBox)
                if (
                    !msgInputBox.contains(ev.rawX.toInt(), ev.rawY.toInt())
                ) {
                    input.clearFocus()
                    (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                        .hideSoftInputFromWindow(
                            input.windowToken,
                            InputMethodManager.RESULT_UNCHANGED_SHOWN
                        )
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    enum class SearchBy {
        NAME, ID
    }
}
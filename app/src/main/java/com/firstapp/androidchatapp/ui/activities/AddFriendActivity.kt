package com.firstapp.androidchatapp.ui.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firstapp.androidchatapp.R
import com.firstapp.androidchatapp.adapters.SearchResultAdapter
import com.firstapp.androidchatapp.models.FriendRequest
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModel
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModelFactory
import com.firstapp.androidchatapp.utils.Constants.Companion.AVATAR_URI
import com.firstapp.androidchatapp.utils.Constants.Companion.NAME
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AddFriendActivity : AppCompatActivity() {

    private lateinit var dbViewModel: DatabaseViewModel
    private lateinit var input: TextInputEditText
    private lateinit var searchByGroup: RadioGroup
    private lateinit var noResultView: TextView
    private lateinit var rcvSearchResult: RecyclerView
    private lateinit var loadingIndicator: CircularProgressIndicator
    private var searchResults = MutableLiveData<List<FriendRequest>>()
    private var searchJob: Job? = null
    private var filterJob: Job? = null
    private var searchFilter = MutableLiveData(SearchBy.NAME)
    private var searchContent = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_friend)
        // get views
        input = findViewById(R.id.input)
        searchByGroup = findViewById(R.id.rgSearchBy)
        noResultView = findViewById(R.id.tvNoResult)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        rcvSearchResult = findViewById(R.id.rcvSearchResults)
        searchByGroup.check(R.id.rbName)

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

        //
        searchByGroup.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == R.id.rbName)
                searchFilter.postValue(SearchBy.NAME)
            else if (checkedId == R.id.rbID)
                searchFilter.postValue(SearchBy.ID)
        }
    }

    override fun onStart() {
        super.onStart()
        // observe search results change
        searchResults.observe(this) {
            rcvSearchResult.adapter = SearchResultAdapter(dbViewModel, it)
            rcvSearchResult.layoutManager = LinearLayoutManager(this)
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

    private suspend fun searchUsersByName(name: String) {
        noResultView.visibility = View.GONE
        if (name.isEmpty())
            searchResults.postValue(emptyList())
        else {
            loadingIndicator.visibility = View.VISIBLE
            val res = mutableListOf<FriendRequest>()
            val tmp = dbViewModel.getUsersByName(name)
            for (i in tmp) {
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
        loadingIndicator.visibility = View.GONE
    }

    private suspend fun searchUserByID(id: String) {
        noResultView.visibility = View.GONE
        if (id.isEmpty())
            searchResults.postValue(emptyList())
        else {
            loadingIndicator.visibility = View.VISIBLE
            try {
                val user = dbViewModel.getUserById(id)
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
            loadingIndicator.visibility = View.GONE
        }
    }

    fun back(view: View) = finish()

    enum class SearchBy {
        NAME, ID
    }
}
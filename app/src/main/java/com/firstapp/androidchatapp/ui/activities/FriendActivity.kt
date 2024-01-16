package com.firstapp.androidchatapp.ui.activities

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firstapp.androidchatapp.R
import com.firstapp.androidchatapp.adapters.FriendAdapter
import com.firstapp.androidchatapp.models.Friend
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModel
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModelFactory
import com.firstapp.androidchatapp.utils.Functions
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FriendActivity : AppCompatActivity() {

    private lateinit var rcvFriends: RecyclerView
    private lateinit var dbViewModel: DatabaseViewModel
    private lateinit var loading: CircularProgressIndicator
    private lateinit var input: TextInputEditText
    private var friends: List<Friend>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend)
        window.statusBarColor = getColor(R.color.dialog_bg)

        //get views
        rcvFriends = findViewById(R.id.rcvFriends)
        loading = findViewById(R.id.loading)
        input = findViewById(R.id.searchInput)
        rcvFriends.layoutManager = LinearLayoutManager(this)

        dbViewModel = ViewModelProvider(
            this,
            DatabaseViewModelFactory(this)
        )[DatabaseViewModel::class.java]

        CoroutineScope(Dispatchers.Main).launch {
            loading.visibility = View.VISIBLE
            friends = dbViewModel.getFriends()
            rcvFriends.adapter = FriendAdapter(dbViewModel, friends!!)
            loading.visibility = View.GONE
        }

        input.addTextChangedListener { text ->
            if (text != null) {
                friends?.let {
                    rcvFriends.adapter = FriendAdapter(dbViewModel, it.filter { friend ->
                        Functions.search(
                            it.map { f -> f.name }, text.toString()
                        ).contains(friend.name)
                    })
                }
            }
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
}
package com.firstapp.androidchatapp.ui.activities

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firstapp.androidchatapp.R
import com.firstapp.androidchatapp.adapters.FriendAdapter
import com.firstapp.androidchatapp.adapters.MessageBoxAdapter
import com.firstapp.androidchatapp.localdb.entities.UserInfo
import com.firstapp.androidchatapp.models.Friend
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModel
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModelFactory
import com.firstapp.androidchatapp.ui.viewmodels.MainViewModel
import com.firstapp.androidchatapp.utils.Constants.Companion.AVATAR_URI
import com.firstapp.androidchatapp.utils.Constants.Companion.NAME
import com.firstapp.androidchatapp.utils.Functions
import com.firstapp.androidchatapp.utils.Functions.Companion.throwUserNotLoginError
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private lateinit var rcvMessageBoxes: RecyclerView
    private lateinit var rcvOnlineFriends: RecyclerView
    private lateinit var dbViewModel: DatabaseViewModel
    private lateinit var mainViewModel: MainViewModel
    private lateinit var fragMenu: FragmentContainerView
    private lateinit var msgBoxLoading: CircularProgressIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.statusBarColor = getColor(R.color.black)
        // get views
        rcvMessageBoxes = findViewById(R.id.rcvMsgBoxList)
        rcvOnlineFriends = findViewById(R.id.rcvOnlineFriendList)
        fragMenu = findViewById(R.id.fragMenu)
        msgBoxLoading = findViewById(R.id.msgBoxLoading)

        // view models
        dbViewModel = ViewModelProvider(
            this,
            DatabaseViewModelFactory(this)
        )[DatabaseViewModel::class.java]
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                msgBoxLoading.visibility = View.VISIBLE
                val messageBoxes = dbViewModel.getMessageBoxes()
                rcvMessageBoxes.adapter = MessageBoxAdapter(messageBoxes)
                rcvMessageBoxes.layoutManager = LinearLayoutManager(this@MainActivity)
                msgBoxLoading.visibility = View.GONE
            }
        }

        val friends = listOf(
            Friend(
                uid = "",
                name = "Pham Quoc Dat",
                avatarURI = "https://firebasestorage.googleapis.com/v0/b/androidchatapp-6df26.appspot.com/o/avatars%2Favatar_4.jpg?alt=media&token=2f630629-be82-41c6-86e6-6df69d350ff5",
                conversationID = ""
            ),
            Friend(
                uid = "",
                name = "Pham Quoc Dat",
                avatarURI = "https://firebasestorage.googleapis.com/v0/b/androidchatapp-6df26.appspot.com/o/avatars%2Favatar_4.jpg?alt=media&token=2f630629-be82-41c6-86e6-6df69d350ff5",
                conversationID = ""
            ),
            Friend(
                uid = "",
                name = "Pham Quoc Dat",
                avatarURI = "https://firebasestorage.googleapis.com/v0/b/androidchatapp-6df26.appspot.com/o/avatars%2Favatar_4.jpg?alt=media&token=2f630629-be82-41c6-86e6-6df69d350ff5",
                conversationID = ""
            ),
            Friend(
                uid = "",
                name = "Pham Quoc Dat",
                avatarURI = "https://firebasestorage.googleapis.com/v0/b/androidchatapp-6df26.appspot.com/o/avatars%2Favatar_4.jpg?alt=media&token=2f630629-be82-41c6-86e6-6df69d350ff5",
                conversationID = ""
            ),
            Friend(
                uid = "",
                name = "Pham Quoc Dat",
                avatarURI = "https://firebasestorage.googleapis.com/v0/b/androidchatapp-6df26.appspot.com/o/avatars%2Favatar_4.jpg?alt=media&token=2f630629-be82-41c6-86e6-6df69d350ff5",
                conversationID = ""
            )
        )
        rcvOnlineFriends.adapter = FriendAdapter(friends)
        rcvOnlineFriends.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)

        dbViewModel.getCachedUserInfo().observe(this) {
            if (it?.name == null)
                cacheUserOnLocalDB()
            else {
                Glide.with(this).load(it.avatarURI)
                    .into(fragMenu.findViewById<ImageView>(R.id.ivAvatar))
                fragMenu.findViewById<TextView>(R.id.tvName).text = it.name
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            mainViewModel.openMenu.collectLatest { open ->
                fragMenu.visibility = if (open) View.VISIBLE else View.GONE
            }
        }
    }

    private fun cacheUserOnLocalDB() {
        lifecycleScope.launch {
            val signedInUser = firebaseAuth.currentUser
            if (signedInUser == null)
                throwUserNotLoginError()
            val user = dbViewModel.getUserById(userID = signedInUser!!.uid)
            dbViewModel.cacheUser(
                UserInfo(
                    name = user.getString(NAME)!!,
                    avatarURI = user.getString(AVATAR_URI)!!
                )
            )
        }
    }

    fun openMenu(btn: View) {
        Functions.scaleDownUpAnimation(btn)
        fragMenu.visibility = View.VISIBLE
        lifecycleScope.launch {
            mainViewModel.openMenu.emit(true)
        }
    }
}
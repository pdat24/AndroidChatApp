package com.firstapp.androidchatapp.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firstapp.androidchatapp.R
import com.firstapp.androidchatapp.adapters.MessageBoxAdapter
import com.firstapp.androidchatapp.localdb.entities.UserInfo
import com.firstapp.androidchatapp.models.MessageBox
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModel
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModelFactory
import com.firstapp.androidchatapp.ui.viewmodels.MainViewModel
import com.firstapp.androidchatapp.utils.Constants.Companion.AVATAR_URI
import com.firstapp.androidchatapp.utils.Constants.Companion.NAME
import com.firstapp.androidchatapp.utils.Functions.Companion.throwUserNotLoginError
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private lateinit var rcvMessageBoxes: RecyclerView
    private lateinit var dbViewModel: DatabaseViewModel
    private lateinit var mainViewModel: MainViewModel
    private lateinit var avatarView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.statusBarColor = getColor(R.color.black)
        // get views
        rcvMessageBoxes = findViewById(R.id.rcvMsgBoxList)
        avatarView = findViewById(R.id.ivUserAvatar)

        // view models
        dbViewModel = ViewModelProvider(
            this,
            DatabaseViewModelFactory(this)
        )[DatabaseViewModel::class.java]
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        val messageBoxes = listOf(
            MessageBox(
                "https://firebasestorage.googleapis.com/v0/b/androidchatapp-6df26.appspot.com/o/avatars%2Favatar_1.jpg?alt=media&token=f7eec50b-a91c-4e76-a799-1880d8faba74",
                "Pham Quoc Dat",
                4,
                ""
            ),
            MessageBox(
                "https://firebasestorage.googleapis.com/v0/b/androidchatapp-6df26.appspot.com/o/avatars%2Favatar_10.jpg?alt=media&token=28899241-e0c9-4e7e-b344-afa9dd6e2548",
                "Cao Thi Thuy",
                1,
                "Let's start the conversations, That's great how about you!",
            ),
            MessageBox(
                "https://firebasestorage.googleapis.com/v0/b/androidchatapp-6df26.appspot.com/o/avatars%2Favatar_4.jpg?alt=media&token=2f630629-be82-41c6-86e6-6df69d350ff5",
                "Bui Anh Duong",
                14,
                "Welcome to Chat with me",
            )
        )
        rcvMessageBoxes.adapter = MessageBoxAdapter(messageBoxes)
        rcvMessageBoxes.layoutManager = LinearLayoutManager(this)
        dbViewModel.getCachedUserInfo().observe(this) {
            if (it?.name == null)
                cacheUserOnLocalDB()
            else {
                Glide.with(this).load(it.avatarURI).into(avatarView)
            }
        }
    }

    private fun cacheUserOnLocalDB() {
        lifecycleScope.launch {
            val signedInUser = firebaseAuth.currentUser
            if (signedInUser == null)
                throwUserNotLoginError()
            val user = dbViewModel.getUser(userID = signedInUser!!.uid)
            dbViewModel.cacheUser(
                UserInfo(
                    name = user.getString(NAME)!!,
                    avatarURI = user.getString(AVATAR_URI)!!
                )
            )
        }
    }

    fun toSettingActivity(view: View) {
        startActivity(
            Intent(this, SettingsActivity::class.java)
        )
    }
}
package com.firstapp.androidchatapp.ui.activities

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firstapp.androidchatapp.MainApp
import com.firstapp.androidchatapp.R
import com.firstapp.androidchatapp.adapters.MessageBoxAdapter
import com.firstapp.androidchatapp.adapters.OnlineFriendAdapter
import com.firstapp.androidchatapp.localdb.entities.UserInfo
import com.firstapp.androidchatapp.models.MessageBoxesList
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModel
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModelFactory
import com.firstapp.androidchatapp.ui.viewmodels.MainViewModel
import com.firstapp.androidchatapp.utils.Constants.Companion.AVATAR_URI
import com.firstapp.androidchatapp.utils.Constants.Companion.GROUP_MESSAGES
import com.firstapp.androidchatapp.utils.Constants.Companion.NAME
import com.firstapp.androidchatapp.utils.Constants.Companion.PERMISSION_REQUEST_CODE
import com.firstapp.androidchatapp.utils.Constants.Companion.PREVIEW_MESSAGE
import com.firstapp.androidchatapp.utils.Constants.Companion.SENDER_ID
import com.firstapp.androidchatapp.utils.Constants.Companion.TIME
import com.firstapp.androidchatapp.utils.Constants.Companion.USERS_COLLECTION_PATH
import com.firstapp.androidchatapp.utils.Functions
import com.firstapp.androidchatapp.utils.Functions.Companion.throwUserNotLoginError
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {
    companion object {
        var active = false
    }

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val currentUserUID = FirebaseAuth.getInstance().currentUser!!.uid
    private val usersDB =
        FirebaseFirestore.getInstance().collection(USERS_COLLECTION_PATH)
    private lateinit var rcvMessageBoxes: RecyclerView
    private lateinit var rcvOnlineFriends: RecyclerView
    private lateinit var onlineNumberView: TextView
    private lateinit var fragMenu: FragmentContainerView
    private lateinit var msgBoxLoading: CircularProgressIndicator
    private lateinit var onlineFriendsLoading: CircularProgressIndicator
    private lateinit var addFriendBtn: FrameLayout
    lateinit var dbViewModel: DatabaseViewModel
    lateinit var mainViewModel: MainViewModel
    val username = MutableLiveData<String>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.statusBarColor = getColor(R.color.dialog_bg)
        requestNotificationPermission()

        // get views
        rcvMessageBoxes = findViewById(R.id.rcvMsgBoxList)
        rcvOnlineFriends = findViewById(R.id.rcvOnlineFriendList)
        fragMenu = findViewById(R.id.fragMenu)
        msgBoxLoading = findViewById(R.id.msgBoxLoading)
        onlineFriendsLoading = findViewById(R.id.onlineFriendsLoading)
        onlineNumberView = findViewById(R.id.tvOnlineNumber)
        addFriendBtn = findViewById(R.id.btnAddFriend)

        rcvMessageBoxes.layoutManager = LinearLayoutManager(this)
        addFriendBtn.setOnClickListener {
            lifecycleScope.launch {
                val token = FirebaseMessaging.getInstance().token.await()
                FirebaseMessaging.getInstance().send(
                    RemoteMessage.Builder(token)
                        .setMessageId("tmp")
                        .addData("title", "Test title")
                        .addData("body", "Some content...")
                        .build()
                )
            }
        }
        // view models
        dbViewModel = ViewModelProvider(
            this,
            DatabaseViewModelFactory(this)
        )[DatabaseViewModel::class.java]
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                onlineFriendsLoading.visibility = View.VISIBLE
                val friends = dbViewModel.getOnlineFriends()
                onlineNumberView.text = friends.size.toString()
                rcvOnlineFriends.adapter = OnlineFriendAdapter(friends)
                rcvOnlineFriends.layoutManager =
                    LinearLayoutManager(this@MainActivity, RecyclerView.HORIZONTAL, false)
                onlineFriendsLoading.visibility = View.GONE
            }
        }

        dbViewModel.getCachedUserInfo().observe(this) {
            if (it?.name == null)
                cacheUserOnLocalDB()
            else {
                Glide.with(this).load(it.avatarURI)
                    .into(fragMenu.findViewById<ImageView>(R.id.ivAvatar))
                username.postValue(it.name)
                fragMenu.findViewById<TextView>(R.id.tvName).text = it.name
            }
        }
    }

    override fun onStart() {
        super.onStart()
        observeDrawerMenuState()
        observeMessageBoxesChanges()
        observeOnlineFriends()
    }

    override fun onResume() {
        super.onResume()
        active = true
        synchronizeCachedMessageBoxes()
        MainApp.cancelPrepareOfflineJob(this)
        MainApp.startOnlineStatus()
    }

    override fun onStop() {
        super.onStop()
        active = false
        MainApp.prepareOffline(this)
    }

    private fun synchronizeCachedMessageBoxes() = lifecycleScope.launch {
        if (dbViewModel.getCachedMessageBoxNumber() < 1)
            msgBoxLoading.visibility = View.VISIBLE
        val messageBoxes = dbViewModel.getMessageBoxes(
            dbViewModel.getMessageBoxList()
        ).map {
            val con = dbViewModel.getConversation(it.conversationID)
            val friend = dbViewModel.getUserById(it.friendUID)

            // set preview message
            val tmp = (con[GROUP_MESSAGES] as List<*>)
            var previewMsg = con[PREVIEW_MESSAGE] as String
            if (tmp.isNotEmpty()) {
                val lastGroup = tmp.last() as HashMap<*, *>
                if (lastGroup[SENDER_ID] == firebaseAuth.currentUser!!.uid)
                    previewMsg = "You: $previewMsg"
            }

            it.name = friend[NAME] as String
            it.avatarURI = friend[AVATAR_URI] as String
            it.previewMessage = previewMsg
            it.time = con[TIME] as Long
            it
        }

        // cache message boxes number
        dbViewModel.updateMessageBoxList(MessageBoxesList(messageBoxes))
        dbViewModel.cacheMessageBoxNumber(messageBoxes.size)
        dbViewModel.cacheMessageBoxes(messageBoxes)
        msgBoxLoading.visibility = View.GONE
    }

    private fun observeOnlineFriends() =
        usersDB.document(currentUserUID).addSnapshotListener { value, error ->
            lifecycleScope.launch {
                if (value != null) {
                    val onlineFriends = dbViewModel.getOnlineFriends(value)
                    withContext(Dispatchers.Main) {
                        rcvOnlineFriends.adapter = OnlineFriendAdapter(onlineFriends)
                    }
                }
            }
        }

    private fun observeMessageBoxesChanges() =
        dbViewModel.getCachedMessageBoxes().observe(this@MainActivity) {
            lifecycleScope.launch {
                // update the number of cached message boxes
                dbViewModel.cacheMessageBoxNumber(it.size)
                withContext(Dispatchers.Main) {
                    rcvMessageBoxes.adapter =
                        MessageBoxAdapter(dbViewModel, it)
                }
            }
        }

    private fun observeDrawerMenuState() =
        mainViewModel.openMenu.observe(this) { isOpen ->
            fragMenu.visibility = if (isOpen) View.VISIBLE else View.GONE
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
        mainViewModel.openMenu.postValue(true)
    }

    private fun requestNotificationPermission() {
        if (!EasyPermissions.hasPermissions(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            )
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                EasyPermissions.requestPermissions(
                    this,
                    "You'll not receive any notifications if deny this permission!",
                    PERMISSION_REQUEST_CODE,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(
                this,
                listOf(Manifest.permission.POST_NOTIFICATIONS)
            )
        ) {
            SettingsDialog.Builder(this).build().show()
        } else {
            requestNotificationPermission()
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {}

//    override fun onDestroy() {
//        super.onDestroy()
//        startService(Intent(this, OnlineService::class.java).apply {
//            action = ACTION_OFFLINE
//        })
//    }
}
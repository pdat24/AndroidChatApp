package com.firstapp.androidchatapp.ui.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firstapp.androidchatapp.MainApp
import com.firstapp.androidchatapp.R
import com.firstapp.androidchatapp.adapters.MessageBoxAdapter
import com.firstapp.androidchatapp.adapters.OnlineFriendAdapter
import com.firstapp.androidchatapp.localdb.entities.UserInfo
import com.firstapp.androidchatapp.models.MessageBox
import com.firstapp.androidchatapp.models.MessageBoxesList
import com.firstapp.androidchatapp.repositories.UserManager
import com.firstapp.androidchatapp.services.NotificationsService
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModel
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModelFactory
import com.firstapp.androidchatapp.ui.viewmodels.MainViewModel
import com.firstapp.androidchatapp.utils.Constants.Companion.AVATAR_URI
import com.firstapp.androidchatapp.utils.Constants.Companion.GROUP_MESSAGES
import com.firstapp.androidchatapp.utils.Constants.Companion.MAIN_SHARED_PREFERENCE
import com.firstapp.androidchatapp.utils.Constants.Companion.NAME
import com.firstapp.androidchatapp.utils.Constants.Companion.NOTIFICATION_ON
import com.firstapp.androidchatapp.utils.Constants.Companion.PERMISSION_REQUEST_CODE
import com.firstapp.androidchatapp.utils.Constants.Companion.PREVIEW_MESSAGE
import com.firstapp.androidchatapp.utils.Constants.Companion.SENDER_ID
import com.firstapp.androidchatapp.utils.Constants.Companion.TIME
import com.firstapp.androidchatapp.utils.Constants.Companion.USERS_COLLECTION_PATH
import com.firstapp.androidchatapp.utils.Functions
import com.firstapp.androidchatapp.utils.Functions.Companion.throwUserNotLoginError
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {
    companion object {
        var active = false
    }

    init {
        this.applyOverrideConfiguration(Configuration().apply {
            setLocale(MainApp.locale)
        })
    }

    private var cachedLocale: Locale? = null
    private var cachedNightModeState = false
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val currentUserUID = FirebaseAuth.getInstance().currentUser!!.uid
    private val usersDB =
        FirebaseFirestore.getInstance().collection(USERS_COLLECTION_PATH)
    private lateinit var rcvMessageBoxes: RecyclerView
    private lateinit var rcvOnlineFriends: RecyclerView
    private lateinit var friendOnlineNumber: TextView
    private lateinit var fragMenu: FragmentContainerView
    private lateinit var msgBoxLoading: CircularProgressIndicator
    private lateinit var onlineFriendsLoading: CircularProgressIndicator
    lateinit var dbViewModel: DatabaseViewModel
    lateinit var mainViewModel: MainViewModel
    val username = MutableLiveData<String>(null)
    private lateinit var sharedPreferences: SharedPreferences
    private var messageBoxes: List<MessageBox>? = null
    private val searchInput: TextInputEditText by lazy {
        findViewById(R.id.searchInput)
    }
    private val tvNoResult: TextView by lazy {
        findViewById(R.id.tvNoResult)
    }
    private val tvNoMessageBox: FlexboxLayout by lazy {
        findViewById(R.id.tvNoMessageBox)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sharedPreferences = getSharedPreferences(MAIN_SHARED_PREFERENCE, MODE_PRIVATE)
        window.statusBarColor = getColor(R.color.bg_main_activity)
        ViewCompat.getWindowInsetsController(window.decorView)
            ?.isAppearanceLightStatusBars = !MainApp.nightModeIsOn
        requestNotificationPermission()
        cachedLocale = MainApp.locale
        cachedNightModeState = MainApp.nightModeIsOn

        // get views
        rcvMessageBoxes = findViewById(R.id.rcvMsgBoxList)
        rcvOnlineFriends = findViewById(R.id.rcvOnlineFriendList)
        fragMenu = findViewById(R.id.fragMenu)
        msgBoxLoading = findViewById(R.id.msgBoxLoading)
        onlineFriendsLoading = findViewById(R.id.onlineFriendsLoading)
        friendOnlineNumber = findViewById(R.id.tvOnlineNumber)

        rcvMessageBoxes.layoutManager = LinearLayoutManager(this)
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
                friendOnlineNumber.text = friends.size.toString()
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
                    .into(fragMenu.findViewById(R.id.ivAvatar))
                username.postValue(it.name)
                fragMenu.findViewById<TextView>(R.id.tvName).text = it.name
            }
        }
    }

    override fun onStart() {
        super.onStart()
        active = true
        observeDrawerMenuState()
        observeMessageBoxesChanges()
        observeOnlineFriends()
        observeConversationsUpdates()
        handleSearchMessageBox()
        handleSwipeMessageBox()
        turnOnNotificationIfIsSet()
        synchronizeCachedReceivedFriendRequests()
    }

    override fun onResume() {
        super.onResume()
        active = true
        MainApp.cancelPrepareOfflineJob(this)
        MainApp.startOnlineStatus(this)

        // only need reload activity once
        // if it reloaded before it doesn't need any more
        if (cachedNightModeState != MainApp.nightModeIsOn) {
            finish()
            overridePendingTransition(0, 0)
            AppCompatDelegate.setDefaultNightMode(
                if (MainApp.nightModeIsOn)
                    AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
            startActivity(intent)
        } else if (MainApp.locale != cachedLocale) {
            Functions.changeLanguage(this, MainApp.locale!!.language)
        }
    }

    override fun onStop() {
        super.onStop()
        active = false
        MainApp.prepareOffline(this)
    }

    private fun turnOnNotificationIfIsSet() {
        if (
            sharedPreferences.getBoolean(NOTIFICATION_ON, true) &&
            !Functions.isServiceRunning(this, NotificationsService::class.java)
        )
            startService(Intent(this, NotificationsService::class.java))
    }

    private fun handleSwipeMessageBox() {
        val limitedScrollX = convertDpToPx(64f, this)
        var scrollDirection = Direction.LEFT

        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.ACTION_STATE_IDLE,
            ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    var scrollOffset = (-dX).toInt()
                    if (isCurrentlyActive) {
                        scrollDirection = Direction.LEFT
                        // when user scroll to left and scroll gap >= limitedScrollX
                        // then scroll itemView to limitedScrollX
                        if (scrollOffset >= limitedScrollX && scrollDirection == Direction.LEFT) {
                            scrollOffset = limitedScrollX
                        }
                    } else {
                        // handle when the user releases their finger
                        scrollOffset = if (scrollOffset < limitedScrollX) {
                            0
                        } else {
                            limitedScrollX
                        }
                    }
                    viewHolder.itemView.scrollTo(scrollOffset, 0)
                }
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(rcvMessageBoxes)
    }

    private fun convertDpToPx(dp: Float, context: Context): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    private fun handleSearchMessageBox() {
        searchInput.addTextChangedListener { text ->
            if (text != null) {
                messageBoxes?.let {
                    if (it.isNotEmpty()) {
                        val resultSet = it.filter { box ->
                            Functions.search(
                                it.map { f -> f.name }, text.toString()
                            ).contains(box.name)
                        }
                        if (resultSet.isEmpty()) {
                            tvNoResult.visibility = View.VISIBLE
                            tvNoMessageBox.visibility = View.GONE
                        } else {
                            tvNoResult.visibility = View.GONE
                        }
                        rcvMessageBoxes.adapter =
                            MessageBoxAdapter(dbViewModel, resultSet)
                    }
                }
            }
        }
    }

    private fun synchronizeCachedReceivedFriendRequests() = lifecycleScope.launch {
        val requests = dbViewModel.getUserRequests(UserManager.RequestType.RECEIVED)
        dbViewModel.clearReceivedFriendRequests()
        for (req in requests) {
            dbViewModel.cacheReceivedFriendRequest(req)
        }
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
            val groupMessages = (con[GROUP_MESSAGES] as List<*>)
            var previewMsg = con[PREVIEW_MESSAGE] as String
            if (groupMessages.isNotEmpty()) {
                val latestGroup = groupMessages.first() as HashMap<*, *>
                if (latestGroup[SENDER_ID] == firebaseAuth.currentUser!!.uid)
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
        dbViewModel.removeCachedMessageBoxes()
        dbViewModel.cacheMessageBoxes(messageBoxes)
        msgBoxLoading.visibility = View.GONE
    }

    private fun observeOnlineFriends() =
        usersDB.document(currentUserUID).addSnapshotListener { value, _ ->
            value?.let {
                lifecycleScope.launch {
                    val onlineFriends = dbViewModel.getOnlineFriends()
                    withContext(Dispatchers.Main) {
                        rcvOnlineFriends.adapter = OnlineFriendAdapter(onlineFriends)
                        friendOnlineNumber.text = onlineFriends.size.toString()
                    }
                }
            }
        }

    private fun observeConversationsUpdates() {
        Functions.observeConversationsChanges {
            if (active)
                synchronizeCachedMessageBoxes()
        }
    }

    private fun observeMessageBoxesChanges() =
        dbViewModel.getCachedMessageBoxes().observe(this@MainActivity) {
            println("Fucking: ${it.size}")
            lifecycleScope.launch {
                // update the number of cached message boxes
                dbViewModel.cacheMessageBoxNumber(it.size)
                messageBoxes = it
                withContext(Dispatchers.Main) {
                    if (it.isEmpty())
                        tvNoMessageBox.visibility = View.VISIBLE
                    else
                        tvNoMessageBox.visibility = View.GONE
                    rcvMessageBoxes.adapter = MessageBoxAdapter(dbViewModel, it)
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

    fun toAddFriendActivity(view: View) {
        startActivity(Intent(this, AddFriendActivity::class.java))
    }

    fun toFriendsActivity(view: View) =
        startActivity(Intent(this, FriendsActivity::class.java))

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

    enum class Direction {
        LEFT, RIGHT
    }
}
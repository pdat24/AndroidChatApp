package com.firstapp.androidchatapp.ui.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firstapp.androidchatapp.MainApp
import com.firstapp.androidchatapp.R
import com.firstapp.androidchatapp.adapters.GroupMessageAdapter
import com.firstapp.androidchatapp.models.Message
import com.firstapp.androidchatapp.models.MessageBox
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModel
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModelFactory
import com.firstapp.androidchatapp.ui.viewmodels.MainViewModel
import com.firstapp.androidchatapp.utils.Constants.Companion.ATTRIBUTE_ACTIVE_STATUS_ON
import com.firstapp.androidchatapp.utils.Constants.Companion.AVATAR_URI
import com.firstapp.androidchatapp.utils.Constants.Companion.CONVERSATIONS_COLLECTION_PATH
import com.firstapp.androidchatapp.utils.Constants.Companion.CONVERSATION_ID
import com.firstapp.androidchatapp.utils.Constants.Companion.FILE
import com.firstapp.androidchatapp.utils.Constants.Companion.FILE_STORAGE_PATH
import com.firstapp.androidchatapp.utils.Constants.Companion.FRIEND_UID
import com.firstapp.androidchatapp.utils.Constants.Companion.ICON_LIKE
import com.firstapp.androidchatapp.utils.Constants.Companion.IMAGE
import com.firstapp.androidchatapp.utils.Constants.Companion.IMAGE_STORAGE_PATH
import com.firstapp.androidchatapp.utils.Constants.Companion.IS_FRIEND
import com.firstapp.androidchatapp.utils.Constants.Companion.MAIN_SHARED_PREFERENCE
import com.firstapp.androidchatapp.utils.Constants.Companion.MESSAGE_BOXES
import com.firstapp.androidchatapp.utils.Constants.Companion.MESSAGE_BOXES_COLLECTION_PATH
import com.firstapp.androidchatapp.utils.Constants.Companion.NAME
import com.firstapp.androidchatapp.utils.Constants.Companion.TEXT
import com.firstapp.androidchatapp.utils.Constants.Companion.USERS_COLLECTION_PATH
import com.firstapp.androidchatapp.utils.Functions
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneOffset

class ChatActivity : AppCompatActivity() {
    companion object {
        var active = false
    }

    init {
        this.applyOverrideConfiguration(Configuration().apply {
            setLocale(MainApp.locale)
        })
    }

    private lateinit var choosePhotoBtn: ImageView
    private lateinit var takePhotoBtn: ImageView
    private lateinit var attachFileBtn: ImageView
    private lateinit var collapseMsgInputBtn: ImageView
    private lateinit var likeBtn: ImageView
    private lateinit var sendMsgBtn: ImageView
    private lateinit var messageInput: TextInputEditText
    private lateinit var chatBarContainer: RelativeLayout
    private lateinit var chatToolBar: LinearLayout
    private lateinit var tvNotFriend: TextView
    private lateinit var rcvMessages: RecyclerView
    private lateinit var loadingView: View
    private lateinit var tvActiveStatus: TextView
    private lateinit var statusIndicator: RelativeLayout
    private lateinit var emptyConversationView: View
    private lateinit var pickImgLauncher: ActivityResultLauncher<Intent>
    private lateinit var takePhotoLauncher: ActivityResultLauncher<Intent>
    private lateinit var pickFileLauncher: ActivityResultLauncher<Intent>
    private lateinit var dbViewModel: DatabaseViewModel
    private lateinit var mainViewModel: MainViewModel
    private val firestore = FirebaseFirestore.getInstance()
    private var fileStoragePath: String? = null
    private var imageStoragePath: String? = null
    private var messageBoxAvatarURI: String? = null
    private var messageBoxListUID: String? = null
    private var messageBoxName: String? = null
    private var userIsFriend: Boolean? = null
    private val currentUserUID = FirebaseAuth.getInstance().currentUser!!.uid
    private lateinit var conversationID: String
    private val sentStatusFlow = MutableStateFlow(true)
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        window.statusBarColor = getColor(R.color.top_and_bottom_section)
        sharedPreferences = getSharedPreferences(MAIN_SHARED_PREFERENCE, MODE_PRIVATE)
        ViewCompat.getWindowInsetsController(window.decorView)
            ?.isAppearanceLightStatusBars = !MainApp.nightModeIsOn
        // get views
        choosePhotoBtn = findViewById(R.id.ivChoosePhoto)
        takePhotoBtn = findViewById(R.id.ivTakePhoto)
        attachFileBtn = findViewById(R.id.ivAttachFile)
        collapseMsgInputBtn = findViewById(R.id.ivCollapseMsgInput)
        likeBtn = findViewById(R.id.ivLikeIcon)
        sendMsgBtn = findViewById(R.id.ivSendMsg)
        messageInput = findViewById(R.id.messageInput)
        chatBarContainer = findViewById(R.id.chatBarContainer)
        loadingView = findViewById(R.id.loading)
        emptyConversationView = findViewById(R.id.emptyConversionView)
        rcvMessages = findViewById(R.id.rcvMessages)
        chatToolBar = findViewById(R.id.chatToolbar)
        tvNotFriend = findViewById(R.id.tvNotFriend)
        tvActiveStatus = findViewById(R.id.tvStatus)
        statusIndicator = findViewById(R.id.statusIndicator)

        rcvMessages.layoutManager = LinearLayoutManager(this@ChatActivity).apply {
            stackFromEnd = true
        }

        // change friend's avatar and name
        val intentExtras = intent.extras!!
        messageBoxName = intentExtras.getString(NAME)
        messageBoxAvatarURI = intentExtras.getString(AVATAR_URI)
        messageBoxListUID = intentExtras.getString(FRIEND_UID)
        userIsFriend = intentExtras.getBoolean(IS_FRIEND)
        conversationID = intentExtras.getString(CONVERSATION_ID)
            ?: throw IllegalArgumentException("Conversation ID is null!")
        Glide.with(this).load(intentExtras.getString(AVATAR_URI))
            .into(findViewById<ImageView>(R.id.ivUserAvatar))
        findViewById<TextView>(R.id.tvName).text = messageBoxName

        imageStoragePath = "$IMAGE_STORAGE_PATH/$conversationID"
        fileStoragePath = "$FILE_STORAGE_PATH/$conversationID"

        // view models
        dbViewModel = ViewModelProvider(
            this,
            DatabaseViewModelFactory(this)
        )[DatabaseViewModel::class.java]
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        renderUserActiveStatus()
        blockChatIfNotFriend()

        // add event listeners
        choosePhotoBtn.setOnClickListener { view ->
            createClickAnimation(view)
            pickImage()
        }
        takePhotoBtn.setOnClickListener { view ->
            createClickAnimation(view)
            takePhoto()
        }
        attachFileBtn.setOnClickListener { view ->
            createClickAnimation(view)
            chooseFile()
        }
        collapseMsgInputBtn.setOnClickListener { view ->
            createClickAnimation(view)
            collapseMessageInput()
        }
        likeBtn.setOnClickListener { view ->
            createClickAnimation(view)
            sendMessage(getString(R.string.like_icon_link), type = ICON_LIKE)
        }
        sendMsgBtn.setOnClickListener {
            val text = messageInput.text.toString()
            messageInput.text?.clear()
            sendMessage(text.trim(), type = TEXT)
        }
        messageInput.setOnClickListener {
            scaleMessageInput()
        }
        messageInput.addTextChangedListener {
            scaleMessageInput()
            if (it.toString().trim().isNotEmpty()) {
                likeBtn.visibility = View.INVISIBLE
                sendMsgBtn.visibility = View.VISIBLE
            } else {
                likeBtn.visibility = View.VISIBLE
                sendMsgBtn.visibility = View.INVISIBLE
            }
        }
        messageInput.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                lifecycleScope.launch {
                    delay(250)
                    scrollToBottom()
                }
                (rcvMessages.layoutManager as LinearLayoutManager).stackFromEnd = true
                scaleMessageInput()
            } else collapseMessageInput()
        }
        renderMessages(conversationID)
        observeConversationUpdates()
        observeOnlineStatusChange()

        // intent result launchers
        pickImgLauncher = registerIntentResult { result ->
            if (result.data != null) {
                handlePickImageResult(result.data!!)
            }
        }
        takePhotoLauncher = registerIntentResult { result ->
            if (result.data != null) {
                handleTakePhotoResult(result.data!!)
            }
        }
        pickFileLauncher = registerIntentResult { result ->
            if (result.data != null) {
                handlePickFileResult(result.data!!)
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

    private fun renderUserActiveStatus() {
        CoroutineScope(Dispatchers.Main).launch {
            val user = dbViewModel.getUserById(messageBoxListUID!!)
            val activeStatusOn = user[ATTRIBUTE_ACTIVE_STATUS_ON] as Boolean
            if (!activeStatusOn) {
                tvActiveStatus.visibility = View.GONE
                statusIndicator.visibility = View.GONE
            } else {
                var text = R.string.inactive
                var indicator = R.drawable.view_offline_indicator
                dbViewModel.getOnlineFriends().forEach {
                    // the default is inactive
                    if (it.uid == messageBoxListUID) {
                        // active
                        text = R.string.active
                        indicator = R.drawable.view_online_indicator
                    }
                }
                tvActiveStatus.text = getString(text)
                statusIndicator.setBackgroundResource(indicator)
            }
        }
    }

    private fun observeOnlineStatusChange() {
        firestore.collection(USERS_COLLECTION_PATH).document(currentUserUID)
            .addSnapshotListener { value, _ ->
                value?.let {
                    renderUserActiveStatus()
                }
            }
    }

    private fun registerIntentResult(
        callback: ActivityResultCallback<ActivityResult>
    ): ActivityResultLauncher<Intent> {
        return registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            callback
        )
    }

    private fun handlePickImageResult(result: Intent) =
        lifecycleScope.launch {
            val data = result.clipData
            if (data != null) {
                for (i in 0 until data.itemCount) {
                    // TODO: Show image uploading on ui
                    sendMessage(
                        dbViewModel.uploadFileByUri(
                            "$imageStoragePath/${Functions.createUniqueString()}",
                            data.getItemAt(i).uri
                        ), type = IMAGE
                    )
                }
            }
        }

    private fun getFileName(uri: Uri): String {
        val file = File(uri.toString())
        var cursor: Cursor? = null
        var name = "Attached file"
        if (uri.toString().startsWith("content://")) {
            try {
                cursor = contentResolver.query(uri, null, null, null, null)
                if (cursor != null && cursor.moveToFirst())
                    name =
                        cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            } finally {
                cursor?.close()
            }
        } else if (uri.toString().startsWith("file://"))
            name = file.name
        return name
    }

    private fun handlePickFileResult(result: Intent) =
        lifecycleScope.launch {
            val uri = result.data
            if (uri != null) {
                // TODO: Show file uploading on ui
                sendMessage(
                    dbViewModel.uploadFileByUri(
                        "$fileStoragePath/${getFileName(uri) + "|" + Functions.createUniqueString()}",
                        uri
                    ), type = FILE
                )
            }
        }

    private fun handleTakePhotoResult(result: Intent) =
        lifecycleScope.launch {
            val bitmap = result.extras?.get("data") as Bitmap
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            // TODO: Show file uploading on ui
            sendMessage(
                dbViewModel.uploadImageByBytes(
                    "$imageStoragePath/${Functions.createUniqueString()}",
                    stream.toByteArray()
                ), type = IMAGE
            )
        }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        pickImgLauncher.launch(intent)
    }

    private fun chooseFile() {
        pickFileLauncher.launch(
            Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "file/*"
            }
        )
    }

    private fun takePhoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePhotoLauncher.launch(intent)
    }

    private fun observeConversationUpdates() {
        firestore.collection(CONVERSATIONS_COLLECTION_PATH).document(conversationID)
            .addSnapshotListener { value, _ ->
                lifecycleScope.launch {
                    value?.let {
                        withContext(Dispatchers.Main) {
                            val groups = Functions.getGroupMessagesInConversation(it)
                            if (groups.isNotEmpty())
                                emptyConversationView.visibility = View.GONE
                            (rcvMessages.layoutManager as LinearLayoutManager).stackFromEnd = true
                            rcvMessages.adapter =
                                GroupMessageAdapter(
                                    sentStatusFlow,
                                    this@ChatActivity,
                                    messageBoxAvatarURI!!,
                                    groups.asReversed()
                                )
                        }
                        // Always update the state of message box is read when user in chat activity
                        dbViewModel.updateMsgBoxReadState(conversationID, true)
                        dbViewModel.updateUnreadMsgNumber(conversationID, 0)
                    }
                }
            }
    }

    private fun scrollToBottom(smooth: Boolean = true) {
        if (smooth)
            rcvMessages.smoothScrollToPosition(rcvMessages.adapter!!.itemCount - 1)
        else
            rcvMessages.scrollToPosition(rcvMessages.adapter!!.itemCount - 1)
    }

    private suspend fun getConversation(id: String): DocumentSnapshot {
        return dbViewModel.getConversation(id)
    }

    /**
     * Show messages on screen
     * @param id the id of conversation
     */
    private fun renderMessages(id: String) {
        loadingView.visibility = View.VISIBLE
        lifecycleScope.launch {
            val groups = Functions.getGroupMessagesInConversation(
                getConversation(id)
            )
            if (groups.isEmpty())
                emptyConversationView.visibility = View.VISIBLE
            withContext(Dispatchers.Main) {
                loadingView.visibility = View.GONE
                rcvMessages.adapter =
                    GroupMessageAdapter(
                        sentStatusFlow,
                        this@ChatActivity,
                        messageBoxAvatarURI!!,
                        groups.asReversed()
                    )
            }
        }
    }

    private fun sendMessage(content: String, type: String) {
        if (Functions.isInternetConnected(this))
            CoroutineScope(Dispatchers.IO).launch {
                val sendTime = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
                updateFriendMessageBox()
                // get preview message
                val previewMsg = Functions.getPreviewMessage(
                    this@ChatActivity,
                    Message(content, type)
                )
                dbViewModel.addMessage(conversationID, Message(content, type), previewMsg, sendTime)
                // make the message box of friend to unread
                // change order of the message box of current user
                lifecycleScope.launch {
                    // create message box if not exists for current user
                    createMessageBoxIfNotExists(
                        currentUserUID,
                        MessageBox(
                            index = 0,
                            friendUID = messageBoxListUID!!,
                            avatarURI = messageBoxAvatarURI!!,
                            name = messageBoxName!!,
                            conversationID = conversationID,
                            previewMessage = previewMsg,
                            time = sendTime,
                            read = true,
                            unreadMessages = 0
                        )
                    )
                    dbViewModel.putMessageBoxOnTop(
                        dbViewModel.getMessageBoxListId(currentUserUID),
                        conversationID
                    )
                }
                // change order of the message box of friend
                Functions.observeLiveValueOneTime(dbViewModel.getCachedUserInfo()) {
                    lifecycleScope.launch {
                        createMessageBoxIfNotExists(
                            messageBoxListUID!!,
                            MessageBox(
                                index = 0,
                                friendUID = currentUserUID,
                                avatarURI = it.avatarURI,
                                name = it.name,
                                conversationID = conversationID,
                                previewMessage = previewMsg,
                                time = sendTime,
                            )
                        )
                        dbViewModel.putMessageBoxOnTop(
                            dbViewModel.getMessageBoxListId(messageBoxListUID!!),
                            conversationID
                        )
                    }
                }
            }
        else
            Functions.showNoInternetNotification(this)
    }

    /**
     * Create new message box for user
     * @param userID the id of user will be created message box
     * @return true if new messagebox is created else false
     */
    private suspend fun createMessageBoxIfNotExists(
        userID: String,
        messageBox: MessageBox
    ): Boolean {
        val messageBoxes = dbViewModel.getMessageBoxes(dbViewModel.getMessageBoxList(userID))
        var isExisted = false
        messageBoxes.forEach {
            if (it.conversationID == conversationID)
                isExisted = true
        }
        if (!isExisted) {
            dbViewModel.createMessageBoxOnTop(
                dbViewModel.getMessageBoxListId(userID),
                messageBox
            )
            return true
        }
        return false
    }

    private suspend fun updateFriendMessageBox() {
        val msgBox = dbViewModel.getMessageBoxes(
            dbViewModel.getMessageBoxList(messageBoxListUID)
        ).toMutableList()
        msgBox.forEach {
            if (it.conversationID == conversationID) {
                it.read = false
                it.unreadMessages++
            }
        }
        FirebaseFirestore.getInstance().collection(MESSAGE_BOXES_COLLECTION_PATH).document(
            dbViewModel.getMessageBoxListId(messageBoxListUID!!)
        ).update(MESSAGE_BOXES, msgBox)
    }

    private fun scaleMessageInput() {
        choosePhotoBtn.visibility = View.GONE
        takePhotoBtn.visibility = View.GONE
        attachFileBtn.visibility = View.GONE
        collapseMsgInputBtn.visibility = View.VISIBLE
    }

    private fun collapseMessageInput() {
        choosePhotoBtn.visibility = View.VISIBLE
        takePhotoBtn.visibility = View.VISIBLE
        attachFileBtn.visibility = View.VISIBLE
        collapseMsgInputBtn.visibility = View.GONE
    }

    private fun closeMessageInput() {
        messageInput.clearFocus()
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(
                messageInput.windowToken,
                InputMethodManager.RESULT_UNCHANGED_SHOWN
            )
    }

    private fun blockChatIfNotFriend() = lifecycleScope.launch {
        if (userIsFriend == false) {
            tvNotFriend.visibility = View.VISIBLE
            chatToolBar.visibility = View.GONE
        } else {
            tvNotFriend.visibility = View.GONE
            chatToolBar.visibility = View.VISIBLE
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            val view = currentFocus
            if (view is TextInputEditText) {
                val msgInputBox = Rect()
                val chatBarBox = Rect()
                view.getGlobalVisibleRect(msgInputBox)
                chatBarContainer.getGlobalVisibleRect(chatBarBox)
                if (
                    !msgInputBox.contains(ev.rawX.toInt(), ev.rawY.toInt()) &&
                    !chatBarBox.contains(ev.rawX.toInt(), ev.rawY.toInt())
                ) {
                    closeMessageInput()
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun createClickAnimation(view: View) {
        Functions.scaleDownUpAnimation(view)
    }

    fun back(view: View) = finish()
}
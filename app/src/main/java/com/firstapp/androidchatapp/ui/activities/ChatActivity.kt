package com.firstapp.androidchatapp.ui.activities

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.provider.MediaStore
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
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModel
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModelFactory
import com.firstapp.androidchatapp.ui.viewmodels.MainViewModel
import com.firstapp.androidchatapp.utils.Constants.Companion.AVATAR_URI
import com.firstapp.androidchatapp.utils.Constants.Companion.CONVERSATIONS_COLLECTION_PATH
import com.firstapp.androidchatapp.utils.Constants.Companion.CONVERSATION_ID
import com.firstapp.androidchatapp.utils.Constants.Companion.FILE
import com.firstapp.androidchatapp.utils.Constants.Companion.FILE_STORAGE_PATH
import com.firstapp.androidchatapp.utils.Constants.Companion.FRIEND_UID
import com.firstapp.androidchatapp.utils.Constants.Companion.ICON
import com.firstapp.androidchatapp.utils.Constants.Companion.IMAGE
import com.firstapp.androidchatapp.utils.Constants.Companion.IMAGE_STORAGE_PATH
import com.firstapp.androidchatapp.utils.Constants.Companion.IS_FRIEND
import com.firstapp.androidchatapp.utils.Constants.Companion.MESSAGE_BOXES
import com.firstapp.androidchatapp.utils.Constants.Companion.MESSAGE_BOXES_COLLECTION_PATH
import com.firstapp.androidchatapp.utils.Constants.Companion.MESSAGE_BOX_INDEX
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.ZoneOffset

class ChatActivity : AppCompatActivity() {
    companion object {
        var active = false
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
    private lateinit var conversationID: String
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
    private var friendAvatarURI: String? = null
    private var userUID: String? = null
    private var userIsFriend: Boolean? = null
    private var msgBoxIndex: Int? = null
    private val currentUserUID = FirebaseAuth.getInstance().currentUser!!.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        window.statusBarColor = getColor(R.color.light_black)
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

        rcvMessages.layoutManager = LinearLayoutManager(this@ChatActivity)

        // change friend's avatar and name
        val intentExtras = intent.extras!!
        Glide.with(this).load(intentExtras.getString(AVATAR_URI))
            .into(findViewById<ImageView>(R.id.ivUserAvatar))
        findViewById<TextView>(R.id.tvName).text = intentExtras.getString(NAME)
        friendAvatarURI = intentExtras.getString(AVATAR_URI)
        msgBoxIndex = intentExtras.getInt(MESSAGE_BOX_INDEX)
        userUID = intentExtras.getString(FRIEND_UID)
        userIsFriend = intentExtras.getBoolean(IS_FRIEND)
        conversationID = intentExtras.getString(CONVERSATION_ID)
            ?: throw IllegalArgumentException("Conversation ID is null!")

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
            sendMessage(getString(R.string.like_icon_link), type = ICON)
        }
        sendMsgBtn.setOnClickListener { view ->
            val text = messageInput.text.toString()
            messageInput.text?.clear()
            if (Functions.isInternetConnected(this))
                sendMessage(text.trim(), type = TEXT)
            else
                Functions.showNoInternetNotification()
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
        messageInput.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) scaleMessageInput()
            else collapseMessageInput()
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
        MainApp.startOnlineStatus()
    }

    override fun onStop() {
        super.onStop()
        active = false
        MainApp.prepareOffline(this)
    }

    private fun renderUserActiveStatus() {
        CoroutineScope(Dispatchers.Main).launch {
            var text = R.string.inactive
            var indicator = R.drawable.view_offline_indicator
            dbViewModel.getOnlineFriends().forEach {
                // the default is inactive
                if (it.uid == userUID) {
                    // active
                    text = R.string.active
                    indicator = R.drawable.view_online_indicator
                }
            }
            tvActiveStatus.text = getString(text)
            statusIndicator.setBackgroundResource(indicator)
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

    private fun handlePickFileResult(result: Intent) =
        lifecycleScope.launch {
            val uri = result.data
            if (uri != null) {
                // TODO: Show file uploading on ui
                sendMessage(
                    dbViewModel.uploadFileByUri(
                        "$fileStoragePath/${Functions.createUniqueString()}", uri
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
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "file/*"
        pickFileLauncher.launch(intent)
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
                            rcvMessages.adapter =
                                GroupMessageAdapter(friendAvatarURI!!, groups.asReversed())
                            rcvMessages.scrollToPosition(groups.size - 1)
                        }
                        // Always update the state of message box is read when user in chat activity
                        dbViewModel.updateMsgBoxReadState(msgBoxIndex!!, true)
                        dbViewModel.updateUnreadMsgNumber(msgBoxIndex!!, 0)
                    }
                }
            }
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
                rcvMessages.adapter = GroupMessageAdapter(friendAvatarURI!!, groups.asReversed())
                rcvMessages.scrollToPosition(groups.size - 1)
                loadingView.visibility = View.GONE
            }
        }
    }

    private fun sendMessage(content: String, type: String) {
        CoroutineScope(Dispatchers.IO).launch {
            dbViewModel.addMessage(conversationID, Message(content, type))
            var previewMsg = ""
            when (type) {
                TEXT -> previewMsg = content
                IMAGE -> previewMsg = getString(R.string.sent_an_image)
                FILE -> previewMsg = getString(R.string.sent_a_file)
            }
            dbViewModel.updatePreviewMessage(conversationID, previewMsg)
            dbViewModel.updateLastSendTime(
                conversationID, LocalDateTime.now().toEpochSecond(
                    ZoneOffset.UTC
                )
            )
            unreadFriendMessageBox()
        }
    }

    private suspend fun unreadFriendMessageBox() {
        val tmp = dbViewModel.getMessageBoxList(userUID)
        val msgBox = dbViewModel.getMessageBoxes(tmp).toMutableList()
        msgBox.forEach {
            if (it.conversationID == conversationID) {
                it.read = false
                it.unreadMessages++
            }
        }
        FirebaseFirestore.getInstance().collection(MESSAGE_BOXES_COLLECTION_PATH).document(
            dbViewModel.getMessageBoxListId(userUID!!)
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
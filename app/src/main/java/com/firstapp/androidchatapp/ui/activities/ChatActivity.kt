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
import com.firstapp.androidchatapp.R
import com.firstapp.androidchatapp.adapters.GroupMessageAdapter
import com.firstapp.androidchatapp.models.Message
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModel
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModelFactory
import com.firstapp.androidchatapp.utils.Constants.Companion.AVATAR_URI
import com.firstapp.androidchatapp.utils.Constants.Companion.CONVERSATIONS_COLLECTION_PATH
import com.firstapp.androidchatapp.utils.Constants.Companion.CONVERSATION_ID
import com.firstapp.androidchatapp.utils.Constants.Companion.FILE
import com.firstapp.androidchatapp.utils.Constants.Companion.FILE_STORAGE_PATH
import com.firstapp.androidchatapp.utils.Constants.Companion.ICON
import com.firstapp.androidchatapp.utils.Constants.Companion.IMAGE
import com.firstapp.androidchatapp.utils.Constants.Companion.IMAGE_STORAGE_PATH
import com.firstapp.androidchatapp.utils.Constants.Companion.MESSAGE_BOX_INDEX
import com.firstapp.androidchatapp.utils.Constants.Companion.NAME
import com.firstapp.androidchatapp.utils.Constants.Companion.TEXT
import com.firstapp.androidchatapp.utils.Functions
import com.google.android.material.textfield.TextInputEditText
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

    private lateinit var choosePhotoBtn: ImageView
    private lateinit var takePhotoBtn: ImageView
    private lateinit var attachFileBtn: ImageView
    private lateinit var collapseMsgInputBtn: ImageView
    private lateinit var likeBtn: ImageView
    private lateinit var sendMsgBtn: ImageView
    private lateinit var messageInput: TextInputEditText
    private lateinit var chatBar: LinearLayout
    private lateinit var rcvMessages: RecyclerView
    private lateinit var conversationID: String
    private lateinit var loadingView: View
    private lateinit var pickImgLauncher: ActivityResultLauncher<Intent>
    private lateinit var takePhotoLauncher: ActivityResultLauncher<Intent>
    private lateinit var pickFileLauncher: ActivityResultLauncher<Intent>
    private lateinit var dbViewModel: DatabaseViewModel
    private val firestore = FirebaseFirestore.getInstance()
    private var fileStoragePath: String? = null
    private var imageStoragePath: String? = null
    private var friendAvatarURI: String? = null
    private var msgBoxIndex: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        window.statusBarColor = getColor(R.color.chat_input_bg)
        // get views
        choosePhotoBtn = findViewById(R.id.ivChoosePhoto)
        takePhotoBtn = findViewById(R.id.ivTakePhoto)
        attachFileBtn = findViewById(R.id.ivAttachFile)
        collapseMsgInputBtn = findViewById(R.id.ivCollapseMsgInput)
        likeBtn = findViewById(R.id.ivLikeIcon)
        sendMsgBtn = findViewById(R.id.ivSendMsg)
        messageInput = findViewById(R.id.messageInput)
        chatBar = findViewById(R.id.chatBar)
        loadingView = findViewById(R.id.loading)
        rcvMessages = findViewById(R.id.rcvMessages)

        // change friend's avatar and name
        val intentExtras = intent.extras!!
        Glide.with(this).load(intentExtras.getString(AVATAR_URI))
            .into(findViewById<ImageView>(R.id.ivUserAvatar))
        findViewById<TextView>(R.id.tvName).text = intentExtras.getString(NAME)
        friendAvatarURI = intentExtras.getString(AVATAR_URI)
        msgBoxIndex = intentExtras.getInt(MESSAGE_BOX_INDEX)

        conversationID = intentExtras.getString(CONVERSATION_ID)
            ?: throw IllegalArgumentException("Conversation ID is null!")

        imageStoragePath = "$IMAGE_STORAGE_PATH/$conversationID"
        fileStoragePath = "$FILE_STORAGE_PATH/$conversationID"

        // view models
        dbViewModel =
            ViewModelProvider(this, DatabaseViewModelFactory(this))[DatabaseViewModel::class.java]

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
        messageInput.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) scaleMessageInput()
            else collapseMessageInput()
        }
        renderMessages(conversationID)
        observeConversationUpdates()

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
                            rcvMessages.adapter = GroupMessageAdapter(
                                friendAvatarURI!!,
                                Functions.getGroupMessagesInConversation(it)
                            )
                        }
                        // TODO: Cache messages
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
            withContext(Dispatchers.Main) {
                rcvMessages.adapter = GroupMessageAdapter(friendAvatarURI!!, groups)
                rcvMessages.layoutManager = LinearLayoutManager(this@ChatActivity)
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
            dbViewModel.updatePreviewMessage(msgBoxIndex!!, previewMsg)
            dbViewModel.updateLastSendTime(
                msgBoxIndex!!, LocalDateTime.now().toEpochSecond(
                    ZoneOffset.UTC
                )
            )
            // TODO: Cache message
        }
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

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            val view = currentFocus
            if (view is TextInputEditText) {
                val msgInputBox = Rect()
                val chatBarBox = Rect()
                view.getGlobalVisibleRect(msgInputBox)
                chatBar.getGlobalVisibleRect(chatBarBox)
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
package com.firstapp.androidchatapp.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.firstapp.androidchatapp.R
import com.firstapp.androidchatapp.models.FriendRequest
import com.firstapp.androidchatapp.models.Message
import com.firstapp.androidchatapp.receivers.ReplyReceiver
import com.firstapp.androidchatapp.repositories.LocalRepository
import com.firstapp.androidchatapp.repositories.UserManager
import com.firstapp.androidchatapp.ui.activities.AddFriendActivity
import com.firstapp.androidchatapp.ui.activities.ChatActivity
import com.firstapp.androidchatapp.ui.activities.FriendRequestsActivity
import com.firstapp.androidchatapp.ui.activities.FriendsActivity
import com.firstapp.androidchatapp.ui.activities.MainActivity
import com.firstapp.androidchatapp.ui.activities.SettingsActivity
import com.firstapp.androidchatapp.utils.Constants.Companion.ATTRIBUTE_NOTIFICATION_ID
import com.firstapp.androidchatapp.utils.Constants.Companion.AVATAR_URI
import com.firstapp.androidchatapp.utils.Constants.Companion.CONVERSATION_ID
import com.firstapp.androidchatapp.utils.Constants.Companion.FILE
import com.firstapp.androidchatapp.utils.Constants.Companion.FOREGROUND_NOTIFICATION_CHANNEL_ID
import com.firstapp.androidchatapp.utils.Constants.Companion.FOREGROUND_NOTIFICATION_CHANNEL_NAME
import com.firstapp.androidchatapp.utils.Constants.Companion.FRIEND_UID
import com.firstapp.androidchatapp.utils.Constants.Companion.ICON_LIKE
import com.firstapp.androidchatapp.utils.Constants.Companion.IMAGE
import com.firstapp.androidchatapp.utils.Constants.Companion.IS_FRIEND
import com.firstapp.androidchatapp.utils.Constants.Companion.MAIN_NOTIFICATION_CHANNEL_ID
import com.firstapp.androidchatapp.utils.Constants.Companion.MAIN_NOTIFICATION_CHANNEL_NAME
import com.firstapp.androidchatapp.utils.Constants.Companion.NAME
import com.firstapp.androidchatapp.utils.Constants.Companion.NOTIFICATION_ID
import com.firstapp.androidchatapp.utils.Constants.Companion.NOTIFICATION_SERVICE_NOTIFICATION_ID
import com.firstapp.androidchatapp.utils.Constants.Companion.REPLY_RESULT_KEY
import com.firstapp.androidchatapp.utils.Constants.Companion.USERS_COLLECTION_PATH
import com.firstapp.androidchatapp.utils.Functions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotificationsService : LifecycleService() {

    private lateinit var notificationManager: NotificationManager
    private val userManager: UserManager by lazy {
        UserManager()
    }
    private val localRepository: LocalRepository by lazy {
        LocalRepository(applicationContext)
    }
    private val currentUserUID: String by lazy {
        FirebaseAuth.getInstance().currentUser!!.uid
    }
    private val observerIsFinished = MutableStateFlow(false)
    private lateinit var resources: Resources
    private val pushFriendReqNotificationObserver: (List<FriendRequest>) -> Unit = {
        lifecycleScope.launch {
            val receivedRequestsOnDB = userManager.getUserRequests(
                currentUserUID,
                UserManager.RequestType.RECEIVED
            )
            val cachedUidList: List<String> = it.map { req -> req.uid }
            for (req in receivedRequestsOnDB) {
                if (!cachedUidList.contains(req.uid)) {
                    pushNewFriendRequestNotification(req)
                }
            }
            observerIsFinished.emit(true)
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        resources = applicationContext.resources
        createMainNotificationChannel()
        createForegroundNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_SERVICE_NOTIFICATION_ID, createServiceNotification())
        intent?.let {
            observeNewFriendRequests()
            observeNewMessages()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun createMainNotificationChannel() =
        notificationManager.createNotificationChannel(
            NotificationChannel(
                MAIN_NOTIFICATION_CHANNEL_ID,
                MAIN_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
            }
        )

    private fun createForegroundNotificationChannel() =
        notificationManager.createNotificationChannel(
            NotificationChannel(
                FOREGROUND_NOTIFICATION_CHANNEL_ID,
                FOREGROUND_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
        )

    private fun createServiceNotification(): Notification =
        NotificationCompat
            .Builder(applicationContext, FOREGROUND_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.cirlce_app_icon)
            .setContentIntent(
                PendingIntent.getActivity(
                    applicationContext,
                    1,
                    Intent(applicationContext, SettingsActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setContentTitle(resources.getString(R.string.receiving_notifications))
            .build()

    private fun observeNewFriendRequests() {
        FirebaseFirestore.getInstance()
            .collection(USERS_COLLECTION_PATH)
            .document(currentUserUID)
            .addSnapshotListener { value, _ ->
                value?.let {
                    lifecycleScope.launch {
                        val cachedReceivedRequests = localRepository.getReceivedFriendRequests()
                        cachedReceivedRequests.observeForever(pushFriendReqNotificationObserver)
                        observerIsFinished.collectLatest { isFinished ->
                            if (isFinished) {
                                cachedReceivedRequests.removeObserver(
                                    pushFriendReqNotificationObserver
                                )
                                observerIsFinished.emit(false)
                            }
                        }
                    }
                }
            }
    }

    private fun observeNewMessages() {
        Functions.observeConversationsChanges { documentChanges ->
            for (con in documentChanges) {
                val latestGroup =
                    Functions.getGroupMessagesInConversation(con).first()
                val senderID = latestGroup.senderID
                if (
                    senderID != currentUserUID &&
                    latestGroup.messages.isNotEmpty() &&
                    !(
                            MainActivity.active ||
                                    AddFriendActivity.active ||
                                    ChatActivity.active ||
                                    FriendsActivity.active ||
                                    FriendRequestsActivity.active ||
                                    SettingsActivity.active
                            )
                ) {
                    pushDirectReplyNotification(
                        con.id, senderID, latestGroup.messages.last()
                    )
                }
            }
        }
    }

    private fun pushDirectReplyNotification(
        conversationID: String, senderID: String, message: Message
    ) = lifecycleScope.launch {
        val sender = userManager.getUserById(senderID)
        val senderName = sender[NAME] as String
        val senderAvatar = sender[AVATAR_URI] as String
        val notificationID = (sender[ATTRIBUTE_NOTIFICATION_ID] as Long).toInt()

        val clickPendingIntent =
            PendingIntent.getActivity(
                applicationContext, 0,
                Intent(applicationContext, ChatActivity::class.java).apply {
                    putExtra(CONVERSATION_ID, conversationID)
                    putExtra(FRIEND_UID, senderID)
                    putExtra(AVATAR_URI, senderAvatar)
                    putExtra(NAME, senderName)
                    putExtra(IS_FRIEND, true)
                },
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
        // create reply action
        val replyAction = NotificationCompat.Action.Builder(
            0,
            resources.getString(R.string.reply),
            PendingIntent.getBroadcast(
                applicationContext,
                1,
                Intent(applicationContext, ReplyReceiver::class.java).apply {
                    putExtra(CONVERSATION_ID, conversationID)
                    putExtra(NOTIFICATION_ID, notificationID)
                },
                PendingIntent.FLAG_MUTABLE
            )
        ).addRemoteInput(
            RemoteInput.Builder(REPLY_RESULT_KEY).setLabel("Aa").build()
        ).build()
        // create notification style
        val person = Person.Builder().setName(senderName).build()
        val notificationStyle = NotificationCompat.MessagingStyle(person).addMessage( // add message
            when (message.type) {
                IMAGE -> resources.getString(R.string.sent_an_image)
                FILE -> resources.getString(R.string.sent_a_file)
                ICON_LIKE -> resources.getString(R.string.sent_icon)
                else -> message.content
            },
            System.currentTimeMillis(), person
        )
        Glide.with(applicationContext).asBitmap()
            .load(senderAvatar).into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    notificationManager.notify(
                        notificationID,
                        NotificationCompat
                            .Builder(applicationContext, MAIN_NOTIFICATION_CHANNEL_ID)
                            .setSmallIcon(R.drawable.cirlce_app_icon)
                            .setAutoCancel(true)
                            .setLargeIcon(resource)
                            .setShowWhen(true)
                            .setStyle(notificationStyle)
                            .addAction(replyAction)
                            .setContentIntent(clickPendingIntent)
                            .build()
                    )
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    private fun pushNewFriendRequestNotification(friendReq: FriendRequest) {
        val pendingIntent =
            PendingIntent.getActivity(
                applicationContext,
                0,
                Intent(applicationContext, FriendRequestsActivity::class.java),
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
        notificationManager.notify(
            Functions.createRandomInteger(),
            NotificationCompat
                .Builder(applicationContext, MAIN_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.cirlce_app_icon)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(1000, 1000, 1000))
                .setShowWhen(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .setContentTitle(getString(R.string.received_friend_request))
                .setContentText(
                    "${friendReq.name} ${getString(R.string.sent_you_friend_request)}"
                )
                .build()
        )
    }
}
package com.firstapp.androidchatapp.repositories

import com.firstapp.androidchatapp.models.Friend
import com.firstapp.androidchatapp.models.FriendRequest
import com.firstapp.androidchatapp.models.User
import com.firstapp.androidchatapp.utils.Constants.Companion.AVATAR_URI
import com.firstapp.androidchatapp.utils.Constants.Companion.CONVERSATION_ID
import com.firstapp.androidchatapp.utils.Constants.Companion.DB_ACTIVE_STATUS_ON
import com.firstapp.androidchatapp.utils.Constants.Companion.FRIENDS
import com.firstapp.androidchatapp.utils.Constants.Companion.NAME
import com.firstapp.androidchatapp.utils.Constants.Companion.ONLINE_FRIENDS
import com.firstapp.androidchatapp.utils.Constants.Companion.RECEIVED_REQUESTS
import com.firstapp.androidchatapp.utils.Constants.Companion.SENT_REQUESTS
import com.firstapp.androidchatapp.utils.Constants.Companion.UID
import com.firstapp.androidchatapp.utils.Constants.Companion.USERS_COLLECTION_PATH
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserManager {

    private val userDB = FirebaseFirestore.getInstance().collection(USERS_COLLECTION_PATH)
    private val firebaseAuth = FirebaseAuth.getInstance()

    /**
     * Get user on firestore
     * @param userID ID of required user
     * @return user data
     * @see User
     */
    suspend fun getUserById(userID: String): DocumentSnapshot {
        return userDB.document(userID).get().await()
    }

    suspend fun getUsersByName(name: String): List<DocumentSnapshot> {
        val uid = firebaseAuth.currentUser!!.uid
        return userDB.get().await().filter { user ->
            val tmp = (user[NAME] as String)
            user.id != uid && (
                    tmp.startsWith(name, true) || tmp.split(" ").contains(name)
                    )
        }
    }

    suspend fun getOnlineFriends(user: DocumentSnapshot): List<Friend> {
        val result = mutableListOf<Friend>()
        val friends = user[ONLINE_FRIENDS] as List<*>
        for (f in friends) {
            val friendAsHashMap = f as HashMap<*, *>
            val friendId = friendAsHashMap[UID] as String
            val friend = getUserById(friendId)
            result.add(
                Friend(
                    uid = friendId,
                    name = friend[NAME] as String,
                    avatarURI = friend[AVATAR_URI] as String,
                    conversationID = friendAsHashMap[CONVERSATION_ID] as String
                )
            )
        }
        return result
    }

    suspend fun getFriends(userID: String): List<Friend> {
        val result = mutableListOf<Friend>()
        val friends = getUserById(userID)[FRIENDS] as List<*>
        for (f in friends) {
            val friendAsHashMap = f as HashMap<*, *>
            val friendId = friendAsHashMap[UID] as String
            val friend = getUserById(friendId)
            result.add(
                Friend(
                    uid = friendId,
                    name = friend[NAME] as String,
                    avatarURI = friend[AVATAR_URI] as String,
                    conversationID = friendAsHashMap[CONVERSATION_ID] as String
                )
            )
        }
        return result
    }

    suspend fun updateOnlineState(userID: String, isOnline: Boolean) {
        /**
         * Each user have a list to store online friends, we'll get all friends of [userID]
         * and add current user to that list of every friend
         */
        val currentUserUID = firebaseAuth.currentUser!!.uid
        val friends = getUserById(userID)[FRIENDS] as List<*>
        for (f in friends) {
            val friend = getUserById((f as HashMap<*, *>)[UID] as String)
            // online friends of a friend
            val res = getOnlineFriends(
                getUserById(friend.id)
            ).toMutableList()
            // add current user to the list of online friends of the friend
            // or remove from the list
            if (!isOnline)
                res.removeIf {
                    it.uid == currentUserUID
                }
            else if (isOnline && !onlineStatusIsUpdatedBefore(friend)) {
                // get current user info in the list of online friend of the friend
                // to get conversation id
                val friendsOfFriend = friend[FRIENDS] as List<*>
                for (i in friendsOfFriend) {
                    val friendOfFriend = i as HashMap<*, *>
                    if (friendOfFriend[UID] == currentUserUID) {
                        res.add(
                            Friend(
                                uid = friendOfFriend[UID] as String,
                                name = friendOfFriend[NAME] as String,
                                avatarURI = friendOfFriend[AVATAR_URI] as String,
                                conversationID = friendOfFriend[CONVERSATION_ID] as String
                            )
                        )
                    }
                }
            }
            userDB.document(friend.id).update(ONLINE_FRIENDS, res)
        }
    }

    fun updateActiveStatus(userID: String, on: Boolean) {
        userDB.document(userID).update(DB_ACTIVE_STATUS_ON, on)
    }

    /**
     * Check whether online status is previously updated or not
     */
    private fun onlineStatusIsUpdatedBefore(friend: DocumentSnapshot): Boolean {
        val currentUserUID = firebaseAuth.currentUser!!.uid
        var result = false
        (friend[ONLINE_FRIENDS] as List<*>).forEach {
            val t = it as HashMap<*, *>
            if (t[UID] == currentUserUID)
                result = true
        }
        return result
    }

    /**
     * Add new user to firestore.
     *
     * If [id] is not null, it'll be the id of new user
     * else the id will be created automatically.
     *
     * Note: if the id is set intentionally and be duplicated,
     * the data of old user will be override!
     * @param user the user to save
     * @param id the id of user
     * @return user ID on firestore
     */
    suspend fun create(user: User, id: String? = null): String {
        if (id != null) {
            userDB.document(id).set(user).await()
            return id
        }
        return userDB.add(user).await().id
    }

    /**
     * Update user name on firestore
     * @param userID The ID of user
     * @param name new name
     */
    suspend fun updateName(userID: String, name: String) {
        val user = userDB.document(userID)
        user.update(NAME, name).await()
    }

    /**
     * Update user name on firestore
     * @param userID The ID of user
     * @param avatarURI new avatar
     */
    suspend fun updateAvatar(userID: String, avatarURI: String) {
        val user = userDB.document(userID)
        user.update(AVATAR_URI, avatarURI).await()
    }

    suspend fun addFriend(userID: String, newFriend: Friend) {
        val user = getUserById(userID)
        val tmp = user[FRIENDS] as List<*>
        val friends = mutableListOf<Friend>()
        for (i in tmp) {
            val friend = i as HashMap<*, *>
            friends.add(
                Friend(
                    uid = friend[UID] as String,
                    name = friend[NAME] as String,
                    avatarURI = friend[AVATAR_URI] as String,
                    conversationID = friend[CONVERSATION_ID] as String
                )
            )
        }
        friends.add(newFriend)
        userDB.document(userID).update(FRIENDS, friends).await()
    }

    suspend fun removeFriend(userID: String, friendId: String) {
        val user = getUserById(userID)
        val tmp = user[FRIENDS] as List<*>
        val res = mutableListOf<Friend>()
        tmp.forEach {
            val friend = it as HashMap<*, *>
            if (friend[UID] != friendId)
                res.add(
                    Friend(
                        uid = friend[UID] as String,
                        name = friend[NAME] as String,
                        avatarURI = friend[AVATAR_URI] as String,
                        conversationID = friend[CONVERSATION_ID] as String
                    )
                )
        }
        userDB.document(userID).update(FRIENDS, res).await()
    }

    suspend fun addSentRequest(userID: String, newRequest: FriendRequest) {
        val requests = getUserRequests(userID, RequestType.SENT).toMutableList()
        requests.add(newRequest)
        userDB.document(userID).update(SENT_REQUESTS, requests).await()
    }

    suspend fun addReceivedRequest(senderId: String) {
        val currentUserID = firebaseAuth.currentUser!!.uid
        val user = getUserById(currentUserID)
        val requests = getUserRequests(senderId, RequestType.RECEIVED).toMutableList()
        val req = FriendRequest(
            name = user[NAME] as String,
            uid = currentUserID,
            avatarURI = user[AVATAR_URI] as String
        )
        requests.add(req)
        userDB.document(senderId).update(RECEIVED_REQUESTS, requests).await()
    }

    suspend fun removeSentRequest(senderId: String, receiverId: String) {
        val requests = getUserRequests(senderId, RequestType.SENT) { id ->
            id != receiverId
        }
        userDB.document(senderId).update(SENT_REQUESTS, requests).await()
    }

    /**
     * Remove friend request of [senderId] to [receiverId]
     * @param receiverId the id of current user
     * @param senderId the id of user want to reject
     */
    suspend fun removeReceivedRequest(senderId: String, receiverId: String) {
        val requests = getUserRequests(receiverId, RequestType.RECEIVED) { id ->
            id != senderId
        }
        userDB.document(receiverId).update(RECEIVED_REQUESTS, requests).await()
    }

    /**
     * @param id user ID
     */
    suspend fun getUserRequests(
        id: String,
        type: RequestType,
        filter: ((String) -> Boolean)? = null
    ): List<FriendRequest> {
        val user = getUserById(id)
        val tmp = (
                if (type == RequestType.RECEIVED)
                    user[RECEIVED_REQUESTS]
                else
                    user[SENT_REQUESTS]
                ) as List<*>
        val requests = mutableListOf<FriendRequest>()
        for (i in tmp) {
            val req = i as HashMap<*, *>
            val r = FriendRequest(
                uid = req[UID] as String,
                name = req[NAME] as String,
                avatarURI = req[AVATAR_URI] as String,
            )
            if (filter == null)
                requests.add(r)
            else if (filter(req[UID] as String))
                requests.add(r)
        }
        return requests
    }

    enum class RequestType {
        SENT, RECEIVED
    }
}
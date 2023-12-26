package com.firstapp.androidchatapp.repositories

import com.firstapp.androidchatapp.models.User
import com.firstapp.androidchatapp.utils.Constants.Companion.AVATAR_URI
import com.firstapp.androidchatapp.utils.Constants.Companion.NAME
import com.firstapp.androidchatapp.utils.Constants.Companion.USERS_COLLECTION_PATH
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserManager {

    private val userDB = FirebaseFirestore.getInstance().collection(USERS_COLLECTION_PATH)

    /**
     * Get user on firestore
     * @param userID ID of required user
     * @return user data
     */
    suspend fun getUser(userID: String): DocumentSnapshot {
        return userDB.document(userID).get().await()
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
}
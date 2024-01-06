package com.firstapp.androidchatapp.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firstapp.androidchatapp.R
import com.firstapp.androidchatapp.localdb.entities.UserInfo
import com.firstapp.androidchatapp.models.Friend
import com.firstapp.androidchatapp.models.FriendRequest
import com.firstapp.androidchatapp.models.MessageBox
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset

class ReceivedRequestAdapter(
    private val dbViewModel: DatabaseViewModel,
    private val requests: List<FriendRequest>
) : RecyclerView.Adapter<ReceivedRequestAdapter.ViewHolder>() {

    private lateinit var context: Context

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        val name: TextView = itemView.findViewById(R.id.tvName)
        val id: TextView = itemView.findViewById(R.id.tvID)
        val btnReject: TextView = itemView.findViewById(R.id.btnReject)
        val btnAccept: TextView = itemView.findViewById(R.id.btnAccept)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        return ViewHolder(
            LayoutInflater.from(context)
                .inflate(R.layout.view_received_request, parent, false)
        )
    }

    override fun getItemCount(): Int = requests.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val req = requests[position]
        Glide.with(context).load(req.avatarURI).into(holder.avatar)
        holder.name.text = req.name
        holder.id.text = req.uid
        holder.btnAccept.setOnClickListener {
            acceptRequest(req)
        }
        holder.btnReject.setOnClickListener {
            rejectRequest(req)
        }
    }

    private fun rejectRequest(req: FriendRequest) {
        CoroutineScope(Dispatchers.IO).launch {
            dbViewModel.removeSentRequest(req.uid)
            dbViewModel.removeReceivedRequest(req.uid)
        }
    }

    private fun acceptRequest(req: FriendRequest) {
        CoroutineScope(Dispatchers.IO).launch {
            // create new conversation
            val conID = dbViewModel.createEmptyConversation()
            addFriend(req, conID)
            createMessageBoxes(req, conID)
            removeRequests(req)
        }
    }

    /**
     * Create new message box for user and friend
     */
    private suspend fun createMessageBoxes(req: FriendRequest, conID: String) {
        // create message box for current user
        dbViewModel.createMessageBox(
            msgBoxListId = dbViewModel.getMessageBoxListId(
                dbViewModel.firebaseAuth.currentUser!!.uid
            ),
            msgBox = MessageBox(
                avatarURI = req.avatarURI,
                name = req.name,
                conversationID = conID,
                time = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
            )
        )
        val getUserInfo: (UserInfo) -> Unit = {
            // create message box for user is sent this request
            CoroutineScope(Dispatchers.IO).launch {
                dbViewModel.createMessageBox(
                    msgBoxListId = dbViewModel.getMessageBoxListId(req.uid),
                    msgBox = MessageBox(
                        avatarURI = it.avatarURI,
                        name = it.name,
                        conversationID = conID,
                        time = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
                    ),
                )
            }
        }
        CoroutineScope(Dispatchers.Main).launch {
            dbViewModel.getCachedUserInfo().observeForever(getUserInfo)
            dbViewModel.getCachedUserInfo().removeObserver(getUserInfo)
        }
    }

    /**
     * Add friend for user and friend
     * @param conID the id of conversation
     */
    private suspend fun addFriend(req: FriendRequest, conID: String) {
        dbViewModel.addFriend(
            Friend(
                uid = req.uid,
                name = req.name,
                avatarURI = req.avatarURI,
                conversationID = conID
            )
        )
    }

    /**
     * Remove the sent request of new friend and the received request of current user
     */
    private suspend fun removeRequests(req: FriendRequest) {
        //  remove sent and received request of correspond users
        dbViewModel.removeSentRequest(req.uid)
        dbViewModel.removeReceivedRequest(req.uid)
    }
}
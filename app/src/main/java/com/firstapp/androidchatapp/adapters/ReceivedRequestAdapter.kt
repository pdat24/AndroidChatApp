package com.firstapp.androidchatapp.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firstapp.androidchatapp.R
import com.firstapp.androidchatapp.localdb.entities.UserInfo
import com.firstapp.androidchatapp.models.Friend
import com.firstapp.androidchatapp.models.FriendRequest
import com.firstapp.androidchatapp.models.MessageBox
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModel
import com.firstapp.androidchatapp.utils.Functions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset

class ReceivedRequestAdapter(
    private val dbViewModel: DatabaseViewModel,
    private val requests: List<FriendRequest>
) : RecyclerView.Adapter<ReceivedRequestAdapter.ViewHolder>() {

    private lateinit var context: Context

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val container: LinearLayout = itemView.findViewById(R.id.container)
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
            if (isInternetConnected()) {
                CoroutineScope(Dispatchers.Main).launch {
                    acceptRequest(req).join()
                    holder.container.visibility = View.GONE
                    Toast.makeText(context, "Accepted Request", Toast.LENGTH_SHORT).show()
                }
            } else
                Functions.showNoInternetNotification()
        }

        holder.btnReject.setOnClickListener {
            if (isInternetConnected()) {
                CoroutineScope(Dispatchers.Main).launch {
                    rejectRequest(req).join()
                    holder.container.visibility = View.GONE
                    Toast.makeText(context, "Rejected Request", Toast.LENGTH_SHORT).show()
                }
            } else
                Functions.showNoInternetNotification()
        }
    }

    private fun isInternetConnected(): Boolean =
        Functions.isInternetConnected(context)

    private fun rejectRequest(req: FriendRequest): Job =
        CoroutineScope(Dispatchers.IO).launch {
            CoroutineScope(Dispatchers.IO).launch {
                dbViewModel.removeSentRequest(req.uid)
            }
            CoroutineScope(Dispatchers.IO).launch {
                dbViewModel.removeReceivedRequest(req.uid)
            }
        }

    private fun acceptRequest(req: FriendRequest): Job =
        CoroutineScope(Dispatchers.IO).launch {
            // create new conversation
            val conID = dbViewModel.createEmptyConversation()
            addFriend(req, conID)
            createMessageBoxes(req, conID)
            removeRequests(req)
        }

    /**
     * Create new message box for user and friend
     */
    private fun createMessageBoxes(req: FriendRequest, conID: String) {
        CoroutineScope(Dispatchers.IO).launch {
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
        }
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
    private fun addFriend(req: FriendRequest, conID: String) {
        CoroutineScope(Dispatchers.IO).launch {
            dbViewModel.addFriend(
                Friend(
                    uid = req.uid,
                    name = req.name,
                    avatarURI = req.avatarURI,
                    conversationID = conID
                )
            )
        }
    }

    /**
     * Remove the sent request of new friend and the received request of current user
     */
    private fun removeRequests(req: FriendRequest) {
        //  remove sent and received request of correspond users
        CoroutineScope(Dispatchers.IO).launch {
            dbViewModel.removeSentRequest(req.uid)
        }
        CoroutineScope(Dispatchers.IO).launch {
            dbViewModel.removeReceivedRequest(req.uid)
        }
    }
}
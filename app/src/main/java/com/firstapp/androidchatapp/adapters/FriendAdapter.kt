package com.firstapp.androidchatapp.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firstapp.androidchatapp.R
import com.firstapp.androidchatapp.models.Friend
import com.firstapp.androidchatapp.ui.activities.ChatActivity
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModel
import com.firstapp.androidchatapp.utils.Constants.Companion.AVATAR_URI
import com.firstapp.androidchatapp.utils.Constants.Companion.CONVERSATION_ID
import com.firstapp.androidchatapp.utils.Constants.Companion.FRIEND_UID
import com.firstapp.androidchatapp.utils.Constants.Companion.IS_FRIEND
import com.firstapp.androidchatapp.utils.Constants.Companion.NAME
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FriendAdapter(
    private val flow: MutableStateFlow<String?>,
    private val dbViewModel: DatabaseViewModel,
    private val friends: List<Friend>
) : RecyclerView.Adapter<FriendAdapter.ViewHolder>() {

    private lateinit var context: Context
    private var friendsID: List<String>? = null

    class ViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        val avatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        val name: TextView = itemView.findViewById(R.id.tvName)
        val uid: TextView = itemView.findViewById(R.id.tvID)
        val btnUnfriend: MaterialButton = itemView.findViewById(R.id.btnUnfriend)
        val btnChat: TextView = itemView.findViewById(R.id.btnChat)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        return ViewHolder(
            LayoutInflater.from(context).inflate(R.layout.view_friend, parent, false)
        )
    }

    override fun getItemCount(): Int = friends.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = friends[position]
        Glide.with(context).load(user.avatarURI).into(holder.avatar)
        holder.name.text = user.name
        holder.uid.text = user.uid
        holder.btnUnfriend.setOnClickListener {
            MaterialAlertDialogBuilder(context)
                .setTitle("Unfriend")
                .setBackground(
                    AppCompatResources.getDrawable(context, R.drawable.bg_sign_out_dialog)
                )
                .setMessage("Are your sure want to unfriend?")
                .setPositiveButton("Confirm") { _, _ ->
                    handleUnfriend(user)
                }.setNegativeButton("Cancel") { dialog, _ ->
                    dialog.cancel()
                }.show()
        }
        holder.btnChat.setOnClickListener {
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra(CONVERSATION_ID, user.conversationID)
            intent.putExtra(FRIEND_UID, user.uid)
            intent.putExtra(AVATAR_URI, user.avatarURI)
            intent.putExtra(NAME, user.name)
            CoroutineScope(Dispatchers.Main).launch {
                intent.putExtra(IS_FRIEND, true)
                context.startActivity(intent)
            }
        }
    }

    private fun handleUnfriend(friend: Friend) = CoroutineScope(Dispatchers.IO).launch {
        dbViewModel.removeFriend(friend.uid)
        flow.emit(friend.uid)
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Unfriended with ${friend.name}", Toast.LENGTH_SHORT).show()
        }
    }
}
package com.firstapp.androidchatapp.ui.fragments

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.firstapp.androidchatapp.R
import com.firstapp.androidchatapp.ui.activities.AddFriendActivity
import com.firstapp.androidchatapp.ui.activities.FriendActivity
import com.firstapp.androidchatapp.ui.activities.FriendRequestsActivity
import com.firstapp.androidchatapp.ui.activities.MainActivity
import com.firstapp.androidchatapp.ui.activities.SettingsActivity
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModel
import com.firstapp.androidchatapp.ui.viewmodels.MainViewModel
import com.firstapp.androidchatapp.utils.Functions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MenuFragment : Fragment(R.layout.fragment_menu) {

    private lateinit var container: MotionLayout
    private lateinit var coverLayer: FrameLayout
    private lateinit var addFriendNavBtn: TextView
    private lateinit var friendsNavBtn: TextView
    private lateinit var friendReqNavBtn: TextView
    private lateinit var settingsNavBtn: TextView
    private lateinit var imgPickerFragment: FragmentContainerView
    private lateinit var mainViewModel: MainViewModel
    private lateinit var dbViewModel: DatabaseViewModel
    private lateinit var changeAvatarBtn: RelativeLayout
    private lateinit var editNameBtn: ImageView
    private lateinit var avatarView: ImageView
    private var username = MutableLiveData<String>(null)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // get views
        addFriendNavBtn = view.findViewById(R.id.tvAddFriend)
        friendReqNavBtn = view.findViewById(R.id.tvFriendRequests)
        friendsNavBtn = view.findViewById(R.id.tvFriends)
        settingsNavBtn = view.findViewById(R.id.tvSettings)
        changeAvatarBtn = view.findViewById(R.id.changeAvatarBtn)
        editNameBtn = view.findViewById(R.id.ivEditName)
        imgPickerFragment = requireActivity().findViewById(R.id.frmImagePicker)
        avatarView = view.findViewById(R.id.ivAvatar)
        container = view.findViewById(R.id.container)
        coverLayer = view.findViewById(R.id.coverLayer)

        mainViewModel = (requireActivity() as MainActivity).mainViewModel
        dbViewModel = (requireActivity() as MainActivity).dbViewModel
        username = (requireActivity() as MainActivity).username

        // collect menu state
        mainViewModel.openMenu.observe(requireActivity()) { isOpen ->
            if (isOpen) container.transitionToEnd()
        }
        addEventListeners()
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                // collect image picker state
                mainViewModel.imgPickerState.collectLatest { openImgPicker ->
                    imgPickerFragment.visibility =
                        if (openImgPicker) View.VISIBLE else View.GONE
                }
            }

            // collect avatar state
            mainViewModel.avatarState.collectLatest { avatarURI ->
                if (avatarURI.isNotEmpty()) {
                    Glide.with(requireContext()).load(avatarURI).into(avatarView)
                }
            }
        }
    }

    private fun addEventListeners() {
        // add event listeners
        friendsNavBtn.setOnClickListener {
            startActivity(Intent(requireContext(), FriendActivity::class.java))
        }
        addFriendNavBtn.setOnClickListener {
            startActivity(Intent(requireContext(), AddFriendActivity::class.java))
        }
        friendReqNavBtn.setOnClickListener {
            startActivity(Intent(requireContext(), FriendRequestsActivity::class.java))
        }
        settingsNavBtn.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }
        coverLayer.setOnClickListener {
            lifecycleScope.launch {
                withContext(Dispatchers.Main) {
                    container.transitionToStart()
                }
                delay(resources.getInteger(R.integer.open_menu_duration).toLong())
                mainViewModel.openMenu.postValue(false)
            }
        }
        changeAvatarBtn.setOnClickListener {
            if (internetIsConnected()) {
                showImgPickerDialog()
            } else showNoInternetNotification()
        }
        editNameBtn.setOnClickListener {
            if (internetIsConnected()) {
                openEditNameDialog()
            } else showNoInternetNotification()
        }
    }

    private fun openEditNameDialog() {
        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_edit_name, LinearLayout(context), false)
        val dialog = MaterialAlertDialogBuilder(requireContext()).setView(dialogView).create()
        val input = dialogView.findViewById<TextInputEditText>(R.id.inputName)
        val confirmBtn = dialogView.findViewById<TextView>(R.id.btnConfirm)
        val cancelBtn = dialogView.findViewById<TextView>(R.id.btnCancel)
        username.observe(requireActivity()) {
            input.setText(it)
        }
        input.requestFocus()
        confirmBtn.setOnClickListener {
            val name = input.text.toString()
            if (name.isNotEmpty())
                changeName(name)
            // TODO: Show loading and close dialog
        }
        cancelBtn.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun changeName(name: String): Job = lifecycleScope.launch {
        dbViewModel.changeUserName(name)
        dbViewModel.changeCachedName(name)
    }

    private fun showNoInternetNotification() {
        Functions.showNoInternetNotification()
    }

    private fun showImgPickerDialog() {
        imgPickerFragment.visibility = View.VISIBLE
        lifecycleScope.launch {
            mainViewModel.imgPickerState.emit(true)
        }
    }

    private fun internetIsConnected(): Boolean {
        val cm =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo?.isConnected == true
    }
}
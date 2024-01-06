package com.firstapp.androidchatapp.ui.fragments

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.firstapp.androidchatapp.R
import com.firstapp.androidchatapp.ui.activities.AddFriendActivity
import com.firstapp.androidchatapp.ui.activities.FriendRequestsActivity
import com.firstapp.androidchatapp.ui.activities.SettingsActivity
import com.firstapp.androidchatapp.ui.viewmodels.MainViewModel
import com.firstapp.androidchatapp.utils.Functions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MenuFragment : Fragment(R.layout.fragment_menu) {

    private lateinit var container: MotionLayout
    private lateinit var coverLayer: FrameLayout
    private lateinit var addFriendNavBtn: TextView
    private lateinit var friendReqNavBtn: TextView
    private lateinit var settingsNavBtn: TextView
    private lateinit var imgPickerFragment: FragmentContainerView
    private lateinit var mainViewModel: MainViewModel
    private lateinit var changeAvatarBtn: RelativeLayout
    private lateinit var avatarView: ImageView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // get views
        addFriendNavBtn = view.findViewById(R.id.tvAddFriend)
        friendReqNavBtn = view.findViewById(R.id.tvFriendRequests)
        settingsNavBtn = view.findViewById(R.id.tvSettings)
        changeAvatarBtn = view.findViewById(R.id.changeAvatarBtn)
        imgPickerFragment = requireActivity().findViewById(R.id.frmImagePicker)
        avatarView = view.findViewById(R.id.ivAvatar)
        container = view.findViewById(R.id.container)
        coverLayer = view.findViewById(R.id.coverLayer)

        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        addEventListeners()
    }

    private fun addEventListeners() {
        // add event listeners
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
                delay(resources.getInteger(R.integer.open_menu_duration).toLong())
                mainViewModel.openMenu.emit(false)
            }
        }
        changeAvatarBtn.setOnClickListener {
            if (internetIsConnected()) {
                showImgPickerDialog()
            } else showNoInternetNotification()
        }
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

                // collect menu state
                mainViewModel.openMenu.collectLatest { open ->
                    if (open) container.transitionToEnd()
                    else container.transitionToStart()
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
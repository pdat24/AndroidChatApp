package com.firstapp.androidchatapp.ui.fragments

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.RelativeLayout
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.firstapp.androidchatapp.R
import com.firstapp.androidchatapp.ui.activities.MainActivity
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModel
import com.firstapp.androidchatapp.ui.viewmodels.MainViewModel
import com.firstapp.androidchatapp.utils.Constants
import com.firstapp.androidchatapp.utils.Constants.Companion.AVATAR_STORAGE_PATH
import com.firstapp.androidchatapp.utils.Functions
import com.google.android.gms.tasks.Task
import com.google.android.material.button.MaterialButton
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class AvatarPickerFragment : Fragment(R.layout.fragment_image_picker) {

    private lateinit var container: MotionLayout
    private lateinit var coverLayer: RelativeLayout
    private lateinit var cancelBtn: MaterialButton
    private lateinit var choosePhotoBtn: MaterialButton
    private lateinit var takePhotoBtn: MaterialButton
    private lateinit var mainViewModel: MainViewModel
    private lateinit var dbViewModel: DatabaseViewModel
    private lateinit var imgPickingResult: ActivityResultLauncher<Intent>
    private lateinit var imgCapturingResult: ActivityResultLauncher<Intent>
    private lateinit var loadingView: View
    private var allowDeleteAvatar = false
    private val storage = FirebaseStorage.getInstance()
    private val avatarStoragePath = AVATAR_STORAGE_PATH

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        coverLayer = view.findViewById(R.id.coverLayer)
        container = view.findViewById(R.id.container)
        cancelBtn = view.findViewById(R.id.btnCancel)
        choosePhotoBtn = view.findViewById(R.id.btnChoosePhoto)
        takePhotoBtn = view.findViewById(R.id.btnTakePhoto)
        loadingView = requireActivity().findViewById(R.id.viewLoading)

        mainViewModel = (requireActivity() as MainActivity).mainViewModel
        dbViewModel = (requireActivity() as MainActivity).dbViewModel

        coverLayer.setOnClickListener { closeFragment() }
        cancelBtn.setOnClickListener { closeFragment() }

        choosePhotoBtn.setOnClickListener { pickPhoto() }
        takePhotoBtn.setOnClickListener { captureImage() }

        lifecycleScope.launch {
            mainViewModel.imgPickerState.collectLatest { open ->
                if (open)
                    withContext(Dispatchers.Main) {
                        transitionToEnd()
                    }
            }
        }

        imgPickingResult = registerIntentResult {
            handleImgPickingActivityResult(it)
        }
        imgCapturingResult = registerIntentResult {
            handleImgCapturingActivityResult(it)
        }
    }

    private suspend fun showLoading() {
        withContext(Dispatchers.Main) {
            loadingView.visibility = View.VISIBLE
        }
    }

    private suspend fun hideLoading() {
        withContext(Dispatchers.Main) {
            loadingView.visibility = View.GONE
        }
    }

    private fun handleImgPickingActivityResult(result: ActivityResult) {
        lifecycleScope.launch {
            closeFragment()
            showLoading()
            val uri = result.data?.data
            if (uri != null) {
                changeAvatar(
                    dbViewModel.uploadFileByUri(
                        "$avatarStoragePath/${Functions.createUniqueString()}",
                        uri
                    )
                )
            }
            hideLoading()
        }
    }

    private fun handleImgCapturingActivityResult(result: ActivityResult) {
        lifecycleScope.launch {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            if (bitmap != null) {
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                closeFragment()
                showLoading()
                changeAvatar(
                    dbViewModel.uploadImageByBytes(
                        "$avatarStoragePath/${Functions.createUniqueString()}",
                        stream.toByteArray()
                    )
                )
                hideLoading()
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

    private fun transitionToStart() = container.transitionToStart()

    private fun transitionToEnd() = container.transitionToEnd()

    private fun closeFragment() {
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                transitionToStart()
            }
            // delay for complete animation
            delay(resources.getInteger(R.integer.img_picker_animation_duration).toLong())
            mainViewModel.imgPickerState.emit(false)
        }
    }

    private fun pickPhoto() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        imgPickingResult.launch(intent)
    }

    private fun captureImage() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        imgCapturingResult.launch(intent)
    }

    /**
     * Change avatar on remote and local database
     * @param downloadUri downloadUri of avatar on firebase storage
     */
    private fun changeAvatar(downloadUri: String?) {
        lifecycleScope.launch {
            if (downloadUri != null) {
                allowDeleteAvatar = true
                updateAvatarChange(downloadUri)
            }
        }
    }

    /**
     * Update new avatar and remove old avatar on firestore, local sqlite database and
     * emit avatar change
     */
    private suspend fun updateAvatarChange(avatarURI: String) =
        withContext(Dispatchers.Main) {
            // This observe function is required to get cached user info
            // you can use another way if can
            dbViewModel.getCachedUserInfo().observe(requireActivity()) {
                lifecycleScope.launch {
                    /**
                     * [allowDeleteAvatar] only is true when call function [changeAvatar]
                     * After removing old avatar immediately set [allowDeleteAvatar] is false
                     * to prevent this observer delete new avatar
                     */
                    if (allowDeleteAvatar) {
                        allowDeleteAvatar = false
                        deleteImgFromStorage(it.avatarURI)?.await()
                        // update on database
                        dbViewModel.changeUserAvatar(avatarURI)
                        // emit state
                        mainViewModel.avatarState.emit(avatarURI)
                        dbViewModel.changeCachedAvatar(avatarURI)
                    }
                }
            }
        }

    /**
     * delete image from storage if it's not in [Constants.DEFAULT_AVATAR_URIS]
     */
    private fun deleteImgFromStorage(downloadURI: String): Task<Void>? {
        if (Constants.DEFAULT_AVATAR_URIS.contains(downloadURI))
            return null
        return storage.getReferenceFromUrl(downloadURI).delete()
    }
}
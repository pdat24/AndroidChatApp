package com.firstapp.androidchatapp.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.firstapp.androidchatapp.repositories.LocalRepository
import kotlinx.coroutines.flow.MutableStateFlow

class MainViewModel: ViewModel() {
    var imgPickerState = MutableStateFlow(false)
    var avatarState = MutableStateFlow("")
}
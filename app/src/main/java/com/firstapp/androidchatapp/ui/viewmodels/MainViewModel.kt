package com.firstapp.androidchatapp.ui.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class MainViewModel: ViewModel() {
    var imgPickerState = MutableStateFlow(false)
    var avatarState = MutableStateFlow("")
}
package com.firstapp.androidchatapp.ui.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class MainViewModel : ViewModel() {
    var imgPickerState = MutableStateFlow(false)
    var openMenu = MutableLiveData(false)
    var avatarState = MutableStateFlow("")
    var tabPosition = 0
}
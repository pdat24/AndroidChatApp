package com.firstapp.androidchatapp.utils

import com.firstapp.androidchatapp.utils.Constants.Companion.NOT_LOGIN_ERROR_CODE
import com.google.firebase.auth.FirebaseAuthException

class Functions {
    companion object {

        fun throwUserNotLoginError() {
            throw FirebaseAuthException(NOT_LOGIN_ERROR_CODE, "user is not already login!")
        }
    }
}
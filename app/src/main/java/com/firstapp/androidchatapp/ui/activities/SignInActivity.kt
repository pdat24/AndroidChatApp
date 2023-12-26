package com.firstapp.androidchatapp.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.firstapp.androidchatapp.R
import com.firstapp.androidchatapp.models.MessageBoxesList
import com.firstapp.androidchatapp.models.User
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModel
import com.firstapp.androidchatapp.ui.viewmodels.DatabaseViewModelFactory
import com.firstapp.androidchatapp.utils.Constants.Companion.DEFAULT_AVATAR_URIS
import com.firstapp.androidchatapp.utils.Constants.Companion.USERS_COLLECTION_PATH
import com.firstapp.androidchatapp.utils.Functions.Companion.throwUserNotLoginError
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SignInActivity : AppCompatActivity() {

    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var emailWarning: TextView
    private lateinit var passwordWarning: TextView
    private lateinit var googleSignInResult: ActivityResultLauncher<Intent>
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val userDB = FirebaseFirestore.getInstance().collection(USERS_COLLECTION_PATH)
    private lateinit var dbViewModel: DatabaseViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)
        dbViewModel = ViewModelProvider(
            this,
            DatabaseViewModelFactory(this)
        )[DatabaseViewModel::class.java]

        // get views
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        emailWarning = findViewById(R.id.tvEmailWarning)
        passwordWarning = findViewById(R.id.tvPasswordWarning)

        // register activity result
        googleSignInResult = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleGoogleIntentData(result.data)
        }
    }

    private fun warningIncorrectPassword() {
        passwordWarning.text = getString(R.string.wrong_password_warning)
        passwordWarning.visibility = View.VISIBLE
        createVibration()
    }

    private fun createVibration() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val vibrationEffect = VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
        vibrator.vibrate(vibrationEffect)
    }

    /**
     *  Toggle email and password warnings on UI
     *  @param emailIsValid result after email is validated
     *  @param passwordIsValid result after password is validated
     *  @see checkCredentialFormat
     *  @see EmailPasswordCheckFormatResult
     * */
    private fun toggleWarnings(emailIsValid: Boolean, passwordIsValid: Boolean) {
        emailWarning.text = getString(R.string.email_warning)
        emailWarning.visibility =
            if (!emailIsValid) View.VISIBLE else View.INVISIBLE
        passwordWarning.text = getString(R.string.password_warning)
        passwordWarning.visibility =
            if (!passwordIsValid) View.VISIBLE else View.INVISIBLE
    }

    /**
     * Return random avatar uri from DEFAULT_AVATAR_URIS in class Constants
     * @return avatar uri
     * */
    private fun getRandomAvatarURI(): String {
        return DEFAULT_AVATAR_URIS[(0..9).random()]
    }

    /**
     * Return user name after signed in or return Exception if not already
     *
     * Note: user name can be displayName or the left side of sign '@' in email address
     *
     * Return displayName if user sign in with google account
     * @throws FirebaseAuthException
     */
    private fun getUserName(): String {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null)
            throwUserNotLoginError()
        return if (currentUser!!.displayName != null)
            currentUser.displayName!!
        else {
            "Anonymous"
        }
    }

    /**
     * Create new conversation, message box for new user and
     * save user on firebase firestore after sign in
     *
     * Note:
     * - This method must be called after user is created and signed in
     * - User is only saved if id is not existing to avoid user data is overwritten
     * */
    private fun trySaveUserOnDatabase() {
        CoroutineScope(Dispatchers.IO).launch {
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null)
                throwUserNotLoginError()
            // check id is existing or not
            if (!userDB.document(currentUser!!.uid).get().await().exists()) {
                dbViewModel.createUser(
                    User(
                        name = getUserName(),
                        avatarURI = getRandomAvatarURI(),
                        messageBoxListId = dbViewModel.createMsgBoxesList(MessageBoxesList())
                    ),
                    id = currentUser.uid
                )
            }
        }
    }

    /**
     * Create new user and sign in if email isn't already in use or
     * show an error message if already
     * @param email
     * @param password
     */
    private fun tryCreateUser(email: String, password: String) {
        firebaseAuth
            .createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                // sign in after create user
                firebaseAuth.signInWithEmailAndPassword(email, password).addOnSuccessListener {
                    signedInOK()
                    trySaveUserOnDatabase()
                }
            }
            .addOnFailureListener { e ->
                if (e is FirebaseAuthUserCollisionException)
                    warningIncorrectPassword()
            }
    }

    /**
     * Check the email and password format
     * @return An object containing the validation result of email and password
     * @see EmailPasswordCheckFormatResult
     * */
    private fun checkCredentialFormat(
        email: String,
        password: String
    ): EmailPasswordCheckFormatResult {
        val emailPattern = """^[a-zA-Z0-9.+-_%]+@[a-zA-Z0-9-.]+\.[a-zA-Z]{2,}$"""
        val passwordPattern = "^[a-z0-9A-Z]{6,}$"
        var emailIsValid = true
        var passwordIsValid = true
        // validate email
        if (!Regex(emailPattern).matches(email))
            emailIsValid = false
        // validate password
        if (!Regex(passwordPattern).matches(password))
            passwordIsValid = false
        return EmailPasswordCheckFormatResult(emailIsValid, passwordIsValid)
    }

    fun signInWithEmailAndPassword(view: View) {
        val email = emailInput.text.toString()
        val password = passwordInput.text.toString()
        val validation = checkCredentialFormat(email, password)
        if (validation.emailIsValid && validation.passwordIsValid) {
            // sign in if email and password are correct format
            firebaseAuth
                .signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    signedInOK()
                }
                .addOnFailureListener { e ->
                    // catch invalid credentials error
                    if (e is FirebaseAuthInvalidCredentialsException) {
                        tryCreateUser(email, password)
                    }
                }
        } else {
            // show warnings if credential is invalid
            toggleWarnings(validation.emailIsValid, validation.passwordIsValid)
            createVibration()
        }
    }

    fun signInWithFacebook(view: View) {
        // TODO: sign in with Facebook
    }

    fun signInWithGoogle(view: View) {
        val googleOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_server_client_id))
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(this, googleOptions)
        client.revokeAccess()
        googleSignInResult.launch(client.signInIntent)
    }

    /**
     * Sign in with Google using intent data
     * @param intentData  result of google client sign in intent
     * @see signInWithGoogle
     */
    private fun handleGoogleIntentData(intentData: Intent?) {
        try {
            val account =
                GoogleSignIn.getSignedInAccountFromIntent(intentData)
                    .getResult(ApiException::class.java)
            // sign in
            firebaseAuth.signInWithCredential(
                GoogleAuthProvider.getCredential(account.idToken, null)
            ).addOnSuccessListener {
                signedInOK()
                trySaveUserOnDatabase()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Processing after successful authentication
     */
    private fun signedInOK() {
        startActivity(
            Intent(this@SignInActivity, MainActivity::class.java)
        )
        finish()
    }

    /**
     * Contains 2 fields that describe email and password is correct format
     * */
    data class EmailPasswordCheckFormatResult(
        var emailIsValid: Boolean,
        var passwordIsValid: Boolean,
    )
}

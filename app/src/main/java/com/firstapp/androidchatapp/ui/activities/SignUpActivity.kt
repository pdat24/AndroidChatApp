package com.firstapp.androidchatapp.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.firstapp.androidchatapp.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider

class SignUpActivity : AppCompatActivity() {

    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var emailWarning: TextView
    private lateinit var passwordWarning: TextView
    private val GOOGLE_SIGN_IN_RC = 0
    private val FACEBOOK_SIGN_IN_RC = 1
    private val firebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // get views
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        emailWarning = findViewById(R.id.tvEmailWarning)
        passwordWarning = findViewById(R.id.tvPasswordWarning)
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
     * Create new user if email isn't already in use or
     * show an error message if already
     * @param email
     * @param password
     */
    private fun tryCreateUser(email: String, password: String) {
        firebaseAuth
            .createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                signedInOK()
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
        startActivityIfNeeded(client.signInIntent, GOOGLE_SIGN_IN_RC)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // sign in with google
        if (requestCode == GOOGLE_SIGN_IN_RC) {
            try {
                val account =
                    GoogleSignIn.getSignedInAccountFromIntent(data)
                        .getResult(ApiException::class.java)
                // sign in
                firebaseAuth.signInWithCredential(
                    GoogleAuthProvider.getCredential(account.idToken, null)
                ).addOnSuccessListener {
                    signedInOK()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Processing after successful authentication
     */
    private fun signedInOK() {
        startActivity(
            Intent(this, MainActivity::class.java)
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

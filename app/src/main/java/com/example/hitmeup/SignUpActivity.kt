package com.example.hitmeup

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseUser


class SignUpActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var signUp: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var redopassword: EditText

    val textWatcher = object : TextWatcher {
        override fun afterTextChanged(p0: Editable?) {}

        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

        override fun onTextChanged(newString: CharSequence, start: Int, before: Int, count: Int) {
            val inputtedUsername: String = username.text.toString().trim()
            val inputtedPassword: String = password.text.toString().trim()
            val enabled: Boolean = inputtedUsername.isNotEmpty() && inputtedPassword.isNotEmpty()

            // Kotlin shorthand for login.setEnabled(enabled)
            signUp.isEnabled = enabled
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        val preferences: SharedPreferences =
            getSharedPreferences("android-tweets", Context.MODE_PRIVATE)

        // The "id" used here is what we had set in XML in the "id" field
        username = findViewById(R.id.username)
        password = findViewById(R.id.password)
        redopassword = findViewById(R.id.redopassword)

        signUp = findViewById(R.id.signUp)
        progressBar = findViewById(R.id.progressBar)

        //check if login was clicked
        signUp.setOnClickListener {
            firebaseAnalytics.logEvent("signup_clicked", null)

            val inputtedUsername: String = username.text.toString().trim()
            val inputtedPassword: String = password.text.toString().trim()
            val inputtedRedoPass: String = redopassword.text.toString().trim()

            if(inputtedPassword == inputtedRedoPass){
                firebaseAuth
                    .createUserWithEmailAndPassword(inputtedUsername, inputtedPassword)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            firebaseAnalytics.logEvent("signup_success", null)
                            val currentUser: FirebaseUser? = firebaseAuth.currentUser
                            val email = currentUser?.email
                            Toast.makeText(this, "Registered as $email", Toast.LENGTH_SHORT).show()

                            // Save the inputted username to file
                            preferences
                                .edit()
                                .putString("SAVED_USERNAME", email)
                                .apply()

                            //THIS CONTROLS WHICH INTENT, UNCOMMENT TO GO TO SOCIAL LOGIN
                            val user_name = email?.split("@")?.get(0)
                            Log.e("USER NAME", user_name)
                            val intent = Intent(this, SocialLoginActivity::class.java)
                            intent.putExtra("user_name", user_name)
                            startActivity(intent)
                        } else {
                            firebaseAnalytics.logEvent("signup_failed", null)
                            val exception = task.exception
                            Toast.makeText(this, "Registration failed: $exception", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
            }
            else{
                // The UI can only be updated from the UI Thread
                Toast.makeText(
                    this@SignUpActivity,
                    "Passwords Don't match!",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }


    }
}

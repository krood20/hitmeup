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
import com.google.firebase.auth.TwitterAuthProvider
import com.twitter.sdk.android.core.Callback
import com.twitter.sdk.android.core.TwitterException
import com.twitter.sdk.android.core.TwitterSession
import com.twitter.sdk.android.core.identity.TwitterLoginButton
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.doAsync


class MainActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var login: Button
    private lateinit var signUp: Button
    private lateinit var progressBar: ProgressBar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        val preferences: SharedPreferences = getSharedPreferences("android-tweets", Context.MODE_PRIVATE)

        // The "id" used here is what we had set in XML in the "id" field
        username = findViewById(R.id.username)
        password = findViewById(R.id.password)
        login = findViewById(R.id.login)
        signUp = findViewById(R.id.signUp)
        signUp.isEnabled = true
        progressBar = findViewById(R.id.progressBar)

        // Kotlin shorthand for login.setEnabled(false)
        login.isEnabled = true
        username.setText(preferences.getString("SAVED_USERNAME", ""))
        password.setText(preferences.getString("SAVED_PASS", ""))
        username.addTextChangedListener(textWatcher)
        password.addTextChangedListener(textWatcher)

        //check if login was clicked
        login.setOnClickListener {
            firebaseAnalytics.logEvent("login_clicked", null)

            val inputtedUsername: String = username.text.toString().trim()
            val inputtedPassword: String = password.text.toString().trim()

            firebaseAuth
                .signInWithEmailAndPassword(inputtedUsername, inputtedPassword)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {

                        firebaseAnalytics.logEvent("login_success", null)

                        val currentUser: FirebaseUser? = firebaseAuth.currentUser
                        val email = currentUser?.email
                        Toast.makeText(this, "Logged in as $email", Toast.LENGTH_SHORT).show()

                        // Save the inputted username to file
                        preferences
                            .edit()
                            .putString("SAVED_USERNAME", username.text.toString())
                            .apply()


                        preferences
                            .edit()
                            .putString("SAVED_PASS", inputtedPassword)
                            .apply()

                        //THIS CONTROLS WHICH INTENT, UNCOMMENT TO GO TO SOCIAL LOGIN
                        val user_name = email?.split("@")?.get(0)
                        Log.e("USER NAME", user_name)
                        val intent = Intent(this, SocialLoginActivity::class.java)
                        intent.putExtra("user_name", user_name)
                        startActivity(intent)
                    } else {
                        val exception = task.exception

                        // Example of logging some extra metadata (the error reason) with our analytic
                        val reason = if (exception is FirebaseAuthInvalidCredentialsException) "invalid_credentials" else "connection_failure"
                        val bundle = Bundle()
                        bundle.putString("error_type", reason)

                        firebaseAnalytics.logEvent("login_failed", bundle)

                        Toast.makeText(this, "Registration failed: $exception", Toast.LENGTH_SHORT).show()

                    }
                }
        }


        signUp.setOnClickListener {
            //THIS CONTROLS WHICH INTENT, UNCOMMENT TO GO TO SOCIAL LOGIN
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

    }

    private val textWatcher = object : TextWatcher {
        override fun afterTextChanged(p0: Editable?) {}

        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

        override fun onTextChanged(newString: CharSequence, start: Int, before: Int, count: Int) {
            val inputtedUsername: String = username.text.toString().trim()
            val inputtedPassword: String = password.text.toString().trim()
            val enabled: Boolean = inputtedUsername.isNotEmpty() && inputtedPassword.isNotEmpty()

            // Kotlin shorthand for login.setEnabled(enabled)
            login.isEnabled = enabled
        }
    }



}

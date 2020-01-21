package com.example.hitmeup

import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.core.view.isVisible
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.TwitterAuthProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import com.squareup.picasso.Picasso
import com.twitter.sdk.android.core.*
import com.twitter.sdk.android.core.identity.TwitterLoginButton
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_social_login.*
import kotlinx.android.synthetic.main.activity_social_login.btnScan
import org.jetbrains.anko.doAsync
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*


class SocialLoginActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseAnalytics: FirebaseAnalytics
//    private var loginButton: TwitterLoginButton? = null
    private lateinit var searchButton: Button
    private lateinit var searched_handle: EditText
    private lateinit var icon: ImageView
    private lateinit var username_label: TextView
    private lateinit var handle_label: TextView
    private lateinit var user_display_name: String
    private lateinit var iv: ImageView
    internal var bitmap: Bitmap? = null
    var scannedResult: String = ""



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val twitterManager = TwitterManager()

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        // Configure Twitter SDK
        val authConfig = TwitterAuthConfig(
            getString(R.string.twitter_api_key),
            getString(R.string.twitter_api_secret))

        val twitterConfig = TwitterConfig.Builder(this)
            .twitterAuthConfig(authConfig)
            .build()

        Twitter.initialize(twitterConfig)

        // Inflate layout (must be done after Twitter is configured)
        setContentView(R.layout.activity_social_login)
        searchButton = findViewById(R.id.search_button)
        icon = findViewById(R.id.profile_pic_label)
        username_label= findViewById(R.id.username_label)
        handle_label= findViewById(R.id.handle_label)


        //initialize listeners
        buttonSignout.setOnClickListener(this)
        searchButton.setOnClickListener(this)

        //set things on the screen
        status.text = intent.getStringExtra("user_name")
        user_display_name = intent.getStringExtra("user_name")

        //generate QR code
        iv = findViewById(R.id.iv) as ImageView
        try {
            val user_url :String = "https://twitter.com/".plus(user_display_name) //NOTE: Just use user_display_name to generate qr codes for this app
            bitmap = TextToImageEncode(user_url)                                  //this gets us a URL, we just want username for my app
            iv!!.setImageBitmap(bitmap)
            val path = saveImage(bitmap)  //give read write permission
            Toast.makeText(this@SocialLoginActivity, "QRCode saved to -> $path", Toast.LENGTH_SHORT).show()
        } catch (e: WriterException) {
            e.printStackTrace()
        }

//        loginButton = findViewById(R.id.buttonTwitterLogin)
//
//        loginButton?.callback = object : Callback<TwitterSession>() {
//            override fun success(result: Result<TwitterSession>) {
//                Log.e("LOGIN SUCCESS", "twitterLogin:success$result")
//                handleTwitterSession(result.data)
//            }
//
//            override fun failure(exception: TwitterException) {
//                Log.e("FAILURE", "twitterLogin:failure", exception)
//            }
//        }

        btnScan.setOnClickListener {
            run {
                IntentIntegrator(this@SocialLoginActivity).initiateScan();
            }
        }

        followButton.setOnClickListener{
            run{
                Log.e("FOLLOW BUTTON", "ONCLICKLISTENER")
                val handle: EditText = findViewById(R.id.searched_handle)
                getURL(handle.text.toString())
            }
        }

        buttonSignout.setOnClickListener{
            run{
                signOut()
            }
        }

    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = firebaseAuth.currentUser
        updateUI(currentUser)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        var result: IntentResult? = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)

        if(result != null){

            if(result.contents != null){
                scannedResult = result.contents
                getURL(scannedResult)
                //could also just make it the searched value
//                get_user_info(scannedResult)
            } else {
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState?.putString("scannedResult", scannedResult)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)

        savedInstanceState?.let {
            if(isNullOrEmpty(it.getString("scannedResult"))){
                scannedResult = ""
            }
            else{
                scannedResult = it.getString("scannedResult") as String
            }
        }
    }

    fun isNullOrEmpty(str: String?): Boolean {
        if (str != null && !str.isEmpty())
            return false
        return true
    }

    private fun handleTwitterSession(session: TwitterSession) {
        val credential = TwitterAuthProvider.getCredential(
            session.authToken.token,
            session.authToken.secret)

        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    val user = firebaseAuth.currentUser
                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun signOut() {
        firebaseAuth.signOut()
        updateUI(null)
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    private fun follow(handle : String){
        doAsync {
            val twitterManager = TwitterManager()
            val token = twitterManager.retrieveOAuthToken(
                getString(R.string.twitter_api_key),
                getString(R.string.twitter_api_secret))

            Log.e("Follow Status", twitterManager.followUser(token, handle))
        }
    }

    //TODO: add in a functionality to see the user before following them (public API)
    private fun get_user_info(handle : String){
        var info = TwitterProfile("","","")
        doAsync {
            val twitterManager = TwitterManager()
            val token = twitterManager.retrieveOAuthToken(
                getString(R.string.twitter_api_key),
                getString(R.string.twitter_api_secret))

            info = twitterManager.getUserInfo(token, handle)
        }

        //stall until info changes
        while(info == TwitterProfile("","","")){}

        //getting all the information
        Log.e("GETTINGUSERINFO", info.toString())

        var profilePic: String? = info.profile_pic
        var name: String? = info.name

        // Load the profile picture into our icon ImageView
        Picasso
            .get()
            .load(profilePic)
            .into(icon)

        username_label.setText(name)
        handle_label.setText(handle)
    }

    private fun getURL(handle : String){
        //initialize web intent --> go to external page
        val user_url :String = "https://twitter.com/".plus(handle)
        val webintent = Intent (Intent.ACTION_VIEW)
        webintent.data = Uri.parse(user_url)
        startActivity(webintent)
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            //status.text = user.email//getString(R.string.twitter_status_fmt, user.displayName)
            status.text = intent.getStringExtra("user_name")

        } else {
            status.setText(R.string.signed_out)
        }

    }

    override fun onClick(v: View) {
        val i = v.id
//        val handle = "elonmusk"
        if (i == R.id.buttonSignout) {
            //ALTERNATIVE TO FOLLOW --> dealing with URL and bringing you to their page automatically
            signOut()
        }
        else if(i == R.id.search_button){
            searchButton = findViewById(R.id.search_button)
            searched_handle = findViewById(R.id.searched_handle)
            if (searched_handle.text.isBlank()) {
                //TODO: Maybe add in a pop up asking for a handle to follow
                Log.e("Empty Handle", "Enter a different value")
            }
            else{
                Log.e("Searching for...", searched_handle.text.toString())
                var current_handle:String = searched_handle.text.toString()
                get_user_info(current_handle)
            }
            followButton.setVisibility(View.VISIBLE)
            icon.setVisibility(View.VISIBLE)
        }
    }


    //functions to help iwth generating a qr code
    fun saveImage(myBitmap: Bitmap?): String {
        val bytes = ByteArrayOutputStream()
        myBitmap!!.compress(Bitmap.CompressFormat.JPEG, 90, bytes)
        val wallpaperDirectory = File(
            Environment.getExternalStorageDirectory().toString() + IMAGE_DIRECTORY)
        // have the object build the directory structure, if needed.

        if (!wallpaperDirectory.exists()) {
            Log.d("dirrrrrr", "" + wallpaperDirectory.mkdirs())
            wallpaperDirectory.mkdirs()
        }

        try {
            val f = File(wallpaperDirectory, Calendar.getInstance()
                .timeInMillis.toString() + ".jpg")
            f.createNewFile()   //give read write permission
            val fo = FileOutputStream(f)
            fo.write(bytes.toByteArray())
            MediaScannerConnection.scanFile(this,
                arrayOf(f.path),
                arrayOf("image/jpeg"), null)
            fo.close()
            Log.d("TAG", "File Saved::--->" + f.absolutePath)

            return f.absolutePath
        } catch (e1: IOException) {
            e1.printStackTrace()
        }

        return ""

    }

    @Throws(WriterException::class)
    private fun TextToImageEncode(Value: String): Bitmap? {
        val bitMatrix: BitMatrix
        try {
            bitMatrix = MultiFormatWriter().encode(
                Value,
                BarcodeFormat.QR_CODE,
                QRcodeWidth, QRcodeWidth, null
            )

        } catch (Illegalargumentexception: IllegalArgumentException) {

            return null
        }

        val bitMatrixWidth = bitMatrix.getWidth()

        val bitMatrixHeight = bitMatrix.getHeight()

        val pixels = IntArray(bitMatrixWidth * bitMatrixHeight)

        for (y in 0 until bitMatrixHeight) {
            val offset = y * bitMatrixWidth

            for (x in 0 until bitMatrixWidth) {

                pixels[offset + x] = if (bitMatrix.get(x, y))
                    resources.getColor(R.color.black)
                else
                    resources.getColor(R.color.white)
            }
        }
        val bitmap = Bitmap.createBitmap(bitMatrixWidth, bitMatrixHeight, Bitmap.Config.ARGB_4444)

        bitmap.setPixels(pixels, 0, 400, 0, 0, bitMatrixWidth, bitMatrixHeight)
        return bitmap
    }


    companion object {
        private const val TAG = "TwitterLogin"
        val QRcodeWidth = 400
        private val IMAGE_DIRECTORY = "/QRcodeDemonuts"
    }


}
package com.example.hitmeup

import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.maps.model.LatLng
import com.squareup.picasso.Picasso
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

class TwitterManager {

    // We don't have to supply a value here, since we have an init block.
    private val okHttpClient: OkHttpClient

    // An init block is similar to having a longer constructor in Java - it allows us to run
    // extra code during initialization. All variables must be set by the end of the init block.
    init {
        // Turn on console logging for our network traffic, useful during development
        val builder = OkHttpClient.Builder()
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY

        builder.addInterceptor(logging)
        okHttpClient = builder.build()
    }

    // Twitter's APIs are protected by OAuth. APIs from other companies (like Yelp) do not require OAuth.
    fun retrieveOAuthToken(
        apiKey: String,
        apiSecret: String
    ): String? {

        // Encoding for a URL -- converts things like spaces into %20
        val encodedKey = URLEncoder.encode(apiKey, "UTF-8")
        val encodedSecret = URLEncoder.encode(apiSecret, "UTF-8")

        // Concatenate the two together, with a colon inbetween
        val combinedEncoded = "$encodedKey:$encodedSecret"

        // Base-64 encode the combined string
        // https://en.wikipedia.org/wiki/Base64
        val base64Combined = Base64.encodeToString(
            combinedEncoded.toByteArray(), Base64.NO_WRAP)

        val requestBody = "grant_type=client_credentials"
            .toRequestBody(
                contentType = "application/x-www-form-urlencoded".toMediaType()
            )

        val request = Request.Builder()
            .url("https://api.twitter.com/oauth2/token")
            .header("Authorization", "Basic $base64Combined")
            .post(requestBody)
            .build()

        val response = okHttpClient.newCall(request).execute()
        val responseString = response.body?.string()

        if (response.isSuccessful && !responseString.isNullOrEmpty()) {
            // Parse JSON
            val json = JSONObject(responseString)
            val token = json.getString("access_token")

            return token
        } else {
            // API request failed - ideally we should think about returning null or an Exception
            return ""
        }
    }

    fun followUser(
        oAuthToken: String?,
        handle: String
    ): String? {
        // Build our request to turn - for now, using a hardcoded OAuth token
        val json = """
            "screen_name": $handle
            "follow": true
            }
            """.trimIndent()

        val url: String = "https://api.twitter.com/1.1/friendships/create.json?screen_name=$handle&follow=true"
        val body: RequestBody =
            json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $oAuthToken")
            .post(body)
            .build()

        //execute post request
        val response = okHttpClient.newCall(request).execute()
        val responseString: String? = response.body?.string()

        Log.e("RESPONSE", responseString)

        return responseString

    }

    fun getUserInfo(
        oAuthToken: String?,
        handle: String
    ): TwitterProfile {
        val url: String = "https://api.twitter.com/1.1/users/lookup.json?screen_name=$handle"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $oAuthToken")
            .build()

        //execute post request


        // Calling .execute actually makes the network request, blocking the thread until the server
        // responds, or the request times out.
        // If there are any connection or network errors, .execute will throw an Exception.
        val response = okHttpClient.newCall(request).execute()
        val responseString: String? = response.body?.string()

        //dealing with getting the actual URL
        //TODO: JSON not being read correctly, am I structuring it right?
        val jsonArray = JSONArray(responseString)
        val jsonObject: JSONObject = jsonArray.getJSONObject(0)
        var profile_pic: String? = jsonObject.get("profile_image_url_https").toString()
        var name: String? = jsonObject.get("name").toString()

        if(profile_pic.isNullOrEmpty()){
            profile_pic = jsonObject.get("profile_image_url").toString()
        }

        val profile = TwitterProfile(
            handle = handle,
            name = name,
            profile_pic = profile_pic
        )

        return profile
    }

}
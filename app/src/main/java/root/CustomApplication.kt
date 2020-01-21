package root

import android.app.Application
import android.util.Log
import com.example.hitmeup.R
import com.twitter.sdk.android.core.DefaultLogger
import com.twitter.sdk.android.core.Twitter
import com.twitter.sdk.android.core.TwitterAuthConfig
import com.twitter.sdk.android.core.TwitterConfig

class CustomApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val config = TwitterConfig.Builder(this)
            .logger(DefaultLogger(Log.DEBUG))
            .twitterAuthConfig(
                TwitterAuthConfig(
                resources.getString(R.string.twitter_api_key),
                resources.getString(R.string.twitter_api_secret))
            )
            .debug(true)
            .build()
        Twitter.initialize(config)
    }
}
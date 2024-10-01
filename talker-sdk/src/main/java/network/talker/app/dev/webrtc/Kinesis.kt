package network.talker.app.dev.webrtc

import android.app.Application
import android.util.Log
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.mobile.client.AWSMobileClient
import network.talker.app.dev.LOG_TAG
import org.json.JSONException
import org.json.JSONObject

internal object Kinesis : Application() {
    val credentialsProvider: AWSCredentialsProvider
        get() = AWSMobileClient.getInstance()

    val auth : AWSMobileClient
        get() = AWSMobileClient.getInstance()
    val region: String?
        get() {
            val configuration = AWSMobileClient.getInstance().configuration
                ?: throw IllegalStateException("awsconfiguration.json has not been properly configured!")

            val jsonObject = configuration.optJsonObject("CredentialsProvider")

            var region: String? = null
            try {
                region =
                    ((jsonObject["CognitoIdentity"] as JSONObject)["Default"] as JSONObject)["Region"] as String
            } catch (e: JSONException) {
                Log.e(LOG_TAG, "Got exception when extracting region from cognito setting.", e)
            }
            return region
        }
}

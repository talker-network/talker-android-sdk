package network.talker.sdk.webrtc

import android.content.Context
import android.util.Log
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.UserStateDetails
import com.amazonaws.mobile.config.AWSConfiguration
import network.talker.sdk.LOG_TAG
import network.talker.sdk.networking.data.Cognito
import org.json.JSONObject
import java.util.concurrent.CountDownLatch

internal fun initializeMobileClient(
    client: AWSMobileClient,
    applicationContext: Context,
    config: Cognito
) {
    val latch = CountDownLatch(1)
    client.initialize(applicationContext, AWSConfiguration(
        JSONObject(
            "{\n" +
                    "  \"Version\": \"${config.Version}\",\n" +
                    "  \"CredentialsProvider\": {\n" +
                    "    \"CognitoIdentity\": {\n" +
                    "      \"Default\": {\n" +
                    "        \"PoolId\": \"${config.CredentialsProvider.CognitoIdentity.Default.PoolId}\",\n" +
                    "        \"Region\": \"${config.CredentialsProvider.CognitoIdentity.Default.Region}\"\n" +
                    "      }\n" +
                    "    }\n" +
                    "  },\n" +
                    "  \"IdentityManager\": {\n" +
                    "    \"Default\": {}\n" +
                    "  },\n" +
                    "  \"CognitoUserPool\": {\n" +
                    "    \"Default\": {\n" +
                    "      \"AppClientSecret\": \"${config.CognitoUserPool.Default.AppClientSecret}\",\n" +
                    "      \"AppClientId\": \"${config.CognitoUserPool.Default.AppClientId}\",\n" +
                    "      \"PoolId\": \"${config.CognitoUserPool.Default.PoolId}\",\n" +
                    "      \"Region\": \"us-east-2\"\n" +
                    "    }\n" +
                    "  }\n" +
                    "}"
        )
    ) ,object : Callback<UserStateDetails> {
        override fun onResult(result: UserStateDetails) {
            Log.d(
                LOG_TAG, "user state: " + result.userState
            )
            Log.d(
                "@@@@",
                client.configuration.configuration
            )
            latch.countDown()
        }

        override fun onError(e: java.lang.Exception) {
            Log.e(
                LOG_TAG, "onError: Initialization error of the mobile client", e
            )
            latch.countDown()
        }
    })
    try {
        latch.await()
    } catch (e: InterruptedException) {
        e.printStackTrace()
    }
}
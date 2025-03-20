package network.talker.sdk.utils

import android.content.Context
import android.widget.Toast
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSSessionCredentials
import java.net.URI
import java.util.Date
import java.util.Optional

internal fun getSignedUri(
    endpoint: String,
    mCreds : AWSCredentials?,
    context : Context,
    mWssEndpoint : String?,
    mRegion : String
): URI? {
    val accessKey = mCreds!!.awsAccessKeyId
    val secretKey = mCreds!!.awsSecretKey
    val sessionToken = Optional.of(mCreds!!)
        .filter { creds: AWSCredentials? -> creds is AWSSessionCredentials }
        .map { awsCredentials: AWSCredentials -> awsCredentials as AWSSessionCredentials }
        .map { obj: AWSSessionCredentials -> obj.sessionToken }.orElse("")
    if (accessKey.isEmpty() || secretKey.isEmpty()) {
        Toast.makeText(context, "Failed to fetch credentials!", Toast.LENGTH_LONG).show()
        return null
    }

    return AwsV4Signer.sign(
        URI.create(endpoint),
        accessKey,
        secretKey,
        sessionToken,
        URI.create(mWssEndpoint),
        mRegion,
        Date().time
    )
}
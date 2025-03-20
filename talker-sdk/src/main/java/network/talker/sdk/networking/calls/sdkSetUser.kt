package network.talker.sdk.networking.calls

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.talker.sdk.networking.RetrofitClient
import network.talker.sdk.networking.data.CreateUserModel
import network.talker.sdk.networking.data.ErrorData
import network.talker.sdk.networking.data.SetUserModelRequest
import network.talker.sdk.networking.data.SetUserModelRequestWithPrevId
import network.talker.sdk.sharedPreference.SharedPreference
import network.talker.sdk.utils.AppUtils

internal fun sdkSetUser(
    context: Context,
    sdkKey: String,
    userId: String,
    onSuccess: (CreateUserModel) -> Unit = {},
    onError: (ErrorData) -> Unit = {},
    onInternetNotAvailable: () -> Unit = {},
    fcmToken: String = "testing"
) {
    if (AppUtils.isNetworkAvailable(context)) {
        val sharedPreference = SharedPreference(context)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val response = if (sharedPreference.getPrevUserId()
                        .isNotBlank() && sharedPreference.getPrevUserId() != userId
                ) RetrofitClient.retrofitApiService.SdkSetUserAPIWithPrevId(
                    token = sdkKey,
                    body = SetUserModelRequestWithPrevId(
                        prev_user_id = sharedPreference.getPrevUserId(),
                        fcm_token = fcmToken,
                        user_id = userId
                    )
                ) else RetrofitClient.retrofitApiService.SdkSetUserAPI(
                    token = sdkKey,
                    body = SetUserModelRequest(
                        fcm_token = fcmToken,
                        user_id = userId
                    )
                )
                if (response.isSuccessful && response.body() != null && response.code() == 200) {
                    val data = response.body()
                    if (data != null) {
                        withContext(Dispatchers.Main) {
                            onSuccess(data)
                        }
                    }
                } else {
                    onError(
                        ErrorData(
                            status = response.code(),
                            message = response.message()
                        )
                    )
                }
            } catch (e: Exception) {
                onError(
                    ErrorData(
                        status = 0,
                        message = e.localizedMessage ?: "Some error occurred"
                    )
                )
            }
        }
    } else {
        onInternetNotAvailable()
    }
}
package network.talker.app.dev.networking.calls

import android.content.Context
import network.talker.app.dev.networking.RetrofitClient
import network.talker.app.dev.networking.data.CreateUserModel
import network.talker.app.dev.networking.data.ErrorData
import network.talker.app.dev.networking.data.SetUserModelRequest
import network.talker.app.dev.utils.AppUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.talker.app.dev.sharedPreference.SharedPreference

internal fun sdkSetUser(
    context : Context,
    sdkKey : String,
    userId : String,
    onSuccess : (CreateUserModel) -> Unit = {},
    onError : (ErrorData) -> Unit = {},
    onInternetNotAvailable : () -> Unit = {},
    fcmToken : String = "testing"
){
    if (AppUtils.isNetworkAvailable(context)){
        val sharedPreference = SharedPreference(context)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.retrofitApiService.SdkSetUserAPI(
                    token = sdkKey,
                    body = SetUserModelRequest(
                        prev_user_id = sharedPreference.getUserData().user_id.ifEmpty { userId },
                        fcm_token = fcmToken,
                        user_id = userId
                    )
                )
                if (response.isSuccessful && response.body() != null && response.code() == 200){
                    val data = response.body()
                    if (data != null) {
                        withContext(Dispatchers.Main){
                            onSuccess(data)
                        }
                    }
                }else{
                    onError(
                        ErrorData(
                            status = response.code(),
                            message = response.message()
                        )
                    )
                }
            }catch (e : Exception){
                onError(
                    ErrorData(
                        status = 0,
                        message = e.localizedMessage ?: "Some error occurred"
                    )
                )
            }
        }
    }else{
        onInternetNotAvailable()
    }
}
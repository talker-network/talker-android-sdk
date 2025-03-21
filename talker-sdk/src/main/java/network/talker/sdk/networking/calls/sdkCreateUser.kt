package network.talker.sdk.networking.calls

import android.content.Context
import network.talker.sdk.networking.RetrofitClient
import network.talker.sdk.networking.data.CreateUserModel
import network.talker.sdk.networking.data.CreateUserModelRequest
import network.talker.sdk.networking.data.ErrorData
import network.talker.sdk.utils.AppUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.talker.sdk.networking.data.CreateUserModelRequestWithPrevId
import network.talker.sdk.sharedPreference.SharedPreference

internal fun sdkCreateUser(
    context : Context,
    sdkKey : String,
    name : String,
    fcmToken : String,
    onSuccess : (CreateUserModel) -> Unit = {},
    onError : (ErrorData) -> Unit = {},
    onInternetNotAvailable : () -> Unit = {},
){
    val sharedPreference = SharedPreference(context)
    if (AppUtils.isNetworkAvailable(context)){
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val response = if (sharedPreference.getPrevUserId().isBlank()) RetrofitClient.retrofitApiService.SdkCreateUserAPI(
                    token = sdkKey,
                    body = CreateUserModelRequest(
                        name = name,
                        fcm_token = fcmToken
                    )
                ) else RetrofitClient.retrofitApiService.SdkCreateUserAPIWithPrevID(
                    token = sdkKey,
                    body = CreateUserModelRequestWithPrevId(
                        name = name,
                        fcm_token = fcmToken,
                        prev_user_id = sharedPreference.getPrevUserId()
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
                    withContext(Dispatchers.Main){
                        onError(
                            ErrorData(
                                status = response.code(),
                                message = response.message()
                            )
                        )
                    }
                }
            }catch (e : Exception){
                withContext(Dispatchers.Main){
                    onError(
                        ErrorData(
                            status = 0,
                            message = e.localizedMessage ?: "Some error occurred"
                        )
                    )
                }
            }
        }
    }else{
        onInternetNotAvailable()
    }
}
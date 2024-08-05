package com.dev.talkersdk.sdk.networking.calls

import android.content.Context
import com.dev.talkersdk.sdk.networking.RetrofitClient
import com.dev.talkersdk.sdk.networking.data.CreateUserModel
import com.dev.talkersdk.sdk.networking.data.ErrorData
import com.dev.talkersdk.sdk.networking.data.SetUserModelRequest
import com.dev.talkersdk.sdk.utils.AppUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun sdkSetUser(
    context : Context,
    userId : String,
    onSuccess : (CreateUserModel) -> Unit = {},
    onError : (ErrorData) -> Unit = {},
    onInternetNotAvailable : () -> Unit = {},
    fcmToken : String = "testing"
){
    if (AppUtils.isNetworkAvailable(context)){
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.retrofitApiService.SdkSetUserAPI(
                    body = SetUserModelRequest(
                        prev_user_id = userId,
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
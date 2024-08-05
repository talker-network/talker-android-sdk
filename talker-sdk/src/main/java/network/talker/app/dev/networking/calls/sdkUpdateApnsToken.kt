package network.talker.app.dev.networking.calls

import android.content.Context
import network.talker.app.dev.networking.RetrofitClient
import network.talker.app.dev.networking.data.ErrorData
import network.talker.app.dev.networking.data.UpdateApnsTokenRequest
import network.talker.app.dev.networking.data.UpdateApnsTokenResponse
import network.talker.app.dev.utils.AppUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal fun sdkUpdateApnsToken(
    context : Context,
    sdkKey : String,
    onSuccess : (UpdateApnsTokenResponse) -> Unit = {},
    onError : (ErrorData) -> Unit = {},
    onInternetNotAvailable : () -> Unit = {},
){
    if (AppUtils.isNetworkAvailable(context)){
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.retrofitApiService.SdkUpdateApnsPttAPI(
                    token = sdkKey,
                    body = UpdateApnsTokenRequest(
                        apns_ptt_token = "d9b9e174-82e8-4bfb-9ecb-8e342e37e70f"
                    )
                )
                if (response.isSuccessful && response.body() != null && response.code() == 200){
                    val data = response.body()
                    if (data != null) {
                        onSuccess(data)
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
package network.talker.sdk.networking.calls

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.talker.sdk.LOG_TAG
import network.talker.sdk.TalkerGlobalVariables
import network.talker.sdk.networking.RetrofitClient
import network.talker.sdk.networking.data.ErrorData
import network.talker.sdk.networking.data.ExitChannelResponse
import network.talker.sdk.utils.AppUtils

internal fun sdkExitChannel(
    context : Context,
    token : String,
    channel_id : String,
    onSuccess : (ExitChannelResponse) -> Unit = {},
    onError : (ErrorData) -> Unit = {},
    onInternetNotAvailable : () -> Unit = {},
){
    if (AppUtils.isNetworkAvailable(context)){
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.retrofitApiService.SdkExitChannel(
                    token = token,
//                    ExitChannelRequest(
                        channel_id,
//                    )
                )
                if (response.isSuccessful && response.body() != null && response.code() == 200){
                    val data = response.body()
                    if (data != null) {
                        withContext(Dispatchers.Main){
                            onSuccess(data)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main){
                        if (TalkerGlobalVariables.printLogs){
                            Log.d(
                                LOG_TAG,
                                "Error : ${response.body()}"
                            )
                        }
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
                    if (TalkerGlobalVariables.printLogs){
                        Log.d(
                            LOG_TAG,
                            "Error : ${e}"
                        )
                    }
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
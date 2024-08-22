package network.talker.app.dev.networking.calls

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.talker.app.dev.networking.RetrofitClient
import network.talker.app.dev.networking.data.AddNewAdminModel
import network.talker.app.dev.networking.data.AddNewAdminModelRequest
import network.talker.app.dev.networking.data.ErrorData
import network.talker.app.dev.networking.data.UpdateChannelNameModel
import network.talker.app.dev.networking.data.UpdateChannelNameModelRequest
import network.talker.app.dev.utils.AppUtils

internal fun sdkUpdateChannelName(
    context : Context,
    token : String,
    channel_id : String,
    new_name : String,
    onSuccess : (UpdateChannelNameModel) -> Unit = {},
    onError : (ErrorData) -> Unit = {},
    onInternetNotAvailable : () -> Unit = {},
){
    if (AppUtils.isNetworkAvailable(context)){
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.retrofitApiService.SdkUpdateChannelName(
                    token = token,
                    UpdateChannelNameModelRequest(
                        channel_id,
                        new_name
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
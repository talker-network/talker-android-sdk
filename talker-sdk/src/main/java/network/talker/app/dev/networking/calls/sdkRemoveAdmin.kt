package network.talker.app.dev.networking.calls

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.talker.app.dev.networking.RetrofitClient
import network.talker.app.dev.networking.data.AdminRemoveModel
import network.talker.app.dev.networking.data.AdminRemoveModelRequest
import network.talker.app.dev.networking.data.ErrorData
import network.talker.app.dev.utils.AppUtils

internal fun sdkRemoveAdmin(
    context : Context,
    token : String,
    channel_id : String,
    remove_admin_id : String,
    onSuccess : (AdminRemoveModel) -> Unit = {},
    onError : (ErrorData) -> Unit = {},
    onInternetNotAvailable : () -> Unit = {},
){
    if (AppUtils.isNetworkAvailable(context)){
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.retrofitApiService.SdkRemoveAdmin(
                    token = token,
                    AdminRemoveModelRequest(
                        channel_id,
                        remove_admin_id
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
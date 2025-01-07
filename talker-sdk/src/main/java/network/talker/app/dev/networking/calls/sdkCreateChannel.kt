package network.talker.app.dev.networking.calls

import android.content.Context
import android.net.Uri
import network.talker.app.dev.utils.createImgBodyPart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.talker.app.dev.networking.RetrofitClient
import network.talker.app.dev.networking.data.CreateChannelModel
import network.talker.app.dev.networking.data.ErrorData
import network.talker.app.dev.sharedPreference.SharedPreference
import network.talker.app.dev.utils.AppUtils
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

internal fun sdkCreateChannel(
    context : Context,
    token : String,
    name : String,
    participants : String,
    type : String,
    workspace_id : String,
    group_icon : Uri,
    onSuccess : (CreateChannelModel) -> Unit = {},
    onError : (ErrorData) -> Unit = {},
    onInternetNotAvailable : () -> Unit = {},
){
    if (AppUtils.isNetworkAvailable(context)){
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.retrofitApiService.SdkCreateChannel(
                    token = SharedPreference(context).getUserData().user_auth_token,
                    name.toRequestBody("text/plain".toMediaTypeOrNull()),
                    participants.toRequestBody("text/plain".toMediaTypeOrNull()),
                    type.toRequestBody("text/plain".toMediaTypeOrNull()),
//                    workspace_id.toRequestBody("text/plain".toMediaTypeOrNull()),
                    createImgBodyPart(
                        context,
                        group_icon,
                        "group_icon",
                        false,
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
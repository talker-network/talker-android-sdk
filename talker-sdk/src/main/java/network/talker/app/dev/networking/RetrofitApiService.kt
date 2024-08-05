package network.talker.app.dev.networking

import network.talker.app.dev.networking.apiRoutes.ApiRoutes
import network.talker.app.dev.networking.data.CreateUserModel
import network.talker.app.dev.networking.data.CreateUserModelRequest
import network.talker.app.dev.networking.data.CredModel
import network.talker.app.dev.networking.data.SetUserModelRequest
import network.talker.app.dev.networking.data.UpdateApnsTokenRequest
import network.talker.app.dev.networking.data.UpdateApnsTokenResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT

internal interface RetrofitApiService {

    @POST(ApiRoutes.CREATE_USER)
    suspend fun SdkCreateUserAPI(
        @Header("Authorization") token: String,
        @Body body: CreateUserModelRequest
    ) : retrofit2.Response<CreateUserModel>

    @PUT(ApiRoutes.SET_USER)
    suspend fun SdkSetUserAPI(
        @Header("Authorization") token: String,
        @Body body: SetUserModelRequest
    ) : retrofit2.Response<CreateUserModel>

    @PUT(ApiRoutes.UPDATE_APNS_PTT)
    suspend fun SdkUpdateApnsPttAPI(
        @Header("Authorization") token: String,
        @Body body: UpdateApnsTokenRequest
    ) : retrofit2.Response<UpdateApnsTokenResponse>

    @GET(ApiRoutes.CRED)
    suspend fun SdkCredAPI(
        @Header("Authorization") sdkKey: String
    ) : retrofit2.Response<CredModel>

}
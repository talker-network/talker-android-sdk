package com.dev.talkersdk.networking

import com.dev.talkersdk.networking.apiRoutes.ApiRoutes
import com.dev.talkersdk.networking.data.CreateUserModel
import com.dev.talkersdk.networking.data.CreateUserModelRequest
import com.dev.talkersdk.networking.data.CredModel
import com.dev.talkersdk.networking.data.SetUserModelRequest
import com.dev.talkersdk.networking.data.UpdateApnsTokenRequest
import com.dev.talkersdk.networking.data.UpdateApnsTokenResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT

interface RetrofitApiService {

    @POST(ApiRoutes.CREATE_USER)
    suspend fun SdkCreateUserAPI(
        @Header("Authorization") token: String = ApiRoutes.SDK_KEY,
        @Body body: CreateUserModelRequest
    ) : retrofit2.Response<CreateUserModel>

    @PUT(ApiRoutes.SET_USER)
    suspend fun SdkSetUserAPI(
        @Header("Authorization") token: String = ApiRoutes.SDK_KEY,
        @Body body: SetUserModelRequest
    ) : retrofit2.Response<CreateUserModel>

    @PUT(ApiRoutes.UPDATE_APNS_PTT)
    suspend fun SdkUpdateApnsPttAPI(
        @Header("Authorization") token: String = ApiRoutes.SDK_KEY,
        @Body body: UpdateApnsTokenRequest
    ) : retrofit2.Response<UpdateApnsTokenResponse>

    @GET(ApiRoutes.CRED)
    suspend fun SdkCredAPI(
        @Header("Authorization") sdkKey: String = ApiRoutes.SDK_KEY
    ) : retrofit2.Response<CredModel>

}
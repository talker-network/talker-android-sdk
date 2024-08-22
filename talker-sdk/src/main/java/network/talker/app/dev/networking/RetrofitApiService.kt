package network.talker.app.dev.networking

import network.talker.app.dev.networking.apiRoutes.ApiRoutes
import network.talker.app.dev.networking.data.AddNewAdminModel
import network.talker.app.dev.networking.data.AddNewAdminModelRequest
import network.talker.app.dev.networking.data.AddNewParticipantModel
import network.talker.app.dev.networking.data.AddNewParticipantModelRequest
import network.talker.app.dev.networking.data.AdminRemoveModel
import network.talker.app.dev.networking.data.AdminRemoveModelRequest
import network.talker.app.dev.networking.data.CreateChannelModel
import network.talker.app.dev.networking.data.CreateUserModel
import network.talker.app.dev.networking.data.CreateUserModelRequest
import network.talker.app.dev.networking.data.CredModel
import network.talker.app.dev.networking.data.GetAllUserModel
import network.talker.app.dev.networking.data.GetChannelListModel
import network.talker.app.dev.networking.data.RemoveParticipantModel
import network.talker.app.dev.networking.data.RemoveParticipantModelRequest
import network.talker.app.dev.networking.data.SetUserModelRequest
import network.talker.app.dev.networking.data.UpdateApnsTokenRequest
import network.talker.app.dev.networking.data.UpdateApnsTokenResponse
import network.talker.app.dev.networking.data.UpdateChannelNameModel
import network.talker.app.dev.networking.data.UpdateChannelNameModelRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part

internal interface RetrofitApiService {

    @POST(ApiRoutes.CREATE_USER)
    suspend fun SdkCreateUserAPI(
        @Header("Authorization") token: String,
        @Body body: CreateUserModelRequest
    ) : Response<CreateUserModel>

    @PUT(ApiRoutes.SET_USER)
    suspend fun SdkSetUserAPI(
        @Header("Authorization") token: String,
        @Body body: SetUserModelRequest
    ) : Response<CreateUserModel>

    @PUT(ApiRoutes.UPDATE_APNS_PTT)
    suspend fun SdkUpdateApnsPttAPI(
        @Header("Authorization") token: String,
        @Body body: UpdateApnsTokenRequest
    ) : Response<UpdateApnsTokenResponse>

    @GET(ApiRoutes.CRED)
    suspend fun SdkCredAPI(
        @Header("Authorization") sdkKey: String
    ) : Response<CredModel>

    @GET(ApiRoutes.GET_ALL_USERS)
    suspend fun SdkGetAllUsers(
        @Header("Authorization") sdkKey: String
    ) : Response<GetAllUserModel>

    @GET(ApiRoutes.GET_ALL_CHANNELS)
    suspend fun SdkGetAllChannels(
        @Header("Authorization") token: String
    ) : Response<GetChannelListModel>

    @Multipart
    @POST(ApiRoutes.CREATE_CHANNEL)
    suspend fun SdkCreateChannel(
        @Header("Authorization") token: String,
        @Part("name") name: RequestBody,
        @Part("participants") participants: RequestBody,
        @Part("type") type: RequestBody,
//        @Part("workspace_id") workspace_id: RequestBody,
        @Part group_icon: MultipartBody.Part?,
    ) : Response<CreateChannelModel>



    @PUT(ApiRoutes.EDIT_CHANNEL)
    suspend fun SdkAddNewAdmin(
        @Header("Authorization") token: String,
        @Body body : AddNewAdminModelRequest,
    ) : Response<AddNewAdminModel>

    @PUT(ApiRoutes.EDIT_CHANNEL)
    suspend fun SdkRemoveAdmin(
        @Header("Authorization") token: String,
        @Body body : AdminRemoveModelRequest,
    ) : Response<AdminRemoveModel>


    @PUT(ApiRoutes.EDIT_CHANNEL)
    suspend fun SdkUpdateChannelName(
        @Header("Authorization") token: String,
        @Body body : UpdateChannelNameModelRequest,
    ) : Response<UpdateChannelNameModel>



    @PUT(ApiRoutes.EDIT_CHANNEL)
    suspend fun SdkAddNewParticipant(
        @Header("Authorization") sdkKey: String,
        @Body body : AddNewParticipantModelRequest,
    ) : Response<AddNewParticipantModel>

    @PUT(ApiRoutes.EDIT_CHANNEL)
    suspend fun SdkRemoveParticipant(
        @Header("Authorization") token: String,
        @Body body : RemoveParticipantModelRequest,
    ) : Response<RemoveParticipantModel>

}
package network.talker.app.dev

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.SignOutOptions
import com.amazonaws.mobile.client.results.SignInResult
import com.amazonaws.services.kinesisvideo.model.ChannelRole
import com.amazonaws.services.kinesisvideo.model.ResourceEndpointListItem
import com.amazonaws.services.kinesisvideosignaling.model.IceServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import network.talker.app.dev.model.AudioData
import network.talker.app.dev.model.Event
import network.talker.app.dev.model.Message
import network.talker.app.dev.networking.calls.sdkAddNewAdmin
import network.talker.app.dev.networking.calls.sdkAddNewParticipant
import network.talker.app.dev.networking.calls.sdkCreateChannel
import network.talker.app.dev.networking.calls.sdkCreateUser
import network.talker.app.dev.networking.calls.sdkCredAPI
import network.talker.app.dev.networking.calls.sdkExitChannel
import network.talker.app.dev.networking.calls.sdkGetAllChannels
import network.talker.app.dev.networking.calls.sdkGetAllUsersApi
import network.talker.app.dev.networking.calls.sdkRemoveAdmin
import network.talker.app.dev.networking.calls.sdkRemoveParticipant
import network.talker.app.dev.networking.calls.sdkSendMsg
import network.talker.app.dev.networking.calls.sdkSetUser
import network.talker.app.dev.networking.calls.sdkUpdateChannelName
import network.talker.app.dev.networking.data.AddNewAdminModelData
import network.talker.app.dev.networking.data.AddNewParticipantModelData
import network.talker.app.dev.networking.data.AdminRemoveModelData
import network.talker.app.dev.networking.data.Channel
import network.talker.app.dev.networking.data.CreateChannelModel
import network.talker.app.dev.networking.data.GetAllUserModelData
import network.talker.app.dev.networking.data.MessageObject
import network.talker.app.dev.networking.data.MessageObjectForLocalDB
import network.talker.app.dev.networking.data.RemoveParticipantModelData
import network.talker.app.dev.networking.data.UpdateChannelNameModelData
import network.talker.app.dev.okhttp.SignalingListener
import network.talker.app.dev.okhttp.SignalingServiceWebSocketClient
import network.talker.app.dev.sharedPreference.SharedPreference
import network.talker.app.dev.socket.SocketHandler
import network.talker.app.dev.utils.Constants
import network.talker.app.dev.utils.getSignedUri
import network.talker.app.dev.utils.isFileSizeSmaller
import network.talker.app.dev.webrtc.AudioStatus
import network.talker.app.dev.webrtc.CreateLocalPeerConnection
import network.talker.app.dev.webrtc.Kinesis
import network.talker.app.dev.webrtc.KinesisVideoSdpObserver
import network.talker.app.dev.webrtc.ServerConnectionState
import network.talker.app.dev.webrtc.UpdateSignalingChannelInfoTask
import network.talker.app.dev.webrtc.checkAndAddIceCandidate
import network.talker.app.dev.webrtc.createSdpAnswer
import network.talker.app.dev.webrtc.createSdpOffer
import network.talker.app.dev.webrtc.handlePendingIceCandidates
import org.webrtc.AudioTrack
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpSender
import org.webrtc.SessionDescription
import org.webrtc.audio.JavaAudioDeviceModule
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.Queue
import java.util.UUID
import java.util.concurrent.Executors

object Talker {
    private val CHANNEL_ID = "WebRtcDataChannel"
    private val AudioTrackID = "KvsAudioTrack"
    private var recipientClientId = ""
    private var mWssEndpoint = ""
    private var remoteAudioTrack: AudioTrack? = null
    private val mUserNames: ArrayList<String> = arrayListOf()
    private val mPasswords: ArrayList<String> = arrayListOf()
    private val ttls: ArrayList<Int> = arrayListOf()
    private val mUrisList: java.util.ArrayList<List<String>> = arrayListOf()
    private var createLocalPeerConnection: CreateLocalPeerConnection? = null
    private val mIceServerList: ArrayList<IceServer> = java.util.ArrayList()
    private val mEndpointList: ArrayList<ResourceEndpointListItem> = java.util.ArrayList()
    private var peerIceServers: ArrayList<PeerConnection.IceServer> = ArrayList()
    private var isAudioSent = true
    private var localAudioTrack: AudioTrack? = null
    private var originalAudioMode = 0
    private var originalSpeakerphoneOn = false
    private var mChannelArn: String? = null
    private var client: SignalingServiceWebSocketClient? = null
    private var mNotificationId = 0
    private var peerConnectionFoundMap = HashMap<String, PeerConnection>()
    private var pendingIceCandidatesMap = HashMap<String, Queue<IceCandidate>>()
    private var webrtcEndpoint: String? = null
    private var mStreamArn: String? = null
    private var localPeer: PeerConnection? = null
    private var applicationContext: Context? = null
    private var mRegion: String = ""
    private var mClientId: String? = null
    private var audioManager: AudioManager? = null
    private var mChannelId: String = ""
    private var mChannelName: String = ""
    private var sdkKey: String = ""
    private var mCreds: AWSCredentials? = null
    private var dataChannel: DataChannel? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var rtcSender: RtpSender? = null
    private var isRecording = false
    private var sampleRate = 16000
    private var audioFile: File? = null
    private var broadcastReceiver: BroadcastReceiver? = null
    private lateinit var audioRecord: AudioRecord
    private lateinit var buffer: ByteArray
    private const val SDK_KEY_ERROR = "Kindly initialize Talker with SDK key or Api Key"
    private var hasStartedTalking = false
    private var retryCount = 0
    private var hasToRetry = false
    private var isFirstTime = true

    class EventListeners {
        var onServerConnectionChange: ((serverConnectionState: ServerConnectionState, message: String) -> Unit)? =
            null
        var onAudioStatusChange: ((audioStatus: AudioStatus) -> Unit)? = null
        var onNewChannel: ((data: Channel) -> Unit)? = null
        var onChannelUpdated: ((data: UpdateChannelNameModelData) -> Unit)? = null
        var onUserRemovedFromChannel: ((data: RemoveParticipantModelData) -> Unit)? = null
        var onAddedUserInChannel: ((data: AddNewParticipantModelData) -> Unit)? = null
        var onNewSdkUser: ((data: GetAllUserModelData) -> Unit)? = null
        var onAdminAdded: ((data: AddNewAdminModelData) -> Unit)? = null
        var onAdminRemoved: ((data: AdminRemoveModelData) -> Unit)? = null
        var onNewMessageReceived : ((data: MessageObject) -> Unit)? = null
        var currentPttAudio : ((data: AudioData) -> Unit)? = null
    }

    // call this event listener to get events for all the things happening during the session.
    val eventListener: EventListeners = EventListeners()

    private fun validateSDKKey() {
        if (sdkKey.isEmpty()) {
            throw Exception(SDK_KEY_ERROR)
        }
    }

    internal fun isUserLoggedIn(): Boolean {
        validateSDKKey()
        return Kinesis.auth.isSignedIn
    }

    // call this function on the onCreate of the application or Activity.
    // without this function the sdk won't work properly
    fun init(sdkKey: String) {
        Talker.sdkKey = sdkKey
    }

    // call this function to create the user and join in.
    // this will make api call and create a user on the server.
    // after that the user will be logged in to the kineses sdk from the credentials found from the api call's response.
    // also when the api calls are success then the data will be fetched from another api and will be stored in the local database.
    fun createUser(
        context: Context,
        name: String,
        fcmToken: String,
        onSuccess: (message: String) -> Unit = {},
        onFailure: (message: String) -> Unit = {},
        channelName: String = "",
        region: String = "",
    ) {
        applicationContext = context
        validateSDKKey()
        hasStartedTalking = false
        retryCount = 0
        hasToRetry = false
        isFirstTime = true
        val sharedPreference = SharedPreference(context)
        CoroutineScope(Dispatchers.Main).launch {
            TalkerSdkBackgroundService.database.roomDao().clearUsers()
            TalkerSdkBackgroundService.database.roomDao().clearChannels()
            getAllUsersInternal(onFailure)
        }
        sdkCreateUser(context, name = name, onSuccess = { res ->
            sharedPreference.setUserData(res.data)
            getChannelListInternal(
                onFailure
            )

            sdkCredAPI(
                context, onSuccess = { result ->
                    mRegion =
                        result.data.cognito.CredentialsProvider.CognitoIdentity.Default.Region
                    mChannelName = result.data.webrtc_channel_name
                    mChannelId = result.data.general_channel.channel_id
                    TalkerSdkBackgroundService.initAWS(
                        result.data.cognito
//                        JSONObject(
//                            Gson().toJson(result.data.cognito)
//                        )
                    )

                    TalkerSdkBackgroundService.auth.signIn(res.data.a_username,
                        res.data.a_pass,
                        emptyMap(),
                        mapOf(
                            "name" to res.data.name,
                            "user_id" to res.data.user_id,
                            "user_auth_token" to res.data.user_auth_token
                        ),
                        object : Callback<SignInResult> {
                            override fun onResult(result: SignInResult?) {
                                getMainLooper {
                                    onSuccess("User register success")
                                }
                                establishConnection(
                                    context,
                                    channelName,
                                    region,
                                )
                            }

                            override fun onError(e: java.lang.Exception?) {
                                getMainLooper {
                                    onFailure("User register failed. Error : ${e?.message}")
                                    e?.printStackTrace()
                                }
                            }
                        }
                    )
                },
                onError = { error ->
                    Log.d(
                        LOG_TAG,
                        "Error : $error"
                    )
                    sendBroadCast(
                        "CONNECTION_FAILURE",
                        "Webrtc Error : " + error.message,
                        "WEBRTC"
                    )
                    return@sdkCredAPI
                },
                onInternetNotAvailable = {
                    sendBroadCast(
                        "CONNECTION_FAILURE",
                        "Webrtc Error : Network not available",
                        "WEBRTC"
                    )
                    return@sdkCredAPI
                },
                sdkKey = sdkKey
            )


        }, onError = { errorData ->
            getMainLooper {
                onFailure(errorData.message)
            }
        }, onInternetNotAvailable = {
            getMainLooper {
                onFailure("Network not available")
            }
        }, fcmToken = fcmToken,
            sdkKey = sdkKey
        )
    }

    // call this function when you want to login the user.
    // this will make api call to get the user's data and login in the kineses sdk using that credentials
    // after that it will check if the previously logged in user is same as the current one.
    // if not than we will again fetch the users and channels and then we will store the data in the local database.
    fun setUser(
        context: Context,
        userId: String,
        fcmToken: String,
        onSuccess: (message: String) -> Unit = {},
        onFailure: (message: String) -> Unit = {},
        channelName: String = "",
        region: String = "",
    ) {
        applicationContext = context
        validateSDKKey()
        if (userId.isEmpty()) {
            onFailure("UserId cannot be empty")
            return
        }
        if (getCurrentUserId(context) != userId) {
            CoroutineScope(Dispatchers.Main).launch {
                TalkerSdkBackgroundService.database.roomDao().clearUsers()
                TalkerSdkBackgroundService.database.roomDao().clearChannels()
                getAllUsersInternal(onFailure)
            }
        }

        sdkSetUser(
            context,
            this.sdkKey,
            userId,
            onSuccess = { res ->
                if (res.data.user_id != getCurrentUserId(context)) {
                    SharedPreference(context).setUserData(res.data)
                    getChannelListInternal(onFailure)
                }else{
                    SharedPreference(context).setUserData(res.data)
                }

                sdkCredAPI(
                    context, onSuccess = { result ->
                        mRegion =
                            result.data.cognito.CredentialsProvider.CognitoIdentity.Default.Region
                        mChannelName = result.data.webrtc_channel_name
                        mChannelId = result.data.general_channel.channel_id
                        TalkerSdkBackgroundService.initAWS(
                            result.data.cognito
//                            JSONObject(
//                                Gson().toJson(result.data.cognito)
//                            )
                        )

                        TalkerSdkBackgroundService.auth.signIn(res.data.a_username,
                            res.data.a_pass,
                            emptyMap(),
                            mapOf(
                                "name" to res.data.name,
                                "user_id" to res.data.user_id,
                                "user_auth_token" to res.data.user_auth_token
                            ),
                            object : Callback<SignInResult> {
                                override fun onResult(result: SignInResult?) {
                                    getMainLooper {
                                        onSuccess("User login success")
                                    }
                                    establishConnection(
                                        context,
                                        channelName,
                                        region,
                                    )
                                }

                                override fun onError(e: java.lang.Exception?) {
                                    getMainLooper {
                                        onFailure("User sign in failed. Error : ${e?.message}")
                                        e?.printStackTrace()
                                        return@getMainLooper
                                    }
                                }
                            })
                    },
                    onError = { error ->
                        Log.d(
                            LOG_TAG,
                            "Error : $error"
                        )
                        sendBroadCast(
                            "CONNECTION_FAILURE",
                            "Webrtc Error : " + error.message,
                            "WEBRTC"
                        )
                        return@sdkCredAPI
                    },
                    onInternetNotAvailable = {
                        sendBroadCast(
                            "CONNECTION_FAILURE",
                            "Webrtc Error : Network not available",
                            "WEBRTC"
                        )
                        return@sdkCredAPI
                    },
                    sdkKey = sdkKey
                )



            },
            onError = { errorData ->
                getMainLooper {
                    onFailure(errorData.message)
                    return@getMainLooper
                }
            }, onInternetNotAvailable = {
                getMainLooper {
                    onFailure("Network not available")
                    return@getMainLooper
                }
            }, fcmToken = fcmToken
        )
    }

    // this will return channel's list as flow.
    // you will get all channel's the user is part of.
    fun getChannelList(): kotlinx.coroutines.flow.Flow<List<Channel>> =
        TalkerSdkBackgroundService.database.roomDao().getAllChannels()

    // this function will return the user's list that exists in the application.
    // this will exclude the current logged in user.
    fun getAllUsers(): kotlinx.coroutines.flow.Flow<List<GetAllUserModelData>> =
        TalkerSdkBackgroundService.database.roomDao().getAllUsersExcept(
            SharedPreference(TalkerSdkBackgroundService.talkerApplicationContext).getUserData().user_id
        )

    /**
     * @return Returns the user_id of the current logged in user.
     */
    fun getCurrentUserId(context: Context): String {
        val sharedPreference = SharedPreference(context)
        return sharedPreference.getUserData().user_id
    }

    /**
     * Get the current logged in user's data
     *
     * @return Returns a Pair<String, String> where first parameter gives the userId and second gives the name.
     */
    fun getCurrentUser(context: Context) : Pair<String, String> {
        val sharedPreference = SharedPreference(context)
        return Pair(sharedPreference.getUserData().user_id, sharedPreference.getUserData().name)
    }


    /**
     * Get the current logged in user's data
     *
     * @return Returns a Pair<String, String> where first parameter gives the userId and second gives the name.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getChannelMessages(channelId : String) : Flow<List<MessageObject>> {
        return TalkerSdkBackgroundService.database.roomDao().getChannelMessages(
            channelId
        ).mapLatest { list ->
            list.map { item ->
                MessageObject(
                    attachments = item.attachments,
                    channel_id = item.channel_id,
                    channel_name = item.channel_name,
                    description = item.text,
                    id = item.id,
                    sent_at = item.sent_at,
                    sender_id = item.sender_id
                )
            }
        }
    }


    // call this function to edit the channel's name
    // this function will call the api for changing the name
    // and also it will update the local databse as well.
    fun editChannelName(
        context: Context,
        newName: String,
        channelId: String,
        onSuccess: (newName: String) -> Unit,
        onFailure: (message : String) -> Unit
    ) {
        validateSDKKey()

        if (channelId.isBlank()) {
            onFailure("Invalid channelId")
            return
        }
        if (newName.isBlank()) {
            onFailure("Invalid newName")
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            val channelType = TalkerSdkBackgroundService.database.roomDao().getChannelById(channelId)?.channel_type
            if (channelType == "workspace_general"){
                onFailure("Cannot edit general channel")
                return@launch
            }
            if (channelType == "direct") {
                onFailure("Cannot edit direct channel")
                return@launch
            }
            sdkUpdateChannelName(
                context,
                SharedPreference(context).getUserData().user_auth_token,
                channelId,
                newName,
                onSuccess = {
                    if (it.success) {
                        CoroutineScope(Dispatchers.Main).launch {
                            val channel =
                                TalkerSdkBackgroundService.database.roomDao().getChannelById(channelId)
                            val updatedChannel =
                                channel?.copy(group_name = it.data.new_name)
                            if (updatedChannel != null) {
                                TalkerSdkBackgroundService.database.roomDao().updateChannel(updatedChannel)
                            }
                            onSuccess(it.data.new_name)
                        }
                    } else {
                        onFailure("Failed")
                    }
                },
                onError = {
                    onFailure(it.message)
                },
                onInternetNotAvailable = {
                    onFailure("Failure due to no internet connectivity")
                }
            )
        }
    }

    // call this function to remove a participant from some channel
    // updating local database in handled internally.
    fun removeParticipant(
        context: Context,
        channelId: String,
        removingParticipantId: String,
        onSuccess: (message: String) -> Unit,
        onFailure: (message : String) -> Unit
    ) {
        validateSDKKey()
        if (channelId.isBlank()) {
            onFailure("Invalid channelId")
            return
        }
        if (removingParticipantId.isBlank()) {
            onFailure("Invalid removingParticipantId")
            return
        }
        CoroutineScope(Dispatchers.Main).launch {
            val channelType =
                TalkerSdkBackgroundService.database.roomDao().getChannelById(channelId)?.channel_type
            if (channelType == "workspace_general") {
                onFailure("Cannot remove participant from general channel")
                return@launch
            }
            if (channelType == "direct") {
                onFailure("Cannot remove participant from direct channel")
                return@launch
            }

            sdkRemoveParticipant(
                context,
                SharedPreference(context).getUserData().user_auth_token,
                channelId,
                removingParticipantId,
                onSuccess = {
                    if (it.success) {
                        onSuccess("Participant removed")
                    } else {
                        onFailure("Failed")
                    }
                },
                onError = {
                    if (TalkerGlobalVariables.printLogs) {
                        Log.d(
                            LOG_TAG,
                            "Error : ${it.message}"
                        )
                    }
                    onFailure(it.message)
                },
                onInternetNotAvailable = {
                    onFailure("Failure due to no internet connectivity")
                }
            )
        }
    }

    // call this function to add a participant in some particular channel
    // local database in handled internally.
    fun addParticipant(
        context: Context,
        channelId: String,
        newParticipant: String,
        onSuccess: (message: String) -> Unit,
        onFailure: (message : String) -> Unit
    ) {
        validateSDKKey()
        if (channelId.isBlank()) {
            onFailure("Invalid channelId")
            return
        }
        if (newParticipant.isBlank()) {
            onFailure("Invalid newParticipant")
            return
        }
        CoroutineScope(Dispatchers.Main).launch {
            val channelType =
                TalkerSdkBackgroundService.database.roomDao().getChannelById(channelId)?.channel_type
            if (channelType == "workspace_general") {
                onFailure("Cannot add participant in general channel")
                return@launch
            }
            if (channelType == "direct") {
                onFailure("Cannot add more participant in direct channel")
                return@launch
            }

            sdkAddNewParticipant(
                context,
                SharedPreference(context).getUserData().user_auth_token,
                channelId,
                newParticipant,
                onSuccess = {
                    if (it.success) {
                        onSuccess("Added SuccessFully")
                    } else {
                        onFailure("Failed")
                    }
                },
                onError = {
                    onFailure(it.message)
                },
                onInternetNotAvailable = {
                    onFailure("Failure due to no internet connectivity")
                }
            )
        }
    }

    // call this function to make a participant of some channel the admin
    fun addAdmin(
        context: Context,
        channelId: String,
        newAdminId: String,
        onSuccess: (message: String) -> Unit,
        onFailure: (message : String) -> Unit
    ) {
        validateSDKKey()

        if (channelId.isBlank()) {
            onFailure("Invalid channelId")
            return
        }
        if (newAdminId.isBlank()) {
            onFailure("Invalid newAdminId")
            return
        }


        CoroutineScope(Dispatchers.Main).launch {
            val channelType =
                TalkerSdkBackgroundService.database.roomDao().getChannelById(channelId)?.channel_type
            if (channelType == "workspace_general") {
                onFailure("Cannot change admin in general channel")
                return@launch
            }
            if (channelType == "direct") {
                onFailure("Cannot change admin in direct channel")
                return@launch
            }
            sdkAddNewAdmin(
                context,
                SharedPreference(context).getUserData().user_auth_token,
                channelId,
                newAdminId,
                onSuccess = {
                    if (it.success) {
                        onSuccess("Admin added")
                    } else {
                        onFailure("Failed")
                    }
                },
                onError = {
                    onFailure(it.message)
                },
                onInternetNotAvailable = {
                    onFailure("Failure due to no internet connectivity")
                }
            )
        }
    }

    // call this function to remove some participant from the post of admin
    fun removeAdmin(
        context: Context,
        channelId: String,
        removeAdminID: String,
        onSuccess: (message: String) -> Unit,
        onFailure: (message : String) -> Unit
    ) {
        validateSDKKey()


        if (channelId.isBlank()) {
            onFailure("Invalid channelId")
            return
        }

        if (removeAdminID.isBlank()) {
            onFailure("Invalid removeAdminId")
            return
        }
        CoroutineScope(Dispatchers.Main).launch {
            val channelType =
                TalkerSdkBackgroundService.database.roomDao().getChannelById(channelId)?.channel_type
            if (channelType == "workspace_general") {
                onFailure("Cannot remove admin from general channel")
                return@launch
            }
            if (channelType == "direct") {
                onFailure("Cannot remove admin from direct channel")
                return@launch
            }
            sdkRemoveAdmin(
                context,
                SharedPreference(context).getUserData().user_auth_token,
                channelId,
                removeAdminID,
                onSuccess = {
                    if (it.success) {
                        onSuccess("Admin removed")
                    } else {
                        onFailure("Failed")
                    }
                },
                onError = {
                    onFailure(it.message)
                },
                onInternetNotAvailable = {
                    onFailure("Failure due to no internet connectivity")
                }
            )
        }
    }


    fun sendTextMsg(
        context: Context,
        channelId : String,
        text : String,
        onSuccess : (String) -> Unit,
        onFailure : (String) -> Unit
    ) {
        validateSDKKey()
        if (channelId.isBlank()) {
            onFailure("Invalid channelId")
            return
        }
        if (text.isBlank()) {
            onFailure("Invalid text")
            return
        }
        CoroutineScope(Dispatchers.Main).launch {
            sdkSendMsg(
                context,
                SharedPreference(context).getUserData().user_auth_token,
                channelId,
               text = text,
                onSuccess = {
//                    if (it.success) {
                        onSuccess("Admin removed")
//                    } else {
//                        onFailure("Failed")
//                    }
                },
                onError = {
                    onFailure(it.message)
                },
                onInternetNotAvailable = {
                    onFailure("Failure due to no internet connectivity")
                }
            )
        }
    }

    fun uploadImage(
        context: Context,
        channelId : String,
        caption : String,
        imageUri : Uri,
        onSuccess : (String) -> Unit,
        onFailure : (String) -> Unit
    ) {
        validateSDKKey()
        if (channelId.isBlank()) {
            onFailure("Invalid channelId")
            return
        }
        if (imageUri == Uri.EMPTY) {
            onFailure("Invalid imageUri")
            return
        }
        if (context.contentResolver.getType(imageUri)?.startsWith("image/") != true) {
            onFailure("Invalid imageUri")
            return
        }
        if (!isFileSizeSmaller(
            uri = imageUri,
            context,
            5
        )) {
            onFailure("File size greater than 5MB")
            return
        }
        CoroutineScope(Dispatchers.Main).launch {
            sdkSendMsg(
                context,
                SharedPreference(context).getUserData().user_auth_token,
                channelId,
               text = caption,
                imageUri = imageUri,
                onSuccess = {
//                    if (it.success) {
                        onSuccess("Image sent")
//                    } else {
//                        onFailure("Failed")
//                    }
                },
                onError = {
                    onFailure(it.message)
                },
                onInternetNotAvailable = {
                    onFailure("Failure due to no internet connectivity")
                }
            )
        }
    }

    fun uploadDocument(
        context: Context,
        channelId : String,
        documentUri : Uri,
        onSuccess : (String) -> Unit,
        onFailure : (String) -> Unit
    ) {
        validateSDKKey()
        if (channelId.isBlank()) {
            onFailure("Invalid channelId")
            return
        }
        if (documentUri == Uri.EMPTY) {
            onFailure("Invalid documentUri")
            return
        }
        if (context.contentResolver.getType(documentUri)?.startsWith("application/pdf") != true) {
            onFailure("Invalid documentUri")
            return
        }
        if (!isFileSizeSmaller(
                uri = documentUri,
                context,
                5
            )) {
            onFailure("File size greater than 5MB")
            return
        }
        CoroutineScope(Dispatchers.Main).launch {
            sdkSendMsg(
                context,
                SharedPreference(context).getUserData().user_auth_token,
                channelId,
               text = "",
                documentUri = documentUri,
                onSuccess = {
//                    if (it.success) {
                        onSuccess("Document sent")
//                    } else {
//                        onFailure("Failed")
//                    }
                },
                onError = {
                    onFailure(it.message)
                },
                onInternetNotAvailable = {
                    onFailure("Failure due to no internet connectivity")
                }
            )
        }
    }

    // call this function to start sharing audio in some particular channel.
    // this will return a call back function which will tell if the channel is available for speaking or not.
    fun startPttAudio(channelId: String, isChannelAvailable: (Boolean) -> Unit) {
        validateSDKKey()
        mChannelId = channelId
        if (hasToRetry) {
            hasToRetry = false
            this@Talker.retryCount = 0
            sendBroadCast(
                "CONNECTING",
                "Connection Retrying...."
            )
            applicationContext?.let {
                closeConnectionForRetry {
                    establishConnection(
                        it,
                        mChannelName,
                        mRegion
                    )
                }
            }
        } else {
            sendBroadCast(
                "CONNECTING",
                "Getting info about channel availability"
            )
            if (!hasStartedTalking) {
                startEndAudioSending(false, isChannelAvailable = {
                    if (it) {
                        hasStartedTalking = true
                        sendBroadCast("SENDING", "The channel is occupied")
                    } else {
                        sendBroadCast("BUSY", "Sending audio")
                    }
                    isChannelAvailable(it)
                })
            }
        }
    }

    // call this function to stop speaking.
    fun stopPttAudio() {
        validateSDKKey()
        if (hasStartedTalking) {
            hasStartedTalking = false
            startEndAudioSending(true)
            sendBroadCast("STOPPED", "Stopped sending audio")
        }
    }

    // close everything....
    // this function will close all the connections.
    // call this to end session
    // and also call this in onDestroy() of the main activity of yours.
    fun closeConnection() {
        runOnUiThread {
            synchronized(this) {
                hasStartedTalking = false
                validateSDKKey()
                class MyWorker(context: Context, workerParameters: WorkerParameters) :
                    Worker(context, workerParameters) {
                    override fun doWork(): Result {
                        SocketHandler.broadCastStop(mChannelId)
                        SocketHandler.closeConnection()
                        audioManager?.mode = originalAudioMode
                        audioManager?.isSpeakerphoneOn = originalSpeakerphoneOn
                        pendingIceCandidatesMap.clear()
                        peerConnectionFoundMap.clear()
                        peerConnectionFoundMap.clear()
                        pendingIceCandidatesMap.clear()
                        createLocalPeerConnection?.closeAll()
                        localPeer?.let {
                            it.close()
                            it.dispose()
                            localPeer = null
                        }
                        client?.disconnect()
                        client = null
                        CoroutineScope(Dispatchers.Main).launch {
                            sendBroadCast(
                                "CONNECTION_CLOSED",
                                "Connection closed"
                            )
                        }
                        return Result.success()
                    }
                }
                try {
                    val workRequestBuilder = OneTimeWorkRequestBuilder<MyWorker>()
                        .build()
                    applicationContext?.let {
                        WorkManager.getInstance(it).enqueue(workRequestBuilder)
                    }
                } catch (e: Exception) {
                    if (TalkerGlobalVariables.printLogs) {
                        Log.e(LOG_TAG, "Error while closing and disposing localPeer", e)
                    }
                }

            }
        }
    }

    fun logoutUser() {
        runOnUiThread {
            synchronized(this) {
                hasStartedTalking = false
                validateSDKKey()
                class MyWorker(context: Context, workerParameters: WorkerParameters) :
                    Worker(context, workerParameters) {
                    override fun doWork(): Result {
                        SocketHandler.broadCastStop(mChannelId)
                        SocketHandler.closeConnection()
                        audioManager?.mode = originalAudioMode
                        audioManager?.isSpeakerphoneOn = originalSpeakerphoneOn
                        pendingIceCandidatesMap.clear()
                        peerConnectionFoundMap.clear()
                        peerConnectionFoundMap.clear()
                        pendingIceCandidatesMap.clear()
                        createLocalPeerConnection?.closeAll()
                        localPeer?.let {
                            it.close()
                            it.dispose()
                            localPeer = null
                        }
                        client?.disconnect()
                        client = null
                        TalkerSdkBackgroundService.auth.signOut(
                            SignOutOptions.builder().signOutGlobally(true).build(),
                            object : Callback<Void> {
                                override fun onResult(result: Void?) {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        sendBroadCast(
                                            "CONNECTION_CLOSED",
                                            "Connection closed"
                                        )
                                    }
                                }

                                override fun onError(e: java.lang.Exception?) {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        sendBroadCast(
                                            "CONNECTION_CLOSED",
                                            "Connection closed"
                                        )
                                    }
                                }

                            }
                        )
                        return Result.success()
                    }
                }
                try {
                    val workRequestBuilder = OneTimeWorkRequestBuilder<MyWorker>()
                        .build()
                    applicationContext?.let {
                        WorkManager.getInstance(it).enqueue(workRequestBuilder)
                    }
                } catch (e: Exception) {
                    if (TalkerGlobalVariables.printLogs) {
                        Log.e(LOG_TAG, "Error while closing and disposing localPeer", e)
                    }
                }

            }
        }
    }

    // call this function to create a group channel
    fun createGroupChannel(
        context: Context,
        name: String,
        participantId: String,
        onSuccess: (CreateChannelModel) -> Unit = {},
        onError: (String) -> Unit = {},
        onInternetNotAvailable: () -> Unit = {},
    ) {
        validateSDKKey()
        if (name.isBlank()) {
            onError("Invalid name")
            return
        }

        if (participantId.isBlank()) {
            onError("Invalid participantId")
            return
        }
        sdkCreateChannel(
            context,
            sdkKey,
            name,
            participantId,
            type = "group",
            "",
            Uri.EMPTY,
            onSuccess,
            onError = {
                onError(it.message)
            },
            onInternetNotAvailable
        )
    }

    // call this function to create a peer to peer channel with some particular user.
    fun createDirectChannel(
        context: Context,
        participantId: String,
        onSuccess: (CreateChannelModel) -> Unit = {},
        onError: (String) -> Unit = {},
        onInternetNotAvailable: () -> Unit = {},
    ) {
        validateSDKKey()
        if (participantId.isBlank()) {
            onError("Invalid participantId")
            return
        }
        Log.d(
            LOG_TAG,
            "Validated sdk key"
        )
        sdkCreateChannel(
            context,
            sdkKey,
            "",
            participantId,
            type = "direct",
            "",
            Uri.EMPTY,
            onSuccess,
            onError = {
                Log.d(
                    LOG_TAG,
                    "Error occured : ${it.message}"
                )
                onError(it.message)
            },
            onInternetNotAvailable
        )
    }

    // call this function when you want to leave some channel.
    // remember that you cannot leave any general channel or direct channel
    fun exitChannel(
        context: Context,
        channelId : String,
        onSuccess : (message : String) -> Unit = {},
        onError : (String) -> Unit = {},
        onInternetNotAvailable: () -> Unit = {}
    ) {
        validateSDKKey()
        if (channelId.isBlank()) {
            onError("Invalid Channel id")
            return
        }
        CoroutineScope(Dispatchers.Main).launch {
            val channelType = TalkerSdkBackgroundService.database.roomDao().getChannelById(channelId)?.channel_type
            if (channelType == "workspace_general"){
                onError("Cannot leave general channel")
                return@launch
            }
            if (channelType == "direct") {
                onError("Cannot leave this channel")
                return@launch
            }
            sdkExitChannel(
                context,
                SharedPreference(context).getUserData().user_auth_token,
                channelId,
                onSuccess = {
                    if (it.success) {
                        onSuccess("Left channel successfully")
                    }else{
                        onError(it.message)
                    }
                },
                onError = {
                    onError(it.message)
                },
                onInternetNotAvailable
            )
        }
    }

    private fun isChannelAvailable(channelId: String): Boolean {
        var isChannelAvailable = false
        SocketHandler.broadCastStart(channelId) { ack ->
            isChannelAvailable = ack.getOrNull(0) == true
        }
        SocketHandler.broadCastStop(channelId)
        return isChannelAvailable
    }

    private fun getChannelListInternal(
        onFailure: (message: String) -> Unit
    ) {
        validateSDKKey()
        applicationContext?.let {
            sdkGetAllChannels(
                it,
                SharedPreference(applicationContext!!).getUserData().user_auth_token,
                onSuccess = {
                    CoroutineScope(Dispatchers.Main).launch {
                        TalkerSdkBackgroundService.database.roomDao().insertChannels(it.data.channels)
                    }
                },
                onError = {
                    runOnUiThread {
                        onFailure("Failed fetching channels")
                    }
                },
                onInternetNotAvailable = {
                    runOnUiThread {
                        onFailure("Failure due to no internet connectivity")
                    }
                }
            )
        }
    }

    private fun getAllUsersInternal(onFailure: (message: String) -> Unit) {
        validateSDKKey()
        applicationContext?.let {
            sdkGetAllUsersApi(
                it,
                sdkKey,
                onSuccess = {
                    CoroutineScope(Dispatchers.Main).launch {
                        TalkerSdkBackgroundService.database.roomDao().insertUsers(it.data)
                    }
                },
                onError = {
                    runOnUiThread {
                        onFailure("Failed featching users list")
                    }
                },
                onInternetNotAvailable = {
                    runOnUiThread {
                        onFailure("Failure due to no internet connectivity")
                    }
                }
            )
        }
    }

//    private class MyWorker(context: Context, workerParameters: WorkerParameters) :
//        Worker(context, workerParameters) {
//        override fun doWork(): Result {
//
//            return Result.success()
//        }
//    }

    // close everything....
    private fun closeConnectionForRetry(
        onComplete: () -> Unit
    ) {
        runOnUiThread {
            synchronized(this) {
                hasStartedTalking = false
                validateSDKKey()

                try {
                    SocketHandler.broadCastStop(mChannelId)
                    SocketHandler.closeConnection()
                    audioManager?.mode = originalAudioMode
                    audioManager?.isSpeakerphoneOn = originalSpeakerphoneOn
                    pendingIceCandidatesMap.clear()
                    peerConnectionFoundMap.clear()
                    peerConnectionFoundMap.clear()
                    pendingIceCandidatesMap.clear()
                    createLocalPeerConnection?.closeAll()
                    localPeer?.let {
                        it.close()
                        it.dispose()
                        localPeer = null
                    }
                    client?.disconnect()
                    client = null
                    onComplete()
//                    val workRequestBuilder = OneTimeWorkRequestBuilder<MyWorker>()
//                        .setConstraints(
//                            Constraints.NONE
//                        )
//                        .build()
//                    applicationContext?.let {
//                        val result = WorkManager.getInstance(it).enqueue(workRequestBuilder).result
//                        if (result.isDone) {
//                            onComplete()
//                        }else{
//                            onComplete()
//                        }
//                    }
                } catch (e: Exception) {
                    if (TalkerGlobalVariables.printLogs) {
                        Log.e(LOG_TAG, "Error while closing and disposing localPeer", e)
                    }
                }

            }
        }
    }

    private fun establishConnection(
        applicationContext: Context,
        channelName: String,
        region: String,
    ) {
        CoroutineScope(
            Dispatchers.IO
        ).launch {
            Talker.applicationContext = applicationContext
            mCreds = TalkerSdkBackgroundService.auth.credentials
            audioManager = applicationContext.getSystemService(AudioManager::class.java)
            val sharedPreference = SharedPreference(applicationContext)
            mClientId = sharedPreference.getUserData().user_auth_token
                .split("|")
                .getOrNull(2) ?: ""
            if (!isUserLoggedIn()) {
                getMainLooper {
                    eventListener.onServerConnectionChange?.invoke(
                        ServerConnectionState.Failure,
                        "User not logged in"
                    )
                }
                return@launch
            }
            getMainLooper {
                sendBroadCast(
                    "CONNECTING",
                    "Peer connecting..."
                )
            }
            if (retryCount == 0) {
                broadcastReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        val action = intent?.extras?.getString("action")
                        val message = intent?.extras?.getString("message")
                        val failureFrom = intent?.extras?.getString("failure_from")
                        when (action) {
                            "CONNECTION_SUCCESSFUL" -> {
                                getMainLooper {
                                    isFirstTime = false
                                    hasToRetry = false
                                    retryCount = 0
                                    SocketHandler.setSocket(
                                        sharedPreference.getUserData().user_auth_token,
                                        applicationContext
                                    )
                                    SocketHandler.establishConnection(applicationContext)
                                    eventListener.onServerConnectionChange?.invoke(
                                        ServerConnectionState.Success,
                                        "Peer connected successfully"
                                    )
                                }
                            }

                            "CONNECTION_FAILURE" -> {
                                getMainLooper {
                                    // if the user is not connecting for the first time and
                                    // also if retrying for less than 2nd time
                                    // then we need to retry
                                    if (isFirstTime) {
                                        // else if we are coming for the first time and we get some error than we inform the user as failure
                                        isFirstTime = false
                                        hasToRetry = false
                                        retryCount = 0
                                        eventListener.onServerConnectionChange?.invoke(
                                            ServerConnectionState.Failure,
                                            message ?: "Error"
                                        )
                                        hasStartedTalking = false
                                        closeConnection()
                                        applicationContext.unregisterReceiver(broadcastReceiver)
                                    } else {
                                        if (this@Talker.retryCount < 2) {
                                            sendBroadCast(
                                                "CONNECTING"
                                            )
                                            this@Talker.retryCount++
                                            if (TalkerGlobalVariables.printLogs) {
                                                Log.d(
                                                    LOG_TAG,
                                                    "CONNECTION_FAILURE : $failureFrom"
                                                )
                                            }
                                            closeConnectionForRetry {
//                                                if (failureFrom == "WEBRTC") {
                                                    establishConnection(
                                                        applicationContext,
                                                        channelName,
                                                        region
                                                    )
//                                                }
//                                                if (failureFrom == "SOCKET") {
//                                                    SocketHandler.setSocket(
//                                                        sharedPreference.getUserData().user_auth_token,
//                                                        applicationContext
//                                                    )
//                                                    SocketHandler.establishConnection(
//                                                        applicationContext
//                                                    )
//                                                }
                                            }
                                        } else {
                                            eventListener.onServerConnectionChange?.invoke(
                                                ServerConnectionState.Failure,
                                                message ?: "Error"
                                            )
                                            hasToRetry = true
                                        }
                                    }
                                }
                            }

                            "CONNECTION_CLOSED" -> {
                                getMainLooper {
                                    try {
                                        isFirstTime = false
                                        hasToRetry = false
                                        retryCount = 0
                                        eventListener.onServerConnectionChange?.invoke(
                                            ServerConnectionState.Closed,
                                            "Peer connection closed"
                                        )
                                        hasStartedTalking = false
                                        applicationContext.unregisterReceiver(broadcastReceiver)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }

                            "CONNECTING" -> {
                                getMainLooper {
                                    eventListener.onAudioStatusChange?.invoke(
                                        AudioStatus.Connecting
                                    )
                                }
                            }

                            "BUSY" -> {
                                getMainLooper {
                                    eventListener.onAudioStatusChange?.invoke(
                                        AudioStatus.Busy
                                    )
                                }
                            }

                            "SENDING" -> {
                                getMainLooper {
                                    eventListener.onAudioStatusChange?.invoke(
                                        AudioStatus.Sending
                                    )
                                }
                            }

                            "STOPPED" -> {
                                getMainLooper {
                                    eventListener.onAudioStatusChange?.invoke(
                                        AudioStatus.Stopped
                                    )
                                }
                            }

                            "NEW_CHANNEL" -> {
                                getMainLooper {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        intent.getSerializableExtra(
                                            "channel_obj", Channel::class.java
                                        )?.let {
                                            CoroutineScope(Dispatchers.Main).launch {
                                                TalkerSdkBackgroundService.database.roomDao()
                                                    .insertChannels(
                                                        listOf(it)
                                                    )
                                                eventListener.onNewChannel?.invoke(
                                                    it
                                                )
                                            }
                                        }
                                    } else {
                                        val channelObj = intent.getSerializableExtra(
                                            "channel_obj"
                                        ) as Channel
                                        CoroutineScope(Dispatchers.Main).launch {
                                            TalkerSdkBackgroundService.database.roomDao().insertChannels(
                                                listOf(channelObj)
                                            )
                                            eventListener.onNewChannel?.invoke(
                                                channelObj
                                            )
                                        }
                                    }
                                    Toast.makeText(context, "New channel added", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }

                            "ROOM_NAME_UPDATE" -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    intent.getSerializableExtra(
                                        "channel_obj", UpdateChannelNameModelData::class.java
                                    )?.let {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            val channel = TalkerSdkBackgroundService.database.roomDao()
                                                .getChannelById(it.channel_id)
                                            val updatedChannel = channel?.copy(group_name = it.new_name)
                                            if (updatedChannel != null) {
                                                TalkerSdkBackgroundService.database.roomDao()
                                                    .updateChannel(updatedChannel)
                                            }

                                            eventListener.onChannelUpdated?.invoke(
                                                it
                                            )
                                        }
                                    }
                                } else {
                                    val channelObj = intent.getSerializableExtra(
                                        "channel_obj"
                                    ) as UpdateChannelNameModelData
                                    CoroutineScope(Dispatchers.Main).launch {
                                        val channel = TalkerSdkBackgroundService.database.roomDao()
                                            .getChannelById(channelObj.channel_id)
                                        val updatedChannel = channel
                                            ?.copy(group_name = channelObj.new_name)
                                        if (updatedChannel != null) {
                                            TalkerSdkBackgroundService.database.roomDao()
                                                .updateChannel(updatedChannel)
                                        }
                                        eventListener.onChannelUpdated?.invoke(
                                            channelObj
                                        )
                                    }
                                }
                                Toast.makeText(context, "Name Updated", Toast.LENGTH_SHORT).show()
                            }

                            "ROOM_PARTICIPANT_REMOVED" -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    intent.getSerializableExtra(
                                        "channel_obj", RemoveParticipantModelData::class.java
                                    )?.let { removedUser ->
                                        CoroutineScope(Dispatchers.Main).launch {
                                            val channel = TalkerSdkBackgroundService.database.roomDao()
                                                .getChannelById(removedUser.channel_id)
                                            channel?.let {
                                                val updatedParticipants =
                                                    it.participants.filter { it.user_id != removedUser.removed_participant }
                                                val updatedChannel =
                                                    it.copy(participants = updatedParticipants)
                                                TalkerSdkBackgroundService.database.roomDao()
                                                    .updateChannel(updatedChannel)
                                            }

                                            if (removedUser.removed_participant == SharedPreference(
                                                    applicationContext
                                                ).getUserData().user_id
                                            ) {
                                                TalkerSdkBackgroundService.database.roomDao()
                                                    .deleteChannel(removedUser.channel_id)
                                            }
                                            eventListener.onUserRemovedFromChannel?.invoke(
                                                removedUser
                                            )
                                        }
                                    }
                                } else {
                                    val channelObj = intent.getSerializableExtra(
                                        "channel_obj"
                                    ) as RemoveParticipantModelData
                                    channelObj.let { removedUser ->
                                        CoroutineScope(Dispatchers.Main).launch {
                                            val channel = TalkerSdkBackgroundService.database.roomDao()
                                                .getChannelById(removedUser.channel_id)
                                            channel?.let {
                                                val updatedParticipants =
                                                    it.participants.filter { it.user_id != removedUser.removed_participant }
                                                val updatedChannel =
                                                    it.copy(participants = updatedParticipants)
                                                TalkerSdkBackgroundService.database.roomDao()
                                                    .updateChannel(updatedChannel)
                                            }

                                            if (removedUser.removed_participant == SharedPreference(
                                                    applicationContext
                                                ).getUserData().user_id
                                            ) {
                                                TalkerSdkBackgroundService.database.roomDao()
                                                    .deleteChannel(removedUser.channel_id)
                                            }
                                            eventListener.onUserRemovedFromChannel?.invoke(
                                                removedUser
                                            )
                                        }
                                    }
                                }
                            }

                            "ROOM_PARTICIPANT_ADDED" -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    intent.getSerializableExtra(
                                        "channel_obj", AddNewParticipantModelData::class.java
                                    )?.let { addedUser ->
                                        CoroutineScope(Dispatchers.Main).launch {
                                            val channel =
                                                TalkerSdkBackgroundService.database.roomDao()
                                                    .getChannelById(addedUser.channel_id)
                                            channel?.let {
                                                val updatedParticipants =
                                                    it.participants.toMutableList()
                                                updatedParticipants.add(addedUser.new_participants[0])
                                                val updatedChannel =
                                                    it.copy(participants = updatedParticipants)
                                                TalkerSdkBackgroundService.database.roomDao()
                                                    .updateChannel(updatedChannel)
                                            }
                                            eventListener.onAddedUserInChannel?.invoke(
                                                addedUser
                                            )
                                        }
                                    }
                                } else {
                                    val channelObj = intent.getSerializableExtra(
                                        "channel_obj"
                                    ) as AddNewParticipantModelData
                                    channelObj.let { addedUser ->
                                        CoroutineScope(Dispatchers.Main).launch {
                                            val channel =
                                                TalkerSdkBackgroundService.database.roomDao()
                                                    .getChannelById(addedUser.channel_id)
                                            channel?.let {
                                                val updatedParticipants =
                                                    it.participants.toMutableList()
                                                updatedParticipants.add(addedUser.new_participants[0])
                                                val updatedChannel =
                                                    it.copy(participants = updatedParticipants)
                                                TalkerSdkBackgroundService.database.roomDao()
                                                    .updateChannel(updatedChannel)
                                            }
                                            eventListener.onAddedUserInChannel?.invoke(
                                                addedUser
                                            )
                                        }
                                    }
                                }
                            }

                            "NEW_SDK_USER" -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    intent.getSerializableExtra(
                                        "channel_obj", GetAllUserModelData::class.java
                                    )?.let { newUser ->
                                        CoroutineScope(Dispatchers.Main).launch {
                                            TalkerSdkBackgroundService.database.roomDao().insertUsers(
                                                listOf(newUser)
                                            )
                                            eventListener.onNewSdkUser?.invoke(
                                                newUser
                                            )
                                        }
                                    }
                                } else {
                                    val channelObj = intent.getSerializableExtra(
                                        "channel_obj"
                                    ) as GetAllUserModelData
                                    channelObj.let { newUser ->
                                        CoroutineScope(Dispatchers.Main).launch {
                                            TalkerSdkBackgroundService.database.roomDao().insertUsers(
                                                listOf(newUser)
                                            )
                                            eventListener.onNewSdkUser?.invoke(
                                                newUser
                                            )
                                        }
                                    }
                                }
                            }

                            "ROOM_ADMIN_ADDED" -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    intent.getSerializableExtra(
                                        "channel_obj", AddNewAdminModelData::class.java
                                    )?.let { newAdmin ->
                                        CoroutineScope(Dispatchers.Main).launch {
                                            val channel = TalkerSdkBackgroundService.database.roomDao()
                                                .getChannelById(newAdmin.channel_id)
                                            channel?.let {
                                                val updatedParticipants =
                                                    it.participants.map { participant ->
                                                        if (participant.user_id == newAdmin.new_admin) {
                                                            participant.copy(admin = true)
                                                        } else {
                                                            participant
                                                        }
                                                    }
                                                val updatedChannel =
                                                    it.copy(participants = updatedParticipants)
                                                TalkerSdkBackgroundService.database.roomDao()
                                                    .updateChannel(updatedChannel)
                                            }
                                            eventListener.onAdminAdded?.invoke(
                                                newAdmin
                                            )
                                        }
                                    }
                                } else {
                                    val channelObj = intent.getSerializableExtra(
                                        "channel_obj"
                                    ) as AddNewAdminModelData
                                    channelObj.let { newAdmin ->
                                        CoroutineScope(Dispatchers.Main).launch {
                                            val channel = TalkerSdkBackgroundService.database.roomDao()
                                                .getChannelById(newAdmin.channel_id)
                                            channel?.let {
                                                val updatedParticipants =
                                                    it.participants.map { participant ->
                                                        if (participant.user_id == newAdmin.new_admin) {
                                                            participant.copy(admin = true)
                                                        } else {
                                                            participant
                                                        }
                                                    }
                                                val updatedChannel =
                                                    it.copy(participants = updatedParticipants)
                                                TalkerSdkBackgroundService.database.roomDao()
                                                    .updateChannel(updatedChannel)
                                            }
                                            eventListener.onAdminAdded?.invoke(
                                                newAdmin
                                            )
                                        }
                                    }
                                }
                            }

                            "ROOM_ADMIN_REMOVED" -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    intent.getSerializableExtra(
                                        "channel_obj", AdminRemoveModelData::class.java
                                    )?.let { removedAdmin ->
                                        CoroutineScope(Dispatchers.Main).launch {
                                            val channel = TalkerSdkBackgroundService.database.roomDao()
                                                .getChannelById(removedAdmin.channel_id)
                                            channel?.let {
                                                val updatedParticipants =
                                                    it.participants.map { participant ->
                                                        if (participant.user_id == removedAdmin.admin_removed) {
                                                            participant.copy(admin = false)
                                                        } else {
                                                            participant
                                                        }
                                                    }
                                                val updatedChannel =
                                                    it.copy(participants = updatedParticipants)
                                                TalkerSdkBackgroundService.database.roomDao()
                                                    .updateChannel(updatedChannel)
                                            }
                                            eventListener.onAdminRemoved?.invoke(
                                                removedAdmin
                                            )
                                        }
                                    }
                                } else {
                                    val channelObj = intent.getSerializableExtra(
                                        "channel_obj"
                                    ) as AdminRemoveModelData
                                    channelObj.let { removedAdmin ->
                                        CoroutineScope(Dispatchers.Main).launch {
                                            val channel = TalkerSdkBackgroundService.database.roomDao()
                                                .getChannelById(removedAdmin.channel_id)
                                            channel?.let {
                                                val updatedParticipants =
                                                    it.participants.map { participant ->
                                                        if (participant.user_id == removedAdmin.admin_removed) {
                                                            participant.copy(admin = false)
                                                        } else {
                                                            participant
                                                        }
                                                    }
                                                val updatedChannel =
                                                    it.copy(participants = updatedParticipants)
                                                TalkerSdkBackgroundService.database.roomDao()
                                                    .updateChannel(updatedChannel)
                                            }
                                            eventListener.onAdminRemoved?.invoke(
                                                removedAdmin
                                            )
                                        }
                                    }
                                }
                            }

                            "MESSAGE" -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    intent.getSerializableExtra(
                                        "channel_obj", MessageObject::class.java
                                    )?.let { message ->
                                        CoroutineScope(Dispatchers.Main).launch {
                                            val userId = context?.let {
                                                SharedPreference(it).getUserData().user_id
                                            } ?: ""
                                            Log.d(
                                                "@@@@",
                                                "From message : $message"
                                            )
                                            if (message.sender_id != userId) {
                                                val senderName = TalkerSdkBackgroundService.database.roomDao()
                                                    .getUserById(message.sender_id)
                                                val channelName2 = TalkerSdkBackgroundService.database.roomDao()
                                                    .getChannelById(message.channel_id)
                                                val messageObject = MessageObjectForLocalDB(
                                                    attachments = message.attachments,
                                                    channel_id = message.channel_id,
                                                    channel_name = channelName2?.group_name ?: "Channel Name",
                                                    text = message.description,
                                                    id = message.id,
                                                    sent_at = message.sent_at,
                                                    sender_id = message.sender_id,
                                                    sender_name = senderName?.name ?: "User"
                                                )
                                                TalkerSdkBackgroundService.database.roomDao()
                                                    .insertMessage(messageObject)
                                                eventListener.onNewMessageReceived?.invoke(message)
                                            }
                                        }
                                    }
                                } else {
                                    val channelObj = intent.getSerializableExtra(
                                        "channel_obj"
                                    ) as MessageObject
                                    channelObj.let { message ->
                                        CoroutineScope(Dispatchers.Main).launch {
                                            val userId = context?.let {
                                                SharedPreference(it).getUserData().user_id
                                            } ?: ""
                                            Log.d(
                                                "@@@@",
                                                "From message : $message"
                                            )
                                            if (message.sender_id != userId){
                                                val senderName = TalkerSdkBackgroundService.database.roomDao()
                                                    .getUserById(message.sender_id)
                                                val channelName2 = TalkerSdkBackgroundService.database.roomDao()
                                                    .getChannelById(message.channel_id)
                                                val messageObject = MessageObjectForLocalDB(
                                                    attachments = message.attachments,
                                                    channel_id = message.channel_id,
                                                    channel_name = channelName2?.group_name ?: "Channel Name",
                                                    text = message.description,
                                                    id = message.id,
                                                    sent_at = message.sent_at,
                                                    sender_id = message.sender_id,
                                                    sender_name = senderName?.name ?: "User"
                                                )
                                                TalkerSdkBackgroundService.database.roomDao()
                                                    .insertMessage(messageObject)
                                                eventListener.onNewMessageReceived?.invoke(message)
                                            }
                                        }
                                    }
                                }
                            }

                            "CURRENT_PTT_AUDIO" -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    intent.getSerializableExtra(
                                        "ptt_audio", AudioData::class.java
                                    )?.let { audioData ->
                                        Log.d(
                                            "audioData",
                                            "audioData : $audioData"
                                        )
                                        CoroutineScope(Dispatchers.Main).launch {
                                            eventListener.currentPttAudio?.invoke(
                                                audioData
                                            )
                                        }
                                    }
                                } else {
                                    val channelObj = intent.getSerializableExtra(
                                        "ptt_audio"
                                    ) as AudioData
                                    channelObj.let { audioData ->
                                        Log.d(
                                            "audioData",
                                            "audioData : $audioData"
                                        )
                                        CoroutineScope(Dispatchers.Main).launch {
                                            CoroutineScope(Dispatchers.Main).launch {
                                                eventListener.currentPttAudio?.invoke(
                                                    audioData
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                try {
                    applicationContext.unregisterReceiver(broadcastReceiver)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                applicationContext.registerReceiver(
                    broadcastReceiver,
                    IntentFilter(
                        "com.talker.sdk"
                    ), Context.RECEIVER_EXPORTED
                )
            } else {
                SocketHandler.setSocket(
                    sharedPreference.getUserData().user_auth_token,
                    applicationContext
                )
                SocketHandler.establishConnection(applicationContext)
            }


            if (updateSignalingChannelInfo(
                    mRegion,
                    mChannelName,
                    ChannelRole.VIEWER
                )
            ) {
                //if the mIceServerList is not empty then the following code will be executed
                if (mIceServerList.isNotEmpty()) {
                    for (iceServer in mIceServerList) {
                        //get the usernames, passwords, ttls and uris from the mIceServerList and add them to the respective lists
                        mUserNames.add(iceServer.username)
                        mPasswords.add(iceServer.password)
                        ttls.add(iceServer.ttl)
                        mUrisList.add(iceServer.uris)
                    }
                }

                //get the webrtc endpoint in which we have to make the connection
                for (endpoint in mEndpointList) {
                    if (endpoint.protocol == "WSS") {
                        mWssEndpoint = endpoint.resourceEndpoint
                    }
                }

                //create a stun server and add it to the peerIceServers list
                val stun = PeerConnection.IceServer.builder(
                    String.format(
                        "stun:stun.kinesisvideo.%s.amazonaws.com:443", mRegion
                    )
                ).createIceServer()
                //function to add the stun server to the peerIceServers list
                peerIceServers.add(stun)

                //create a turn server and add it to the peerIceServers list
                for (i in mUrisList.indices) {
                    val turnServer = mUrisList[i].toString()
                    val iceServer = PeerConnection.IceServer.builder(
                        turnServer.replace(
                            "[", ""
                        ).replace("]", "")
                    ).setUsername(mUserNames[i]).setPassword(mPasswords[i])
                        .createIceServer()
                    //function to add the turn server to the peerIceServers list
                    peerIceServers.add(iceServer)
                }
                Log.d(
                    LOG_TAG, "Added ice servers to peerIceServers list"
                )

                // initializing the peerConnection factory
                PeerConnectionFactory.initialize(
                    //this@MainActivity
                    PeerConnectionFactory.InitializationOptions.builder(applicationContext)
                        .createInitializationOptions()
                )

                //creating an instance of the PeerConnection factory and passing the audio device module so that we could specify what modules we are using.
                peerConnectionFactory =
                    PeerConnectionFactory.builder().setAudioDeviceModule(
                        JavaAudioDeviceModule.builder(applicationContext)
                            .createAudioDeviceModule()
                    ).createPeerConnectionFactory()

                //create an audio track and add it to the peerConnectionFactory.
                //this will be the local audio track which we will be sending to the connection server and which the remote peer will here
                if (isAudioSent) {
                    val audioSource =
                        peerConnectionFactory?.createAudioSource(MediaConstraints())
                    localAudioTrack = peerConnectionFactory?.createAudioTrack(
                        AudioTrackID, audioSource
                    )
                    localAudioTrack?.setEnabled(false)
                    Log.d(
                        LOG_TAG, "Local audio source added"
                    )

                    // callback function which will be called when the localAudio track is successfully added.
                    originalAudioMode = audioManager!!.mode
                    originalSpeakerphoneOn = audioManager!!.isSpeakerphoneOn
                }


                //creating the master and viewer end points.
                // if we are connecting as master then we don't need any client id but when joining as viewer we need to specify that what will be our clientid so
                // that the server can distinguish when multiple viewers are joining. All this viewers will be identified on the basis of their respective client ids.
                // the client id will be the last part of their user_auth_token.
                // we will get the mChannelArn from the api. i.e. when we fetch info about the channel using its name.
                val viewerEndpoint =
                    ((mWssEndpoint + "?" + Constants.CHANNEL_ARN_QUERY_PARAM) + "=" + mChannelArn + "&" + Constants.CLIENT_ID_QUERY_PARAM) + "=" + mClientId
                if (TalkerGlobalVariables.printLogs) {
                    Log.d(
                        LOG_TAG,
                        "end point created : $viewerEndpoint"
                    )
                }


                // this function will generate the signed uri for the signaling service.
                // this is done to basically authenticate the user to the signaling service.
                val signedUri = getSignedUri(
                    viewerEndpoint,
                    mCreds,
                    applicationContext,
                    mWssEndpoint,
                    mRegion
                )

                //check if the signedUri is not null.
                checkNotNull(signedUri)

                val wsHost = signedUri.toString()

                // creating the signaling listeners which will listen to the events from the signaling service. Events will be for sending sdp offer, on receiving sdp answer of our
                // sdp offer that we have sent and when receiving an ice candidates.
                val signalingListener: SignalingListener = object : SignalingListener() {
                    //this method will be called when the user will receive an sdp offer from some other remote user.
                    override fun onSdpOffer(event: Event) {
                        Log.d(
                            LOG_TAG,
                            "Recieved sdp offer from the client : ${event.senderClientId}"
                        )
                        //extracting the sdp from the event and creating a session description object.
                        val sdp: String = Event.parseOfferEvent(event)
                        if (localPeer != null) {
                            // we need to set this description so that the local peer could come to known
                            // the current state of this process.
                            // for example here we set it to 'SessionDescription.Type.OFFER' so that it could come to know that current we have
                            // received an sdp offer and we are ready to send an sdp answer.
                            localPeer?.setRemoteDescription(
                                KinesisVideoSdpObserver(),
                                SessionDescription(SessionDescription.Type.OFFER, sdp)
                            )
                        } else {
                            throw Exception().initCause(Throwable("local peer is null"))
                        }

                        // sender client id will be now recipient client id as we will be sending the sdp answer to this client id only.
                        recipientClientId = event.senderClientId

                        // function to create sdp answer
                        createSdpAnswer(
                            localPeer,
                            recipientClientId,
                            client,
                            peerConnectionFoundMap,
                            pendingIceCandidatesMap
                        )

                        // notify the user that media has started recording to the stream
                        if (webrtcEndpoint != null) {
                            if (TalkerGlobalVariables.printLogs) {
                                Log.i(
                                    LOG_TAG, "Media is being recorded to $mStreamArn"
                                )
                            }
                        }
                    }


                    //this method will be called when the user will receive the sdp answer o the sdp offer that he/she has sent to the remote user.
                    override fun onSdpAnswer(event: Event) {
                        if (TalkerGlobalVariables.printLogs) {
                            Log.d(
                                LOG_TAG,
                                "SDP answer received from remote client : ${event.senderClientId}"
                            )
                        }
                        // parsing the answer event and getting the sdp from it.
                        val sdp: String = Event.parseSdpEvent(event)
                        // creating and setting the answer session description so that local peer could come to known
                        // that we have received an sdp answer from the remote peer.
                        val sdpAnswer =
                            SessionDescription(SessionDescription.Type.ANSWER, sdp)
//                val sdpAnswer = SessionDescription(SessionDescription.Type.ANSWER, sdp.replace("\r\n", "\\r\\n"))

                        Log.d(
                            LOG_TAG, "sdpAnswer.description : ${sdpAnswer.description}"
                        )
                        Log.d(
                            LOG_TAG, "sdpAnswer.type : ${sdpAnswer.type}"
                        )
                        if (localPeer != null) {
                            localPeer!!.setRemoteDescription(object :
                                KinesisVideoSdpObserver() {
                                override fun onCreateFailure(error: String) {
                                    super.onCreateFailure(error)
                                    throw Exception().initCause(Throwable(error))
                                }

                                override fun onSetFailure(error: String) {
                                    super.onSetFailure(error)
                                    Log.d(
                                        LOG_TAG, "onSetSessionDescriptionFailure : $error"
                                    )
                                }
                            }, sdpAnswer)
                        } else {
                            throw Exception().initCause(Throwable("local peer is null"))
                        }

                        //adding this local peer connection to this map which contains the current active peer connections the user has joined in.
                        peerConnectionFoundMap[event.senderClientId] = localPeer!!

                        // handle the pending ice candidates which are remaining to be added.
                        handlePendingIceCandidates(
                            event.senderClientId,
                            pendingIceCandidatesMap,
                            peerConnectionFoundMap
                        )
                    }

                    // this method will be called when the user will receive the ice candidates from the remote user.
                    override fun onIceCandidate(event: Event) {
                        if (TalkerGlobalVariables.printLogs) {
                            Log.d(
                                LOG_TAG,
                                "Received ICE candidate from remote client : ${event.senderClientId}"
                            )
                        }

                        // parsing the ice candidate event and getting the ice candidate from it.
                        val iceCandidate: IceCandidate? = Event.parseIceCandidate(event)
                        if (iceCandidate != null) {
                            // check if the ice candidate is already in the pending ice candidate map or not and if not the adding it in it.
                            checkAndAddIceCandidate(
                                event,
                                iceCandidate,
                                peerConnectionFoundMap,
                                pendingIceCandidatesMap
                            )
                        } else {
                            if (TalkerGlobalVariables.printLogs) {
                                Log.e(
                                    LOG_TAG, "Invalid ICE candidate"
                                )
                            }
                        }
                    }

                    // method that will be called when some error would occur in the signaling client.
                    override fun onError(event: Event) {
                        if (TalkerGlobalVariables.printLogs) {
                            Log.e(
                                LOG_TAG, "Received error in signaling client : $event"
                            )
                            return
                        }
                        sendBroadCast(
                            "CONNECTION_FAILURE",
                            "Received Webrtc error in signaling client : $event",
                            "WEBRTC"
                        )
                    }


                    // method will be called when some exception is thrown in the signaling client.
                    override fun onException(e: java.lang.Exception) {
                        if (TalkerGlobalVariables.printLogs) {
                            Log.d(
                                LOG_TAG, "Signaling client returned exception: " + e.message
                            )
                        }
                        sendBroadCast(
                            "CONNECTION_FAILURE",
                            "Signaling client returned Webrtc exception: " + e.message,
                            "WEBRTC"
                        )
                    }

                    // method will be called when permission is changed for allowing other users to speak.
                    override fun onPermissionChanged(event: Event) {
                        if (TalkerGlobalVariables.printLogs) {
                            Log.d(
                                "onPermissionChanged", "onPermissionChanged : $event"
                            )
                        }
                    }
                }

                try {
                    // assigning the websocket client to the client variable
                    // we pass the signed uri to which client connection will be made
                    // we also pass the signaling listeners which will listen to events.
                    // we also pass how many parallel threads could run this process
                    client = SignalingServiceWebSocketClient(
                        wsHost, signalingListener, Executors.newFixedThreadPool(10)
                    )
                    if (TalkerGlobalVariables.printLogs) {
                        Log.d(
                            LOG_TAG,
                            "Client connection " + (if (client!!.isOpen) "Successful" else "Failed")
                        )
                    }
                    if (client?.isOpen == false) {
                        sendBroadCast(
                            "CONNECTION_FAILURE",
                            "Webrtc Client connection failed",
                            "WEBRTC"
                        )
                    }
                } catch (e: java.lang.Exception) {
                    if (TalkerGlobalVariables.printLogs) {
                        Log.e(
                            LOG_TAG, "Exception with websocket client: $e"
                        )
                    }
                    sendBroadCast(
                        "CONNECTION_FAILURE",
                        "Webrtc Exception with websocket client: $e",
                        "WEBRTC"
                    )
                }

                if (client?.isOpen == true) {
                    if (TalkerGlobalVariables.printLogs) {
                        Log.d(
                            LOG_TAG,
                            "Client connected to Signaling service " + client!!.isOpen
                        )
                    }
                    if (TalkerGlobalVariables.printLogs) {
                        Log.d(
                            LOG_TAG, "Sending sdp offer as viewer to remote peer"
                        )
                    }
                    // we will have to send an sdp offer to the master so that we could join the master.
                    // after sending this offer to master we will receive an answer from the master which we implemented earlier in this file inside
                    // signaling listeners.
                    createSdpOffer(
                        peerIceServers,
                        peerConnectionFactory!!,
                        onLocalPeer = {
                            localPeer = it
                        },
                        applicationContext,
                        client!!,
                        CHANNEL_ID,
                        mNotificationId = {
                            mNotificationId++
                        },
                        mClientId,
                        recipientClientId,
                        onRemoteAudioTrack = {
                            remoteAudioTrack = it
                            remoteAudioTrack?.setEnabled(true)
                            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
                            audioManager?.isSpeakerphoneOn = true
                        },
                        audioManager,
                        onFinish = {
                            closeConnection()
                        },
                        localPeer,
                        mChannelName,
                        onDataChannel = { dataChannel2: DataChannel? ->
                            dataChannel = dataChannel2
                        }
                    )
                } else {
                    if (TalkerGlobalVariables.printLogs) {
                        Log.e(
                            LOG_TAG, "Error in connecting to signaling service"
                        )
                    }
                    sendBroadCast(
                        "CONNECTION_FAILURE",
                        "Webrtc Error in connecting to signaling service",
                        "WEBRTC"
                    )
                }
            } else {
                sendBroadCast(
                    "CONNECTION_FAILURE",
                    "Webrtc Connection failed",
                    "WEBRTC"
                )
                return@launch
            }
        }
    }

    private fun getMainLooper(execute: () -> Unit) {
        Handler(Looper.getMainLooper()).post {
            execute()
        }
    }

    private fun sendBroadCast(
        action: String = "",
        message: String = "",
        failureFrom: String? = null
    ) {
        applicationContext?.sendBroadcast(
            Intent()
                .setPackage(applicationContext!!.packageName)
                .setAction("com.talker.sdk")
                .apply {
                    putExtra("action", action)
                    putExtra("message", message)
                    failureFrom?.let {
                        putExtra(
                            "failure_from", failureFrom
                        )
                    }
                }
        )
    }

    @SuppressLint("MissingPermission")
    private fun setupAudioRecorder() {
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        Log.d(
            LOG_TAG, "buffersize : $bufferSize"
        )
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        buffer = ByteArray(bufferSize)
    }

    private fun startRecording() {
        setupAudioRecorder()
        audioFile = File(
            applicationContext?.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
            "${UUID.randomUUID()}.wav"
        )
        audioRecord.startRecording()
        isRecording = true
        CoroutineScope(Dispatchers.IO).launch {
            val fileOutputStream = FileOutputStream(audioFile)
            try {
                writeWavHeader(fileOutputStream, sampleRate)
                while (isRecording) {
                    val read = audioRecord.read(buffer, 0, buffer.size)
                    fileOutputStream.write(buffer, 0, read)
                    // Send audio data over WebRTC
                    val byteBuffer = ByteBuffer.wrap(buffer, 0, read)
                    dataChannel?.send(DataChannel.Buffer(byteBuffer, true))
                }
                updateWavHeader(fileOutputStream)
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                fileOutputStream.close()
            }
        }
    }

    private fun stopRecording() {
        if (::audioRecord.isInitialized && isRecording) {
            isRecording = false
            audioRecord.stop()
            audioRecord.release()
        }
    }

    @Throws(IOException::class)
    private fun updateWavHeader(outputStream: FileOutputStream) {
        val fileSize = outputStream.channel.size()
        val dataSize = fileSize - 44

        val fileSizeBytes = ByteArray(4)
        fileSizeBytes[0] = (fileSize and 0xff).toByte()
        fileSizeBytes[1] = ((fileSize shr 8) and 0xff).toByte()
        fileSizeBytes[2] = ((fileSize shr 16) and 0xff).toByte()
        fileSizeBytes[3] = ((fileSize shr 24) and 0xff).toByte()

        val dataSizeBytes = ByteArray(4)
        dataSizeBytes[0] = (dataSize and 0xff).toByte()
        dataSizeBytes[1] = ((dataSize shr 8) and 0xff).toByte()
        dataSizeBytes[2] = ((dataSize shr 16) and 0xff).toByte()
        dataSizeBytes[3] = ((dataSize shr 24) and 0xff).toByte()

        outputStream.channel.position(4)
        outputStream.write(fileSizeBytes)
        outputStream.channel.position(40)
        outputStream.write(dataSizeBytes)
    }

    @Throws(IOException::class)
    private fun writeWavHeader(outputStream: FileOutputStream, sampleRate: Int) {
        val byteRate = 16 * sampleRate * 1 / 8
        val header = byteArrayOf(
            'R'.toByte(),
            'I'.toByte(),
            'F'.toByte(),
            'F'.toByte(), // ChunkID
            0,
            0,
            0,
            0, // ChunkSize (to be updated later)
            'W'.toByte(),
            'A'.toByte(),
            'V'.toByte(),
            'E'.toByte(), // Format
            'f'.toByte(),
            'm'.toByte(),
            't'.toByte(),
            ' '.toByte(), // Subchunk1ID
            16,
            0,
            0,
            0, // Subchunk1Size
            1,
            0, // AudioFormat
            1,
            0, // NumChannels
            (sampleRate and 0xff).toByte(),
            ((sampleRate shr 8) and 0xff).toByte(),
            ((sampleRate shr 16) and 0xff).toByte(),
            ((sampleRate shr 24) and 0xff).toByte(), // SampleRate
            (byteRate and 0xff).toByte(),
            ((byteRate shr 8) and 0xff).toByte(),
            ((byteRate shr 16) and 0xff).toByte(),
            ((byteRate shr 24) and 0xff).toByte(), // ByteRate
            ((1 * 16) / 8).toByte(),
            0, // BlockAlign
            16,
            0, // BitsPerSample
            'd'.toByte(),
            'a'.toByte(),
            't'.toByte(),
            'a'.toByte(), // Subchunk2ID
            0,
            0,
            0,
            0 // Subchunk2Size (to be updated later)
        )
        outputStream.write(header, 0, 44)
    }

    // use this function before initializing function
    // this will get the channel arn from the api, mIceServerList, and endpoint list as well
    // without calling this function the initialize function will be of no use.
    private fun updateSignalingChannelInfo(
        region: String, channelName: String, role: ChannelRole
    ): Boolean {
        val TAG = "updateSignalingChannelInfo"
        mChannelArn = null
        mIceServerList.clear()
        mEndpointList.clear()
        val task = UpdateSignalingChannelInfoTask(onMChannelArn = { res ->
            mChannelArn = res
        }, onMEndpointList = { res ->
            mEndpointList.addAll(res)
        }, onMIceServerList = { res ->
            mIceServerList.addAll(res)
        })

        var errorMessage: String? = null
        try {
            errorMessage = task.execute(region, channelName, role).get()
        } catch (e: java.lang.Exception) {
            Log.e(
                TAG, "Failed to wait for response of UpdateSignalingChannelInfoTask", e
            )
        }

        if (errorMessage != null) {
            Log.e(
                TAG, "updateSignalingChannelInfo() encountered an error: $errorMessage"
            )
        }
        return errorMessage == null
    }

    // this method will be used to toggle the local audio track.
    // when user presses the button of push to talk than this is method should be called with parameter set to false.
    // by doing that we specify that we are acquiring the rights speak and so no other person can speak.
    // this function sends the the permission to the backend and also handles the local audio source which responsible for sharing
    // your audio with other users.
    private fun startEndAudioSending(
        allowOthersToSpeak: Boolean,
        isChannelAvailable: (Boolean) -> Unit = {}
    ) {
        if (allowOthersToSpeak) {
            // stop sharing audio
            SocketHandler.broadCastStop(mChannelId)
            localAudioTrack?.setEnabled(false)
            client?.sendPermission(
                Message(
                    "PERMISSION_CHANGED",
                    recipientClientId,
                    (mClientId ?: ""),
                    String(
                        Base64.encode(
                            ("{\"type\":\"permission\",\"sdp\":\"true\"}").replace(
                                "\r\n", "\\r\\n"
                            ).toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP
                        )
                    )
                )
            )
            endStream()
            stopRecording()
        } else {
            // start sending audio
            // this function will return true inside ack if the channel is available.
            // i.e. no other person in the room is speaking right now.
            SocketHandler.broadCastStart(mChannelId, onAck = { args ->
                runOnUiThread {
                    if (args.getOrNull(0) == true) {
                        localAudioTrack?.setEnabled(true)
                        client?.sendPermission(
                            Message(
                                "PERMISSION_CHANGED",
                                recipientClientId,
                                (mClientId ?: ""),
                                String(
                                    Base64.encode(
                                        ("{\"type\":\"permission\",\"sdp\":\"false\"}").replace(
                                            "\r\n", "\\r\\n"
                                        ).toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP
                                    )
                                )
                            )
                        )
                        startStream()
                        startRecording()
                        Log.d(
                            LOG_TAG, "rtcSender : $rtcSender"
                        )
                        isChannelAvailable(true)
                    } else {
                        isChannelAvailable(false)
                    }
                    if (TalkerGlobalVariables.printLogs) {
                        Log.d(
                            LOG_TAG, "Is channel available : ${args.getOrNull(0) == true}"
                        )
                    }
                }
            })
        }
    }

    private fun startStream() {
        val messageId = UUID.randomUUID()
        dataChannel?.send(
            DataChannel.Buffer(
                ByteBuffer.wrap(
                    "{\"type\": \"stream_start\",\"channel_id\": \"$mChannelId\",\"message_id\": \"${messageId}\",\"bit_depth\": \"8\",\"sample_rate\": \"16000\",\"receiver_count\": \"2\"}".toByteArray(
                        Charset.defaultCharset()
                    )
                ), false
            )
        )
        if (TalkerGlobalVariables.printLogs) {
            Log.d(
                LOG_TAG, "Stream started..."
            )
            Log.d(
                LOG_TAG, "messageId $messageId"
            )
        }
    }

    private fun endStream() {
        val messageId = UUID.randomUUID()
        dataChannel?.send(
            DataChannel.Buffer(
                ByteBuffer.wrap(
                    "{\"type\": \"stream_end\",\"channel_id\": \"$mChannelId\"}".toByteArray(
                        Charset.defaultCharset()
                    )
                ), false
            )
        )
        if (TalkerGlobalVariables.printLogs) {
            Log.d(
                LOG_TAG, "Stream ended..."
            )
            Log.d(
                LOG_TAG, "messageId $messageId"
            )
        }
    }
}
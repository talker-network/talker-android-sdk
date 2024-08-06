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
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.SignOutOptions
import com.amazonaws.mobile.client.results.SignInResult
import com.amazonaws.regions.Region
import com.amazonaws.services.kinesisvideo.model.ChannelRole
import com.amazonaws.services.kinesisvideo.model.ResourceEndpointListItem
import com.amazonaws.services.kinesisvideosignaling.model.IceServer
import com.amazonaws.services.kinesisvideowebrtcstorage.AWSKinesisVideoWebRTCStorageClient
import com.amazonaws.services.kinesisvideowebrtcstorage.model.JoinStorageSessionRequest
import network.talker.app.dev.model.Event
import network.talker.app.dev.model.Message
import network.talker.app.dev.networking.calls.sdkCreateUser
import network.talker.app.dev.networking.calls.sdkCredAPI
import network.talker.app.dev.networking.calls.sdkSetUser
import network.talker.app.dev.okhttp.SignalingListener
import network.talker.app.dev.okhttp.SignalingServiceWebSocketClient
import network.talker.app.dev.sharedPreference.SharedPreference
import network.talker.app.dev.socket.SocketHandler
import network.talker.app.dev.utils.Constants
import network.talker.app.dev.utils.getSignedUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import network.talker.app.dev.webrtc.AudioStatus
import network.talker.app.dev.webrtc.CreateLocalPeerConnection
import network.talker.app.dev.webrtc.EventListener
import network.talker.app.dev.webrtc.KinesisVideoSdpObserver
import network.talker.app.dev.webrtc.PeerConnectionState
import network.talker.app.dev.webrtc.RegistrationState
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
    private var isMaster: Boolean = true
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
    private fun validateSDKKey() {
        if (sdkKey.isEmpty()) {
            throw Exception(SDK_KEY_ERROR)
        }
    }

    fun isUserLoggedIn(): Boolean {
        validateSDKKey()
        return KinesisTalkerApp.auth.isSignedIn
    }

    fun init(sdkKey: String) {
        Talker.sdkKey = sdkKey
    }

    fun createUser(
        context: Context,
        name: String,
        fcmToken: String,
        eventListener: EventListener
    ) {
        validateSDKKey()
        val sharedPreference = SharedPreference(context)
        sdkCreateUser(context, name = name, onSuccess = { res ->
            sharedPreference.setUserData(res.data)
            TalkerSDKApplication().auth.signIn(res.data.a_username,
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
                            eventListener.onRegistrationStateChange(
                                RegistrationState.Success,
                                "User register success"
                            )
                        }
                        establishConnection(
                            context,
                            false,
                            eventListener
                        )
                    }

                    override fun onError(e: java.lang.Exception?) {
                        getMainLooper {
                            eventListener.onRegistrationStateChange(
                                RegistrationState.Failure,
                                "User register failed. Error : ${e?.message}"
                            )
                            e?.printStackTrace()
                        }
                    }
                })
        }, onError = { errorData ->
            getMainLooper {
                eventListener.onRegistrationStateChange(
                    RegistrationState.Failure,
                    errorData.message
                )
            }
        }, onInternetNotAvailable = {
            getMainLooper {
                eventListener.onRegistrationStateChange(
                    RegistrationState.Failure,
                    "Network not available"
                )
            }
        }, fcmToken = fcmToken,
            sdkKey = sdkKey
        )
    }

    fun startPttAudio(isChannelAvailable: (Boolean) -> Unit) {
        validateSDKKey()
        sendBroadCast(
            "CONNECTING",
            "Getting info about channel availability"
        )
        if (!hasStartedTalking) {
            toggleLocalAudioTrack(false, isChannelAvailable = {
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

    fun stopPttAudio() {
        validateSDKKey()
        if (hasStartedTalking) {
            hasStartedTalking = false
            toggleLocalAudioTrack(true)
            sendBroadCast("STOPPED", "Stopped sending audio")
        }
    }

    // close everything....
    fun closeConnection() {
        hasStartedTalking = false
        validateSDKKey()
        SocketHandler.broadCastStop(mChannelId)
        SocketHandler.closeConnection()
        audioManager?.mode = originalAudioMode
        audioManager?.isSpeakerphoneOn = originalSpeakerphoneOn
        pendingIceCandidatesMap.clear()
        peerConnectionFoundMap.clear()
        peerConnectionFoundMap.clear()
        pendingIceCandidatesMap.clear()
        createLocalPeerConnection?.closeAll()
        try {
            runOnUiThread {
                synchronized(this) {
                    localPeer?.let {
                        it.close()
                        it.dispose()
                        localPeer = null
                    }
                }
            }
        } catch (e: Exception) {
            if (TalkerGlobalVariables.printLogs) {
                Log.e(LOG_TAG, "Error while closing and disposing localPeer", e)
            }
        }
        client?.disconnect()
        client = null
        logoutUser(
            onLogoutSuccess = {
                CoroutineScope(Dispatchers.Main).launch {
                    sendBroadCast(
                        "CONNECTION_CLOSED",
                        "Connection closed"
                    )
                }
            }
        )

    }

    private fun logoutUser(
        onLogoutSuccess: (message: String) -> Unit = {}
    ) {
        validateSDKKey()
        KinesisTalkerApp.auth.signOut(
            SignOutOptions.Builder()
                .signOutGlobally(true)
                .build(),
            object : Callback<Void?> {
                override fun onResult(result: Void?) {
                    getMainLooper {
                        onLogoutSuccess("Sign out successful")
                    }
                }

                override fun onError(e: java.lang.Exception?) {
                    getMainLooper {
                        onLogoutSuccess("Sign out successful")
                    }
                    e?.printStackTrace()
                }

            }
        )
    }

    private fun establishConnection(
        applicationContext: Context,
        isMaster: Boolean,
        eventListener: EventListener
    ) {
        CoroutineScope(
            Dispatchers.IO + SupervisorJob()
        ).launch {
            Talker.applicationContext = applicationContext
            Talker.isMaster = isMaster
            mCreds = TalkerSDKApplication().auth.credentials
            audioManager = applicationContext.getSystemService(AudioManager::class.java)
            val sharedPreference = SharedPreference(applicationContext)
            mClientId = sharedPreference.getUserData().user_auth_token
                .split("|")
                .getOrNull(2) ?: ""
            if (!isUserLoggedIn()) {
                getMainLooper {
                    eventListener.onPeerConnectionStateChange(
                        PeerConnectionState.Failure,
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
            broadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val action = intent?.extras?.getString("action")
                    val message = intent?.extras?.getString("message")
                    when (action) {
                        "CONNECTION_SUCCESSFUL" -> {
                            getMainLooper {
                                SocketHandler.setSocket(sharedPreference.getUserData().user_auth_token)
                                SocketHandler.establishConnection(applicationContext)
                                eventListener.onPeerConnectionStateChange(
                                    PeerConnectionState.Success,
                                    "Peer connected successfully"
                                )
                            }
                        }

                        "CONNECTION_FAILURE" -> {
                            getMainLooper {
                                eventListener.onPeerConnectionStateChange(
                                    PeerConnectionState.Failure,
                                    message ?: "Error"
                                )
                                hasStartedTalking = false
                                closeConnection()
                                applicationContext.unregisterReceiver(broadcastReceiver)
                            }
                        }

                        "CONNECTION_CLOSED" -> {
                            getMainLooper {
                                eventListener.onPeerConnectionStateChange(
                                    PeerConnectionState.Closed,
                                    "Peer connection closed"
                                )
                                hasStartedTalking = false
                                applicationContext.unregisterReceiver(broadcastReceiver)
                            }
                        }

                        "CONNECTING" -> {
                            getMainLooper {
                                eventListener.onAudioStatusChange(AudioStatus.Connecting)
                            }
                        }

                        "BUSY" -> {
                            getMainLooper {
                                eventListener.onAudioStatusChange(AudioStatus.Busy)
                            }
                        }

                        "SENDING" -> {
                            getMainLooper {
                                eventListener.onAudioStatusChange(AudioStatus.Sending)
                            }
                        }

                        "STOPPED" -> {
                            getMainLooper {
                                eventListener.onAudioStatusChange(AudioStatus.Stopped)
                            }
                        }
                    }
                }
            }

            applicationContext.registerReceiver(
                broadcastReceiver,
                IntentFilter(
                    "com.talker.sdk"
                ), Context.RECEIVER_EXPORTED
            )
            sdkCredAPI(
                applicationContext, onSuccess = { res ->
                    mRegion =
                        res.data.cognito.CredentialsProvider.CognitoIdentity.Default.Region
                    mChannelName = res.data.webrtc_channel_name
                    mChannelId = res.data.general_channel.channel_id
                    if (updateSignalingChannelInfo(
                            mRegion,
                            mChannelName,
                            if (isMaster) ChannelRole.MASTER else ChannelRole.VIEWER
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
                        val masterEndpoint =
                            (mWssEndpoint + "?" + Constants.CHANNEL_ARN_QUERY_PARAM) + "=" + mChannelArn
                        val viewerEndpoint =
                            ((mWssEndpoint + "?" + Constants.CHANNEL_ARN_QUERY_PARAM) + "=" + mChannelArn + "&" + Constants.CLIENT_ID_QUERY_PARAM) + "=" + mClientId
                        if (TalkerGlobalVariables.printLogs) {
                            Log.d(
                                LOG_TAG,
                                "end point created : ${if (isMaster) masterEndpoint else viewerEndpoint}"
                            )
                        }


                        // this function will generate the signed uri for the signaling service.
                        // this is done to basically authenticate the user to the signaling service.
                        val signedUri = getSignedUri(
                            if (isMaster) masterEndpoint else viewerEndpoint,
                            mCreds,
                            applicationContext,
                            mWssEndpoint,
                            mRegion
                        )

                        //check if the signedUri is not null.
                        checkNotNull(signedUri)


                        //if the user is joining as a master then local peer connection will be established first bcoz their will be no localpeerconnection established.
                        // if we are joining as a viewer than we will assume that their is already an master who has created this and is waiting for viewers to joing.
                        if (isMaster) {
                            createLocalPeerConnection = CreateLocalPeerConnection(
                                peerIceServers,
                                peerConnectionFactory!!,
                                onLocalPeer = {
                                    localPeer = it
                                },
                                applicationContext,
                                client,
//                printStatsExecutor,
                                CHANNEL_ID,
                                mNotificationId = {
                                    mNotificationId++
                                },
                                isMaster,
                                mClientId,
                                recipientClientId,
                                onRemoteAudioTrack = {
                                    remoteAudioTrack = it
                                    remoteAudioTrack?.setEnabled(true)
                                    audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
                                    audioManager?.isSpeakerphoneOn = true
                                },
                                audioManager,
                                mChannelName,
                                onDataChannel = { dataChannel2: DataChannel? ->
                                    dataChannel = dataChannel2
                                }
                            )
                        }

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
                                    isMaster,
                                    recipientClientId,
                                    client,
                                    peerConnectionFoundMap,
                                    pendingIceCandidatesMap
                                )

                                // notify the user that media has started recording to the stream
                                if (isMaster && webrtcEndpoint != null) {
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
                                        LOG_TAG, "Received error in signaling client : ${event}"
                                    )
                                    return
                                }
                                sendBroadCast(
                                    "CONNECTION_FAILURE",
                                    "Received error in signaling client : ${event}"
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
                                    "Signaling client returned exception: " + e.message
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
                            if (!client!!.isOpen) {
                                sendBroadCast(
                                    "CONNECTION_FAILURE",
                                    "Client connection failed"
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
                                "Exception with websocket client: $e"
                            )
                        }

                        if (client?.isOpen == true) {
                            if (TalkerGlobalVariables.printLogs) {
                                Log.d(
                                    LOG_TAG,
                                    "Client connected to Signaling service " + client!!.isOpen
                                )
                            }
                            if (isMaster) {
                                // If webrtc endpoint is non-null ==> Ingest media was checked
                                // this code is used to establish the user's storage session.
                                // this will be usefully only if the user wants to send messages to the other user.
                                if (webrtcEndpoint != null) {
                                    Thread {
                                        try {
                                            val storageClient: AWSKinesisVideoWebRTCStorageClient =
                                                AWSKinesisVideoWebRTCStorageClient(
                                                    KinesisTalkerApp.credentialsProvider.credentials
                                                )
                                            storageClient.setRegion(Region.getRegion(mRegion))
                                            storageClient.signerRegionOverride = mRegion
                                            storageClient.setServiceNameIntern("kinesisvideo")
                                            storageClient.endpoint = webrtcEndpoint
                                            storageClient.joinStorageSession(
                                                JoinStorageSessionRequest().withChannelArn(
                                                    mChannelArn
                                                )
                                            )
                                            if (TalkerGlobalVariables.printLogs) {
                                                Log.i(
                                                    LOG_TAG, "Join storage session request sent!"
                                                )
                                            }
                                        } catch (ex: java.lang.Exception) {
                                            if (TalkerGlobalVariables.printLogs) {
                                                Log.e(
                                                    LOG_TAG,
                                                    "Error sending join storage session request!",
                                                    ex
                                                )
                                            }
                                            sendBroadCast(
                                                "CONNECTION_FAILURE",
                                                "Error sending join storage session request! $ex"
                                            )
                                        }
                                    }.start()
                                }
                            } else {
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
                                    isMaster,
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
                            }
                        } else {
                            if (TalkerGlobalVariables.printLogs) {
                                Log.e(
                                    LOG_TAG, "Error in connecting to signaling service"
                                )
                            }
                            sendBroadCast(
                                "CONNECTION_FAILURE",
                                "Error in connecting to signaling service"
                            )
                        }
                    } else {
                        sendBroadCast(
                            "CONNECTION_FAILURE",
                            "Connection failed"
                        )
                        return@sdkCredAPI
                    }
                },
                onError = { error ->
                    Log.d(
                        LOG_TAG,
                        "Error : $error"
                    )
                    sendBroadCast(
                        "CONNECTION_FAILURE",
                        error.message
                    )
                    return@sdkCredAPI
                },
                onInternetNotAvailable = {
                    sendBroadCast(
                        "CONNECTION_FAILURE",
                        "Network not available"
                    )
                    return@sdkCredAPI
                },
                sdkKey = sdkKey
            )
        }
    }

    private fun getMainLooper(execute: () -> Unit) {
        Handler(Looper.getMainLooper()).post {
            execute()
        }
    }

    private fun sendBroadCast(
        action: String = "",
        message: String = ""
    ) {
        applicationContext?.sendBroadcast(
            Intent()
                .setPackage(applicationContext!!.packageName)
                .setAction("com.talker.sdk")
                .apply {
                    putExtra("action", action)
                    putExtra("message", message)
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
        setupAudioRecorder()
        isRecording = false
        audioRecord.stop()
        audioRecord.release()
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
    private fun toggleLocalAudioTrack(
        allowOthersToSpeak: Boolean,
        isChannelAvailable: (Boolean) -> Unit = {}
    ) {
        if (allowOthersToSpeak) {
            SocketHandler.broadCastStop(mChannelId)
            localAudioTrack?.setEnabled(false)
            client?.sendPermission(
                Message(
                    "PERMISSION_CHANGED",
                    recipientClientId,
                    if (isMaster) "" else (mClientId ?: ""),
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
                                if (isMaster) "" else (mClientId ?: ""),
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

    private fun getCurrentUserId(context: Context): String {
        val sharedPreference = SharedPreference(context)
        return sharedPreference.getUserData().user_id
    }

    private fun isChannelAvailable(channelId: String): Boolean {
        var isChannelAvailable = false
        SocketHandler.broadCastStart(channelId) { ack ->
            isChannelAvailable = ack.getOrNull(0) == true
        }
        SocketHandler.broadCastStop(channelId)
        return isChannelAvailable
    }

    private fun loginUser(
        context: Context,
        userName: String,
        fcmToken: String,
        onLoginSuccess: (message: String) -> Unit = {},
        onLoginFailure: (errorMessage: String) -> Unit = {}
    ) {
        val sharedPreference = SharedPreference(context)
        if (userName.isNotEmpty()) {
            if (isUserLoggedIn()) {
                getMainLooper {
                    onLoginFailure("User is already logged in")
                }
                return
            }
            sdkSetUser(context, userId = userName, onSuccess = { res ->
                sharedPreference.setUserData(res.data)
                TalkerSDKApplication().auth?.signIn(res.data.a_username,
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
                                onLoginSuccess("Sign in successful")
                            }
                        }

                        override fun onError(e: java.lang.Exception?) {
                            getMainLooper {
                                onLoginFailure("Sign in failed. Error : $e")
                            }
                        }
                    })
            }, onError = { errorData ->
                getMainLooper {
                    onLoginFailure(errorData.message)
                }
            }, onInternetNotAvailable = {
                getMainLooper {
                    onLoginFailure("Network not available")
                }
            }, fcmToken = fcmToken,
                sdkKey = sdkKey
            )
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
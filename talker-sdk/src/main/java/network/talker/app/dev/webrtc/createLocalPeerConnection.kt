package network.talker.app.dev.webrtc

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread
import network.talker.app.dev.TalkerGlobalVariables
import network.talker.app.dev.LOG_TAG
import network.talker.app.dev.R
import network.talker.app.dev.model.Message
import network.talker.app.dev.okhttp.SignalingServiceWebSocketClient
import org.webrtc.AudioTrack
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import java.nio.charset.Charset

internal class CreateLocalPeerConnection(
    peerIceServers: ArrayList<PeerConnection.IceServer>,
    peerConnectionFactory: PeerConnectionFactory,
    onLocalPeer: (PeerConnection?) -> Unit = {},
    applicationContext: Context,
    private val client: SignalingServiceWebSocketClient?,
    CHANNEL_ID: String,
    mNotificationId: () -> Int,
    master: Boolean = true,
    mClientId: String?,
    recipientClientId: String,
    onRemoteAudioTrack: (AudioTrack?) -> Unit = {},
    audioManager: AudioManager?,
    channelName: String,
    onDataChannel: (DataChannel?) -> Unit
) {
    val TAG = "createLocalPeerConnection"
    var remoteAudioTrack: AudioTrack? = null
    val rtcConfig = PeerConnection.RTCConfiguration(peerIceServers)
    var localPeer: PeerConnection? = null

    init {
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        rtcConfig.continualGatheringPolicy =
            PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED

        // creating peer connection and passing the rtc config which specifies the properties of this connection
        // also we pass the listeners as well which will be called on different events
        localPeer = peerConnectionFactory.createPeerConnection(rtcConfig,
            object : KinesisVideoPeerConnection() {

                // called when ice candidate is received.
                override fun onIceCandidate(iceCandidate: IceCandidate) {
                    super.onIceCandidate(iceCandidate)
                    // this code will exchange the ice candidates between both the peers.
                    val message: Message = createIceCandidateMessage(
                        iceCandidate,
                        master,
                        mClientId,
                        recipientClientId
                    )
                    if (TalkerGlobalVariables.printLogs){
                        Log.d(
                            LOG_TAG, "Sending IceCandidate to remote peer"
                        )
                    }
                    client?.sendIceCandidate(
                        message
                    )
                }

                // called when a new stream is added to the peer connection.
                // basically when the other user will attach their local audio source then this will be called.
                override fun onAddStream(mediaStream: MediaStream) {
                    super.onAddStream(mediaStream)
                    // extract the remote user's audio source from the media stream and play it on speaker.
                    // the remote audio track if the other user's audio
                    addRemoteStreamToVideoView(
                        mediaStream,
                        onRemoteAudioTrack = { audioTrack ->
                            remoteAudioTrack = audioTrack
                            onRemoteAudioTrack(remoteAudioTrack)
                        },
                        audioManager
                    )
                }

                // method called when ice connection is changed for example a new user joins or an old one leaves or connection gets lost
                override fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState) {
                    super.onIceConnectionChange(iceConnectionState)
                    // if the connection is failed than some error might have occurred while joining.
                    if (iceConnectionState == PeerConnection.IceConnectionState.FAILED) {
                        runOnUiThread {
                            applicationContext.sendBroadcast(
                                Intent()
                                    .setPackage(applicationContext.packageName)
                                    .setAction("com.talker.sdk")
                                    .apply {
                                        putExtra("action", "CONNECTION_FAILURE")
                                        putExtra("message", "Peer connection failed")
                                    }
                            )
                            return@runOnUiThread
                        }
                    } else if (iceConnectionState == PeerConnection.IceConnectionState.CONNECTED) {
                        // the user has successfully joined the remote peer.
                        runOnUiThread {
                            applicationContext.sendBroadcast(
                                Intent()
                                    .setPackage(applicationContext.packageName)
                                    .setAction("com.talker.sdk")
                                    .apply {
                                        putExtra("action", "CONNECTION_SUCCESSFUL")
                                        putExtra("message", "Peer connection success")
                                    }
                            )
                        }
                    }else {
                        // some other error has occured.
                        runOnUiThread {
                            Log.d(
                                "onIceConnectionChange",
                                "onIceConnectionChange : $iceConnectionState"
                            )
                            return@runOnUiThread
                        }
                    }
                }

                // data channel that will be used when the user needs to send messages as well
                override fun onDataChannel(dataChannel: DataChannel) {
                    super.onDataChannel(dataChannel)
                    dataChannel.registerObserver(object : DataChannel.Observer {
                        override fun onBufferedAmountChange(l: Long) {
                            if (TalkerGlobalVariables.printLogs){
                                Log.d(
                                    LOG_TAG,
                                    "data channel buffered amount changed"
                                )
                            }
                        }

                        override fun onStateChange() {
                            if (TalkerGlobalVariables.printLogs){
                                Log.d(
                                    LOG_TAG,
                                    "data channel state changed"
                                )
                            }
                        }

                        // when a message will be recieved from the remote peer.
                        override fun onMessage(buffer: DataChannel.Buffer) {
                            runOnUiThread {
                                //extracting the message from the remote peer.
                                val bytes: ByteArray
                                if (buffer.data.hasArray()) {
                                    bytes = buffer.data.array()
                                } else {
                                    bytes = ByteArray(buffer.data.remaining())
                                    buffer.data[bytes]
                                }

                                // building the notification and showing the message from the remote user inside this notification
                                val builder: NotificationCompat.Builder =
                                    NotificationCompat.Builder(
                                        applicationContext, CHANNEL_ID
                                    ).setSmallIcon(R.mipmap.ic_launcher).setLargeIcon(
                                        BitmapFactory.decodeResource(
                                            applicationContext.resources, R.mipmap.ic_launcher
                                        )
                                    ).setContentTitle("Message from Peer!").setContentText(
                                        String(
                                            bytes, Charset.defaultCharset()
                                        )
                                    ).setPriority(NotificationCompat.PRIORITY_MAX)
                                        .setAutoCancel(true)
                                val notificationManager =
                                    NotificationManagerCompat.from(applicationContext)

                                if (ActivityCompat.checkSelfPermission(
                                        applicationContext, Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    notificationManager.notify(mNotificationId(), builder.build())
                                }
                            }
                        }
                    })
                }
            }
        )

        addDataChannelToLocalPeer(
            localPeer,
            channelName,
            onDataChannel
        )
        // sending the instance of this local peer so that one could use it in an higher order function
        onLocalPeer(
            localPeer
        )
    }

    // function which will disconnect the client from call.
    fun closeAll() {
        localPeer = null
    }
}
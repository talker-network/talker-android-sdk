package network.talker.app.dev.webrtc

import android.util.Log
import network.talker.app.dev.TalkerGlobalVariables
import network.talker.app.dev.LOG_TAG
import org.webrtc.DataChannel
import org.webrtc.PeerConnection
import java.nio.charset.Charset

internal fun addDataChannelToLocalPeer(
    localPeer: PeerConnection?,
    channelName: String,
    onDataChannel: (DataChannel?) -> Unit
) {
    val localDataChannel = localPeer?.createDataChannel(channelName, DataChannel.Init().apply {
        ordered = false
    })
    Log.d(
        LOG_TAG,
        "Data channel created..."
    )
    // registering observer which will observe events related to this data channels changes.
    localDataChannel?.registerObserver(object : DataChannel.Observer {
        override fun onBufferedAmountChange(l: Long) {
            if (TalkerGlobalVariables.printLogs){
                Log.d(
                    LOG_TAG, "Local Data Channel onBufferedAmountChange called with amount $l"
                )
            }
        }

        override fun onStateChange() {
            if (TalkerGlobalVariables.printLogs){
                Log.d(
                    LOG_TAG,
                    "Local Data Channel onStateChange: state: " + localDataChannel.state()
                        .toString()
                )
            }
        }

        override fun onMessage(buffer: DataChannel.Buffer) {
            // Send out data, no op on sender side
            if (TalkerGlobalVariables.printLogs){
                if (buffer.binary) {
                    // Handle binary data
                    val data = ByteArray(buffer.data.remaining())
                    buffer.data.get(data)
                    Log.d(
                        LOG_TAG, "onMessage() : $data"
                    )
                } else {
                    // Handle text data
                    val data = ByteArray(buffer.data.remaining())
                    buffer.data.get(data)
                    val message = String(data, Charset.forName("UTF-8"))
                    Log.d(
                        LOG_TAG, "onMessage() : $message"
                    )
                }


            }
        }
    })

    onDataChannel(localDataChannel)
}
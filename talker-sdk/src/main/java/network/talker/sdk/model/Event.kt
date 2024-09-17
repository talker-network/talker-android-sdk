package network.talker.sdk.model

import android.util.Base64
import android.util.Base64.NO_WRAP
import android.util.Base64.URL_SAFE
import android.util.Log
import network.talker.sdk.LOG_TAG
import network.talker.sdk.TalkerGlobalVariables
import com.google.common.base.Charsets
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.serialization.Serializable
import org.webrtc.IceCandidate
import java.util.Optional

@Serializable
internal data class SDPClass(
    val type : String,
    val sdp : String
)

@Serializable
internal data class CandidateClass(
    val candidate: String,
    val sdpMid: String,
    val sdpMLineIndex: Int,
    val foundation: String,
    val component: String,
    val priority: Long,
    val address: String,
    val protocol: String,
    val port: Int,
    val type: String,
    val tcpType: String? = null,val relatedAddress: String? = null,
    val relatedPort: Int? = null,
    val usernameFragment: String
)

internal class Event(val senderClientId: String, val messageType: String, val messagePayload: String) {
    var statusCode: String? = null

    var body: String? = null

    override fun toString(): String {
        return "Event(" +
                "senderClientId='" + senderClientId + '\'' +
                ", messageType='" + messageType + '\'' +
                ", messagePayload='" + messagePayload + '\'' +
                ", statusCode='" + statusCode + '\'' +
                ", body='" + body + '\'' +
                ')'
    }

    companion object {
        private const val TAG = "Event"
        fun parseIceCandidate(event: Event?): IceCandidate? {
            if (event == null || !"ICE_CANDIDATE".equals(event.messageType, ignoreCase = true)) {
                Log.e(LOG_TAG, event.toString() + " is not an ICE_CANDIDATE type!")
                return null
            }
            val decode = Base64.decode(event.messagePayload,URL_SAFE or NO_WRAP)
            val candidateString = String(decode, Charsets.UTF_8).removeSurrounding("\"", "").replace("\\\"", "\"").dropLast(1)
//            val candidateString = String(decode, Charsets.UTF_8)
//            val candidateString = String(decode, Charsets.UTF_8)
            if (candidateString == "null") {
                Log.w(TAG, "Received null IceCandidate!")
                return null
            }
            if (TalkerGlobalVariables.printLogs){
                Log.d(
                    LOG_TAG,
                    "Received IceCandidate: $candidateString"
                )
            }
            val candidateClass = JsonParser.parseString(candidateString).asJsonObject
            val sdpMid = Optional.ofNullable(candidateClass["sdpMid"])
                .map { obj: JsonElement -> obj.toString() } // Remove quotes
                .map { sdpMidStr: String ->
                    if (sdpMidStr.length > 2) sdpMidStr.substring(
                        1,
                        sdpMidStr.length - 1
                    ) else sdpMidStr
                }
                .orElse("")
            var sdpMLineIndex = -1
            try {
                sdpMLineIndex = candidateClass["sdpMLineIndex"].toString().toInt()
            } catch (e: NumberFormatException) {
                Log.e(LOG_TAG, "Invalid sdpMLineIndex")
            }
            // Ice Candidate needs one of these two to be present
            if (sdpMid.isEmpty() && sdpMLineIndex == -1) {
                return null
            }
            val candidate = Optional.ofNullable(candidateClass["candidate"])
                .map { obj: JsonElement -> obj.toString() } // Remove quotes
                .map { candidateStr: String ->
                    if (candidateStr.length > 2) candidateStr.substring(
                        1,
                        candidateStr.length - 1
                    ) else candidateStr
                }
                .orElse("")
            return IceCandidate(sdpMid, if (sdpMLineIndex == -1) 0 else sdpMLineIndex, candidate)
        }

        fun parseSdpEvent(answerEvent: Event): String {
            val message = String(
                Base64.decode(
                    answerEvent.messagePayload, Base64.URL_SAFE or Base64.NO_WRAP
                )
            ).removeSurrounding("\"", "").replace("\\\"", "\"")
                .replace(
                    "\\\\r\\\\n", "\r\n"
                ).dropLast(1)

            Log.d(
                LOG_TAG,
                "answerEvent : $message"
            )
            val jsonObject = JsonParser.parseString(
                message
            ).asJsonObject
            Log.d(LOG_TAG, "Message : $jsonObject")
            val type = jsonObject["type"].toString()
            if (!type.equals("\"answer\"", ignoreCase = true)) {
                Log.d(LOG_TAG, "Error in answer message")
            }
            return jsonObject["sdp"].asString
        }

        fun parseOfferEvent(offerEvent: Event): String {
            val s = String(Base64.decode(offerEvent.messagePayload, URL_SAFE or NO_WRAP))

            return Optional.of(JsonParser.parseString(s))
                .filter { obj: JsonElement -> obj.isJsonObject }
                .map { obj: JsonElement -> obj.asJsonObject }
                .map { jsonObject: JsonObject -> jsonObject["sdp"] }
                .map { obj: JsonElement -> obj.asString }
                .orElse("")
        }
    }
}

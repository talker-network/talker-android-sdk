package network.talker.app.dev.networking.data

data class ExitChannelRequest(
    val channel_id : String = ""
)

data class ExitChannelResponse(
    val data: ExitChannelResponseData = ExitChannelResponseData(),
    val message: String = "",
    val success: Boolean = false
)

data class ExitChannelResponseData(
    val error_code: Int = 0
)
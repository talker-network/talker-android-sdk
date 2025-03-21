package network.talker.sdk.networking.apiRoutes

internal object ApiRoutes {
//    const val SDK_KEY = "39bd68c5-b3ab-41f7-abc4-fecda1650814"
    const val BASE_URL = "https://api.talker.network/"
    private const val CENTER_PART = "sdk"
    const val CREATE_USER = "$CENTER_PART/create_user"
    const val SET_USER = "$CENTER_PART/set_user"
    const val  UPDATE_APNS_PTT = "$CENTER_PART/update_apns_ptt"
    const val  CRED = "$CENTER_PART/cred"
    const val  GET_ALL_USERS = "/sdk/users"
    const val  GET_ALL_CHANNELS = "/chat/channels"
    const val  CREATE_CHANNEL = "/chat/channel"
    const val  EDIT_CHANNEL = "chat/channel"
    const val  EXIT_CHANNEL = "exit_room"
    const val  UPLOAD_MESSAGE = "/chat/upload"
}
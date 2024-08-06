package network.talker.app.dev.networking.apiRoutes

internal object ApiRoutes {
//    const val SDK_KEY = "39bd68c5-b3ab-41f7-abc4-fecda1650814"
    const val BASE_URL = "https://test-api.talker.network/"
    private const val CENTER_PART = "sdk"
    const val CREATE_USER = "$CENTER_PART/create_user"
    const val SET_USER = "$CENTER_PART/set_user"
    const val  UPDATE_APNS_PTT = "$CENTER_PART/update_apns_ptt"
    const val  CRED = "$CENTER_PART/cred"
}
package network.talker.sdk.networking.data

internal data class CredModel(
    val data: CredModelData = CredModelData(),
    val success: Boolean = false
)

internal data class CredModelData(
    val cognito: Cognito = Cognito(),
    val general_channel: GeneralChannel = GeneralChannel(),
    val webrtc_channel_name: String = ""
)

internal data class Cognito(
    val CognitoUserPool: CognitoUserPool = CognitoUserPool(),
    val CredentialsProvider: CredentialsProvider = CredentialsProvider(),
    val IdentityManager: IdentityManager = IdentityManager(),
    val Version: String = ""
)

internal data class GeneralChannel(
    val channel_id: String = "",
    val channel_name: String = ""
)

internal data class CognitoUserPool(
    val Default: Default = Default()
)

internal data class CredentialsProvider(
    val CognitoIdentity: CognitoIdentity = CognitoIdentity()
)

internal data class IdentityManager(
    val Default: DefaultXX = DefaultXX()
)

internal data class Default(
    val AppClientId: String = "",
    val AppClientSecret: String = "",
    val PoolId: String = "",
    val Region: String = ""
)

internal data class CognitoIdentity(
    val Default: DefaultX = DefaultX()
)

internal data class DefaultX(
    val PoolId: String = "",
    val Region: String = ""
)

internal class DefaultXX

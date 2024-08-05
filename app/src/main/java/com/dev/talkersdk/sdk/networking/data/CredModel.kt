package com.dev.talkersdk.sdk.networking.data

data class CredModel(
    val data: CredModelData = CredModelData(),
    val success: Boolean = false
)

data class CredModelData(
    val cognito: Cognito = Cognito(),
    val general_channel: GeneralChannel = GeneralChannel(),
    val webrtc_channel_name: String = ""
)

data class Cognito(
    val CognitoUserPool: CognitoUserPool = CognitoUserPool(),
    val CredentialsProvider: CredentialsProvider = CredentialsProvider(),
    val IdentityManager: IdentityManager = IdentityManager(),
    val Version: String = ""
)

data class GeneralChannel(
    val channel_id: String = "",
    val channel_name: String = ""
)

data class CognitoUserPool(
    val Default: Default = Default()
)

data class CredentialsProvider(
    val CognitoIdentity: CognitoIdentity = CognitoIdentity()
)

data class IdentityManager(
    val Default: DefaultXX = DefaultXX()
)

data class Default(
    val AppClientId: String = "",
    val AppClientSecret: String = "",
    val PoolId: String = "",
    val Region: String = ""
)

data class CognitoIdentity(
    val Default: DefaultX = DefaultX()
)

data class DefaultX(
    val PoolId: String = "",
    val Region: String = ""
)

class DefaultXX

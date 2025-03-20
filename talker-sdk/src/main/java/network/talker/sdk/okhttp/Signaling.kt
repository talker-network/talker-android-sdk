package network.talker.sdk.okhttp

import network.talker.sdk.model.Event


internal interface Signaling {
    fun onSdpOffer(event: Event)

    fun onSdpAnswer(event: Event)

    fun onIceCandidate(event: Event)

    fun onError(event: Event)

    fun onException(e: Exception)

    fun onPermissionChanged(event: Event)
}

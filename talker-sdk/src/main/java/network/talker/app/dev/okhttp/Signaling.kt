package network.talker.app.dev.okhttp

import network.talker.app.dev.model.Event


internal interface Signaling {
    fun onSdpOffer(event: Event)

    fun onSdpAnswer(event: Event)

    fun onIceCandidate(event: Event)

    fun onError(event: Event)

    fun onException(e: Exception)

    fun onPermissionChanged(event: Event)
}

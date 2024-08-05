package com.dev.talkersdk.sdk.okhttp

import com.dev.talkersdk.sdk.model.Event


interface Signaling {
    fun onSdpOffer(event: Event)

    fun onSdpAnswer(event: Event)

    fun onIceCandidate(event: Event)

    fun onError(event: Event)

    fun onException(e: Exception)

    fun onPermissionChanged(event: Event)
}

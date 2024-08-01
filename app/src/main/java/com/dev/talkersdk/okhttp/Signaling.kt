package com.dev.talkersdk.okhttp

import com.dev.talkersdk.model.Event


interface Signaling {
    fun onSdpOffer(event: Event)

    fun onSdpAnswer(event: Event)

    fun onIceCandidate(event: Event)

    fun onError(event: Event)

    fun onException(e: Exception)

    fun onPermissionChanged(event: Event)
}

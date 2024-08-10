package com.talkersdk.sample

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import network.talker.app.dev.Talker
import network.talker.app.dev.webrtc.AudioStatus
import network.talker.app.dev.webrtc.ServerConnectionState

@Composable
fun SingleButtonSample(fcmToken: String) {
    val context = LocalContext.current
    var isLoading by remember {
        mutableStateOf(false)
    }
    var status by remember {
        mutableStateOf("")
    }
    var showPushToTalkButton by rememberSaveable {
        mutableStateOf(false)
    }

    // event listener which will listen to changes like registration changes,
    // peer connectionChanges and AudioStatus changes.
//    val eventListener = object : EventListener {
//        override fun onRegistrationStateChange(
//            registrationState: RegistrationState,
//            message: String
//        ) {
//            Toast.makeText(context, registrationState.name, Toast.LENGTH_SHORT).show()
//            Log.d(
//                "alkdlskd",
//                "registrationState : ${registrationState.name} message : $message"
//            )
//        }

//        override fun onPeerConnectionStateChange(
//            peerConnectionState: PeerConnectionState,
//            message: String
//        ) {
//            isLoading = false
//            Toast.makeText(context, peerConnectionState.name, Toast.LENGTH_SHORT).show()
//            Log.d(
//                "Talker SDK",
//                "peerConnectionState : ${peerConnectionState.name} ${Talker.getCurrentUserId(context)}"
//            )
//            showPushToTalkButton = peerConnectionState == PeerConnectionState.Success
//        }
//
//        override fun onAudioStatusChange(audioStatus: AudioStatus) {
//            status = audioStatus.name
//            Log.d(
//                "Talker SDK",
//                "audioStatus : ${audioStatus.name}"
//            )
//        }
//    }

    // get event for changing of audio status
    Talker.eventListener.onAudioStatusChange = { audioStatus: AudioStatus ->
        status = audioStatus.name
        Log.d(
            "Talker SDK",
            "audioStatus : ${audioStatus.name}"
        )
    }
    // get event for changing of peer connection state
    Talker.eventListener.onServerConnectionChange = { serverConnectionState: ServerConnectionState, message: String ->
        isLoading = false
        Toast.makeText(context, serverConnectionState.name, Toast.LENGTH_SHORT).show()
        Log.d(
            "Talker SDK",
            "peerConnectionState : ${serverConnectionState.name} ${Talker.getCurrentUserId(context)} $message"
        )
        when(serverConnectionState) {
            ServerConnectionState.Success -> {
                showPushToTalkButton = true
            }
            ServerConnectionState.Failure -> {

            }
            ServerConnectionState.Closed -> {
                showPushToTalkButton = false
            }
        }
    }


    // show loader if some process is executing..
    if (isLoading) {
        Box(modifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures {

                }
            }
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // if peer connection is not successful means that either it is failed or it is disconnected
                // in such case show create user button
                if (!showPushToTalkButton) {
                    if (Talker.getCurrentUserId(context).isEmpty()) {
                        var name by remember {
                            mutableStateOf("")
                        }
                        OutlinedTextField(value = name, onValueChange = { name = it })
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(onClick = {
                            // check if name is not empty
                            if (name.isNotEmpty()) {
                                status = ""
                                // start showing loader
                                isLoading = true
                                // create user and connect to peer and pass the listener and fcm token
                                Talker.createUser(
                                    context,
                                    name,
                                    fcmToken,
//                                    eventListener,
                                    onSuccess = {
                                        Toast.makeText(context, "Success", Toast.LENGTH_SHORT).show()
                                        Log.d(
                                            "Talker SDK",
                                            "registrationState : Success} message : $it"
                                        )
                                    },
                                    onFailure = {
                                        showPushToTalkButton = false
                                        isLoading = false
                                        Toast.makeText(context, "Failure", Toast.LENGTH_SHORT).show()
                                        Log.d(
                                            "Talker SDK",
                                            "registrationState : Failure message : $it"
                                        )
                                    }
                                )
                            }
                        }) {
                            Text(text = "Create")
                        }
                    } else {
                        var userName by remember {
                            mutableStateOf(Talker.getCurrentUserId(context))
                        }
                        OutlinedTextField(value = userName, onValueChange = { userName = it })
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(onClick = {
                            // check if name is not empty
                            if (userName.isNotEmpty()) {
                                status = ""
                                // start showing loader
                                isLoading = true
                                // create user and connect to peer and pass the listener and fcm token
                                Talker.sdkSetUser(
                                    context,
                                    userName,
                                    fcmToken,
//                                    eventListener,
                                    onSuccess = {
                                        Toast.makeText(context, "Success", Toast.LENGTH_SHORT).show()
                                        Log.d(
                                            "Talker SDK",
                                            "registrationState : Success} message : $it"
                                        )
                                    },
                                    onFailure = {
                                        showPushToTalkButton = false
                                        isLoading = false
                                        Toast.makeText(context, "Failure", Toast.LENGTH_SHORT).show()
                                        Log.d(
                                            "Talker SDK",
                                            "registrationState : Failure message : $it"
                                        )
                                    }
                                )
                            }
                        }) {
                            Text(text = "Login")
                        }
                    }
                } else {
                    val interactionSource = remember {
                        MutableInteractionSource()
                    }
                    LaunchedEffect(key1 = interactionSource) {
                        interactionSource.interactions.collect() {
                            when (it) {
                                is PressInteraction.Press -> {
                                    // when user presses the button call this function to start sharing audio
                                    // it will return boolean which will inform user if the channel is available for
                                    // sending audio data or not
                                    Talker.startPttAudio { isChannelAvailable ->
                                        if (!isChannelAvailable) {
                                            Toast.makeText(
                                                context,
                                                "The other person is talking...",
                                                Toast.LENGTH_SHORT
                                            )
                                                .show()
                                        }
                                    }
                                }

                                // stop sending data when user cancels or releases the button
                                is PressInteraction.Cancel -> {
                                    Talker.stopPttAudio()
                                }

                                is PressInteraction.Release -> {
                                    Talker.stopPttAudio()
                                }
                            }
                        }
                    }

                    Text(text = "Audio Status : $status")

                    Spacer(modifier = Modifier.height(50.dp))

                    Button(
                        onClick = {},
                        modifier = Modifier.padding(16.dp),
                        interactionSource = interactionSource // pass the interaction source so that we can get to know if user has pressed the button or not.
                    ) {
                        Text(text = "Push to talk")
                    }
                    Spacer(modifier = Modifier.height(50.dp))
                    Button(
                        onClick = {
                            isLoading = true
                            // close the connection and logout the user.
                            // also don't forget to call this method in onDestroy() or current activity.
                            // it will prevent data leaks
                            Talker.closeConnection()
                        },
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(text = "Stop / Logout")
                    }
                }
            }
        }
    }
}
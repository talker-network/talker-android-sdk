package com.talkersdk.sample

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import network.talker.app.dev.webrtc.AudioStatus
import network.talker.app.dev.webrtc.EventListener
import network.talker.app.dev.webrtc.PeerConnectionState
import network.talker.app.dev.webrtc.RegistrationState
import network.talker.app.dev.webrtc.Talker

@Composable
fun DoubleButtonSample(fcmToken: String) {
    val context = LocalContext.current
    var hasStartedSpeaking by remember {
        mutableStateOf(false)
    }
    var isLoading by remember {
        mutableStateOf(false)
    }
    var status by remember {
        mutableStateOf("")
    }
    var showPushToTalkButton  by rememberSaveable {
        mutableStateOf(false)
    }
    val eventListener = object : EventListener {
        override fun onRegistrationStateChange(
            registrationState: RegistrationState,
            message: String
        ) {
            Toast.makeText(context, registrationState.name, Toast.LENGTH_SHORT).show()
            Log.d(
                "alkdlskd",
                "registrationState : ${registrationState.name}"
            )
        }

        override fun onPeerConnectionStateChange(
            peerConnectionState: PeerConnectionState,
            message: String
        ) {
            isLoading = false
            hasStartedSpeaking = false
            Toast.makeText(context, peerConnectionState.name, Toast.LENGTH_SHORT).show()
            Log.d(
                "alkdlskd",
                "peerConnectionState : ${peerConnectionState.name}"
            )
            showPushToTalkButton = peerConnectionState == PeerConnectionState.Success
        }

        override fun onAudioStatusChange(audioStatus: AudioStatus) {
            status = audioStatus.name
            Log.d(
                "alkdlskd",
                "audioStatus : ${audioStatus.name}"
            )
        }
    }

    if (isLoading) {
        Box(modifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures { }
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
                if (!showPushToTalkButton) {
                    var name by remember {
                        mutableStateOf("")
                    }
                    OutlinedTextField(value = name, onValueChange = { name = it })
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = {
                        if (name.isNotEmpty()) {
                            status = ""
                            isLoading = true
                            Talker.createUser(
                                context,
                                name,
                                fcmToken,
                                eventListener
                            )
                        }
                    }) {
                        Text(text = "Create")
                    }
                } else {

                    Text(text = "Audio Status : $status")

                    Spacer(modifier = Modifier.height(50.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (!hasStartedSpeaking) {
                                    Talker.startPttAudio { isChannelAvailable ->
                                        if (!isChannelAvailable) {
                                            Toast.makeText(
                                                context,
                                                "The other person is talking...",
                                                Toast.LENGTH_SHORT
                                            )
                                                .show()
                                        }else{
                                            hasStartedSpeaking = true
                                        }
                                    }
                                }

                            },
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(text = "Start")
                        }
                        Spacer(modifier = Modifier.width(30.dp))
                        Button(
                            onClick = {
                                if (hasStartedSpeaking) {
                                    hasStartedSpeaking = false
                                    Talker.stopPttAudio()
                                }
                            },
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(text = "Stop")
                        }
                    }
                    Spacer(modifier = Modifier.height(50.dp))

                    Button(
                        onClick = {
                            isLoading = true
                            hasStartedSpeaking = false
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
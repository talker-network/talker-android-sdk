package com.talkersdk.sample

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import network.talker.app.dev.webrtc.AudioStatus
import network.talker.app.dev.webrtc.ServerConnectionState
import network.talker.app.dev.Talker
import network.talker.app.dev.networking.data.Channel
import network.talker.app.dev.networking.data.GetAllUserModelData

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
//    val eventListener = object : EventListener {
//        override fun onRegistrationStateChange(
//            registrationState: RegistrationState,
//            message: String
//        ) {
//            Toast.makeText(context, registrationState.name, Toast.LENGTH_SHORT).show()
//            Log.d(
//                "alkdlskd",
//                "registrationState : ${registrationState.name}"
//            )
//        }

//        override fun onPeerConnectionStateChange(
//            peerConnectionState: PeerConnectionState,
//            message: String
//        ) {
//            isLoading = false
//            hasStartedSpeaking = false
//            Toast.makeText(context, peerConnectionState.name, Toast.LENGTH_SHORT).show()
//            Log.d(
//                "alkdlskd",
//                "peerConnectionState : ${peerConnectionState.name}"
//            )
//            showPushToTalkButton = peerConnectionState == PeerConnectionState.Success
//        }
//
//        override fun onAudioStatusChange(audioStatus: AudioStatus) {
//            status = audioStatus.name
//            Log.d(
//                "alkdlskd",
//                "audioStatus : ${audioStatus.name}"
//            )
//        }
//    }



    Talker.eventListener.onAudioStatusChange = { audioStatus: AudioStatus ->
        status = audioStatus.name
        Log.d(
            "Talker SDK",
            "audioStatus : ${audioStatus.name}"
        )
    }
    Talker.eventListener.onServerConnectionChange = { serverConnectionState: ServerConnectionState, message: String ->
        isLoading = false
        Toast.makeText(context, serverConnectionState.name, Toast.LENGTH_SHORT).show()
        Log.d(
            "Talker SDK",
            "peerConnectionState : ${serverConnectionState.name} ${Talker.getCurrentUserId(context)}"
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
                    if (Talker.getCurrentUserId(context).isEmpty()){
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
                    }else{
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
                                        Toast.makeText(context, "Failure", Toast.LENGTH_SHORT).show()
                                        showPushToTalkButton = false
                                        isLoading = false
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
                    var isDropDownExpanded by rememberSaveable {
                        mutableStateOf(false)
                    }
                    var selectedChannel by rememberSaveable {
                        mutableStateOf(Channel())
                    }
                    var channels by remember {
                        mutableStateOf(emptyList<Channel>())
                    }
                    var users by remember {
                        mutableStateOf(emptyList<GetAllUserModelData>())
                    }
                    LaunchedEffect(key1 = Unit) {
                        Talker.getChannelList(
                            onChannelFetched = {
                                channels = it
                                selectedChannel = channels.getOrNull(0) ?: Channel()
                            }
                        )
                    }
                    LaunchedEffect(key1 = Unit) {
                        Talker.getAllUsers(
                            onUsersFetched = {
                                users = it
                            }
                        )
                    }
                    Text(text = "Audio Status : $status")

                    Spacer(modifier = Modifier.height(50.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (!hasStartedSpeaking) {
                                    Talker.startPttAudio(selectedChannel.channel_id) { isChannelAvailable ->
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
                    Spacer(modifier = Modifier.height(50.dp))
                    Box(
                        modifier = Modifier.wrapContentSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(onClick = {
                            isDropDownExpanded = true
                        }) {
                            Text(text = selectedChannel.group_name)
                        }
                        DropdownMenu(
                            expanded = isDropDownExpanded,
                            onDismissRequest = {
                                isDropDownExpanded = !isDropDownExpanded
                            }
                        ) {
                            channels.forEach {
                                DropdownMenuItem(text = {
                                    Text(
                                        text = it.group_name,
                                        color = if (selectedChannel.channel_id == it.channel_id) Color.Cyan else Color.White
                                    )
                                }, onClick = {
                                    isDropDownExpanded = false
                                    selectedChannel = it
                                })
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(50.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(15.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        users.forEach {
                            Button(onClick = { }) {
                                Text(text = it.name)
                            }
                        }
                    }
                }
            }
        }
    }
}
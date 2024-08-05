package com.dev.talkersdk.sample

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import com.dev.talkersdk.sdk.networking.apiRoutes.ApiRoutes
import com.dev.talkersdk.sdk.webrtc.Talker
import com.dev.talkersdk.sdk.sharedPreference.SharedPreference
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {
    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS),
                9379
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.Black.copy(0.3f).toArgb()
        var fcmToken = ""
        FirebaseMessaging.getInstance().token.addOnCompleteListener {
            Log.d(
                "FCM",
                "FCM Token : ${it.result}"
            )
            fcmToken = it.result
        }

        // initializing Talker...
        Talker.init(ApiRoutes.SDK_KEY)

        setContent {
            val sharedPreference = SharedPreference(this)
            var isLoading by remember {
                mutableStateOf(false)
            }
            var status by remember {
                mutableStateOf("")
            }
            LaunchedEffect(key1 = Unit) {
                Log.i(
                    "User Data : ",
                    sharedPreference.getUserData().toString()
                )
            }
            val eventListener = object : com.dev.talkersdk.sdk.webrtc.EventListener {
                override fun onRegistrationStateChange(
                    registrationState: com.dev.talkersdk.sdk.webrtc.RegistrationState,
                    message: String
                ) {
                    Toast.makeText(applicationContext, registrationState.name, Toast.LENGTH_SHORT).show()
                    Log.d(
                        "alkdlskd",
                        "registrationState : ${registrationState.name}"
                    )
                }

                override fun onPeerConnectionStateChange(
                    peerConnectionState: com.dev.talkersdk.sdk.webrtc.PeerConnectionState,
                    message: String
                ) {
                    isLoading = false
                    Toast.makeText(applicationContext, peerConnectionState.name, Toast.LENGTH_SHORT).show()
                    Log.d(
                        "alkdlskd",
                        "peerConnectionState : ${peerConnectionState.name}"
                    )
                }

                override fun onAudioStatusChange(audioStatus: com.dev.talkersdk.sdk.webrtc.AudioStatus) {
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
                    .fillMaxSize(),
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
                        if (!Talker.isUserLoggedIn()) {
                            var userId by remember {
                                mutableStateOf("")
                            }
                            Spacer(modifier = Modifier.height(20.dp))

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
                                        applicationContext,
                                        name,
                                        fcmToken,
                                        eventListener
                                    )
                                }
                            }) {
                                Text(text = "Create")
                            }
                        } else {
                            val interactionSource = remember {
                                MutableInteractionSource()
                            }
                            LaunchedEffect(key1 = interactionSource) {
                                interactionSource.interactions.collect() {
                                    when (it) {
                                        is PressInteraction.Press -> {
                                            Talker.startPttAudio { isChannelAvailable ->
                                                if (!isChannelAvailable) {
                                                    Toast.makeText(
                                                        applicationContext,
                                                        "The other person is talking...",
                                                        Toast.LENGTH_SHORT
                                                    )
                                                        .show()
                                                }
                                            }
                                        }

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
                                interactionSource = interactionSource
                            ) {
                                Text(text = "Push to talk")
                            }
                            Spacer(modifier = Modifier.height(50.dp))

                            Button(
                                onClick = {
                                    isLoading = true
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
    }



    override fun onDestroy() {
        super.onDestroy()
        Talker.closeConnection()
    }
}

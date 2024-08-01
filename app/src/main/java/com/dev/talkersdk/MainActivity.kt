package com.dev.talkersdk

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
import com.dev.talkersdk.sharedPreference.SharedPreference
import com.dev.talkersdk.webrtc.EventListener
import com.dev.talkersdk.webrtc.Talker
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var showPushToTalkButton = false

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
        setContent {
            val sharedPreference = SharedPreference(this)
            var isLoading by remember {
                mutableStateOf(false)
            }
            fun closeConnection() {
                isLoading = true
                Talker.logoutUser(
                    onLogoutSuccess = {
                        Toast.makeText(
                            this@MainActivity,
                            "User sign out success",
                            Toast.LENGTH_SHORT
                        ).show()
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(1000)
                            isLoading = false
                        }
                    },
                    onLogoutFailure = {
                        Toast.makeText(
                            this@MainActivity,
                            "User sign out failure",
                            Toast.LENGTH_SHORT
                        ).show()
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(1000)
                            isLoading = false
                        }
                    }
                )
            }



            LaunchedEffect(key1 = Unit) {
                Log.i(
                    "User Data : ",
                    sharedPreference.getUserData().toString()
                )
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
                        var labelText by remember {
                            mutableStateOf("")
                        }
                        if (!Talker.isUserLoggedIn()) {
                            var userId by remember {
                                mutableStateOf("")
                            }
                            OutlinedTextField(value = userId, onValueChange = { userId = it })
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(onClick = {
                                if (userId.isNotEmpty()) {
                                    isLoading = true
                                    Talker.loginUser(
                                        this@MainActivity,
                                        userId,
                                        fcmToken,
                                        onLoginSuccess = {
                                            Toast.makeText(
                                                this@MainActivity,
                                                "User login success",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            Talker.establishConnection(
                                                applicationContext,
                                                isMaster = false,
                                                eventListener = object : EventListener {
                                                    override fun onConnectionSuccess() {
                                                        Toast.makeText(
                                                            this@MainActivity,
                                                            "Connection successful",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        isLoading = false
                                                        showPushToTalkButton = true
                                                    }

                                                    override fun onConnectionFailure(errorMessage: String) {
                                                        Toast.makeText(
                                                            this@MainActivity,
                                                            "Connection failure $errorMessage",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        Log.d(
                                                            "Connection failure",
                                                            "Connection failed with error : $errorMessage"
                                                        )
//                                                        closeConnection()
                                                    }

                                                    override fun onConnectionClosed() {
                                                        Toast.makeText(
                                                            this@MainActivity,
                                                            "Connection closed",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            )
                                        },
                                        onLoginFailure = {
                                            Toast.makeText(
                                                this@MainActivity,
                                                "User login failed",
                                                Toast.LENGTH_SHORT
                                            ).show()
//                                            closeConnection()
                                        }
                                    )
                                }
                            }) {
                                Text(text = "Login")
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            var name by remember {
                                mutableStateOf("")
                            }
                            OutlinedTextField(value = name, onValueChange = { name = it })
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(onClick = {
                                if (name.isNotEmpty()) {
                                    isLoading = true
                                    Talker.registerUser(
                                        this@MainActivity,
                                        name,
                                        fcmToken,
                                        onRegisterSuccess = {
                                            Toast.makeText(
                                                this@MainActivity,
                                                "User register success",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            Talker.establishConnection(
                                                applicationContext,
                                                isMaster = false,
                                                eventListener = object : EventListener {
                                                    override fun onConnectionSuccess() {
                                                        Toast.makeText(
                                                            this@MainActivity,
                                                            "Connection successful",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        isLoading = false
                                                        showPushToTalkButton = true
                                                    }

                                                    override fun onConnectionFailure(errorMessage: String) {
                                                        Toast.makeText(
                                                            this@MainActivity,
                                                            "Connection failure $errorMessage",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        Log.d(
                                                            "Connection failure",
                                                            "Connection failed with error : $errorMessage"
                                                        )
                                                        closeConnection()
                                                        showPushToTalkButton = false
                                                    }

                                                    override fun onConnectionClosed() {
                                                        Toast.makeText(
                                                            this@MainActivity,
                                                            "Connection closed",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        closeConnection()
                                                        showPushToTalkButton = false
                                                    }
                                                }
                                            )
                                        },
                                        onRegisterFailure = {
                                            Toast.makeText(
                                                this@MainActivity,
                                                "User register failed",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            isLoading = false
//                                            closeConnection()
                                        }
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
                                                    labelText =
                                                        "Connecting... The other person is talking..."
                                                    Toast.makeText(
                                                        applicationContext,
                                                        "The other person is talking...",
                                                        Toast.LENGTH_SHORT
                                                    )
                                                        .show()
                                                } else {
                                                    labelText = "Sending audio to remote..."
                                                }
                                            }
                                        }

                                        is PressInteraction.Cancel -> {
                                            Talker.stopPttAudio()
                                            labelText = "Stopped speaking..."
                                        }

                                        is PressInteraction.Release -> {
                                            Talker.stopPttAudio()
                                            labelText = "Stopped speaking..."
                                        }
                                    }
                                }
                            }

                            Text(text = labelText)
                            Spacer(modifier = Modifier.height(60.dp))
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
                                    Talker.closeConnection()
                                    closeConnection()
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
        Talker.logoutUser()
    }
}

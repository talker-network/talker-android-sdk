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
                                            Toast.makeText(this@MainActivity, "User login success", Toast.LENGTH_SHORT).show()
                                            isLoading = false
                                        },
                                        onLoginFailure = {
                                            Toast.makeText(this@MainActivity, "User login failed", Toast.LENGTH_SHORT).show()
                                            isLoading = false
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
                                            Toast.makeText(this@MainActivity, "User register success", Toast.LENGTH_SHORT).show()
                                            isLoading = false
                                        },
                                        onRegisterFailure = {
                                            Toast.makeText(this@MainActivity, "User register failed", Toast.LENGTH_SHORT).show()
                                            isLoading = false
                                        }
                                    )
                                }
                            }) {
                                Text(text = "Create")
                            }
                        }else{
                            if (showPushToTalkButton){
                            val interactionSource = remember {
                                MutableInteractionSource()
                            }
                            LaunchedEffect(key1 = interactionSource) {
                                interactionSource.interactions.collect() {
                                    when (it) {
                                        is PressInteraction.Press -> {
                                            Talker.startPttAudio { isChannelAvailable ->
                                                if (!isChannelAvailable){
                                                    Toast.makeText(applicationContext, "The other person is talking...", Toast.LENGTH_SHORT)
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

                            Button(
                                onClick = {

                                },
                                modifier = Modifier.padding(16.dp),
                                interactionSource = interactionSource
                            ) {
                                Text(text = "Push to talk")
                            }
                            Spacer(modifier = Modifier.height(50.dp))

                            Button(
                                onClick = {
                                    isLoading = true
                                    showPushToTalkButton = false
                                    Talker.closeConnection()
                                },
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(text = "Stop")
                            }
                            }else{
                                // connect as viewer and connect as master buttons
                                // logout button as well
                                Button(onClick = {
                                    isLoading = true
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
                                                isLoading = false
                                                showPushToTalkButton = false
                                            }

                                            override fun onConnectionClosed() {
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Connection closed",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                isLoading = false
                                                showPushToTalkButton = false
                                            }
                                        }
                                    )
                                }) {
                                    Text(text = "Connect as Viewer")
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(onClick = {
                                    isLoading = true
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
                                                    "Connection failure",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                isLoading = false
                                                showPushToTalkButton = false
                                            }

                                            override fun onConnectionClosed() {
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Connection closed",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                isLoading = false
                                                showPushToTalkButton = false
                                            }
                                        }
                                    )
                                }) {
                                    Text(text = "Connect as Master")
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(onClick = {
                                    isLoading = true
                                    Talker.logoutUser(
                                        onLogoutSuccess = {
                                            Toast.makeText(this@MainActivity, "User sign out success", Toast.LENGTH_SHORT).show()
                                            isLoading = false
                                        },
                                        onLogoutFailure = {
                                            Toast.makeText(this@MainActivity, "User sign out failure", Toast.LENGTH_SHORT).show()
                                            isLoading = false
                                        }
                                    )
                                }) {
                                    Text(text = "Logout")
                                }
                            }
                        }

//                        if (showPushToTalkButton) {
//                            val mSocket = SocketHandler.getSocket()
//                            val interactionSource = remember {
//                                MutableInteractionSource()
//                            }
//                            LaunchedEffect(key1 = interactionSource) {
//                                interactionSource.interactions.collect() {
//                                    when (it) {
//                                        is PressInteraction.Press -> {
//                                            scope.launch(Dispatchers.IO) {
//                                                initWsConnection?.startSpeaking { isChannelAvailable ->
//
//                                                }
//                                            }
//                                        }
//
//                                        is PressInteraction.Cancel -> {
//                                            scope.launch(Dispatchers.IO) {
//                                                initWsConnection?.stopSpeaking()
//                                            }
//                                        }
//
//                                        is PressInteraction.Release -> {
//                                            scope.launch(Dispatchers.IO) {
//                                                initWsConnection?.stopSpeaking()
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//
//                            Button(
//                                onClick = {
//
//                                },
//                                modifier = Modifier.padding(16.dp),
//                                interactionSource = interactionSource
//                            ) {
//                                Text(text = "Push to talk")
//                            }
//                            Spacer(modifier = Modifier.height(50.dp))
//
//                            Button(
//                                onClick = {
//                                    isLoading = true
//                                    showPushToTalkButton = false
//                                    initWsConnection?.closeConnection()
//                                    isLoading = false
//                                },
//                                modifier = Modifier.padding(16.dp)
//                            ) {
//                                Text(text = "Stop")
//                            }
//                        } else {
//                            if (TalkerSDKApplication().auth?.isSignedIn == false) {
//                                FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
//                                    if (!task.isSuccessful) {
//                                        Log.w("FCM", "Fetching FCM registration token failed", task.exception)
//                                        return@OnCompleteListener
//                                    }
//                                    // Get new FCM registration token
//                                    token = task.result
//                                    Log.w("FCM", "Token : $token", task.exception)
//                                })
//                                var userId by remember {
//                                    mutableStateOf("")
//                                }
//                                OutlinedTextField(value = userId, onValueChange = { userId = it })
//                                Spacer(modifier = Modifier.height(20.dp))
//                                Button(onClick = {
//                                    if (userId.isNotEmpty()) {
//                                        isLoading = true
//                                        WebrtcConnection.loginUser(
//                                            this@MainActivity,
//                                            userId,
//                                            token,
//                                            onLoginSuccess = {
//                                                isLoading = false
//                                            },
//                                            onLoginFailure = {
//                                                isLoading = false
//                                            }
//                                        )
//                                    }
//                                }) {
//                                    Text(text = "Login")
//                                }
//
//                                Spacer(modifier = Modifier.height(20.dp))
//
//                                var name by remember {
//                                    mutableStateOf("")
//                                }
//                                OutlinedTextField(value = name, onValueChange = { name = it })
//                                Spacer(modifier = Modifier.height(20.dp))
//                                Button(onClick = {
//                                    if (name.isNotEmpty()) {
//                                        isLoading = true
//
//                                        sdkCreateUser(this@MainActivity,
//                                            name = name,
//                                            onSuccess = { res ->
//                                                sharedPreference.setUserData(res.data)
//                                                TalkerSDKApplication().auth?.signIn(res.data.user_id,
//                                                    res.data.a_pass,
//                                                    emptyMap(),
//                                                    mapOf(
//                                                        "name" to res.data.name,
//                                                        "user_id" to res.data.user_id,
//                                                        "user_auth_token" to res.data.user_auth_token
//                                                    ),
//                                                    object : Callback<SignInResult> {
//                                                        override fun onResult(result: SignInResult?) {
//                                                            isLoading = false
//                                                            runOnUiThread {
//                                                                Toast.makeText(
//                                                                    this@MainActivity,
//                                                                    "Sign in successful.",
//                                                                    Toast.LENGTH_SHORT
//                                                                ).show()
//                                                            }
//                                                        }
//
//                                                        override fun onError(e: java.lang.Exception?) {
//                                                            runOnUiThread {
//                                                                e?.printStackTrace()
//                                                                Toast.makeText(
//                                                                    this@MainActivity,
//                                                                    "Sign in failed.",
//                                                                    Toast.LENGTH_SHORT
//                                                                ).show()
//                                                            }
//                                                        }
//                                                    })
//                                            },
//                                            onError = {},
//                                            onInternetNotAvailable = {},
//                                            fcmToken = token
//                                        )
//                                    }
//                                }) {
//                                    Text(text = "Create")
//                                }
//                            } else {
//                                Button(onClick = {
//                                    isLoading = true
//                                    scope.launch(Dispatchers.IO) {
////                                        master = false
////                                        mCreds = TalkerSDKApplication().auth.awsCredentials
////                                        Log.d(LOG_TAG, "mCreds : $mCreds")
////                                        if (initWsConnection?.updateSignalingChannelInfo(
////                                                mRegion,
////                                                mChannelName,
////                                                ChannelRole.VIEWER
////                                            ) != true
////                                        ) {
////                                            return@launch
////                                        }
////                                        mClientId =
////                                            sharedPreference.getUserData().user_auth_token
////                                                .split("|")
////                                                .getOrNull(2) ?: ""
////                                        initWebRtcConnection {
////                                            SocketHandler.setSocket(sharedPreference.getUserData().user_auth_token)
////                                            SocketHandler.establishConnection(this@MainActivity)
////                                            runOnUiThread {
////                                                isLoading = false
////                                            }
////                                        }
//                                    }
//                                }) {
//                                    Text(text = "Connect as Viewer")
//                                }
//
//                                Spacer(modifier = Modifier.height(20.dp))
//
//                                Button(onClick = {
//                                    isLoading = true
//                                    master = true
//
//
//                                    scope.launch(
//                                        Dispatchers.IO
//                                    ) {
////                                        mCreds = TalkerSDKApplication().auth.awsCredentials
////                                        Log.d(LOG_TAG, "mCreds : $mCreds")
////                                        if (initWsConnection?.updateSignalingChannelInfo(
////                                                mRegion,
////                                                mChannelName,
////                                                ChannelRole.MASTER
////                                            ) != true
////                                        ) {
////                                            return@launch
////                                        }
////                                        mClientId =
////                                            sharedPreference.getUserData().user_auth_token
////                                                .split("|")
////                                                .getOrNull(2) ?: ""
////                                        initWebRtcConnection {
////                                            SocketHandler.setSocket(sharedPreference.getUserData().user_auth_token)
////                                            SocketHandler.establishConnection(this@MainActivity)
////                                            runOnUiThread {
////                                                isLoading = false
////                                            }
////                                        }
//
//                                    }
//                                }) {
//                                    Text(text = "Connect as Master")
//                                }
//
//                                Spacer(modifier = Modifier.height(20.dp))
//
//                                Button(onClick = {
//                                    isLoading = true
//                                    scope.launch(Dispatchers.IO) {
//                                        TalkerSDKApplication().auth?.signOut(
//                                            SignOutOptions.Builder().signOutGlobally(true).build()
//                                        )
//                                    }.invokeOnCompletion {
//                                        isLoading = false
//                                    }
//                                }) {
//                                    Text(text = "Logout")
//                                }
//                            }
//                        }
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

package com.talkersdk.sample

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.firebase.messaging.FirebaseMessaging
import com.talkersdk.sample.constants.Constants
import com.talkersdk.sample.ui.theme.TalkerSDKTheme
import network.talker.sdk.Talker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // don't forget to initialize Talker using your sdk key or else
        // the sdk will throw some exception
        // also don't forget to check for required permissions and
        // also set the services as mentioned in the menifest file of the sample app
        Talker.init(Constants.SDK_KEY)


        setContent {
            var fcmToken by remember {
                mutableStateOf("")
            }
            LaunchedEffect(key1 = Unit) {
                // get the firebase token on which you will get the notification events.
                // don't forget to add the google-services.json file in the app level module of this project.
                // you can replace the file with yours if you want.
                FirebaseMessaging.getInstance().token.addOnCompleteListener { result ->
                    fcmToken = result.result
                    Log.d(
                        "FCM",
                        "FCM Token : $fcmToken"
                    )
                }
            }
            val requestPermission = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestMultiplePermissions()) { result ->
                if (result.values.none { bool -> !bool }){
                    Log.d(
                        "Talker SDK",
                        "All permissions granted"
                    )
                }else{
                    Log.d(
                        "Talker SDK",
                        "Permissions denied"
                    )
                }
            }
            LaunchedEffect(key1 = Unit) {
                // request all this permissions so that
                // our sdk could function properly.
                requestPermission.launch(
                    arrayOf(
                        Manifest.permission.POST_NOTIFICATIONS,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.FOREGROUND_SERVICE,
                    )
                )
            }
            TalkerSDKTheme {
                SingleButtonSample(fcmToken = fcmToken)
            }
        }
    }

    override fun onDestroy() {
        Talker.closeConnection()
        super.onDestroy()
    }
}
package com.talkersdk.sample

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import network.talker.app.dev.webrtc.AudioStatus
import network.talker.app.dev.webrtc.ServerConnectionState
import network.talker.app.dev.Talker
import network.talker.app.dev.model.AudioData
import network.talker.app.dev.networking.data.Channel
import network.talker.app.dev.networking.data.GetAllUserModelData

@OptIn(ExperimentalMaterial3Api::class)
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

    // get event for changing of audio status
    Talker.eventListener.onAudioStatusChange = { audioStatus: AudioStatus ->
        status = audioStatus.name
        Log.d(
            "Talker SDK",
            "audioStatus : ${audioStatus.name}"
        )
    }
    // get event for changing of peer connection state
    Talker.eventListener.onServerConnectionChange =
        { serverConnectionState: ServerConnectionState, message: String ->
            isLoading = false
            Toast.makeText(context, serverConnectionState.name, Toast.LENGTH_SHORT).show()
            Log.d(
                "Talker SDK",
                "peerConnectionState : ${serverConnectionState.name} ${
                    Talker.getCurrentUserId(
                        context
                    )
                } $message"
            )
            when (serverConnectionState) {
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

    var showCreateChannelDialog by remember {
        mutableStateOf(false)
    }

    var showEditChannelDialog by remember {
        mutableStateOf(false)
    }

    // storing list of channels
    val channels by Talker.getChannelList().collectAsState(initial = emptyList())
    // list of all users available in the sdk
    val users by Talker.getAllUsers().collectAsState(initial = emptyList())

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
                    if (Talker.getCurrentUserId(context).isNotEmpty()) {
                        val userId by remember {
                            mutableStateOf(Talker.getCurrentUserId(context))
                        }
                        Button(onClick = {
                            // check if name is not empty
                            if (userId.isNotEmpty()) {
                                status = ""
                                // start showing loader
                                isLoading = true
                                // create user and connect to peer and pass the listener and fcm token
                                Talker.setUser(
                                    context,
                                    userId,
                                    fcmToken,
//                                    eventListener,
                                    onSuccess = {
                                        Toast.makeText(context, "Success", Toast.LENGTH_SHORT)
                                            .show()
                                        Log.d(
                                            "Talker SDK",
                                            "registrationState : Success} message : $it"
                                        )
                                    },
                                    onFailure = {
                                        showPushToTalkButton = false
                                        isLoading = false
                                        Toast.makeText(context, "Failure", Toast.LENGTH_SHORT)
                                            .show()
                                        Log.d(
                                            "Talker SDK",
                                            "registrationState : Failure message : $it"
                                        )
                                    }
                                )
                            }
                        }) {
                            Text(text = "Re-Login")
                        }
                        Spacer(modifier = Modifier.height(25.dp))
                    }
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
                    var isDropDownExpanded by rememberSaveable {
                        mutableStateOf(false)
                    }
                    var currentSpeaking by remember {
                        mutableStateOf("")
                    }
                    // will store the current channel
                    var selectedChannel by rememberSaveable {
                        mutableStateOf(channels.getOrNull(0) ?: Channel())
                    }
                    // current user
                    var selectedUser by remember {
                        mutableStateOf<GetAllUserModelData?>(null)
                    }

                    Talker.eventListener.onChannelUpdated = { updatedChannel ->
                        if (selectedChannel.channel_id == updatedChannel.channel_id) {
                            selectedChannel = selectedChannel.copy(
                                group_name = updatedChannel.new_name
                            )
                        }
                    }

                    // when you are removed from channel or any other person is removed from channel.
                    Talker.eventListener.onUserRemovedFromChannel = { removedUser ->
                        // if the user that is removed is nothing but me than
                        // close the edit channel dialog.
                        if (removedUser.removed_participant == Talker.getCurrentUserId(context)) {
                            showEditChannelDialog = false
                            selectedChannel = channels[0]
                        }
                    }

                    Talker.eventListener.currentPttAudio = { data: AudioData ->
                        currentSpeaking = data.SenderName
                    }


                    Talker.eventListener.onNewMessageReceived = {
                        Toast.makeText(context, it.description, Toast.LENGTH_SHORT).show()
                    }



                    val imagePicker = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.PickVisualMedia()
                    ) { uri ->
                        uri?.let {
                            Talker.uploadImage(
                                context,
                                selectedChannel.channel_id,
                                "Caption",
                                uri,
                                onSuccess = {

                                },
                                onFailure = {
                                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }


                    val pdfLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
                        uri?.let {
                            Talker.uploadDocument(
                                context,
                                selectedChannel.channel_id,
                                documentUri = it,
                                onSuccess = {

                                },
                                onFailure = {
                                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }

                    Text(text = "Audio Status : $status")
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(text = "Current Speaking : $currentSpeaking")
                    Spacer(modifier = Modifier.height(20.dp))

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
                    Spacer(modifier = Modifier.height(25.dp))

                    Row {
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
                            Text(text = "Disconnect")
                        }

                        Spacer(modifier = Modifier.width(25.dp))

                        Button(
                            onClick = {
                                isLoading = true
                                // close the connection and logout the user.
                                // also don't forget to call this method in onDestroy() or current activity.
                                // it will prevent data leaks
                                Talker.logoutUser()
                            },
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(text = "Logout")
                        }
                    }
                    Spacer(modifier = Modifier.height(25.dp))
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
                    Spacer(modifier = Modifier.height(25.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(15.dp)
                    ) {
                        items(
                            users
                        ) {
                            Button(onClick = {
                                selectedUser = if (selectedUser == it) {
                                    null
                                } else {
                                    it
                                }
                            }) {
                                Text(
                                    text = it.name,
                                    color = if (selectedUser == it) Color.Cyan else Color.White
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    // if their is some user selected then show the button
                    if (selectedUser != null) {
                        Button(onClick = {
                            showCreateChannelDialog = true
                        }) {
                            Text(text = "Create Channel")
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))

                    // if the channel is created from group
                    // only after that allow to edit it.
                    // user won't be allowed to change the name, admin and participants if it created for peer to peer.
                    if (selectedChannel.channel_type == "group") {
                        Button(onClick = {
                            showEditChannelDialog = true
                        }) {
                            Text(text = "Edit")
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (selectedChannel.channel_type == "group") {
                        Button(onClick = {
                            Talker.exitChannel(
                                context,
                                selectedChannel.channel_id,
                                onSuccess = {
                                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                },
                                onError = {
                                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }) {
                            Text(text = "Leave Channel")
                        }
                    }


                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        Modifier
                            .padding(horizontal = 25.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var myMessage by rememberSaveable {
                            mutableStateOf("")
                        }
                        TextField(value = myMessage, onValueChange = { myMessage = it }, placeholder = {
                            Text(text = "Enter message...")
                        }, singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(15.dp))

                        Button(onClick = {
                            Talker.sendTextMsg(
                                context,
                                selectedChannel.channel_id,
                                text = myMessage,
                                onSuccess = {
                                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                },
                                onFailure = {
                                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                }
                            )
                            myMessage = ""
                        }) {
                            Text(text = "Send Msg")
                        }

                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row {
                        Button(onClick = {
                            pdfLauncher.launch("application/pdf")
                        }) {
                            Text(text = "Upload Pdf")
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        Button(onClick = {
                            imagePicker.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }) {
                            Text(text = "Upload Image")
                        }
                    }

                    // show the create channel dialog.
                    if (showCreateChannelDialog) {
                        BasicAlertDialog(
                            onDismissRequest = { showCreateChannelDialog = false },
                            properties = DialogProperties(
                                usePlatformDefaultWidth = false
                            )
                        ) {
                            var name by rememberSaveable {
                                mutableStateOf("")
                            }
                            Column(
                                Modifier
                                    .background(
                                        Color.White,
                                        shape = RoundedCornerShape(22.dp)
                                    )
                                    .padding(30.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                TextField(
                                    value = name,
                                    onValueChange = {
                                        name = it
                                    },
                                    label = {
                                        Text(text = "Fill this to create channel for group")
                                    },
                                    placeholder = {
                                        Text(text = "Channel Name")
                                    }
                                )
                                Spacer(modifier = Modifier.height(26.dp))

                                Button(onClick = {
                                    // if the name of the channel is not empty
                                    // even after trimming the indentation
                                    // then create the channel for group
                                    if (name.trim().isNotEmpty()) {
                                        Log.d(
                                            "TALKER_SDK",
                                            "Calling group channel..."
                                        )
                                        Talker.createGroupChannel(
                                            context,
                                            name,
                                            // the user that we want to add to this room rather than us.
                                            selectedUser?.user_id ?: "",
                                            onSuccess = {
                                                selectedUser = null
                                                showCreateChannelDialog = false
                                            }
                                        )
                                    } else {
                                        Log.d(
                                            "TALKER_SDK",
                                            "Calling direct channel..."
                                        )
                                        // else create a direct channel meaning that user doesn't want to create a channel
                                        // for group.
                                        Talker.createDirectChannel(
                                            context,
                                            // the user which we want to add in our peer-to-peer cconnection...
                                            selectedUser?.user_id ?: "",
                                            onSuccess = {
                                                selectedUser = null
                                                showCreateChannelDialog = false
                                            }
                                        )
                                    }
                                }) {
                                    Text(text = "Create")
                                }
                            }
                        }
                    }

                    // edit the channel info dialog
                    if (showEditChannelDialog) {

                        BasicAlertDialog(
                            onDismissRequest = { showEditChannelDialog = false },
                            properties = DialogProperties(
                                usePlatformDefaultWidth = false
                            )
                        ) {
                            var name by rememberSaveable {
                                mutableStateOf(selectedChannel.group_name)
                            }
                            Column(
                                Modifier
                                    .background(
                                        Color.White,
                                        shape = RoundedCornerShape(22.dp)
                                    )
                                    .padding(30.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                TextField(
                                    value = name,
                                    onValueChange = {
                                        name = it
                                    },
                                    label = {
                                        Text(text = "Edit Channel name")
                                    },
                                    placeholder = {
                                        Text(text = "Channel Name")
                                    }
                                )
                                Spacer(modifier = Modifier.height(26.dp))
                                Text(
                                    text = "Remove Participant :- ",
                                    Modifier.padding(bottom = 5.dp)
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(
                                        15.dp
                                    ),
                                    modifier = Modifier.horizontalScroll(rememberScrollState())
                                ) {
                                    channels.first { it.channel_id == selectedChannel.channel_id }.participants.sortedBy { it.name }
                                        .forEach { user ->
                                            AssistChip(
                                                onClick = {
                                                    // you cannot change your own admin rights
                                                    if (user.user_id != Talker.getCurrentUserId(
                                                            context
                                                        )
                                                    ) {
                                                        // whether they are allow or not
                                                        // to do this
                                                        // is handled by backend.
                                                        // they will return failure if not allowed.
                                                        // kindly check log cat for more details.
                                                        if (user.admin) {
                                                            // if the user is admin then remove them from admin
                                                            Talker.removeAdmin(
                                                                context,
                                                                selectedChannel.channel_id,
                                                                user.user_id,
                                                                onSuccess = {

                                                                },
                                                                onFailure = {
                                                                    Toast.makeText(
                                                                        context,
                                                                        "Failed",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                }
                                                            )
                                                        } else {
                                                            // else make them the admin
                                                            Talker.addAdmin(
                                                                context,
                                                                selectedChannel.channel_id,
                                                                user.user_id,
                                                                onSuccess = {

                                                                },
                                                                onFailure = {
                                                                    Toast.makeText(
                                                                        context,
                                                                        "Failed",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                }
                                                            )
                                                        }
                                                    }
                                                },
                                                label = {
                                                    Text(
                                                        text = user.name,
                                                        modifier = Modifier.padding(
                                                            top = 12.dp
                                                        ),
                                                        color = if (user.admin) Color.Green else Color.Black,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                },
                                                trailingIcon = {
                                                    IconButton(onClick = {
                                                        // you cannot remove yourself
                                                        if (user.user_id == Talker.getCurrentUserId(
                                                                context
                                                            )
                                                        ) {
                                                            Toast.makeText(
                                                                context,
                                                                "You cannot remove yourself",
                                                                Toast.LENGTH_SHORT
                                                            )
                                                                .show()
                                                        } else {
                                                            // else remove the participant
                                                            Talker.removeParticipant(
                                                                context,
                                                                // from which channel we want to remove them
                                                                // they are removed, the particular delegate will be called and then you can update the ui accordingly
                                                                selectedChannel.channel_id,
                                                                // which participant we want to remove
                                                                removingParticipantId = user.user_id,
                                                                onSuccess = {
                                                                    Toast.makeText(
                                                                        context,
                                                                        "Removed participant",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                },
                                                                onFailure = {
                                                                    Toast.makeText(
                                                                        context,
                                                                        "Failed",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                }
                                                            )
                                                        }
                                                    }) {
                                                        Image(
                                                            imageVector = Icons.Default.Clear,
                                                            contentDescription = null
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                }
                                Spacer(modifier = Modifier.height(26.dp))
                                Text(
                                    text = "Add Participant :- ",
                                    Modifier.padding(bottom = 5.dp)
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(
                                        15.dp
                                    ),
                                    modifier = Modifier.horizontalScroll(rememberScrollState())
                                ) {
                                    // only show remaining user
                                    users.filter { user ->
                                        channels.first { it.channel_id == selectedChannel.channel_id }.participants.none { part ->
                                            part.user_id == user.user_id
                                        }
                                    }.sortedBy { it.name }.forEach { user ->
                                        AssistChip(
                                            onClick = { /*TODO*/ },
                                            label = {
                                                Text(
                                                    text = user.name,
                                                    modifier = Modifier.padding(
                                                        top = 12.dp
                                                    ),
                                                    color = Color.Black,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            trailingIcon = {
                                                IconButton(onClick = {
                                                    // if the participant is already added in the channel then the error will be thrown
                                                    // by the backend. Kindly check log cat for more details.
                                                    Talker.addParticipant(
                                                        context,
                                                        selectedChannel.channel_id,
                                                        newParticipant = user.user_id,
                                                        onSuccess = {
                                                            Toast.makeText(
                                                                context,
                                                                "Participant added",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        },
                                                        onFailure = {
                                                            Toast.makeText(
                                                                context,
                                                                "Failed",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    )
                                                }) {
                                                    Image(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = null
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(26.dp))

                                Button(onClick = {
                                    // if the channel name if same then simply close the dialog
                                    if (name.trim() != selectedChannel.group_name.trim()) {
                                        Talker.editChannelName(
                                            context,
                                            name,
                                            selectedChannel.channel_id,
                                            onSuccess = {
                                                showEditChannelDialog = false
                                            },
                                            onFailure = {
                                                Toast.makeText(
                                                    context,
                                                    "Failed",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        )
                                    } else {
                                        showEditChannelDialog = false
                                    }
                                }) {
                                    Text(text = "Save")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
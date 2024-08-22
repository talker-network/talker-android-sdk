package com.talkersdk.sample

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import network.talker.app.dev.Talker
import network.talker.app.dev.networking.data.Channel
import network.talker.app.dev.networking.data.GetAllUserModelData
import network.talker.app.dev.networking.data.Participant
import network.talker.app.dev.webrtc.AudioStatus
import network.talker.app.dev.webrtc.ServerConnectionState

@OptIn(ExperimentalMaterial3Api::class)
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
                            Text(text = "Login")
                        }
                    }
                } else {
                    val interactionSource = remember {
                        MutableInteractionSource()
                    }
                    // to show list of channels joined or created
                    var isDropDownExpanded by rememberSaveable {
                        mutableStateOf(false)
                    }
                    // will store the current channel
                    var selectedChannel by rememberSaveable {
                        mutableStateOf(Channel())
                    }
                    // storing list of channels
                    var channels by remember {
                        mutableStateOf(emptyList<Channel>())
                    }
                    // list of all users available in the sdk
                    val users = remember {
                        mutableStateListOf<GetAllUserModelData>()
                    }
                    // current user
                    var selectedUser by remember {
                        mutableStateOf<GetAllUserModelData?>(null)
                    }

                    // when we create a new channel or some other person adds us to their channel
                    Talker.eventListener.onNewChannel = { channel ->
                        // simply get the updated channel's list from Talker instance and update it in your ui.
                        Talker.getChannelList { list ->
                            channels = list
                        }
                    }

                    // when channel name gets updated..
                    Talker.eventListener.onChannelUpdated = { updatedChannel ->
                        Talker.getChannelList { list ->
                            // update channel list with latest from Talker Instance
                            channels = list
                            list.firstOrNull { it.channel_id == selectedChannel.channel_id }?.let {
                                // update selectedChannel so that it is also updated if it's name has only been changed.
                                selectedChannel = it
                            }
                        }
                    }
                    // when you are removed from channel or any other person is removed from channel.
                    Talker.eventListener.onRemovedUserFromChannel = { removedUser ->
                        // update with latest list..
                        Talker.getChannelList { list ->
                            channels = list
                            // update if the the selected channel is only removed.
                            list.firstOrNull { it.channel_id == selectedChannel.channel_id }?.let {
                                selectedChannel = it
                            }
                        }
                    }

                    // if some user has been added in channel
                    Talker.eventListener.onAddedUserInChannel = { addedUser ->
                        Talker.getChannelList { list ->
                            channels = list
                        }
                    }

                    // when a new user gets created by someone...
                    Talker.eventListener.onNewSdkUser = { newSdkUser ->
                        Talker.getAllUsers {
                            users.clear()
                            users.apply {
                                addAll(it)
                                sortBy { it.name }
                            }
                        }
                    }


                    // on the first time
                    // fetch all the channel's list and by default keep the first one selected
                    LaunchedEffect(key1 = Unit) {
                        Talker.getChannelList(
                            onChannelFetched = {
                                channels = it
                                selectedChannel = channels.getOrNull(0) ?: Channel()
                            }
                        )
                    }

                    // on the first time
                    // fetch all the users available in the app
                    LaunchedEffect(key1 = Unit) {
                        Talker.getAllUsers(
                            onUsersFetched = {
                                users.clear()
                                users.apply {
                                    addAll(it)
                                    sortBy { it.name }
                                }
                            }
                        )
                    }


                    LaunchedEffect(key1 = interactionSource) {
                        interactionSource.interactions.collect() {
                            when (it) {
                                is PressInteraction.Press -> {
                                    // when user presses the button call this function to start sharing audio
                                    // it will return boolean which will inform user if the channel is available for
                                    // sending audio data or not
                                    // send the channel id in which you want to send the data
                                    Talker.startPttAudio(selectedChannel.channel_id) { isChannelAvailable ->
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
                        users.filter { user ->
                            // so that we don't show our own user in the list
                            user.user_id != Talker.getCurrentUserId(context)
                        }.forEach {
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
                        // channel users which are already their in the channel participants list.
                        val channelUsers = remember {
                            mutableStateListOf<Participant>().apply {
                                addAll(selectedChannel.participants)
                            }
                        }
                        // other users which are not their in the channel participant's list
                        val remainingUsers = remember {
                            mutableStateListOf<GetAllUserModelData>().apply {
                                addAll(
                                    // take only those users from user's list
                                    // that are not in the selected channel's participant's list
                                    users.filter { user ->
                                        selectedChannel.participants.none { participant ->
                                            participant.user_id == user.user_id
                                        }
                                    }
                                )
                            }
                        }


                        // when new admin is added update the ui
                        Talker.eventListener.onAdminAdded = { adminAdded ->
                            channelUsers.clear()
                            Talker.getChannelList { list ->
                                list.firstOrNull { channel ->
                                    channel.channel_id == adminAdded.channel_id
                                }?.let { channel ->
                                    channelUsers.addAll(channel.participants)
                                }
                            }
                        }

                        // when admin is removed update the ui
                        Talker.eventListener.onAdminRemoved = { removedAdmin ->
                            channelUsers.clear()
                            Talker.getChannelList { list ->
                                list.firstOrNull { channel ->
                                    channel.channel_id == removedAdmin.channel_id
                                }?.let { channel ->
                                    channelUsers.addAll(channel.participants)
                                }
                            }
                        }

                        // some user is removed from the channel
                        // update the ui with both participants and
                        // also add that user into the add participants list.
                        Talker.eventListener.onRemovedUserFromChannel = { removedUser ->
                            channelUsers.clear()
                            remainingUsers.clear()
                            Talker.getChannelList { list ->
                                list.firstOrNull { channel ->
                                    channel.channel_id == removedUser.channel_id
                                }?.let { channel ->
                                    channelUsers.addAll(channel.participants)
                                }
                            }
                            Talker.getAllUsers { users ->
                                remainingUsers.addAll(users)
                            }

                            // if the user that is removed is nothing but me than
                            // close the edit channel dialog.
                            if (removedUser.removed_participant == Talker.getCurrentUserId(context)){
                                showEditChannelDialog = false
                            }
                        }


                        // doing the opposite of remove user from the channel delegate
                        Talker.eventListener.onAddedUserInChannel = { addedUser ->
                            channelUsers.clear()
                            remainingUsers.clear()
                            Talker.getChannelList { list ->
                                list.firstOrNull { channel ->
                                    channel.channel_id == addedUser.channel_id
                                }?.let { channel ->
                                    channelUsers.addAll(channel.participants)
                                }
                            }
                            Talker.getAllUsers { users ->
                                remainingUsers.addAll(users)
                            }
                        }

                        // when some user is created and add that user to the available to add participants list.
                        Talker.eventListener.onNewSdkUser = { newSdkUser ->
                            Toast.makeText(context, "New user found", Toast.LENGTH_SHORT).show()
                            remainingUsers.clear()
                            Talker.getAllUsers { users ->
                                remainingUsers.apply {
                                    addAll(
                                        users
                                    )
                                }
                            }
                        }

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
                                    channelUsers.sortedBy { it.name }.forEach { user ->
                                        AssistChip(
                                            onClick = {
                                                // you cannot change your own admin rights
                                                if (user.user_id != Talker.getCurrentUserId(context)) {
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
                                                    }else{
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
                                                            // which participant we want to remove
                                                            removingParticipant = user.user_id,
                                                            // from which channel we want to remove them
                                                            // they are removed, the particular delegate will be called and then you can update the ui accordingly
                                                            selectedChannel.channel_id,
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
                                    remainingUsers.filter { user ->
                                        selectedChannel.participants.none { part ->
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
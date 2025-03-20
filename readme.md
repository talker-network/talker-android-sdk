
## Talker Android SDK

### Installation

1. **Add the Maven repository to settings.gradle.kts:**

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://raw.githubusercontent.com/alexgreench/google-webrtc/master") }
    }
}
```

2. **Add the dependency to your module's build.gradle.kts:**

```kotlin
dependencies {
    implementation("com.talker:android-sdk:1.0.0")
}

```

3. **Configure Firebase:**
   * Add your Firebase project's `google-services.json` file to your app's root directory
   * Submit the google-services.json file to Talker for firebase cloud messaging compatibility

### Usage

#### Initialization

```kotlin
import network.talker.sdk.Talker

// Initialize the Talker SDK with required context and SDK key
Talker.initialize(
    applicationContext: Context,
    sdkKey: String
)
```

#### User Management

**User Class Properties**

* `userId`: Unique identifier for the user
* `name`: User's display name

**Why create or set a user in the SDK?**

* To establish a unique identity within the Talker system
* To associate your application with specific user data
* To enable communication between different users using channels

**Creating a User**

When a new user needs to be created in the Talker system:

```kotlin
Talker.createUser(
    context: Context,
    name: String,
    fcmToken: String,
    onSuccess: (message: String) -> Unit = {}, // Optional success callback
    onFailure: (message: String) -> Unit = {}, // Optional failure callback
    channelName: String = "",                  // Optional channel name
    region: String = ""                        // Optional region
)
```

**Setting an Existing User**

When an already existing user in the Talker system needs to be associated with your application:

```kotlin
Talker.setUser(
    context: Context,
    userId: String,
    fcmToken: String,
    onSuccess: (message: String) -> Unit = {}, // Optional success callback
    onFailure: (message: String) -> Unit = {}, // Optional failure callback
)
```

**Getting All Users**

Retrieve a list of all users in the system:

```kotlin
// Returns a Flow of user list
val usersFlow: Flow<List<GetAllUserModelData>> = Talker.getAllUsers()

// Usage example with coroutines
lifecycleScope.launch {
    Talker.getAllUsers().collect { users ->
        // Handle list of users
    }
}
```

**Getting Current User Information**

Retrieve information about the currently logged-in user:

```kotlin
// Get current user ID
val userId: String = Talker.getCurrentUserId(context)

// Get current user ID and name
val (userId, userName) = Talker.getCurrentUser(context)
```

#### Channel Management

**Channel Class Properties**

* `channelId`: Unique identifier for the channel
* `name`: Channel name
* `participants`: Array of channel participants
* `type`: "group" or "direct"

**Channel Participant Properties**

* `userId`: Participant's ID
* `name`: Participant's name
* `admin`: Boolean indicating if participant is channel admin

**Creating a Group Channel**

Create a new group channel with multiple participants:

```kotlin
Talker.createGroupChannel(
    context: Context,
    name: String,
    participantId: String,
    onSuccess: (CreateChannelModel) -> Unit = {},      // Optional success callback
    onError: (String) -> Unit = {},                    // Optional error callback
    onInternetNotAvailable: () -> Unit = {}           // Optional network error callback
)
```

**Creating a Direct Channel**

Create a direct channel between two users:

```kotlin
Talker.createDirectChannel(
    context: Context,
    participantId: String,
    onSuccess: (CreateChannelModel) -> Unit = {},      // Optional success callback
    onError: (String) -> Unit = {},                    // Optional error callback
    onInternetNotAvailable: () -> Unit = {}           // Optional network error callback
)
```

**Getting Channel Messages**

Retrieve messages from a specific channel:

```kotlin
// Returns a Flow of message list
val messagesFlow: Flow<List<MessageObject>> = Talker.getChannelMessages(channelId)

// Usage example with coroutines
lifecycleScope.launch {
    Talker.getChannelMessages(channelId).collect { messages ->
        // Handle list of messages
    }
}
```

#### Message Management

**Sending Text Messages**

Send a text message in a channel:

```kotlin
Talker.sendTextMsg(
    context: Context,
    channelId: String,
    message: String,
    onSuccess: (MessageObject) -> Unit = {},    // Optional success callback
    onError: (String) -> Unit = {}              // Optional error callback
)
```

**Uploading Media**

**Upload Images**

Upload an image to a channel:

```kotlin
Talker.uploadImage(
    context: Context,
    channelId: String,
    uri: Uri,
    onSuccess: (MessageObject) -> Unit = {},    // Optional success callback
    onError: (String) -> Unit = {},            // Optional error callback
    onProgress: (Int) -> Unit = {}             // Optional upload progress callback
)
```

**Upload Documents**

Upload a document to a channel:

```kotlin
Talker.uploadDocument(
    context: Context,
    channelId: String,
    uri: Uri,
    onSuccess: (MessageObject) -> Unit = {},    // Optional success callback
    onError: (String) -> Unit = {},            // Optional error callback
    onProgress: (Int) -> Unit = {}             // Optional upload progress callback
)
```

#### Connection Management

**Closing Connection**

Properly close the connection and clean up resources:

```kotlin
Talker.closeConnection()
```

#### Push-to-Talk Functionality

**Starting PTT Audio**

To start transmitting PTT audio in a specific channel:

```kotlin
Talker.startPttAudio(
    channelId: String,
    isChannelAvailable: (Boolean) -> Unit  // Callback indicating if channel is available for speaking
)
```

**Stopping PTT Audio**

To stop transmitting PTT audio:

```kotlin
Talker.stopPttAudio()
```

#### **Transmitting PTT-Audio Status Events**&#x20;

The SDK provides various status events for PTT functionality through event listeners:

```kotlin
// Listen for audio status changes
Talker.eventListener.onAudioStatusChange = { audioStatus ->
    when (audioStatus) {
        "CONNECTING" -> // Connection being established
        "SENDING" -> // Audio transmission in progress
        "STOPPED" -> // Transmission ended
        "BUSY" -> // Channel occupied by another transmission
    }
}


```

#### **Receiving Audio**

The SDK automatically handles incoming PTT audio playback through a foreground service (`AudioPlayerService`). The service:

* Manages HLS stream playback using ExoPlayer
* Maintains a queue of audio messages
* Handles playback errors and recovery
*   Updates notification with current playback status
    \
    // Listen for current PTT audio information
    \
    Talker.eventListener.currentPttAudio = { audioData ->
    \
    // audioData contains:
    \
    // - sender\_id: String - ID of the user sending audio
    \
    // - channel\_id: String - ID of the channel where audio is being sent
    \
    // - channel\_name: String - Name of the channel
    \
    // - sender\_name: String - Name of the user sending audio

    // Example usage:
    \
    updateUI(
    \
    speakerName = audioData.sender\_name,
    \
    channelName = audioData.channel\_name
    \
    )
    \
    }

**FCM Integration**

To handle PTT audio notifications when the app is in background:

1. Implement FCM in your application
2. When receiving FCM messages, forward Talker-related messages to the SDK:

```kotlin
Talker.handleFcm(remoteMessage.data)
```

#### Event Listeners

The SDK provides the following event listeners to handle real-time updates and state changes:

**Connection Events**

```kotlin
// Listen for server connection state changes
Talker.eventListener.onServerConnectionChange = { state, message ->
    when (state) {
        ServerConnectionState.CONNECTED -> // Handle connected state
        ServerConnectionState.DISCONNECTED -> // Handle disconnected state
    }
}
```

**Audio Status Events**

For transmitting ptt-audio status events, see the [Transmitting PTT-Audio Status Events](android-sdk.md#transmitting-ptt-audio-status-events) section above.

For metadata of the currently playing received ptt-audio stream, see the [Receiving Audio](android-sdk.md#receiving-audio) section above. The `currentPttAudio` event listener provides metadata about the currently playing audio, including sender and channel information.

**Channel Events**

```kotlin
// New channel created or user added to channel
Talker.eventListener.onNewChannel = { channel ->
    // Handle new channel
}

// Channel name updated
Talker.eventListener.onChannelUpdated = { data ->
    // Handle channel update
}

// User removed from channel
Talker.eventListener.onUserRemovedFromChannel = { data ->
    // Handle user removal
    // data.channelId: String
    // data.userId: String
}

// New user added to channel
Talker.eventListener.onAddedUserInChannel = { data ->
    // Handle new participant
    // data.channelId: String
    // data.userId: String
    // data.name: String
    // data.admin: Boolean
}
```

**Admin Events**

```kotlin
// New admin added to channel
Talker.eventListener.onAdminAdded = { data ->
    // Handle new admin
    // data.channelId: String
    // data.userId: String
    // data.admin: Boolean
}

// Admin removed from channel
Talker.eventListener.onAdminRemoved = { data ->
    // Handle admin removal
    // data.channelId: String
    // data.userId: String
}
```

**User Events**

```kotlin
// New user created in the SDK
Talker.eventListener.onNewSdkUser = { userData ->
    // Handle new user
}
```

**Message Events**

```kotlin
// New message received
Talker.eventListener.onNewMessageReceived = { message ->
    // Handle new message
}
```

All these events provide real-time updates about various aspects of the SDK's functionality. They help you:

* Monitor connection state with the Talker servers
* Track audio transmission status
* Handle channel membership changes
* Manage admin privileges
* Process new messages
* Track user additions and updates

To ensure proper functionality, it's recommended to set up these listeners when initializing your application and handle each event type appropriately for your use case.



#### Important Implementation Notes

1. All methods require a valid SDK key to be set during initialization
2. Context is required for most operations
3. Network operations are handled asynchronously
4. User and channel IDs must not be blank
5. Error callbacks provide specific error messages for debugging
6. PTT status is monitored through the isChannelAvailable callback
7. The SDK maintains a foreground service for ptt audio playback that:
   * Handles PTT audio streams received
   * Continues PTT audio playback even when app is in background or killed
   * Auto-recovers from network and playback errors

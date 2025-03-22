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

4. **Add Required Permissions and Services to AndroidManifest.xml:**

```xml
<!-- Required Permissions -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
<uses-permission android:name="android.permission.RECORD_AUDIO"/>
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="32"
    tools:ignore="ScopedStorage" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32"
    tools:ignore="ScopedStorage" />

<!-- Required Service for Audio Playback -->
<service android:name="network.talker.sdk.player.AudioPlayerService"
    android:exported="false"
    android:foregroundServiceType="specialUse"
    android:permission="android.permission.FOREGROUND_SERVICE"
    android:stopWithTask="false"/>

<!-- Firebase Messaging Service (Example) -->
<service android:name=".messaging.YourFirebaseMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```



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
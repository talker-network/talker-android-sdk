plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "network.talker.app.dev"
    compileSdk = 34

    defaultConfig {
        minSdk = 28
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("androidx.media3:media3-exoplayer-hls:1.4.1")
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")

    implementation("org.webrtc:google-webrtc:1.0.+")
    implementation("org.awaitility:awaitility:4.2.2") {
        isTransitive = false
    }
    val aws_version = "2.75.0"
    implementation("com.amazonaws:aws-android-sdk-kinesisvideo:$aws_version")
    implementation("com.amazonaws:aws-android-sdk-kinesisvideo-signaling:$aws_version")
    implementation("com.amazonaws:aws-android-sdk-kinesisvideo-webrtcstorage:$aws_version")
    implementation("com.amazonaws:aws-android-sdk-mobile-client:$aws_version")
    implementation("com.amazonaws:aws-android-sdk-auth-userpools:$aws_version")
    implementation("com.amazonaws:aws-android-sdk-auth-ui:$aws_version")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.12.0"))
    implementation("com.squareup.okhttp3:okhttp")
    implementation("com.squareup.okhttp3:logging-interceptor")

    implementation("io.socket:socket.io-client:2.0.0") {
        exclude(group = "org.json", module = "json")
    }
    implementation("io.socket:engine.io-client:2.1.0")

    implementation("org.apache.commons:commons-lang3:3.9")

    implementation("androidx.work:work-runtime-ktx:2.9.1")

    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    annotationProcessor("androidx.room:room-compiler:$room_version")
    // To use Kotlin Symbol Processing (KSP)
    ksp("androidx.room:room-compiler:$room_version")
    // optional - Kotlin Extensions and Coroutines support for Room
    implementation("androidx.room:room-ktx:$room_version")
}
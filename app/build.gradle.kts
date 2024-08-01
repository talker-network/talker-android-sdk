plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-kapt")
    kotlin("plugin.serialization")
    id("com.google.gms.google-services")
}

android {
    namespace = "network.talker.app.dev"
    compileSdk = 34

    defaultConfig {
        applicationId = "network.talker.app.dev"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.media3.exoplayer.hls)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    implementation("org.webrtc:google-webrtc:1.0.32006")
    implementation("org.awaitility:awaitility:4.2.0")

    val aws_version = "2.75.0"
    implementation("com.amazonaws:aws-android-sdk-kinesisvideo:$aws_version")
    implementation("com.amazonaws:aws-android-sdk-kinesisvideo-signaling:$aws_version")
    implementation("com.amazonaws:aws-android-sdk-kinesisvideo-webrtcstorage:$aws_version")
    implementation("com.amazonaws:aws-android-sdk-mobile-client:$aws_version")
    implementation("com.amazonaws:aws-android-sdk-auth-userpools:$aws_version")
    implementation("com.amazonaws:aws-android-sdk-auth-ui:$aws_version")

    implementation("com.google.guava:guava:32.0.1-android")
    implementation("org.apache.commons:commons-lang3:3.9")

    implementation("androidx.constraintlayout:constraintlayout-compose-android:1.1.0-alpha13")

    implementation(libs.androidx.work.runtime)

    implementation("org.json:json:20190722")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")

    implementation ("io.socket:socket.io-client:2.0.0") {
        exclude( group = "org.json", module = "json")
    }
    implementation("io.socket:engine.io-client:2.1.0")

    implementation("androidx.media3:media3-exoplayer:1.3.1")

    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-messaging-ktx")
}
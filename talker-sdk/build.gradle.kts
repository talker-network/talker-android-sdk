plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
    id("maven-publish")
    id("signing")
}

android {
    namespace = "network.talker.sdk"
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
publishing {

    publications {
        register<MavenPublication>("release") {
            groupId = "network.talker.sdk"  // Change this
            artifactId = "talker-sdk"
            version = "1.0.1"
            afterEvaluate {
                from(components["release"])
            }
            pom {
                name.set("Talker sdk")
                description.set("Push to talk sdk")
                url.set("https://github.com/your-repo")
                licenses {
                    license {
                        name.set("Apache-2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.html")
                    }
                }
                developers {
                    developer {
                        id.set("yourid")
                        name.set("Your Name")
                        email.set("your.email@example.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/your-repo.git")
                    developerConnection.set("scm:git:ssh://github.com:your-repo.git")
                    url.set("https://github.com/your-repo")
                }
            }





        }
    }
//    repositories {
//        maven {
//            url = uri("${rootProject.buildDir}/maven-repo") // Local repository
//        }
//    }
    repositories {
        maven {
            name = "sonatype"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = "2UB4xnX3"
                password = "Ygp/hTEkRdJeW3jle/Iwe6gDowuHZjYgJW5sHQ98tK"
            }
        }
    }


}
signing {
    sign(publishing.publications["release"])
}
// Create a resolvable configuration that extends runtimeOnly
configurations.create("resolvedRuntimeOnly") {
    extendsFrom(configurations.runtimeOnly.get())
    isCanBeResolved = true
    isCanBeConsumed = false
}

tasks.register<Copy>("copyDependencies") {
    from(configurations.getByName("resolvedRuntimeOnly"))
    into(layout.buildDirectory.dir("libs"))
}

tasks.register<Zip>("fatAar") {
    doNotTrackState("The task is not compatible with incremental build")
    archiveBaseName.set("talker-sdk-fat")
    archiveVersion.set("1.0.0")
    archiveExtension.set("aar")

    from(layout.buildDirectory.dir("intermediates/aar_main_jar/release"))

    dependsOn("assembleRelease", "copyDependencies")

    from(layout.buildDirectory.dir("libs"))
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

    implementation("org.webrtc:google-webrtc:1.0.30039")
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
    // To use Kotlin Symbol Processing (KSP)
    ksp("androidx.room:room-compiler:$room_version")
    // optional - Kotlin Extensions and Coroutines support for Room
    implementation("androidx.room:room-ktx:$room_version")

    runtimeOnly("androidx.media3:media3-exoplayer-hls:1.4.1")
    runtimeOnly("androidx.media3:media3-exoplayer:1.4.1")
    runtimeOnly("com.google.code.gson:gson:2.10.1")
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")

    runtimeOnly("org.webrtc:google-webrtc:1.0.30039")
    runtimeOnly("org.awaitility:awaitility:4.2.2") {
        isTransitive = false
    }
    runtimeOnly("com.amazonaws:aws-android-sdk-kinesisvideo:$aws_version")
    runtimeOnly("com.amazonaws:aws-android-sdk-kinesisvideo-signaling:$aws_version")
    runtimeOnly("com.amazonaws:aws-android-sdk-kinesisvideo-webrtcstorage:$aws_version")
    runtimeOnly("com.amazonaws:aws-android-sdk-mobile-client:$aws_version")
    runtimeOnly("com.amazonaws:aws-android-sdk-auth-userpools:$aws_version")
    runtimeOnly("com.amazonaws:aws-android-sdk-auth-ui:$aws_version")

    runtimeOnly("com.squareup.retrofit2:retrofit:2.11.0")
    runtimeOnly("com.squareup.retrofit2:converter-gson:2.11.0")
    runtimeOnly(platform("com.squareup.okhttp3:okhttp-bom:4.12.0"))
    runtimeOnly("com.squareup.okhttp3:okhttp")
    runtimeOnly("com.squareup.okhttp3:logging-interceptor")

    runtimeOnly("io.socket:socket.io-client:2.0.0") {
        exclude(group = "org.json", module = "json")
    }
    runtimeOnly("io.socket:engine.io-client:2.1.0")

    runtimeOnly("org.apache.commons:commons-lang3:3.9")

    runtimeOnly("androidx.work:work-runtime-ktx:2.9.1")
    runtimeOnly("androidx.room:room-runtime:$room_version")
    // optional - Kotlin Extensions and Coroutines support for Room
    runtimeOnly("androidx.room:room-ktx:$room_version")
}
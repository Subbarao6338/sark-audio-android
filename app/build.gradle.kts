import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val buildDate = Date()
// Version Name Format: Dynamic date-based versioning formatted as YYYY.MM.DD (e.g., 2026.11.05)
val appVersionName: String = SimpleDateFormat("yyyy.MM.dd", Locale.US).format(buildDate)
// Version Code: Standardized integer calculated from date components (e.g., YYYYMMDDHH)
val appVersionCode: Int = SimpleDateFormat("yyyyMMddHH", Locale.US).format(buildDate).toInt()

fun ensureKeystoreExists(targetKeystore: File, storePass: String, keyPass: String, alias: String) {
    if (!targetKeystore.exists()) {
        targetKeystore.parentFile?.mkdirs()
        val process = ProcessBuilder(
            "keytool", "-genkeypair", "-v",
            "-keystore", targetKeystore.absolutePath,
            "-alias", alias,
            "-storepass", storePass,
            "-keypass", keyPass,
            "-keyalg", "RSA",
            "-keysize", "2048",
            "-validity", "10000",
            "-dname", "CN=Sark Audio,O=Sark,C=US"
        ).inheritIO().start()
        process.waitFor()
    }
}

val keystorePath = project.findProperty("RELEASE_STORE_FILE")?.toString()
    ?: "${System.getProperty("user.home")}/.android/release.keystore"
val defaultStorePass = project.findProperty("RELEASE_STORE_PASSWORD")?.toString() ?: "android"
val defaultKeyAlias = project.findProperty("RELEASE_KEY_ALIAS")?.toString() ?: "androidreleasekey"
val defaultKeyPass = project.findProperty("RELEASE_KEY_PASSWORD")?.toString() ?: "android"

val keystoreFile = file(keystorePath)
ensureKeystoreExists(keystoreFile, defaultStorePass, defaultKeyPass, defaultKeyAlias)

android {
    namespace = "sark.audio"
    compileSdk = 34

    defaultConfig {
        applicationId = "sark.audio"
        minSdk = 26
        targetSdk = 34
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            abiFilters.clear()
            abiFilters.add("arm64-v8a")
        }
    }

    androidResources {
        localeFilters += listOf("en")
    }

    signingConfigs {
        create("sharedSigning") {
            storeFile = keystoreFile
            storePassword = defaultStorePass
            keyAlias = defaultKeyAlias
            keyPassword = defaultKeyPass
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("sharedSigning")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("sharedSigning")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/*.version",
                "/META-INF/*.kotlin_module",
                "DebugProbesKt.bin",
                "kotlin-tooling-metadata.json"
            )
        }
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}

import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.musicmuni.sonix.sample"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.musicmuni.sonix.sample"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // Read API key from local.properties
        val localPropertiesFile = rootProject.file("local.properties")
        if (!localPropertiesFile.exists()) {
            throw GradleException(
                "local.properties file not found. Please create it and add 'sonix.apiKey=YOUR_API_KEY'"
            )
        }

        val properties = Properties()
        properties.load(localPropertiesFile.inputStream())
        val apiKey = properties.getProperty("sonix.apiKey")
            ?: throw GradleException(
                "Sonix API key not found in local.properties. Please add 'sonix.apiKey=YOUR_API_KEY'"
            )

        buildConfigField("String", "SONIX_API_KEY", "\"$apiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Sonix library
    implementation(files("libs/sonix.aar"))

    // Required by Sonix
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("io.github.aakira:napier:2.7.1")
    implementation("com.squareup.okio:okio:3.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
    implementation("io.ktor:ktor-client-core:2.3.12")
    implementation("io.ktor:ktor-client-android:2.3.12")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}

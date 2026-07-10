plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") // Kotlin 2.0+ Compose compiler plugin
    id("com.google.devtools.ksp")             // codegen Room yang cepat & hemat
}

android {
    namespace = "com.pos.offline"
    compileSdk = 36 // Android 16

    defaultConfig {
        applicationId = "com.pos.offline"
        minSdk = 26      // Android 8.0 — baseline modern (java.time, classic BT)
        targetSdk = 36   // Android 16
        versionCode = 1
        versionName = "1.0"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = true        // R8: buang kode mati → APK kecil & RAM hemat
            isShrinkResources = true
            // Aturan default sudah memuat optimasi R8; tiap library (Compose/Room)
            // menyertakan consumer-rules sendiri, jadi tidak butuh file tambahan.
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

dependencies {
    // ---- Compose (Material 3) ----
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended") // ikon Icons.Rounded.*
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.13.1")

    // ---- Lifecycle: koleksi Flow sadar-siklus → berhenti saat background (hemat baterai) ----
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // ---- Coroutines + Flow ----
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // ---- Room (database offline cepat) ----
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1") // suspend DAO + withTransaction
    ksp("androidx.room:room-compiler:2.6.1")
}

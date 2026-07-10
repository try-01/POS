plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") // Kotlin 2.0+ Compose compiler plugin
    id("com.google.devtools.ksp")             // codegen Room yang cepat & hemat
    id("androidx.room")                       // export skema JSON (untuk migrasi teruji)
}

// Room Gradle Plugin: mengekspor riwayat skema database (JSON) ke app/schemas
// saat kompilasi. Berkas ini WAJIB di-commit ke VCS agar migrasi otomatis
// antar versi bisa dibangkitkan & diuji secara reproduktif.
room {
    schemaDirectory("$projectDir/schemas")
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

        // Runner instrumentasi default untuk androidTest (migration test, dsb.).
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    // Sediakan skema JSON yang diekspor Room sebagai ASET androidTest, agar
    // MigrationTestHelper bisa membaca berkas 1.json / 2.json.
    //
    // Catatan layout: plugin "androidx.room" menempatkan skema pada subfolder
    // per-variant (schemas/<variant>/<package>/PosDatabase/<ver>.json). Karena
    // MigrationTestHelper mencari berkas di path "<package>.PosDatabase/<ver>.json"
    // relatif terhadap root aset, kita mendaftarkan folder variant agar cocok.
    // Mendaftarkan kedua jalur (dengan & tanpa subfolder variant) membuat konfigurasi
    // tetap berfungsi tak peduli layout yang dipakai plugin — yang tak ada akan diabaikan.
    sourceSets {
        getByName("androidTest").assets.srcDirs(
            "$projectDir/schemas/debug",
            "$projectDir/schemas"
        )
    }

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

    // ---- androidTest: uji migrasi Room + runner JUnit4 ----
    androidTestImplementation("androidx.room:room-testing:2.6.1") // MigrationTestHelper
    androidTestImplementation("androidx.test.ext:junit:1.2.1")     // AndroidJUnit4
    androidTestImplementation("androidx.test:runner:1.6.2")        // InstrumentationRegistry
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") // Kotlin 2.0+ Compose compiler plugin
    id("com.google.devtools.ksp")             // codegen Room yang cepat & hemat
    id("androidx.room")                       // export skema JSON (untuk migrasi teruji)
}

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
        versionCode = 2
        versionName = "1.0.0.1"
        vectorDrawables { useSupportLibrary = true }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro" // Pastikan baris ini ada
        )
    }
}

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }

    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
        getByName("debug").assets.srcDir("$projectDir/schemas")
    }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

afterEvaluate {
    tasks.matching { it.name == "mergeDebugAssets" || it.name == "mergeDebugAndroidTestAssets" }
        .configureEach { dependsOn("kspDebugKotlin") }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended") // ikon Icons.Rounded.*
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.13.1")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1") // suspend DAO + withTransaction
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("com.github.DantSu:ESCPOS-ThermalPrinter-Android:3.4.0")

    val cameraxVersion = "1.4.1"
    implementation("androidx.camera:camera-core:${cameraxVersion}")
    implementation("androidx.camera:camera-camera2:${cameraxVersion}")
    implementation("androidx.camera:camera-lifecycle:${cameraxVersion}")
    implementation("androidx.camera:camera-view:${cameraxVersion}")

    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    implementation("org.apache.poi:poi:5.5.1")
    implementation("org.apache.poi:poi-ooxml:5.5.1")

    androidTestImplementation("androidx.room:room-testing:2.6.1") // MigrationTestHelper
    androidTestImplementation("androidx.test.ext:junit:1.2.1")     // AndroidJUnit4
    androidTestImplementation("androidx.test:runner:1.6.2")        // InstrumentationRegistry
}

afterEvaluate {
    val schemaGenTasks = tasks.matching { it.name.matches(Regex("ksp\\w*Kotlin")) }
    tasks.matching { it.name.contains("MergeAssets", ignoreCase = true) }
        .configureEach { dependsOn(schemaGenTasks) }
}
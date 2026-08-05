plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("androidx.room3")
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

android {
    namespace = "com.pos.offline"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pos.offline"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { 
        compose = true 
    }

    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
        getByName("debug").assets.srcDir("$projectDir/schemas")
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/*.version",
                "META-INF/INDEX.LIST"
            )

            pickFirsts += setOf(
                "META-INF/services/javax.xml.stream.XMLInputFactory",
                "META-INF/services/javax.xml.stream.XMLOutputFactory",
                "META-INF/services/javax.xml.stream.XMLEventFactory",
                "META-INF/services/org.codehaus.stax2.validation.XMLValidationSchemaFactory.DTD",
                "META-INF/services/org.codehaus.stax2.validation.XMLValidationSchemaFactory.RELAXNG",
                "META-INF/services/org.codehaus.stax2.validation.XMLValidationSchemaFactory.W3C"
            )
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // ===== ROOM 3.0 =====
    val room_version = "3.0.1"
    implementation("androidx.room3:room3-runtime:$room_version")
    implementation("androidx.room3:room3-ktx:$room_version")
    ksp("androidx.room3:room3-compiler:$room_version")

    // ===== COMPOSE & UI =====
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.19.0")

    // ===== LIFECYCLE =====
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    // ===== PRINTER & BARCODE =====
    implementation("com.github.DantSu:ESCPOS-ThermalPrinter-Android:3.4.0")

    // ===== CAMERA =====
    val cameraxVersion = "1.7.0-alpha02"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // ===== ML KIT =====
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // ===== EXCEL & XML =====
    implementation("org.dhatim:fastexcel:0.20.2")
    implementation("org.dhatim:fastexcel-reader:0.20.2")
    implementation("com.fasterxml:aalto-xml:1.4.0")
    implementation("javax.xml.stream:stax-api:1.0-2")

    // ===== TESTING =====
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.14.11")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    androidTestImplementation("androidx.room:room-testing:2.8.4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
}
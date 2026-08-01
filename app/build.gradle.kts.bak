plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("androidx.room")
}

room {
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
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }

    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
        getByName("debug").assets.srcDir("$projectDir/schemas")
    }

    packaging {
        resources {
            excludes += setOf(
                // ===== META-INF =====
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/MANIFEST.MF",
                "META-INF/*.version",
                "META-INF/INDEX.LIST",
                "META-INF/maven/**",
                "META-INF/versions/**",
                "META-INF/services/javax.xml.stream.*",
                "META-INF/services/org.apache.poi.sl.*",
                "META-INF/services/org.apache.poi.extractor.*",

                // ===== APACHE POI: POWERPOINT (TIDAK DIBUTUHKAN) =====
                "org/apache/poi/xslf/**",
                "org/apache/poi/sl/**",
                "org/apache/poi/hslf/**",

                // ===== APACHE POI: WORD (TIDAK DIBUTUHKAN) =====
                "org/apache/poi/xwpf/**",
                "org/apache/poi/hwpf/**",

                // ===== APACHE POI: VISIO (TIDAK DIBUTUHKAN) =====
                "org/apache/poi/xdgf/**",

                // ===== APACHE POI: PUBLISHER (TIDAK DIBUTUHKAN) =====
                "org/apache/poi/hpbf/**",

                // ===== APACHE POI: OUTLOOK (TIDAK DIBUTUHKAN) =====
                "org/apache/poi/hmef/**",
                "org/apache/poi/hsmf/**",

                // ===== APACHE POI: DIAGRAM/CHART (TIDAK DIBUTUHKAN) =====
                "org/apache/poi/xddf/**",

                // ===== APACHE POI: DRAWING (TIDAK DIBUTUHKAN) =====
                "org/apache/poi/xssf/usermodel/XSSFChart*",
                "org/apache/poi/xssf/usermodel/XSSFDrawing*",
                "org/apache/poi/xssf/usermodel/XSSFPicture*",
                "org/apache/poi/xssf/usermodel/XSSFGraphicFrame*",

                // ===== SCHEMA: POWERPOINT (TIDAK DIBUTUHKAN) =====
                "org/openxmlformats/schemas/presentationml/**",

                // ===== SCHEMA: WORD (TIDAK DIBUTUHKAN) =====
                "org/openxmlformats/schemas/wordprocessingml/**",

                // ===== SCHEMA: DRAWING (TIDAK DIBUTUHKAN) =====
                "org/openxmlformats/schemas/drawingml/**",

                // ===== LOG4J (TIDAK BERGUNA DI ANDROID) =====
                "org/apache/logging/**",
                "log4j2.xml",
                "Log4j-config.xsd",
                "Log4j-events.xsd",
                "Log4j-levels.xsd",
                "META-INF/services/org.apache.logging.*",
                "META-INF/log4j-provider.properties",

                // ===== BOUNCY CASTLE (CRYPTO, TIDAK PERLU UNTUK EXCEL BIASA) =====
                "org/bouncycastle/**",

                // ===== COMMONS-MATH3 (TIDAK DIPAKAI) =====
                "org/apache/commons/math3/**",

                // ===== DOKUMENTASI & FILE TIDAK PERLU =====
                "**/*.md",
                "**/*.txt",
                "**/*.html",
                "**/*.css",
                "**/*.dtd",
                "license/**",
                "LICENSE/**"
            )
        }
    }
}

dependencies {
    // ===== COMPOSE =====
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.13.1")

    // ===== LIFECYCLE =====
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // ===== COROUTINES =====
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // ===== ROOM DATABASE =====
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // ===== THERMAL PRINTER =====
    implementation("com.github.DantSu:ESCPOS-ThermalPrinter-Android:3.4.0")

    // ===== CAMERAX =====
    val cameraxVersion = "1.4.1"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // ===== ML KIT BARCODE =====
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // =====================================================
    //  APACHE POI - OPTIMASI UNTUK EXCEL SAJA
    //
    //  Perubahan:
    //  - poi-ooxml → poi-ooxml-lite (hemat ~5-8 MB schema)
    //  - Exclude dependency yang tidak perlu di Android
    //
    //  poi-ooxml-lite HANYA berisi schema spreadsheet
    //  Tanpa schema PowerPoint, Word, Visio, dll
    //
    //  ExcelManager.kt tetap kompatibel karena:
    //  - SXSSFWorkbook ada di poi-ooxml-lite ✅
    //  - WorkbookFactory ada di poi-ooxml-lite ✅
    //  - DataFormatter ada di poi (core) ✅
    // =====================================================
    implementation("org.apache.poi:poi:5.5.1") {
        // Exclude yang tidak perlu
        exclude(group = "org.apache.logging.log4j")
        exclude(group = "org.apache.commons", module = "commons-math3")
    }
    implementation("org.apache.poi:poi-ooxml-lite:5.5.1") {
        exclude(group = "org.apache.logging.log4j")
        exclude(group = "org.apache.commons", module = "commons-math3")
        exclude(group = "org.bouncycastle")
        exclude(group = "org.apache.santuario")
        exclude(group = "org.apache.pdfbox")
        exclude(group = "de.rototor.pdfbox")
        exclude(group = "com.github.javaparser")
    }

    // Exclude global untuk dependency transitif
    configurations.configureEach {
        exclude(group = "org.apache.logging.log4j")
        exclude(group = "org.apache.commons", module = "commons-math3")
        exclude(group = "org.bouncycastle")
        exclude(group = "org.apache.santuario")
        exclude(group = "de.rototor.pdfbox")
        exclude(group = "org.apache.pdfbox")
        exclude(group = "com.github.javaparser")
    }

    // ===== TESTING =====
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
}
// Top-level build file di mana Anda dapat menambahkan opsi konfigurasi yang berlaku untuk semua sub-proyek/modul.
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("com.google.dagger.hilt.android") version "2.52" apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}

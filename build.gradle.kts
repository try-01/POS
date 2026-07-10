// Build script root — deklarasi plugin (versi), tidak di-apply di sini.
plugins {
    id("com.android.application") version "8.7.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.27" apply false
    // Plugin Room: wajib versi sama dengan runtime Room (2.6.1) agar
    // konfigurasi schemaDirectory dikenali & tidak terjadi mismatch.
    id("androidx.room") version "2.6.1" apply false
}

# =========================================================================
# 1. ATURAN UMUM ANDROID & KOTLIN (Optimasi & Struktur)
# =========================================================================
# Mempertahankan metadata penting untuk debugging, refleksi, dan pelacakan crash
-keepattributes Signature, InnerClasses, EnclosingMethod, Deprecated, SourceFile, LineNumberTable
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, RuntimeVisibleTypeAnnotations, AnnotationDefault

# Menghindari error kompilasi karena kelas metadata internal Kotlin
-keep class kotlin.Metadata { *; }

# Mempertahankan nama file asli untuk laporan crash yang akurat di Firebase Crashlytics
-renamesourcefileattribute SourceFile

# =========================================================================
# 2. MODEL DATABASE & REPOSITORY DTO (Spesifik POS Offline)
# =========================================================================
# --- [Grup 1] Model Database (Entity Room) ---
# Memastikan tabel SQLite (ProductEntity, TransactionEntity, dll) tidak diacak namanya
-keep class com.pos.offline.data.local.entity.** { *; }
-keep @androidx.room.Entity class * { *; }

# --- [Grup 2] DTO Hasil Repository ---
# Memastikan kelas pembungkus hasil query (CheckoutResult, ShiftSummary, dll) tetap utuh
-keep class com.pos.offline.data.repository.** { *; }

# =========================================================================
# 3. MODEL STATE UI & FORMS (Spesifik Jetpack Compose & ViewModel)
# =========================================================================
# --- [Grup 3] Model State UI ---
# Sangat penting jika State UI disimpan menggunakan SavedStateHandle / Parcelable
-keep class * implements android.os.Parcelable { *; }

# Menjaga semua kelas yang berakhiran UiState, FormState, SortOption, dll, 
# agar variabelnya tidak hilang saat dibaca oleh Jetpack Compose.
-keep class **.*UiState { *; }
-keep class **.*FormState { *; }
-keep class **.*SortOption { *; }
-keep class **.*Summary { *; }
-keep class **.*Result { *; }

# Aturan umum untuk ViewModel & Jetpack Compose
-keep class androidx.compose.** { *; }
-keepclasseswithmembers class * {
    @androidx.compose.runtime.Composable *;
}
-keep class * extends androidx.lifecycle.ViewModel { *; }

# =========================================================================
# 4. UTILLITAS & FITUR KHUSUS (Printer, Excel, dll)
# =========================================================================
# --- [Grup 4] Model Utilitas ---
# Menjaga kelancaran parsing Excel (ImportedProductRow, ExcelImportResult) 
# dan deteksi USB Printer (UsbDeviceInfo)
-keep class com.pos.offline.util.** { *; }

# Menjaga kelas pembungkus data cetak struk (ReceiptLine)
-keep class com.pos.offline.ui.receipt.** { *; }

# =========================================================================
# 5. KOTLIN COROUTINES (Asinkronus & Flow)
# =========================================================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.android.HandlerContext$FrameCallbackProvider {
    *** postFrameCallback(...);
}

# =========================================================================
# 6. APACHE POI & XMLBEANS (Modul Excel)
# =========================================================================
-keep class org.apache.poi.** { *; }
-keep class org.apache.poi.ooxml.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class com.microsoft.schemas.** { *; }
-keep class org.openxmlformats.** { *; }
-keep class schemaorg_apache_xmlbeans.** { *; }
-keep class org.etsi.** { *; }
-keep class org.w3.** { *; }

# Mengabaikan warning kelas opsional yang absen saat build rilis
-dontwarn aQute.bnd.annotation.**
-dontwarn com.github.luben.zstd.**
-dontwarn com.github.javaparser.**             # Solusi error missing class javaparser
-dontwarn edu.umd.cs.findbugs.annotations.**
-dontwarn java.awt.**
-dontwarn javax.**
-dontwarn net.sf.saxon.**
-dontwarn org.apache.batik.**
-dontwarn org.apache.commons.compress.compressors.xz.XZCompressorInputStream
-dontwarn org.apache.xmlbeans.**               # Solusi error missing class xmlbeans
-dontwarn org.jspecify.annotations.**
-dontwarn org.osgi.framework.**
-dontwarn org.tukaani.xz.**
# =========================================================================
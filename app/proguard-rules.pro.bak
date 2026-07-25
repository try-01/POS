# =========================================================================
# 1. ATURAN UMUM ANDROID & KOTLIN
# =========================================================================
-keepattributes Signature, InnerClasses, EnclosingMethod, Deprecated, SourceFile, LineNumberTable
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, RuntimeVisibleTypeAnnotations, AnnotationDefault
-keep class kotlin.Metadata { *; }
-renamesourcefileattribute SourceFile

# =========================================================================
# 2. MODEL DATABASE & REPOSITORY
# =========================================================================
-keep class com.pos.offline.data.local.entity.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep class com.pos.offline.data.repository.** { *; }

# =========================================================================
# 3. MODEL STATE UI & COMPOSE
# =========================================================================
-keep class * implements android.os.Parcelable { *; }
-keep class **.*UiState { *; }
-keep class **.*FormState { *; }
-keep class **.*SortOption { *; }
-keep class **.*Summary { *; }
-keep class **.*Result { *; }
-keep class androidx.compose.** { *; }
-keepclasseswithmembers class * {
    @androidx.compose.runtime.Composable *;
}
-keep class * extends androidx.lifecycle.ViewModel { *; }

# =========================================================================
# 4. UTILLITAS & FITUR KHUSUS
# =========================================================================
-keep class com.pos.offline.util.** { *; }
-keep class com.pos.offline.ui.receipt.** { *; }

# =========================================================================
# 5. KOTLIN COROUTINES
# =========================================================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.android.HandlerContext$FrameCallbackProvider {
    *** postFrameCallback(...);
}

# =========================================================================
# 6. CAMERAX & ML KIT (BARCODE SCANNER TETAP AMAN)
# =========================================================================
-keep class androidx.camera.** { *; }
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_barcode.** { *; }
-keepclassmembers class androidx.camera.** { *; }
-dontwarn androidx.camera.**
-dontwarn com.google.mlkit.**

# =========================================================================
# 7. APACHE POI & XMLBEANS (Versi Dioptimalkan untuk Ukuran APK)
# =========================================================================
# [PENTING] Aturan ini TIDAK menambah ukuran APK. Hanya untuk menjaga struktur
# folder resource (.xsb) XMLBeans agar tidak crash saat di-load
-keepdirectories org.apache.poi.**
-keepdirectories org.apache.xmlbeans.**
-keepdirectories org.openxmlformats.**
-keepdirectories schemaorg_apache_xmlbeans.**
-keepdirectories com.microsoft.schemas.**

# [PENTING] Menjaga nama package agar XMLBeans tidak mencari ke folder "M3"
-keeppackagenames org.apache.poi.**
-keeppackagenames org.apache.xmlbeans.**
-keeppackagenames org.openxmlformats.**
-keeppackagenames schemaorg_apache_xmlbeans.**
-keeppackagenames com.microsoft.schemas.**

# --- SPESIFIK HANYA MENYIMPAN CLASS EXCEL (Mengecilkan ukuran APK) ---
-keep class org.apache.poi.ooxml.** { *; }
-keep class org.apache.poi.ss.** { *; }
-keep class org.apache.poi.hssf.** { *; }
-keep class org.apache.poi.xssf.** { *; }
-keep class org.apache.poi.poifs.** { *; }
-keep class org.apache.poi.util.** { *; }
-keep class org.apache.poi.openxml4j.** { *; }
-keep class org.apache.poi.common.** { *; }
-keep class org.apache.poi.ddf.** { *; }

# --- Komponen XMLBeans & Format Bawaan ---
-keep class org.apache.xmlbeans.** { *; }
-keep class com.microsoft.schemas.** { *; }
-keep class org.openxmlformats.** { *; }
-keep class schemaorg_apache_xmlbeans.** { *; }
-keep class org.etsi.** { *; }
-keep class org.w3.** { *; }

# --- Mengabaikan Warning Library Luar ---
-dontwarn com.microsoft.schemas.**
-dontwarn org.openxmlformats.**
-dontwarn org.w3c.dom.**
-dontwarn org.w3.**
-dontwarn org.bouncycastle.**
-dontwarn org.apache.xml.security.**
-dontwarn org.apache.jcp.xml.dsig.**
-dontwarn org.w3.x2000.x09.xmldsig.**
-dontwarn org.etsi.**
-dontwarn org.ietf.jgss.**
-dontwarn org.apache.pdfbox.**
-dontwarn de.rototor.pdfbox.**
-dontwarn java.awt.**
-dontwarn javax.**
-dontwarn java.nio.file.**
-dontwarn java.lang.invoke.**
-dontwarn org.apache.jcp.**
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn schemaorg_apache_xmlbeans.**
-dontwarn aQute.bnd.annotation.**
-dontwarn com.github.luben.zstd.**
-dontwarn com.github.javaparser.**
-dontwarn edu.umd.cs.findbugs.annotations.**
-dontwarn net.sf.saxon.**
-dontwarn org.apache.batik.**
-dontwarn org.apache.commons.compress.compressors.xz.XZCompressorInputStream
-dontwarn org.jspecify.annotations.**
-dontwarn org.osgi.framework.**
-dontwarn org.tukaani.xz.**
-dontwarn com.microsoft.**
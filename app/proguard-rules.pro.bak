# =====================================================================
# ATURAN BAWAAN & ARSITEKTUR APLIKASI (DI-PRESERVE)
# =====================================================================
-keepattributes Signature, InnerClasses, EnclosingMethod, Deprecated, SourceFile, LineNumberTable
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, RuntimeVisibleTypeAnnotations, AnnotationDefault
-keep class kotlin.Metadata { *; }
-renamesourcefileattribute SourceFile

# Database Room
-keep class com.pos.offline.data.local.entity.** { *; }
-keep @androidx.room.Entity class * { *; }

# Data & Repository
-keep class com.pos.offline.data.repository.** { *; }
-keep class * implements android.os.Parcelable { *; }
-keep class **.*UiState { *; }
-keep class **.*FormState { *; }
-keep class **.*SortOption { *; }
-keep class **.*Summary { *; }
-keep class **.*Result { *; }

# Jetpack Compose & Lifecycle
-keep class androidx.compose.** { *; }
-keepclasseswithmembers class * {
    @androidx.compose.runtime.Composable *;
}
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Package Internal Aplikasi
-keep class com.pos.offline.util.** { *; }
-keep class com.pos.offline.ui.receipt.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.android.HandlerContext$FrameCallbackProvider {
    *** postFrameCallback(...);
}

# =====================================================================
# OPTIMALISASI APACHE POI 5.5.1 (HANYA SIMPAN EXCEL .XLS & .XLSX)
# =====================================================================

# [PERBAIKAN UTAMA] Hapus "-keep class org.apache.poi.** { *; }" agar R8 bisa memotong Word/PPT.
# Sebagai gantinya, kita HANYA mengunci package Core, Excel Lama (.xls), dan Excel Baru (.xlsx):
-keep class org.apache.poi.ss.usermodel.** { *; }
-keep class org.apache.poi.hssf.usermodel.** { *; }
-keep class org.apache.poi.xssf.usermodel.** { *; }
-keep class org.apache.poi.xssf.streaming.** { *; }
-keep class org.apache.poi.ss.formula.** { *; }
-keep class org.apache.poi.poifs.filesystem.** { *; }
-keep class org.apache.poi.util.** { *; }

# Hanya simpan skema XML yang dibutuhkan oleh Spreadsheet (Excel)
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.** { *; }

# Tetap pertahankan engine dasar XMLBeans pembaca file kompresi openxml
-keep class org.apache.xmlbeans.** { *; }
-keep class schemaorg_apache_xmlbeans.** { *; }

# Sisanya (Word, PPT, Grafis, dll) TIDAK ditaruh di "-keep" agar otomatis DIBUANG oleh R8.

# =====================================================================
# WARNING SUPPRESSION (TETAP DIPERTAHANKAN & DIOPTIMALKAN)
# =====================================================================
-dontwarn com.microsoft.schemas.**
-dontwarn org.openxmlformats.schemas.drawingml.**
-dontwarn org.openxmlformats.**
-dontwarn org.w3c.dom.events.**
-dontwarn org.w3c.dom.svg.**
-dontwarn org.w3c.dom.traversal.**
-dontwarn org.w3.**
-dontwarn org.bouncycastle.**
-dontwarn org.apache.xml.security.**
-dontwarn org.apache.jcp.xml.dsig.**
-dontwarn org.w3.x2000.x09.xmldsig.**
-dontwarn org.etsi.uri.x01903.v13.**
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
-dontwarn org.etsi.**
-dontwarn com.microsoft.**

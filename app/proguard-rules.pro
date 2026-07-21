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
# Memastikan tabel SQLite (ProductEntity, TransactionEntity, dll) tidak diacak namanya
-keep class com.pos.offline.data.local.entity.** { *; }
-keep @androidx.room.Entity class * { *; }

# Memastikan kelas pembungkus hasil query (CheckoutResult, ShiftSummary, dll) tetap utuh
-keep class com.pos.offline.data.repository.** { *; }

# =========================================================================
# 3. MODEL STATE UI & FORMS (Spesifik Jetpack Compose & ViewModel)
# =========================================================================
# Sangat penting jika State UI disimpan menggunakan SavedStateHandle / Parcelable
-keep class * implements android.os.Parcelable { *; }

# Menjaga semua kelas yang berakhiran UiState, FormState, SortOption, dll
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
# 6. APACHE POI, XMLBEANS, & KETERGANTUNGANNYA (Fokus Excel Saja)
# =========================================================================
# Mempertahankan paket utama dan utilitas dasar
-keep class org.apache.poi.* { *; }
-keep class org.apache.poi.util.** { *; }
-keep class org.apache.poi.poifs.** { *; }

# HANYA mempertahankan modul Spreadsheet/Excel, mengabaikan PowerPoint & Word
-keep class org.apache.poi.ss.** { *; }
-keep class org.apache.poi.xssf.** { *; }
-keep class org.apache.poi.hssf.** { *; }

# Ketergantungan XML Excel
-keep class org.apache.poi.ooxml.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class com.microsoft.schemas.** { *; }
-keep class org.openxmlformats.** { *; }
-keep class schemaorg_apache_xmlbeans.** { *; }
-keep class org.etsi.** { *; }
-keep class org.w3.** { *; }

# =========================================================================
# 7. MENGABAIKAN ERROR KELAS YANG HILANG (Missing Classes)
# =========================================================================
# Skema Dokumen Microsoft & OpenXML
-dontwarn com.microsoft.schemas.**
-dontwarn org.openxmlformats.schemas.drawingml.**
-dontwarn org.openxmlformats.**

# Pemrosesan W3C DOM (XML & Vektor SVG Desktop)
-dontwarn org.w3c.dom.events.**
-dontwarn org.w3c.dom.svg.**
-dontwarn org.w3c.dom.traversal.**
-dontwarn org.w3.**

# Kriptografi, Tanda Tangan Digital, & Keamanan (BouncyCastle & JCP)
-dontwarn org.bouncycastle.**
-dontwarn org.apache.xml.security.**
-dontwarn org.apache.jcp.xml.dsig.**
-dontwarn org.w3.x2000.x09.xmldsig.**
-dontwarn org.etsi.uri.x01903.v13.**

# Java GSS (Keamanan & Autentikasi Desktop)
-dontwarn org.ietf.jgss.**

# Library PDF (PDFBox & Rototor)
-dontwarn org.apache.pdfbox.**
-dontwarn de.rototor.pdfbox.**

# Modul Desktop Java Standar (AWT, NIO, dll)
-dontwarn java.awt.**
-dontwarn javax.**
-dontwarn java.nio.file.**
-dontwarn java.lang.invoke.**
-dontwarn org.apache.jcp.**

# Peringatan Tambahan POI / Eksternal Lainnya
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
# =========================================================================

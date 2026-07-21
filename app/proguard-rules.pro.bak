# =========================================================================
# 1. ATURAN UMUM ANDROID & KOTLIN (Optimasi & Struktur)
# =========================================================================
-keepattributes Signature, InnerClasses, EnclosingMethod, Deprecated, SourceFile, LineNumberTable
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, RuntimeVisibleTypeAnnotations, AnnotationDefault
-keep class kotlin.Metadata { *; }
-renamesourcefileattribute SourceFile

# =========================================================================
# 2. MODEL DATABASE & REPOSITORY DTO (Spesifik POS Offline)
# =========================================================================
-keep class com.pos.offline.data.local.entity.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep class com.pos.offline.data.repository.** { *; }

# =========================================================================
# 3. MODEL STATE UI & FORMS (Spesifik Jetpack Compose & ViewModel)
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
# 4. UTILLITAS & FITUR KHUSUS (Printer, Excel, dll)
# =========================================================================
-keep class com.pos.offline.util.** { *; }
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
# 6. APACHE POI (FOKUS EKSTREM PADA EXCEL UNTUK UKURAN APK KECIL)
# =========================================================================
# Hanya mempertahankan modul Spreadsheet/Excel dan utilitas dasarnya
-keep class org.apache.poi.ss.** { *; }
-keep class org.apache.poi.xssf.** { *; }
-keep class org.apache.poi.hssf.** { *; }
-keep class org.apache.poi.poifs.** { *; }
-keep class org.apache.poi.util.** { *; }

# Mempertahankan XMLBeans dan format OpenXML khusus Spreadsheet (Excel)
# Kita TIDAK LAGI menahan paket org.openxmlformats.** secara keseluruhan agar fitur Word/PowerPoint terbuang.
-keep class org.apache.xmlbeans.** { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.** { *; }
-keep class org.openxmlformats.schemas.officeDocument.x2006.sharedTypes.** { *; }
-keep class schemaorg_apache_xmlbeans.** { *; }

# =========================================================================
# 7. MENGABAIKAN ERROR & MEMBUANG MODUL YANG TIDAK DIPAKAI
# =========================================================================

# --- [A] MEMBUANG MODUL SELAIN EXCEL SECARA PAKSA ---
-dontwarn org.apache.poi.xslf.**     # Buang PowerPoint
-dontwarn org.apache.poi.xwpf.**     # Buang Word
-dontwarn org.apache.poi.hdgf.**     # Buang Visio Lama
-dontwarn org.apache.poi.xdgf.**     # Buang Visio Baru
-dontwarn org.apache.poi.hsmf.**     # Buang Outlook
-dontwarn org.apache.poi.hpbf.**     # Buang Publisher
-dontwarn org.apache.poi.hslf.**     # Buang PowerPoint Lama
-dontwarn org.apache.poi.hwpf.**     # Buang Word Lama

# --- [B] Skema Dokumen Microsoft Umum ---
-dontwarn com.microsoft.schemas.**
-dontwarn org.openxmlformats.schemas.drawingml.**
-dontwarn org.openxmlformats.schemas.wordprocessingml.**
-dontwarn org.openxmlformats.schemas.presentationml.**
-dontwarn org.openxmlformats.**

# --- [C] Pemrosesan W3C DOM (XML & Vektor SVG Desktop) ---
-dontwarn org.w3c.dom.events.**
-dontwarn org.w3c.dom.svg.**
-dontwarn org.w3c.dom.traversal.**
-dontwarn org.w3.**

# --- [D] Kriptografi, Tanda Tangan Digital, & Keamanan ---
-dontwarn org.bouncycastle.**
-dontwarn org.apache.xml.security.**
-dontwarn org.apache.jcp.xml.dsig.**
-dontwarn org.w3.x2000.x09.xmldsig.**
-dontwarn org.etsi.uri.x01903.v13.**

# --- [E] Library Desktop Java (AWT, GSS, NIO, dll) ---
-dontwarn org.ietf.jgss.**
-dontwarn java.awt.**
-dontwarn javax.**
-dontwarn java.nio.file.**
-dontwarn java.lang.invoke.**
-dontwarn org.apache.jcp.**

# --- [F] Library PDF (Bila ada sisa) ---
-dontwarn org.apache.pdfbox.**
-dontwarn de.rototor.pdfbox.**

# --- [G] Peringatan Tambahan Eksternal Lainnya ---
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

# =========================================================================
# TAMBAHAN: Mengabaikan sisa Missing Class Maven, Ant, & XML Resolver
# =========================================================================
-dontwarn com.sun.org.apache.xml.**
-dontwarn org.apache.maven.**
-dontwarn org.apache.tools.ant.**

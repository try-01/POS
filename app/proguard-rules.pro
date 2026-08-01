# ============================================================
#  PROGUARD / R8 RULES — POS OFFLINE
#  Optimized for: Compose + Room + POI (Excel) + CameraX
#                 + ML Kit Barcode + ESC/POS Printer
#
#  Terakhir diupdate: 2025
# ============================================================


# =========================================================
# 1. ATURAN UMUM & KOTLIN
# =========================================================

-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**


# =========================================================
# 2. HAPUS LOG & DEBUG CODE DI RELEASE BUILD
# =========================================================

# Hapus Log.v, Log.d, Log.i (pertahankan Log.w & Log.e)
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Hapus println/print (debug statements)
-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}

# Hapus Kotlin null-check parameter names
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkNotNull(...);
    public static void checkNotNullParameter(...);
    public static void checkParameterIsNotNull(...);
    public static void checkNotNullExpressionValue(...);
    public static void checkExpressionValueIsNotNull(...);
    public static void checkReturnedValueIsNotNull(...);
}

# Hapus Compose source information markers
-assumenosideeffects class androidx.compose.runtime.ComposerKt {
    void sourceInformation(...);
    void sourceInformationMarkerStart(...);
    void sourceInformationMarkerEnd(...);
    void traceEventStart(...);
    void traceEventEnd();
}


# =========================================================
# 3. ROOM DATABASE
#    Package: com.pos.offline.data.local
# =========================================================

# Entity classes
-keep @androidx.room.Entity class * { *; }

# DAO interfaces
-keep @androidx.room.Dao interface * { *; }

# Database class
-keep @androidx.room.Database class * { *; }

# Annotations
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}

# TypeConverters
-keep class * {
    @androidx.room.TypeConverter <methods>;
}

# Keep spesifik: Room entity & enum di project ini
-keep class com.pos.offline.data.local.entity.** { *; }
-keep class com.pos.offline.data.local.PosDatabase { *; }
-keep class com.pos.offline.data.local.PosDatabase_Impl { *; }
-keep class com.pos.offline.data.local.dao.** { *; }

# Room Migrations
-keep class com.pos.offline.data.local.Migrations { *; }
-keep class com.pos.offline.data.local.MigrationsKt { *; }


# =========================================================
# 4. APACHE POI — KHUSUS EXCEL (SPREADSHEET)
#
#    ExcelManager.kt menggunakan:
#    - SXSSFWorkbook (export .xlsx via streaming)
#    - WorkbookFactory.create() (import, support .xls & .xlsx)
#    - DataFormatter (format cell values)
#
#    WorkbookFactory menggunakan ServiceLoader pattern,
#    jadi kita perlu keep beberapa service classes.
# =========================================================

# --- 4a. POI Core (ss = spreadsheet interface) ---
-keep class org.apache.poi.ss.** { *; }
-keep class org.apache.poi.util.** { *; }
-keep class org.apache.poi.common.** { *; }
-keep class org.apache.poi.poifs.** { *; }

# --- 4b. XSSF & SXSSF (.xlsx format) ---
-keep class org.apache.poi.xssf.** { *; }
-keep class org.apache.poi.xssf.streaming.** { *; }

# --- 4c. HSSF (.xls format, dibutuhkan oleh WorkbookFactory) ---
# WorkbookFactory.create() otomatis detect format.
# Jika user import file .xls, HSSF diperlukan.
-keep class org.apache.poi.hssf.** { *; }

# --- 4d. OpenXML4J (ZIP/package layer untuk .xlsx) ---
-keep class org.apache.poi.openxml4j.** { *; }
-keep class org.apache.poi.ooxml.** { *; }
-keep class org.apache.poi.ooxml.util.** { *; }

# --- 4e. XMLBeans (XML parsing layer) ---
-keep class org.apache.xmlbeans.** { *; }

# --- 4f. Spreadsheet Schemas (WAJIB untuk .xlsx) ---
-keep class org.openxmlformats.schemas.spreadsheetml.** { *; }
-keep class org.openxmlformats.schemas.officeDocument.** { *; }

# --- 4g. Schema support classes ---
-keep class schemaorg_apache_xmlbeans.** { *; }

# --- 4h. Pertahankan direktori schema (.xsb files) ---
#     Ini KRITIS: tanpa ini, POI crash saat baca .xlsx
-keeppackagenames org.apache.poi.**
-keeppackagenames org.apache.xmlbeans.**
-keeppackagenames org.openxmlformats.schemas.spreadsheetml.**
-keeppackagenames org.openxmlformats.schemas.officeDocument.**
-keeppackagenames schemaorg_apache_xmlbeans.**

-keepdirectories org.apache.poi.**
-keepdirectories org.apache.xmlbeans.**
-keepdirectories org.openxmlformats.**
-keepdirectories schemaorg_apache_xmlbeans.**

# --- 4i. ServiceLoader (WorkbookFactory discovery) ---
-keep class * implements org.apache.poi.ss.usermodel.WorkbookProvider { *; }
-keepnames class * implements org.apache.poi.ss.usermodel.WorkbookProvider


# =========================================================
# 5. ESCPOS THERMAL PRINTER
# =========================================================

-keep class com.dantsu.escposprinter.** { *; }
-keepclassmembers class com.dantsu.escposprinter.** { *; }


# =========================================================
# 6. CAMERAX
# =========================================================

-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**


# =========================================================
# 7. ML KIT BARCODE SCANNING
# =========================================================

-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_barcode.** { *; }
-dontwarn com.google.mlkit.**


# =========================================================
# 8. JETPACK COMPOSE
# =========================================================

-dontwarn androidx.compose.**


# =========================================================
# 9. ENUM (Semua enum harus dipertahankan)
#    DiscountType, PaymentMethod, TransactionStatus
# =========================================================

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}


# =========================================================
# 10. PARCELABLE & SERIALIZABLE
# =========================================================

-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
}


# =========================================================
# 11. KEEP SPESIFIK PROJECT
# =========================================================

# Data classes yang dipakai oleh ExcelManager
-keep class com.pos.offline.util.ImportedProductRow { *; }
-keep class com.pos.offline.util.ExcelImportResult { *; }
-keep class com.pos.offline.util.ExcelOutcome { *; }
-keep class com.pos.offline.util.ExcelOutcome$* { *; }
-keep class com.pos.offline.util.ExcelManager { *; }

# Backup & Restore (mungkin pakai reflection/file I/O)
-keep class com.pos.offline.data.backup.** { *; }


# =========================================================
# 12. DONTWARN — SUPPRESS WARNINGS
# =========================================================

# Java Desktop APIs (tidak ada di Android)
-dontwarn java.awt.**
-dontwarn javax.**
-dontwarn java.nio.file.**
-dontwarn java.lang.invoke.**

# Apache POI: Format yang tidak dipakai
-dontwarn org.apache.poi.sl.**
-dontwarn org.apache.poi.xslf.**
-dontwarn org.apache.poi.xwpf.**
-dontwarn org.apache.poi.hwpf.**
-dontwarn org.apache.poi.hslf.**
-dontwarn org.apache.poi.xdgf.**
-dontwarn org.apache.poi.hpbf.**
-dontwarn org.apache.poi.hmef.**
-dontwarn org.apache.poi.hsmf.**
-dontwarn org.apache.poi.xddf.**

# Schema yang tidak dipakai
-dontwarn org.openxmlformats.schemas.presentationml.**
-dontwarn org.openxmlformats.schemas.wordprocessingml.**
-dontwarn org.openxmlformats.schemas.drawingml.**
-dontwarn org.openxmlformats.schemas.spreadsheetml.**
-dontwarn org.openxmlformats.schemas.officeDocument.**

# External optional dependencies
-dontwarn org.w3c.dom.**
-dontwarn org.w3.**
-dontwarn org.bouncycastle.**
-dontwarn org.apache.xml.security.**
-dontwarn org.apache.jcp.**
-dontwarn org.apache.jcp.xml.dsig.**
-dontwarn org.w3.x2000.x09.xmldsig.**
-dontwarn org.etsi.**
-dontwarn org.ietf.jgss.**
-dontwarn org.apache.pdfbox.**
-dontwarn de.rototor.pdfbox.**
-dontwarn aQute.bnd.annotation.**
-dontwarn com.github.luben.zstd.**
-dontwarn com.github.javaparser.**
-dontwarn edu.umd.cs.findbugs.annotations.**
-dontwarn net.sf.saxon.**
-dontwarn org.apache.batik.**
-dontwarn org.apache.commons.compress.**
-dontwarn org.apache.commons.math3.**
-dontwarn org.apache.logging.**
-dontwarn org.apache.santuario.**
-dontwarn org.jspecify.annotations.**
-dontwarn org.osgi.framework.**
-dontwarn org.tukaani.xz.**
-dontwarn com.microsoft.**
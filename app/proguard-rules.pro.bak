# =========================================================
# 1. ATURAN UMUM & METADATA (Untuk Crashlytics/Loging)
# =========================================================
-keepattributes Signature, InnerClasses, EnclosingMethod, Deprecated, SourceFile, LineNumberTable
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, RuntimeVisibleTypeAnnotations, AnnotationDefault
-keep class kotlin.Metadata { *; }
-renamesourcefileattribute SourceFile

# =========================================================
# 2. ROOM DATABASE & PARCELABLE
# =========================================================
# Menjaga nama field Entity agar tidak diobfuscate (menghindari error KSP/Room saat mapping SQLite)
-keep @androidx.room.Entity class * { *; }
-keepclassmembers class * {
    @androidx.room.Ignore <fields>;
}

# Standar Android untuk objek Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# =========================================================
# 3. APACHE POI & XMLBEANS (OPTIMALISASI EXCEL)
# =========================================================
# HANYA keep skema XML yang di-load via Reflection oleh POI.
# Jangan pernah menge-keep org.apache.poi.** secara keseluruhan!
-keep class org.apache.xmlbeans.** { *; }
-keep class com.microsoft.schemas.** { *; }
-keep class org.openxmlformats.** { *; }
-keep class schemaorg_apache_xmlbeans.** { *; }

-keeppackagenames org.apache.xmlbeans.**
-keeppackagenames org.openxmlformats.**
-keeppackagenames schemaorg_apache_xmlbeans.**
-keeppackagenames com.microsoft.schemas.**

# =========================================================
# 4. ESCPOS THERMAL PRINTER
# =========================================================
-keep class com.dantsu.escposprinter.** { *; }

# =========================================================
# 5. DONTWARN (MENYEMBUNYIKAN WARNING DEPENDENSI JAVA DESKTOP)
# =========================================================
# Library POI dan MLKit sering mencari class Java Desktop (java.awt, javax.imageio)
# yang tidak ada di Android. Kita beri tahu ProGuard untuk mengabaikannya.
-dontwarn androidx.camera.**
-dontwarn com.google.mlkit.**
-dontwarn java.awt.**
-dontwarn javax.**
-dontwarn java.nio.file.**
-dontwarn java.lang.invoke.**
-dontwarn org.apache.jcp.**
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn schemaorg_apache_xmlbeans.**
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
-dontwarn aQute.bnd.annotation.**
-dontwarn com.github.luben.zstd.**
-dontwarn com.github.javaparser.**
-dontwarn edu.umd.cs.findbugs.annotations.**
-dontwarn net.sf.saxon.**
-dontwarn org.apache.batik.**
-dontwarn org.apache.commons.compress.**
-dontwarn org.jspecify.annotations.**
-dontwarn org.osgi.framework.**
-dontwarn org.tukaani.xz.**
-dontwarn com.microsoft.**
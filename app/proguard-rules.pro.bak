# =========================================================
# 1. ATURAN UMUM & METADATA
# =========================================================
-keepattributes Signature, InnerClasses, EnclosingMethod, Deprecated, SourceFile, LineNumberTable
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, RuntimeVisibleTypeAnnotations, AnnotationDefault
-keep class kotlin.Metadata { *; }
-renamesourcefileattribute SourceFile

# =========================================================
# 2. ROOM DATABASE & PARCELABLE
# =========================================================
-keep @androidx.room.Entity class * { *; }
-keepclassmembers class * {
    @androidx.room.Ignore <fields>;
}

-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# =========================================================
# 3. APACHE POI & XMLBEANS (PERBAIKAN CRASH W5/index.xsb)
# =========================================================
# 1. Cegah R8 mengubah nama paket (Package Repackaging/Obfuscation)
-keeppackagenames org.apache.poi.**
-keeppackagenames org.apache.xmlbeans.**
-keeppackagenames org.openxmlformats.**
-keeppackagenames schemaorg_apache_xmlbeans.**
-keeppackagenames com.microsoft.schemas.**

# 2. Pertahankan struktur folder direktori tempat file .xsb disimpan
-keepdirectories org.apache.poi.**
-keepdirectories org.apache.xmlbeans.**
-keepdirectories org.openxmlformats.**
-keepdirectories schemaorg_apache_xmlbeans.**
-keepdirectories com.microsoft.schemas.**

# 3. Keep penuh seluruh kelas POI & XMLBeans agar loader skema .xsb tidak rusak
-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class org.openxmlformats.** { *; }
-keep class schemaorg_apache_xmlbeans.** { *; }
-keep class com.microsoft.schemas.** { *; }
-keep class org.etsi.** { *; }
-keep class org.w3.** { *; }

# =========================================================
# 4. ESCPOS THERMAL PRINTER
# =========================================================
-keep class com.dantsu.escposprinter.** { *; }

# =========================================================
# 5. DONTWARN (SUPPRESS WARNINGS)
# =========================================================
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
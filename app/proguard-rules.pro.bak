# ================= Apache POI & XMLBeans =================
-keep class org.apache.poi.** { *; }
-keep class org.apache.poi.ooxml.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class com.microsoft.schemas.** { *; }
-keep class org.openxmlformats.** { *; }
-keep class schemaorg_apache_xmlbeans.** { *; }
-keep class org.etsi.** { *; }
-keep class org.w3.** { *; }

# Suppress warnings for missing classes that don't exist on Android
# (POI references AWT, SVG, Saxon, etc., which we never call at runtime)
-dontwarn aQute.bnd.annotation.**
-dontwarn com.github.luben.zstd.**
-dontwarn edu.umd.cs.findbugs.annotations.**
-dontwarn java.awt.**
-dontwarn javax.**
-dontwarn net.sf.saxon.**
-dontwarn org.apache.batik.**
-dontwarn org.apache.commons.compress.compressors.xz.XZCompressorInputStream
-dontwarn org.jspecify.annotations.**
-dontwarn org.osgi.framework.**
-dontwarn org.tukaani.xz.**

# Keep generic signatures for POI reflection
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, RuntimeVisibleTypeAnnotations
# =========================================================
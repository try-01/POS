# ============================================================
#  PROGUARD / R8 RULES — POS OFFLINE
#  Compose + Room + FastExcel 0.20.2 + CameraX
#  + ML Kit + ESC/POS Printer
# ============================================================


# =========================================================
# 1. ATURAN UMUM
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
# 2. HAPUS LOG & DEBUG
# =========================================================

-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}

-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkNotNull(...);
    public static void checkNotNullParameter(...);
    public static void checkParameterIsNotNull(...);
    public static void checkNotNullExpressionValue(...);
    public static void checkExpressionValueIsNotNull(...);
    public static void checkReturnedValueIsNotNull(...);
}

-assumenosideeffects class androidx.compose.runtime.ComposerKt {
    void sourceInformation(...);
    void sourceInformationMarkerStart(...);
    void sourceInformationMarkerEnd(...);
    void traceEventStart(...);
    void traceEventEnd();
}


# =========================================================
# 3. ROOM DATABASE
# =========================================================

-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class * { *; }

-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}

-keep class * {
    @androidx.room.TypeConverter <methods>;
}


# =========================================================
# 4. FASTEXCEL 0.20.2 + AALTO XML 1.4.0
#
#    Writer: menulis XML langsung (tidak butuh Aalto)
#    Reader: parsing XML via Aalto (WAJIB keep)
#    Aalto di-load via ServiceLoader/reflection
# =========================================================

# Aalto XML parser (WAJIB untuk import .xlsx)
-keep class com.fasterxml.aalto.** { *; }
-keep class com.fasterxml.core.** { *; }

# XMLInputFactory ServiceLoader
-keep class javax.xml.stream.XMLInputFactory { *; }
-keep class * implements javax.xml.stream.XMLInputFactory { *; }
-keep class * extends javax.xml.stream.XMLInputFactory { *; }

# FastExcel suppress warnings
-dontwarn org.dhatim.fastexcel.**
-dontwarn org.dhatim.fastexcel.reader.**
-dontwarn com.fasterxml.aalto.**
-dontwarn com.fasterxml.core.**


# =========================================================
# 5. ESCPOS THERMAL PRINTER
# =========================================================

-keep class com.dantsu.escposprinter.** { *; }


# =========================================================
# 6. CAMERAX & ML KIT
# =========================================================

-dontwarn androidx.camera.**
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_barcode.** { *; }
-dontwarn com.google.mlkit.**


# =========================================================
# 7. COMPOSE
# =========================================================

-dontwarn androidx.compose.**


# =========================================================
# 8. ENUM
# =========================================================

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}


# =========================================================
# 9. PARCELABLE & SERIALIZABLE
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
# 10. DONTWARN
# =========================================================

-dontwarn java.awt.**
-dontwarn javax.**
-dontwarn java.nio.file.**
-dontwarn java.lang.invoke.**
-dontwarn java.lang.reflect.AnnotatedType

# FastExcel optional compression dependencies
-dontwarn org.tukaani.xz.**
-dontwarn org.brotli.dec.**
-dontwarn org.objectweb.asm.**
-dontwarn com.github.luben.zstd.**
# ============================================================
#  PROGUARD / R8 RULES — POS OFFLINE
#  Compose + Room + FastExcel + CameraX + ML Kit + ESC/POS
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
# 4. FASTEXCEL
#    Sangat ringan, hampir tidak butuh rules khusus
# =========================================================

-dontwarn org.dhatim.fastexcel.**
-dontwarn org.dhatim.fastexcel.reader.**


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
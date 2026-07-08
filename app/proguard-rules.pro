# Keep Room generated code
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.example.posoffline.**$$serializer { *; }
-keepclassmembers class com.example.posoffline.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.posoffline.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep our entities (Room maps them by name)
-keep class com.example.posoffline.data.entity.** { *; }

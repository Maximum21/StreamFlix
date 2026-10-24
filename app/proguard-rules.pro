# R8 configuration. Libraries ship their own consumer rules (Retrofit, OkHttp, Room, Hilt,
# kotlinx.serialization, Firebase), so this file only adds what the app itself needs.

# Keep line numbers so Crashlytics stack traces can be deobfuscated with the uploaded mapping
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

# Retrofit reads generic return types and annotations of the service interface at runtime
-keepattributes Signature, Exceptions, *Annotation*, InnerClasses, EnclosingMethod
-keep interface com.asadraza.streamflix.core.network.api.TmdbApiService

# kotlinx.serialization looks up generated serializers by name for the DTOs it decodes
-keepclassmembers class com.asadraza.streamflix.**$$serializer { *; }
-keepclasseswithmembers class com.asadraza.streamflix.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Debug and verbose logging is removed from release builds, including the string building
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
-assumenosideeffects class timber.log.Timber {
    public static void v(...);
    public static void d(...);
}

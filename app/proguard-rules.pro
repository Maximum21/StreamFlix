#  ── kotlinx.serialization ──────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.asadraza.streamflix.**$$serializer { *; }

# Your DTOs, kept verbatim (names matter for JSON matching):
-keep class com.asadraza.streamflix.core.network.model.** { *; }

# ── Retrofit (builds your API via reflection) ──────────
-keepattributes Signature, Exceptions
-keep, allowobfuscation interface retrofit2.** { *; }
-keepclasseswithmembers, allowshrinking interface * {
    @retrofit2.http.* <methods>;
}

# ── Room entities (column names must survive) ──────────
-keep class com.asadraza.streamflix.core.database.entity.** { *; }

# ── Shared domain models crossing serialization ────────
-keep class com.asadraza.streamflix.core.model.** { *; }
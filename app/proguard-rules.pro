# ==============================================================
# Pozix ProGuard / R8 Rules
# Applied to both debug (minify=true) and release builds.
# ==============================================================

# Keep source file names and line numbers so crash stack traces
# are readable even in debug builds with minification enabled.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ==============================================================
# App Classes — keep everything in com.hkm.pozix intact
# ==============================================================
-keep class com.hkm.pozix.** { *; }
-keepclassmembers class com.hkm.pozix.** { *; }

# ==============================================================
# Kotlin
# ==============================================================
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings { <fields>; }

# ==============================================================
# Kotlinx Coroutines
# ==============================================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**

# ==============================================================
# Kotlinx Serialization
# ==============================================================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.hkm.pozix.**$$serializer { *; }
-keepclassmembers class com.hkm.pozix.** {
    *** Companion;
}
-keepclasseswithmembers class com.hkm.pozix.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ==============================================================
# OkHttp3
# ==============================================================
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# ==============================================================
# AndroidX DataStore
# ==============================================================
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ==============================================================
# Jetpack Compose
# ==============================================================
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ==============================================================
# AndroidX Lifecycle / ViewModel
# ==============================================================
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**

# ==============================================================
# AndroidX Navigation
# ==============================================================
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# PDFBox's optional JPEG2000 image decoder is not needed for text extraction.
# PDFBox handles its absence; do not suppress any other missing classes.
-dontwarn com.gemalto.jp2.JP2Decoder

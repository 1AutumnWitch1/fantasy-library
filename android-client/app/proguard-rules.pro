# kotlinx.serialization：保留 @Serializable 类与其序列化器
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit / OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault
-keep,allowshrinking,allowobfuscation interface retrofit2.Call
-keepclasseswithmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn javax.annotation.**

# Media3 / ExoPlayer
-dontwarn androidx.media3.**

# Coil
-dontwarn coil.**

# AndroidX Security（Tink）
-dontwarn com.google.crypto.tink.**
-keep class com.google.crypto.tink.** { *; }

# 保留行号便于排错
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ProGuard & R8 rules for Say It Android Application

# Keep Kotlinx Serialization models
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# Keep Compose models & preview
-keep class androidx.compose.** { *; }

# Keep data models used in reflection or serialization
-keep class com.example.sayit.domain.model.** { *; }
-keep class com.example.sayit.data.local.** { *; }
-keep class com.example.sayit.data.ai.** { *; }

# Keep Coroutines internals
-keepclassmembers class kotlinx.coroutines.** { *; }

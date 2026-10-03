# ============================================================================
# ProGuard Rules — Optik CRM Production Build
# ============================================================================

# --- Retrofit + OkHttp ---
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations

# --- Gson ---
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.stream.** { *; }
# Keep semua data model yang di-serialize/deserialize oleh Gson
-keep class com.optik.cengkareng.data.remote.model.** { *; }
-keep class com.optik.cengkareng.data.remote.request.** { *; }
-keep class com.optik.cengkareng.data.remote.response.** { *; }
-keep class com.optik.cengkareng.data.local.entity.** { *; }

# --- Room ---
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# --- MPAndroidChart ---
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# --- Hilt / Dagger ---
-dontwarn dagger.hilt.internal.**
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.** { *; }

# --- Coroutines ---
-dontwarn kotlinx.coroutines.**
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# --- Debugging: Preserve line numbers for crash reports ---
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Keep application entry points ---
-keep class com.optik.cengkareng.OptikApp { *; }
-keep class com.optik.cengkareng.MainActivity { *; }
-keep class com.optik.cengkareng.features.auth.presentation.LoginActivity { *; }
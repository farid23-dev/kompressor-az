# ─────────────────────────────────────────────────────────────
#  Kompressor.az — ProGuard / R8 rules
#  Keep these or your release build will crash at runtime.
# ─────────────────────────────────────────────────────────────

# ── Preserve useful stack traces ──────────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Kotlin ────────────────────────────────────────────────────
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlin.Lazy { *; }

# ── Coroutines ────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# ── Hilt / Dagger ─────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.android.AndroidEntryPoint *;
}

# ── Firebase / Firestore ───────────────────────────────────────
# Keep all data model classes used for Firestore serialization
-keep class az.kompressor.app.data.remote.dto.** { *; }
-keep class az.kompressor.app.domain.model.** { *; }

# Firebase internals
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ── Room ──────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.**

# ── Glide ─────────────────────────────────────────────────────
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-dontwarn com.bumptech.glide.**

# ── Retrofit + OkHttp ─────────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-keep class okio.** { *; }
-dontwarn okio.**

# ── Navigation Safe Args (generated classes) ──────────────────
-keep class az.kompressor.app.**Directions { *; }
-keep class az.kompressor.app.**Args { *; }

# ── ViewBinding ───────────────────────────────────────────────
-keep class az.kompressor.app.databinding.** { *; }

# ── General Android ───────────────────────────────────────────
-keepclassmembers class * extends android.app.Activity { public void *(android.view.View); }
-keep class * extends androidx.fragment.app.Fragment { *; }
-keepclassmembers enum * { public static **[] values(); public static ** valueOf(java.lang.String); }
-keep class * implements android.os.Parcelable { public static final android.os.Parcelable$Creator *; }
-keepclassmembers class **.R$* { public static <fields>; }

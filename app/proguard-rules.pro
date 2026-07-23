# Video Player Pro — ProGuard / R8 rules for release builds.

# --- Media3 / ExoPlayer ---
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# --- Room ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-keep class com.example.videomax.data.local.db.entity.** { *; }

# --- Hilt ---
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# --- Coil ---
-dontwarn coil.**

# --- Kotlin Serialization / Data classes used by Room ---
-keep class com.example.videomax.domain.model.** { *; }

# --- Keep data classes used in Gson/serialization (blacklist encoding) ---
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# --- Paging ---
-keep class * extends androidx.paging.PagingSource
-keepclassmembers class * {
    android.content.Context <init>(...);
}

# --- DataStore ---
-dontwarn androidx.datastore.**

# --- Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# --- App-specific: keep ExoPlayer listener methods and MediaItem tags ---
-keepclassmembers class * extends androidx.media3.common.Player$Listener {
    public void on*(...);
}
-keepclassmembers class * {
    ** tag;
}

# --- Enum safety ---
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

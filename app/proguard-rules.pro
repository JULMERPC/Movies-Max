# Video Player Pro — ProGuard / R8 rules for release builds.

# ============================================================
# ANDROIDX / JETPACK
# ============================================================

# --- Media3 / ExoPlayer ---
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# --- Room (entities + DAOs + DB + PagingSource) ---
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
    @androidx.room.* <fields>;
}
-keep class com.puma.videomax.data.local.db.entity.** { *; }
-keep class com.puma.videomax.data.local.db.dao.** { *; }

# --- Navigation Compose ---
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# --- Paging ---
-keep class * extends androidx.paging.PagingSource { *; }
-keepclassmembers class * extends androidx.paging.PagingSource {
    public <methods>;
}

# ============================================================
# HILT / DAGGER — covers ALL generated classes
# ============================================================
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keep class * extends dagger.hilt.android.components.ViewModelComponent { *; }
-keep class * extends dagger.hilt.android.internal.lifecycle.HiltViewModelFactory { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$ActivityContextWrapper { *; }

# --- CRITICAL: Keep ALL Hilt-generated class names (KSP variant) ---
-keep class **._HiltComponents { *; }
-keep class **._HiltViewModel { *; }
-keep class **._Factory { *; }
-keep class **._MembersInjector { *; }
-dontwarn dagger.hilt.**

# ============================================================
# APP-SPECIFIC: domain, mapper, usecase, util
# ============================================================
-keep class com.puma.videomax.domain.model.** { *; }
-keep class com.puma.videomax.data.mapper.** { *; }
-keep class com.puma.videomax.domain.usecase.** { *; }
-keep class com.puma.videomax.util.** { *; }
-keep class com.puma.videomax.presentation.player.PlaybackQueue { *; }
-keep class com.puma.videomax.data.local.mediastore.** { *; }

# ============================================================
# COIL
# ============================================================
-keep class coil.** { *; }
-dontwarn coil.**

# ============================================================
# DATASTORE
# ============================================================
-dontwarn androidx.datastore.**

# ============================================================
# COROUTINES
# ============================================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { }
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ============================================================
# ENUMS — safety for valueOf() / name()
# ============================================================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public final int ordinal();
    public final java.lang.String name();
}
-optimizations !class/unboxing/**

# ============================================================
# KOTLIN — keep metadata, lambdas, when-mappings
# ============================================================
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keep class kotlin.jvm.internal.** { *; }
-dontwarn kotlin.**

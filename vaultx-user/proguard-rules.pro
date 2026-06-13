# ── Firebase / Firestore ───────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# ── Room ──────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keepclassmembers class * extends androidx.room.RoomDatabase {
    abstract ** **Dao();
}

# ── SQLCipher ─────────────────────────────────────────────────────────────────
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

# ── Kotlin Coroutines ─────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ── Hilt ──────────────────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class *

# ── Serialization (for AccountEntry JSON) ────────────────────────────────────
-keep @kotlinx.serialization.Serializable class *

# ── BuildConfig ───────────────────────────────────────────────────────────────
-keep class com.vaultx.user.BuildConfig { *; }

# ── Security: prevent reflection-based key extraction ─────────────────────────
-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }
-keep class android.security.keystore.** { *; }

# General
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

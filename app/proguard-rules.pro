# ProGuard Rules — KasirPOS
# Optimasi: shrink + obfuscate + optimize untuk APK minim

# ── Room ───────────────────────────────────────────────────────
-keep class com.example.kasirpos.data.local.entity.** { *; }

# ── Coroutines ─────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ── Compose ────────────────────────────────────────────────────
-dontwarn androidx.compose.**

# ── Bluetooth (reflection oleh Android OS) ────────────────────
-keep class android.bluetooth.** { *; }

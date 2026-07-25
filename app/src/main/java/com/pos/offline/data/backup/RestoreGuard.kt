package com.pos.offline.data.backup

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Flag global penanda window kritis restore database sedang berlangsung.
 *
 * Dipakai oleh dua sisi:
 * - [com.pos.offline.data.di.PosApplication] (uncaught exception handler):
 *   mendeteksi crash yang disebabkan Flow/koleksi latar belakang milik
 *   ViewModel LAIN (scope-nya AppRoot, bukan per-tab) yang menembak query ke
 *   koneksi Room tepat setelah ditutup selama proses restore, lalu mengubahnya
 *   menjadi restart terkendali alih-alih dialog crash sistem.
 * - AppRoot (MainActivity): menampilkan overlay blocking PENUH APLIKASI
 *   (bukan hanya di SettingsScreen) selama window ini berlangsung.
 *
 * Sengaja TIDAK di-reset (end()) pada jalur sukses/gagal-wajib-restart —
 * karena proses akan segera dimatikan oleh [BackupManager.restartApp], flag
 * yang tetap true sampai proses benar-benar mati justru diinginkan: kalau ada
 * crash residual tepat sebelum restart selesai, tetap dialihkan ke restart,
 * bukan dibiarkan menampilkan crash dialog.
 */
object RestoreGuard {
    private val _inProgress = AtomicBoolean(false)

    val isInProgress: Boolean get() = _inProgress.get()

    fun begin() { _inProgress.set(true) }

    /** Hanya dipanggil pada jalur yang TERBUKTI aman tanpa restart
     * (mis. RestoreOutcome.InvalidFile — koneksi Room tidak pernah disentuh). */
    fun end() { _inProgress.set(false) }
}

package com.kasirku.pos

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Entry point Hilt untuk seluruh aplikasi.
 * Tidak ada inisialisasi berat di sini (mis. tidak ada eager singleton besar) agar waktu
 * cold-start aplikasi tetap cepat — Room database & repository baru dibuat "lazy" saat
 * pertama kali benar-benar dibutuhkan (via Hilt @Inject).
 */
@HiltAndroidApp
class KasirApplication : Application()

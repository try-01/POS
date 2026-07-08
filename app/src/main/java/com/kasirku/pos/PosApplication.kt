package com.kasirku.pos

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * PosApplication - Entry point aplikasi
 * @HiltAndroidApp mengaktifkan Hilt dependency injection
 */
@HiltAndroidApp
class PosApplication : Application()

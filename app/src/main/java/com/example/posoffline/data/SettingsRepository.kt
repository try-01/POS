package com.example.posoffline.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "pos_settings")

/**
 * Settings (store name, address, tax rate, currency) stored in DataStore.
 *
 * DataStore is preferred over SharedPreferences here because:
 *  - Fully asynchronous (no ANR risk on the main thread).
 *  - Type-safe via Preferences keys.
 *  - Emits a Flow, so the UI can observe settings reactively.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val STORE_NAME = stringPreferencesKey("store_name")
        val STORE_ADDRESS = stringPreferencesKey("store_address")
        val TAX_RATE = doublePreferencesKey("tax_rate")
        val CURRENCY = stringPreferencesKey("currency")
    }

    data class Snapshot(
        val storeName: String,
        val storeAddress: String,
        val taxRate: Double,
        val currency: String
    )

    val flow: Flow<Snapshot> = context.dataStore.data.map { it.toSnapshot() }

    suspend fun getSnapshot(): Snapshot = context.dataStore.data.first().toSnapshot()

    suspend fun save(patch: Snapshot) {
        context.dataStore.edit { prefs ->
            prefs[Keys.STORE_NAME] = patch.storeName
            prefs[Keys.STORE_ADDRESS] = patch.storeAddress
            prefs[Keys.TAX_RATE] = patch.taxRate
            prefs[Keys.CURRENCY] = patch.currency
        }
    }

    private fun Preferences.toSnapshot(): Snapshot = Snapshot(
        storeName = this[Keys.STORE_NAME] ?: "Toko Saya",
        storeAddress = this[Keys.STORE_ADDRESS] ?: "Jl. Contoh No.1",
        taxRate = this[Keys.TAX_RATE] ?: 0.11,
        currency = this[Keys.CURRENCY] ?: "Rp"
    )
}

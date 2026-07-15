package com.pos.offline.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Profil toko & pengaturan struk -- singleton, hanya ada 1 baris (id tetap = 1).
 *  Disimpan di Room (bukan DataStore/SharedPreferences) supaya otomatis ikut
 *  ter-backup/restore lewat mekanisme BackupManager yang sudah ada. */
@Entity(tableName = "store_profile")
data class StoreProfileEntity(
    @PrimaryKey val id: Long = SINGLETON_ID,
    val storeName: String = "",
    val address: String = "",
    val footerNote: String = "",
    /** Logo toko sudah dikompresi/resize (~300x300px, grayscale) sebelum
     *  disimpan sebagai BLOB -- proses kompresi dikerjakan di UI (Batch H4). */
    val logoBytes: ByteArray? = null,
    val autoPrintEnabled: Boolean = false
) {
    companion object {
        const val SINGLETON_ID = 1L
    }

    // data class default membandingkan ByteArray by reference -- override
    // manual supaya perbandingan isi (relevan utk Flow di ViewModel nanti).
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StoreProfileEntity) return false
        return id == other.id &&
            storeName == other.storeName &&
            address == other.address &&
            footerNote == other.footerNote &&
            autoPrintEnabled == other.autoPrintEnabled &&
            (logoBytes?.contentEquals(other.logoBytes) ?: (other.logoBytes == null))
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + storeName.hashCode()
        result = 31 * result + address.hashCode()
        result = 31 * result + footerNote.hashCode()
        result = 31 * result + autoPrintEnabled.hashCode()
        result = 31 * result + (logoBytes?.contentHashCode() ?: 0)
        return result
    }
}
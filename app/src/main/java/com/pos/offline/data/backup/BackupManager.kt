package com.pos.offline.data.backup

import android.content.Context
import android.net.Uri
import com.pos.offline.data.di.ServiceLocator
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Mengelola ekspor dan impor database via Storage Access Framework (SAF).
 *
 * - Export: Force WAL checkpoint -> tutup DB -> salin file fisik -> DB auto-reopen saat dipakai lagi.
 * - Import: Salin ke file temp -> validasi header SQLite -> tutup DB -> hapus file lama -> rename temp jadi DB utama.
 */
object BackupManager {

    private const val DB_NAME = "pos.db"

    /**
     * Mengekspor database ke URI yang dipilih user via SAF (ACTION_CREATE_DOCUMENT).
     */
    fun exportDatabase(context: Context, destinationUri: Uri) {
        // 1. Tutup DB & lakukan WAL Checkpoint
        ServiceLocator.closeDatabase()

        // 2. Salin file DB utama ke URI tujuan
        val dbFile = context.getDatabasePath(DB_NAME)
        if (!dbFile.exists()) {
            throw IOException("File database tidak ditemukan.")
        }

        context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
            dbFile.inputStream().use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: throw IOException("Tidak dapat membuka stream untuk menulis backup.")

        // Setelah ini, instance DB akan dibangkitkan ulang otomatis oleh ServiceLocator
        // saat ada ViewModel yang mengakses repository.
    }

    /**
     * Mengimpor database dari URI hasil pilihan user via SAF (ACTION_OPEN_DOCUMENT).
     */
    fun importDatabase(context: Context, sourceUri: Uri) {
        // 1. Salin URI ke file sementara di cacheDir.
        // Kita tidak boleh menimpa DB aktif langsung dari URI karena jika proses
        // baca terputus di tengah jalan, DB aktif bisa korup.
        val tempFile = File(context.cacheDir, "restore_temp.db")

        context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            tempFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: throw IOException("Tidak dapat membuka stream untuk membaca backup.")

        // 2. Validasi header SQLite agar user tidak sembarangan memilih file gambar/pdf
        // yang ujungnya merusak aplikasi.
        if (!isValidSqliteFile(tempFile)) {
            tempFile.delete()
            throw IllegalArgumentException("File bukan backup database SQLite yang valid.")
        }

        // 3. Tutup DB aktif & reset ServiceLocator agar file tidak dikunci OS
        ServiceLocator.closeDatabase()

        // 4. Hapus file DB lama (serta WAL & SHM journal jika ada)
        val dbFile = context.getDatabasePath(DB_NAME)
        val walFile = context.getDatabasePath("$DB_NAME-wal")
        val shmFile = context.getDatabasePath("$DB_NAME-shm")

        if (dbFile.exists()) dbFile.delete()
        if (walFile.exists()) walFile.delete()
        if (shmFile.exists()) shmFile.delete()

        // 5. Pindahkan file sementara menjadi DB utama
        if (!tempFile.renameTo(dbFile)) {
            throw IOException("Gagal mengganti file database lama dengan yang baru.")
        }

        // DB akan otomatis terbuka kembali saat ada akses repository berikutnya.
        // TODO (BATCH 3): UI layer harus mereset state in-memory (mis. keranjang kasir 
        // yang sedang aktif di PosViewModel) setelah import sukses. Flow Room hanya 
        // meng-update data dari DB, bukan state transien di memori ViewModel.
    }

    private fun isValidSqliteFile(file: File): Boolean {
        if (file.length() < 16) return false
        
        // 16 byte pertama file SQLite selalu: "SQLite format 3\000"
        val expectedHeader = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
        val actualHeader = ByteArray(16)
        
        file.inputStream().use { input ->
            val readBytes = input.read(actualHeader)
            if (readBytes != 16) return false
        }
        
        return actualHeader.contentEquals(expectedHeader)
    }

    /**
     * Membangkitkan nama file default untuk dialog "Simpan Sebagai".
     */
    fun generateBackupFileName(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        return "kasir-backup-$currentDate.db"
    }
}
package com.pos.offline.data.backup

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.pos.offline.data.local.PosDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/** Hasil operasi ekspor database. */
sealed class BackupOutcome {
    object Success : BackupOutcome()
    data class Error(val throwable: Throwable) : BackupOutcome()
}

/** Hasil operasi impor/restore database. */
sealed class RestoreOutcome {
    object Success : RestoreOutcome()
    data class InvalidFile(val reason: String) : RestoreOutcome()
    data class Error(val throwable: Throwable) : RestoreOutcome()
}

/**
 * Menangani ekspor & impor database lokal (`pos.db`) melalui Storage Access
 * Framework (SAF). Murni salin file mentah — TIDAK ada sinkronisasi cloud,
 * TIDAK butuh FileProvider/permission storage sama sekali, sesuai keputusan
 * arsitektur aplikasi ini (single-device, offline, "replace penuh").
 *
 * ALUR EKSPOR:
 *  1. Checkpoint WAL penuh — Room default memakai Write-Ahead Logging,
 *     artinya transaksi terbaru bisa saja masih berada di file `pos.db-wal`
 *     dan belum masuk ke `pos.db` itu sendiri. `PRAGMA wal_checkpoint(FULL)`
 *     memaksa semua frame WAL ditulis balik ke file utama sebelum disalin.
 *  2. Salin `pos.db` mentah ke Uri hasil ACTION_CREATE_DOCUMENT.
 *
 * ALUR IMPOR (restore):
 *  1. Salin file pilihan user ke cache dulu — TIDAK PERNAH langsung menimpa
 *     database aktif sebelum divalidasi.
 *  2. Validasi dua lapis (lihat [validateCandidate]).
 *  3. Kalau valid: tutup koneksi Room aktif, timpa `pos.db` (+ bersihkan sisa
 *     `-wal`/`-shm`/`-journal` lama), lalu caller WAJIB memanggil
 *     [restartApp] — arsitektur Service Locator (semua repo singleton
 *     `by lazy`) tidak aman "disegarkan" tanpa restart proses penuh.
 *  4. Kalau tidak valid: file sementara dibuang, database aktif tidak
 *     tersentuh sama sekali.
 */
object BackupManager {

    private const val DB_NAME = "pos.db"

    /** Nama file default saat dialog "Simpan sebagai" (SAF) muncul. */
    fun suggestedBackupFileName(): String {
        val ts = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
            .format(java.util.Date())
        return "kasir-offline-backup-$ts.db"
    }

    // ---------------------------------------------------------------------
    // EXPORT
    // ---------------------------------------------------------------------

    suspend fun exportDatabase(context: Context, destinationUri: Uri): BackupOutcome =
        withContext(Dispatchers.IO) {
            try {
                checkpointWal(context)

                val dbFile = context.getDatabasePath(DB_NAME)
                if (!dbFile.exists()) {
                    return@withContext BackupOutcome.Error(
                        IllegalStateException("File database tidak ditemukan di ${dbFile.absolutePath}")
                    )
                }

                val resolver = context.contentResolver
                val output = resolver.openOutputStream(destinationUri, "rwt")
                    ?: return@withContext BackupOutcome.Error(
                        IllegalStateException("Tidak bisa membuka tujuan (Uri tidak valid)")
                    )

                output.use { out ->
                    FileInputStream(dbFile).use { input -> input.copyTo(out) }
                }

                BackupOutcome.Success
            } catch (t: Throwable) {
                BackupOutcome.Error(t)
            }
        }

    /**
     * Memaksa semua isi `-wal` ditulis balik ke `pos.db`. WAJIB dijalankan
     * sebelum menyalin file — tanpa ini backup bisa kehilangan transaksi
     * yang baru saja terjadi.
     */
    private fun checkpointWal(context: Context) {
        val writable = PosDatabase.getInstance(context).openHelper.writableDatabase
        writable.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
    }

    // ---------------------------------------------------------------------
    // IMPORT / RESTORE
    // ---------------------------------------------------------------------

    /**
     * Validasi file yang dipilih user & jika lolos, GANTIKAN database aktif.
     *
     * Setelah menerima [RestoreOutcome.Success], caller WAJIB segera memanggil
     * [restartApp] dan TIDAK BOLEH lagi memakai ViewModel/Repository yang
     * sedang berjalan — koneksi Room lama sudah ditutup paksa di sini.
     */
    suspend fun validateAndRestore(context: Context, sourceUri: Uri): RestoreOutcome =
        withContext(Dispatchers.IO) {
            val tempFile = File(context.cacheDir, "restore_candidate.db")
            try {
                // 1) Salin ke cache dulu — jangan pernah sentuh db aktif
                //    sebelum yakin file ini valid.
                val resolver = context.contentResolver
                val input = resolver.openInputStream(sourceUri)
                    ?: return@withContext RestoreOutcome.Error(
                        IllegalStateException("Tidak bisa membaca file sumber (Uri tidak valid)")
                    )
                input.use { inp ->
                    FileOutputStream(tempFile).use { out -> inp.copyTo(out) }
                }

                // 2) Validasi
                val invalidReason = validateCandidate(context, tempFile)
                if (invalidReason != null) {
                    tempFile.delete()
                    return@withContext RestoreOutcome.InvalidFile(invalidReason)
                }

                // 3) Lolos validasi -> tutup koneksi aktif & timpa file
                PosDatabase.closeActiveInstance()

                val targetDb = context.getDatabasePath(DB_NAME)
                deleteRelatedDbFiles(targetDb)
                targetDb.parentFile?.mkdirs()

                FileInputStream(tempFile).use { inp ->
                    FileOutputStream(targetDb).use { out -> inp.copyTo(out) }
                }
                tempFile.delete()

                RestoreOutcome.Success
            } catch (t: Throwable) {
                tempFile.delete()
                RestoreOutcome.Error(t)
            }
        }

    /**
     * Mengembalikan `null` kalau file valid, atau pesan alasan penolakan.
     *
     * Dua lapis pengecekan:
     *  1. **Magic header** 16-byte pertama harus "SQLite format 3\u0000" —
     *     penyaring termurah untuk file yang jelas bukan database SQLite
     *     (mis. user salah pilih foto/PDF/dokumen lain).
     *  2. **`identity_hash`** pada tabel internal Room `room_master_table`
     *     dibandingkan antara file kandidat vs database aktif. Room
     *     menghitung hash ini dari definisi skema (entity, kolom, indeks).
     *     Kalau cocok persis, file tersebut nyaris pasti backup dari
     *     aplikasi & versi skema yang sama — aman langsung dipakai
     *     menggantikan file aktif tanpa migrasi tambahan.
     *
     * Catatan batasan (untuk didiskusikan lagi kalau relevan nanti): kalau
     * suatu saat skema naik ke v4, backup lama (hash v3) akan otomatis
     * ditolak oleh pengecekan ini. Itu memang perilaku aman untuk sekarang
     * (mencegah data korup akibat restore lintas skema) — kalau nanti mau
     * dukung "restore lalu migrasi otomatis", itu perlu batch terpisah.
     */
    private fun validateCandidate(context: Context, candidate: File): String? {
        if (!hasSqliteHeader(candidate)) {
            return "File yang dipilih bukan berkas database SQLite yang valid."
        }

        val candidateHash = try {
            readIdentityHash(candidate.absolutePath)
        } catch (t: Throwable) {
            return "File tidak bisa dibuka sebagai database (rusak/korup)."
        } ?: return "File database tidak memiliki tabel internal Room yang dikenali."

        val activeHash = try {
            readIdentityHash(context.getDatabasePath(DB_NAME).absolutePath)
        } catch (t: Throwable) {
            null // db aktif belum ada/tidak terbaca -> lewati pengecekan kompatibilitas
        }

        if (activeHash != null && candidateHash != activeHash) {
            return "File backup ini berasal dari versi aplikasi yang berbeda (skema tidak cocok)."
        }

        return null
    }

    private fun hasSqliteHeader(file: File): Boolean {
        if (!file.exists() || file.length() < 16) return false
        val header = ByteArray(16)
        FileInputStream(file).use { it.read(header) }
        val magic = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
        return header.contentEquals(magic)
    }

    private fun readIdentityHash(path: String): String? {
        val db = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY)
        return db.use {
            it.rawQuery("SELECT identity_hash FROM room_master_table LIMIT 1", null).use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }
    }

    private fun deleteRelatedDbFiles(dbFile: File) {
        val parent = dbFile.parentFile
        val base = dbFile.name
        listOf(base, "$base-wal", "$base-shm", "$base-journal").forEach { name ->
            File(parent, name).takeIf { it.exists() }?.delete()
        }
    }

    // ---------------------------------------------------------------------
    // RESTART APLIKASI (WAJIB dipanggil setelah restore sukses)
    // ---------------------------------------------------------------------

    /**
     * Merestart total proses aplikasi memakai `Intent.makeRestartActivityTask`
     * (API publik Android, tersedia sejak API 11) — melempar user kembali ke
     * launcher activity di task baru yang bersih, lalu mematikan proses lama.
     *
     * Kenapa restart total (bukan sekadar buka ulang koneksi Room diam-diam)?
     * `ServiceLocator` menyimpan Repository/DAO sebagai singleton `by lazy`
     * yang sudah dipegang oleh ViewModel yang sedang hidup, dan mungkin ada
     * Flow yang sedang di-collect dari DAO lama. Membongkar semua itu dengan
     * aman jauh lebih rumit & rawan bug ("database is closed" mid-collect)
     * dibanding memulai ulang proses dari nol. Untuk aplikasi single-device
     * seperti ini, "app sempat kedip restart" adalah trade-off yang jauh
     * lebih murah.
     */
    fun restartApp(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val componentName = intent?.component
        if (componentName != null) {
            context.startActivity(Intent.makeRestartActivityTask(componentName))
        }
        Runtime.getRuntime().exit(0)
    }
}
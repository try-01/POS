package com.pos.offline.data.backup

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.core.content.FileProvider
import com.pos.offline.data.local.PosDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

sealed class BackupOutcome {
    object Success : BackupOutcome()

    data class Error(
        val throwable: Throwable,
    ) : BackupOutcome()
}

sealed class RestoreOutcome {
    /** requiresRestart = true jika koneksi Room SEMPAT ditutup dalam proses ini,
     * sehingga app WAJIB direstart penuh sebelum operasi DB apa pun berikutnya —
     * berlaku baik outcome-nya sukses maupun gagal-dengan-rollback. */
    abstract val requiresRestart: Boolean

    object Success : RestoreOutcome() {
        override val requiresRestart = true
    }

    /** Gagal validasi SEBELUM koneksi ditutup -> DB lama & koneksi aktif tidak
     * pernah tersentuh, aman melanjutkan tanpa restart. */
    data class InvalidFile(
        val reason: String,
    ) : RestoreOutcome() {
        override val requiresRestart = false
    }

    /** requiresRestart bisa berbeda tergantung tahap kegagalan:
     * - Gagal SEBELUM koneksi ditutup (mis. gagal baca sumber) -> false.
     * - Gagal SETELAH koneksi ditutup (rollback file berhasil, tapi koneksi
     *   Room di proses ini sudah mati) -> true (default, kasus paling umum). */
    data class Error(
        val throwable: Throwable,
        override val requiresRestart: Boolean = true,
    ) : RestoreOutcome()
}

sealed class ShareOutcome {
    data class Success(
        val file: File,
    ) : ShareOutcome()

    data class Error(
        val throwable: Throwable,
    ) : ShareOutcome()
}

object BackupManager {
    private const val DB_NAME = "pos.db"

    fun suggestedBackupFileName(): String {
        val ts =
            java.text
                .SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                .format(java.util.Date())
        return "kasir-offline-backup-$ts.db"
    }

    suspend fun exportDatabase(
        context: Context,
        destinationUri: Uri,
    ): BackupOutcome =
        withContext(Dispatchers.IO) {
            try {
                checkpointWal(context)

                val dbFile = context.getDatabasePath(DB_NAME)
                if (!dbFile.exists()) {
                    return@withContext BackupOutcome.Error(
                        IllegalStateException("File database tidak ditemukan di ${dbFile.absolutePath}"),
                    )
                }

                val resolver = context.contentResolver
                val output =
                    resolver.openOutputStream(destinationUri, "rwt")
                        ?: return@withContext BackupOutcome.Error(
                            IllegalStateException("Tidak bisa membuka tujuan (Uri tidak valid)"),
                        )

                output.use { out ->
                    FileInputStream(dbFile).use { input -> input.copyTo(out) }
                }

                BackupOutcome.Success
            } catch (t: Throwable) {
                BackupOutcome.Error(t)
            }
        }

    private fun checkpointWal(context: Context) {
        val writable = PosDatabase.getInstance(context).openHelper.writableDatabase
        writable.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
    }

    /** Varian TRUNCATE khusus sebelum restore: memaksa file -wal benar-benar
     * dikosongkan (0 byte) setelah semua transaksi dipindah ke .db, sehingga
     * saat dikarantina ke .bak tidak ada sisa data yang ambigu. */
    private fun checkpointWalTruncate(context: Context) {
        val writable = PosDatabase.getInstance(context).openHelper.writableDatabase
        writable.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
    }

    /**
     * Strategi Two-Phase Swap:
     *
     * Fase A (Karantina sumber, tanpa risiko):
     *   1. Ekstrak sumber ke file karantina di cache — DB aktif belum tersentuh.
     *   2. Validasi penuh di karantina (header, PRAGMA integrity_check, kompatibilitas
     *      versi/skema). Gagal di sini = DB aktif 100% masih utuh.
     *   3. Checkpoint WAL (TRUNCATE) & tutup koneksi Room untuk lepas file lock.
     *      -> Titik tidak-bisa-mundur (connectionClosed = true) dimulai di sini.
     *   4. Salin kandidat tervalidasi ke file STAGING di direktori yang SAMA
     *      dengan target, wajib satu filesystem agar rename di Fase C atomic.
     *
     * Fase B (Karantina file lama — rename, BUKAN delete):
     *   5. Rename .db/-wal/-shm/-journal LAMA menjadi *.bak. Setiap rename yang
     *      berhasil dicatat agar bisa di-rollback presisi jika gagal.
     *
     * Fase C (Atomic Swap, titik kritis satu-satunya):
     *   6. Rename staging -> nama target (satu syscall rename() atomic).
     *      - Gagal -> rollback semua *.bak ke nama asli, database lama pulih 100%.
     *      - Sukses -> lanjut Fase D.
     *
     * Fase D (Resolusi akhir):
     *   7. Hapus seluruh *.bak & file karantina — HANYA di sini delete() dipakai
     *      pada file yang berpotensi masih dibutuhkan.
     *
     * CATATAN KRITIS: Ada window (antara Fase B selesai & Fase C selesai) di mana
     * proses bisa di-kill sehingga pos.db HILANG dari nama aslinya (jadi .bak) dan
     * kandidat baru BELUM ter-swap. Ini WAJIB ditangani oleh
     * [recoverFromInterruptedRestore] yang dijalankan sinkron saat app startup,
     * SEBELUM PosDatabase.getInstance() pertama kali dipanggil di proses baru.
     */
    suspend fun validateAndRestore(
        context: Context,
        sourceUri: Uri,
    ): RestoreOutcome =
        withContext(Dispatchers.IO) {
            val quarantineFile = File(context.cacheDir, "restore_candidate.db")
            val targetDb = context.getDatabasePath(DB_NAME)
            val parentDir =
                targetDb.parentFile
                    ?: return@withContext RestoreOutcome.Error(
                        IllegalStateException("Direktori database tidak ditemukan"),
                        requiresRestart = false,
                    )
            val stagingFile = File(parentDir, "$DB_NAME.new")

            val walFile = File(parentDir, "$DB_NAME-wal")
            val shmFile = File(parentDir, "$DB_NAME-shm")
            val journalFile = File(parentDir, "$DB_NAME-journal")

            val bakDb = File(parentDir, "$DB_NAME.bak")
            val bakWal = File(parentDir, "$DB_NAME-wal.bak")
            val bakShm = File(parentDir, "$DB_NAME-shm.bak")
            val bakJournal = File(parentDir, "$DB_NAME-journal.bak")

            val quarantined = mutableListOf<Pair<File, File>>()
            // Menandai titik tidak-bisa-mundur: begitu true, restart WAJIB
            // dilakukan pada outcome apa pun berikutnya (lihat RestoreOutcome.Error).
            var connectionClosed = false

            fun rollbackQuarantine() {
                for ((backup, original) in quarantined.reversed()) {
                    if (backup.exists()) backup.renameTo(original)
                }
                quarantined.clear()
            }

            try {
                // ---- Fase A.1 & A.2 ----
                val resolver = context.contentResolver
                val input =
                    resolver.openInputStream(sourceUri)
                        ?: return@withContext RestoreOutcome.Error(
                            IllegalStateException("Tidak bisa membaca file sumber (Uri tidak valid)"),
                            requiresRestart = false,
                        )
                input.use { inp ->
                    FileOutputStream(quarantineFile).use { out -> inp.copyTo(out) }
                }

                val invalidReason = validateCandidate(context, quarantineFile)
                if (invalidReason != null) {
                    quarantineFile.delete()
                    return@withContext RestoreOutcome.InvalidFile(invalidReason)
                }

                // ---- Fase A.3 ----
                try {
                    checkpointWalTruncate(context)
                } catch (_: Throwable) {
                    // Non-fatal: DB lama mungkin belum pernah dibuka Room.
                }
                PosDatabase.closeActiveInstance()
                connectionClosed = true

                parentDir.mkdirs()

                // ---- Fase A.4 ----
                try {
                    FileInputStream(quarantineFile).use { inp ->
                        FileOutputStream(stagingFile).use { out ->
                            inp.copyTo(out)
                            out.flush()
                            out.fd.sync()
                        }
                    }
                } catch (t: Throwable) {
                    stagingFile.delete()
                    return@withContext RestoreOutcome.Error(t, requiresRestart = true)
                }

                // ---- Fase B ----
                fun quarantineOld(
                    original: File,
                    backup: File,
                ): Boolean {
                    if (!original.exists()) return true
                    backup.delete() // bersihkan sisa .bak dari percobaan gagal sebelumnya
                    return if (original.renameTo(backup)) {
                        quarantined.add(backup to original)
                        true
                    } else {
                        false
                    }
                }

                val quarantineOk =
                    quarantineOld(targetDb, bakDb) &&
                        quarantineOld(walFile, bakWal) &&
                        quarantineOld(shmFile, bakShm) &&
                        quarantineOld(journalFile, bakJournal)

                if (!quarantineOk) {
                    rollbackQuarantine()
                    stagingFile.delete()
                    return@withContext RestoreOutcome.Error(
                        IllegalStateException(
                            "Gagal mengkarantina database lama sebelum swap. " +
                                "Database lama telah dipulihkan, restore dibatalkan.",
                        ),
                        requiresRestart = true,
                    )
                }

                // ---- Fase C: ATOMIC SWAP ----
                val swapped = stagingFile.renameTo(targetDb)
                if (!swapped) {
                    rollbackQuarantine()
                    stagingFile.delete()
                    return@withContext RestoreOutcome.Error(
                        IllegalStateException(
                            "Gagal menukar file database (rename kandidat gagal). " +
                                "Database lama telah dipulihkan, restore dibatalkan.",
                        ),
                        requiresRestart = true,
                    )
                }

                // ---- Fase D ----
                bakDb.delete()
                bakWal.delete()
                bakShm.delete()
                bakJournal.delete()
                quarantineFile.delete()

                RestoreOutcome.Success
            } catch (t: Throwable) {
                rollbackQuarantine()
                quarantineFile.delete()
                stagingFile.delete()
                RestoreOutcome.Error(t, requiresRestart = connectionClosed)
            }
        }

    private fun validateCandidate(
        context: Context,
        candidate: File,
    ): String? {
        if (!hasSqliteHeader(candidate)) {
            return "File yang dipilih bukan berkas database SQLite yang valid."
        }

        val candidateVersion =
            try {
                readUserVersion(candidate.absolutePath)
            } catch (t: Throwable) {
                return "File tidak bisa dibuka sebagai database (rusak/korup)."
            }

        val integrityOk =
            try {
                runIntegrityCheck(candidate.absolutePath)
            } catch (t: Throwable) {
                return "File tidak bisa dibuka sebagai database (rusak/korup)."
            }
        if (!integrityOk) {
            return "File database gagal pemeriksaan integritas (kemungkinan rusak/korup)."
        }

        val candidateHash =
            try {
                readIdentityHash(candidate.absolutePath)
            } catch (t: Throwable) {
                return "File tidak bisa dibuka sebagai database (rusak/korup)."
            } ?: return "File database tidak memiliki tabel internal Room yang dikenali."

        val activeDbPath = context.getDatabasePath(DB_NAME).absolutePath
        val activeVersion =
            try {
                readUserVersion(activeDbPath)
            } catch (t: Throwable) {
                null
            }

        if (activeVersion != null) {
            if (candidateVersion > activeVersion) {
                return "File backup ini dibuat dari versi aplikasi yang lebih baru. " +
                    "Perbarui aplikasi terlebih dahulu sebelum memulihkan."
            }
            if (candidateVersion == activeVersion) {
                val activeHash =
                    try {
                        readIdentityHash(activeDbPath)
                    } catch (t: Throwable) {
                        null
                    }
                if (activeHash != null && candidateHash != activeHash) {
                    return "File backup ini berasal dari struktur database yang berbeda " +
                        "(kemungkinan bukan dari aplikasi ini)."
                }
            }
        }

        return null
    }

    private fun runIntegrityCheck(path: String): Boolean {
        val db = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY)
        return db.use {
            it.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                cursor.moveToFirst() && cursor.getString(0).equals("ok", ignoreCase = true)
            }
        }
    }

    private fun hasSqliteHeader(file: File): Boolean {
        if (!file.exists() || file.length() < 16) return false
        val header = ByteArray(16)
        FileInputStream(file).use { it.read(header) }
        val magic = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
        return header.contentEquals(magic)
    }

    private fun readUserVersion(path: String): Int {
        val db = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY)
        return db.use { it.version }
    }

    private fun readIdentityHash(path: String): String? {
        val db = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY)
        return db.use {
            it.rawQuery("SELECT identity_hash FROM room_master_table LIMIT 1", null).use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }
    }

    /**
     * Recovery WAJIB dipanggil SINKRON di titik paling awal proses aplikasi
     * (lihat [com.pos.offline.data.di.ServiceLocator.initialize]), SEBELUM ada
     * satu baris kode pun yang memanggil PosDatabase.getInstance().
     *
     * Menangani sisa dari [validateAndRestore] yang terhenti akibat proses
     * di-kill paksa (crash/force-stop OS) di tengah jalan:
     *
     * - pos.db ADA + *.bak ADA -> swap sudah sukses, tinggal sampah cleanup
     *   yang belum sempat dieksekusi -> aman, hapus semua *.bak & staging.
     * - pos.db TIDAK ADA + *.bak ADA -> KRITIS: crash terjadi tepat di antara
     *   Fase B (karantina) dan Fase C (swap). Kandidat baru belum terpasang,
     *   dan database utama sudah "hilang" namanya (jadi .bak). WAJIB kembalikan
     *   *.bak ke nama asli agar Room membuka database LAMA yang konsisten,
     *   BUKAN membuat database kosong baru secara tidak sengaja.
     * - pos.db ADA + .new ADA, tanpa *.bak -> crash terjadi sebelum karantina
     *   dimulai, database lama tidak pernah tersentuh -> aman, hapus .new saja.
     *
     * Operasinya murni file-metadata (exists/renameTo/delete), sub-milidetik,
     * sehingga aman dijalankan sinkron tanpa coroutine/background thread —
     * justru WAJIB sinkron demi menjamin urutan eksekusi sebelum Room dibuka.
     */
    fun recoverFromInterruptedRestore(context: Context) {
        try {
            val parentDir = context.getDatabasePath(DB_NAME).parentFile ?: return
            val targetDb = File(parentDir, DB_NAME)
            val stagingFile = File(parentDir, "$DB_NAME.new")

            val pairs =
                listOf(
                    File(parentDir, "$DB_NAME.bak") to targetDb,
                    File(parentDir, "$DB_NAME-wal.bak") to File(parentDir, "$DB_NAME-wal"),
                    File(parentDir, "$DB_NAME-shm.bak") to File(parentDir, "$DB_NAME-shm"),
                    File(parentDir, "$DB_NAME-journal.bak") to File(parentDir, "$DB_NAME-journal"),
                )
            val anyBakExists = pairs.any { (backup, _) -> backup.exists() }

            if (!anyBakExists) {
                // Tidak ada jejak restore yang terhenti di fase karantina.
                stagingFile.delete()
                return
            }

            if (!targetDb.exists()) {
                // KRITIS: swap belum sempat terjadi. Kembalikan *.bak ke nama asli.
                pairs.forEach { (backup, original) ->
                    if (backup.exists()) backup.renameTo(original)
                }
                stagingFile.delete()
            } else {
                // Swap sudah sukses sebelumnya, *.bak tinggal sampah cleanup.
                pairs.forEach { (backup, _) -> backup.delete() }
                stagingFile.delete()
            }
        } catch (t: Throwable) {
            // Best-effort: kegagalan di sini tidak boleh mencegah app startup.
            android.util.Log.e("BackupManager", "Gagal menjalankan recovery restore", t)
        }
    }

    suspend fun prepareShareableCopy(context: Context): ShareOutcome =
        withContext(Dispatchers.IO) {
            try {
                checkpointWal(context)

                val dbFile = context.getDatabasePath(DB_NAME)
                if (!dbFile.exists()) {
                    return@withContext ShareOutcome.Error(
                        IllegalStateException("File database tidak ditemukan di ${dbFile.absolutePath}"),
                    )
                }

                val dir = File(context.cacheDir, "shared_backups").apply { mkdirs() }
                dir.listFiles()?.forEach { it.delete() }

                val file = File(dir, suggestedBackupFileName())
                FileInputStream(dbFile).use { input ->
                    FileOutputStream(file).use { output -> input.copyTo(output) }
                }

                ShareOutcome.Success(file)
            } catch (t: Throwable) {
                ShareOutcome.Error(t)
            }
        }

    fun buildShareIntent(
        context: Context,
        file: File,
    ): Intent {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)

        val sendIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        return Intent.createChooser(sendIntent, "Bagikan Cadangan Database")
    }

    fun restartApp(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val componentName = intent?.component
        if (componentName != null) {
            context.startActivity(Intent.makeRestartActivityTask(componentName))
        }
        Runtime.getRuntime().exit(0)
    }
}

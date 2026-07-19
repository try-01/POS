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
    data class Error(val throwable: Throwable) : BackupOutcome()
}

sealed class RestoreOutcome {
    object Success : RestoreOutcome()
    data class InvalidFile(val reason: String) : RestoreOutcome()
    data class Error(val throwable: Throwable) : RestoreOutcome()
}
sealed class ShareOutcome {
    data class Success(val file: File) : ShareOutcome()
    data class Error(val throwable: Throwable) : ShareOutcome()
}
object BackupManager {

    private const val DB_NAME = "pos.db"

    fun suggestedBackupFileName(): String {
        val ts = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
            .format(java.util.Date())
        return "kasir-offline-backup-$ts.db"
    }
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
    private fun checkpointWal(context: Context) {
        val writable = PosDatabase.getInstance(context).openHelper.writableDatabase
        writable.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
    }
    suspend fun validateAndRestore(context: Context, sourceUri: Uri): RestoreOutcome =
        withContext(Dispatchers.IO) {
            val tempFile = File(context.cacheDir, "restore_candidate.db")
            try {
                val resolver = context.contentResolver
                val input = resolver.openInputStream(sourceUri)
                    ?: return@withContext RestoreOutcome.Error(
                        IllegalStateException("Tidak bisa membaca file sumber (Uri tidak valid)")
                    )
                input.use { inp ->
                    FileOutputStream(tempFile).use { out -> inp.copyTo(out) }
                }

                val invalidReason = validateCandidate(context, tempFile)
                if (invalidReason != null) {
                    tempFile.delete()
                    return@withContext RestoreOutcome.InvalidFile(invalidReason)
                }

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

    private fun validateCandidate(context: Context, candidate: File): String? {
        if (!hasSqliteHeader(candidate)) {
            return "File yang dipilih bukan berkas database SQLite yang valid."
        }

        val candidateVersion = try {
            readUserVersion(candidate.absolutePath)
        } catch (t: Throwable) {
            return "File tidak bisa dibuka sebagai database (rusak/korup)."
        }

        val candidateHash = try {
            readIdentityHash(candidate.absolutePath)
        } catch (t: Throwable) {
            return "File tidak bisa dibuka sebagai database (rusak/korup)."
        } ?: return "File database tidak memiliki tabel internal Room yang dikenali."

        val activeDbPath = context.getDatabasePath(DB_NAME).absolutePath
        val activeVersion = try {
            readUserVersion(activeDbPath)
        } catch (t: Throwable) {
            null // db aktif belum ada/tidak terbaca -> lewati pengecekan kompatibilitas versi
        }

        if (activeVersion != null) {
            if (candidateVersion > activeVersion) {
                return "File backup ini dibuat dari versi aplikasi yang lebih baru. " +
                    "Perbarui aplikasi terlebih dahulu sebelum memulihkan."
            }
            if (candidateVersion == activeVersion) {
                val activeHash = try {
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

    private fun deleteRelatedDbFiles(dbFile: File) {
        val parent = dbFile.parentFile
        val base = dbFile.name
        listOf(base, "$base-wal", "$base-shm", "$base-journal").forEach { name ->
            File(parent, name).takeIf { it.exists() }?.delete()
        }
    }
    suspend fun prepareShareableCopy(context: Context): ShareOutcome =
        withContext(Dispatchers.IO) {
            try {
                checkpointWal(context)

                val dbFile = context.getDatabasePath(DB_NAME)
                if (!dbFile.exists()) {
                    return@withContext ShareOutcome.Error(
                        IllegalStateException("File database tidak ditemukan di ${dbFile.absolutePath}")
                    )
                }

                val dir = File(context.cacheDir, "shared_backups").apply { mkdirs() }
                dir.listFiles()?.forEach { it.delete() } // bersihkan salinan lama

                val file = File(dir, suggestedBackupFileName())
                FileInputStream(dbFile).use { input ->
                    FileOutputStream(file).use { output -> input.copyTo(output) }
                }

                ShareOutcome.Success(file)
            } catch (t: Throwable) {
                ShareOutcome.Error(t)
            }
        }
    fun buildShareIntent(context: Context, file: File): Intent {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
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
package com.pos.offline.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Header transaksi (1 struk = 1 baris). Nomor invoice [id] sebagai PK.
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String,          // nomor invoice, mis. "INV-1700000000000"
    val createdAt: Long,     // epoch millis — dipakai filter harian & urutan
    val subtotal: Long,      // Σ (harga × qty) sebelum diskon/pajak
    val discount: Long,      // nominal diskon (Rupiah)
    val tax: Long,           // nominal pajak (Rupiah), dihitung setelah diskon
    val total: Long,         // subtotal - diskon + pajak (yang harus dibayar)
    val paidAmount: Long,    // uang diterima dari pelanggan
    val change: Long,        // kembalian = max(0, paidAmount - total)

    // ============ KOLOM v3 — lihat Migrations.MIGRATION_2_3 ============
    @ColumnInfo(defaultValue = "'CASH'")
    val paymentMethod: String = PaymentMethod.CASH.name,

    val cashierId: Long? = null,

    @ColumnInfo(defaultValue = "''")
    val cashierName: String = "",

    val shiftId: Long? = null
)

/**
 * Detail item dari sebuah transaksi (baris produk dalam struk).
 * Di-snapshot (tidak FK ke produk) agar riwayat tidak berubah walau produk
 * master diubah/dihapus kemudian hari.
 */
@Entity(
    tableName = "transaction_items",
    indices = [Index(value = ["transactionId"])] // lookup item per struk cepat
)
data class TransactionItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionId: String,
    val productName: String,
    val unitPrice: Long,
    val quantity: Int,
    val lineTotal: Long,      // unitPrice × quantity (disimpan utk laporan cepat)

    // ============ KOLOM BARU (v4) — lihat Migrations.MIGRATION_3_4 ============
    /**
     * Snapshot harga modal (cost) produk PADA SAAT transaksi terjadi — bukan
     * cost produk saat ini. Ini krusial untuk keakuratan laba historis: kalau
     * modal produk berubah minggu depan, laba transaksi minggu lalu tidak
     * boleh ikut berubah retroaktif.
     *
     * Transaksi lama (sebelum v4) akan punya nilai default 0 di sini — kalau
     * suatu saat laba dihitung untuk shift/periode SEBELUM update aplikasi
     * ini, hasilnya akan terlihat overstated (modal dianggap 0). Ini
     * batasan yang disengaja & diketahui, bukan bug — shift BARU (dibuat
     * setelah update) akan selalu punya unitCost yang akurat.
     */
    @ColumnInfo(defaultValue = "0")
    val unitCost: Long = 0L
)
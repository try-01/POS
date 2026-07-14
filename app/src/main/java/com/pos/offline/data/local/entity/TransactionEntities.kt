package com.pos.offline.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String,          // nomor invoice, mis. "INV-1700000000000"
    val createdAt: Long,     // epoch millis — dipakai filter harian & urutan
    val subtotal: Long,      // Σ (harga × qty) sebelum diskon/pajak
    val discount: Long,      // nominal diskon FINAL (Rupiah) — sumber kebenaran kalkulasi
    val tax: Long,           // nominal pajak (Rupiah), dihitung setelah diskon
    val total: Long,         // subtotal - diskon + pajak (yang harus dibayar)
    val paidAmount: Long,    // uang diterima dari pelanggan
    val change: Long,
    @ColumnInfo(defaultValue = "'CASH'")
    val paymentMethod: String = PaymentMethod.CASH.name,

    val cashierId: Long? = null,

    @ColumnInfo(defaultValue = "''")
    val cashierName: String = "",

    val shiftId: Long? = null,
    @ColumnInfo(defaultValue = "'NOMINAL'")
    val discountType: String = DiscountType.NOMINAL.name,

    @ColumnInfo(defaultValue = "0.0")
    val discountValue: Double = 0.0,
    @ColumnInfo(defaultValue = "'COMPLETED'")
    val status: String = TransactionStatus.COMPLETED.name,

    /** Timestamp epoch millis saat transaksi dibatalkan; null jika belum pernah di-void. */
    val voidedAt: Long? = null,
    val voidReason: String? = null,

    /**
     * FK logis ke ReturnEntity.id (tanpa @ForeignKey, ikut pola TransactionItemEntity).
     * null = belum pernah diretur. Sekali terisi, PERMANEN — "sekali retur = final",
     * tombol "Retur Item" di UI disembunyikan selamanya untuk transaksi ini.
     * Ditambahkan di v7 (lihat MIGRATION_6_7). Kolom ini SENGAJA di tabel `transactions`
     * (bukan cuma dilihat dari keberadaan record di tabel `returns`) supaya Room Flow
     * daftar transaksi otomatis ter-refresh reaktif saat retur baru saja diproses.
     */
    val returnId: Long? = null
)

/** Shortcut baca: true jika transaksi ini berstatus VOID (dibatalkan). Ada di TransactionStatus.kt */

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
    val lineTotal: Long,

    @ColumnInfo(defaultValue = "0")
    val unitCost: Long = 0L,
    val productId: Long? = null
)

/** Shortcut baca: true jika transaksi ini sudah pernah diretur (final, tidak bisa diretur lagi). */
val TransactionEntity.hasReturn: Boolean
    get() = returnId != null
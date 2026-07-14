package com.pos.offline.data.local.entity

/**
 * Status transaksi.
 *
 * VOID = dibatalkan (soft-delete): baris TETAP ada di DB untuk audit (nomor
 * struk tidak boleh "hilang" tanpa jejak — gap nomor mencurigakan), tapi
 * DIKECUALIKAN dari semua kalkulasi pendapatan/laba (lihat query di
 * [com.pos.offline.data.local.dao.TransactionDao] & [com.pos.offline.data.local.dao.ShiftDao]).
 *
 * RETURNED SENGAJA TIDAK ADA di sini — retur (Batch E, menyusul) akan
 * ditangani via tabel terpisah (`returns`), bukan status biner di sini,
 * karena retur bisa PARSIAL (sebagian item saja) sehingga status level-
 * transaksi tidak cukup ekspresif untuk merepresentasikannya.
 */
enum class TransactionStatus { COMPLETED, VOID }

/** Shortcut baca: true jika transaksi ini berstatus VOID (dibatalkan). */
val TransactionEntity.isVoid: Boolean
    get() = status == TransactionStatus.VOID.name
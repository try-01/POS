package com.pos.offline.util

/** Sanitasi hasil scan barcode/QR: buang whitespace, batasi karakter valid & panjang. */
fun sanitizeScannedCode(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val cleaned = raw.trim().filter { c -> c.isLetterOrDigit() || c in "-_./: #" }.take(128)
    return cleaned.ifBlank { null }
}
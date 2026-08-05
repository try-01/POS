package com.pos.offline.util
fun sanitizeScannedCode(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val cleaned = raw.trim().filter { c -> c.isLetterOrDigit() || c in "-_./: #" }.take(128)
    return cleaned.ifBlank { null }
}

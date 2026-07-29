package com.pos.offline.data.backup
import java.util.concurrent.atomic.AtomicBoolean
object RestoreGuard {
    private val _inProgress = AtomicBoolean(false)
    val isInProgress: Boolean get() = _inProgress.get()
    fun begin() { _inProgress.set(true) }
    fun end() { _inProgress.set(false) }
}

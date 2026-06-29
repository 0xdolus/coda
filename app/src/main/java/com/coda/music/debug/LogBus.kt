package com.coda.music.debug

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * In-memory log buffer for on-device debugging without ADB/root.
 * Termux and other non-system apps cannot read another app's logcat
 * output (Android restricts READ_LOGS to system/privileged apps since
 * API 16) — this sidesteps that by keeping our own rolling log that the
 * More screen can display directly.
 *
 * TEMP DEBUG TOOL — safe to remove once root-causing is done, or keep
 * permanently as a lightweight in-app diagnostics panel.
 */
object LogBus {
    data class Entry(
        val timestamp: String,
        val tag: String,
        val message: String,
        val isError: Boolean
    )

    private const val MAX_ENTRIES = 200
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries = _entries.asStateFlow()

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val full = if (throwable != null) {
            "$message — ${throwable::class.qualifiedName}: ${throwable.message}"
        } else message
        add(tag, full, isError = true)
        android.util.Log.e(tag, message, throwable)
    }

    fun d(tag: String, message: String) {
        add(tag, message, isError = false)
        android.util.Log.d(tag, message)
    }

    private fun add(tag: String, message: String, isError: Boolean) {
        val entry = Entry(
            timestamp = timeFormat.format(Date()),
            tag = tag,
            message = message,
            isError = isError
        )
        _entries.value = (_entries.value + entry).takeLast(MAX_ENTRIES)
    }

    fun clear() {
        _entries.value = emptyList()
    }
}

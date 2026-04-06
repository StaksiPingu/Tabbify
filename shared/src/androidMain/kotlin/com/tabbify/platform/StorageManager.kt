package com.tabbify.platform

import android.content.Context
import java.io.File

lateinit var appContext: Context

actual class StorageManager actual constructor() {

    private val recordingsDir: File by lazy {
        File(appContext.getExternalFilesDir(null), "recordings").also { it.mkdirs() }
    }

    actual fun getRecordingsDir(): String = recordingsDir.absolutePath

    actual fun newRecordingPath(songId: String, trackId: String): String {
        val dir = File(recordingsDir, songId).also { it.mkdirs() }
        return File(dir, "$trackId.m4a").absolutePath
    }

    actual fun deleteRecording(filePath: String): Boolean {
        return File(filePath).takeIf { it.exists() }?.delete() ?: false
    }

    actual fun recordingExists(filePath: String): Boolean = File(filePath).exists()

    actual fun getRecordingSize(filePath: String): Long = File(filePath).takeIf { it.exists() }?.length() ?: 0L

    actual fun getTotalUsedBytes(): Long {
        return recordingsDir.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    actual fun listRecordings(): List<String> {
        return recordingsDir.walkTopDown()
            .filter { it.isFile && it.extension == "m4a" }
            .map { it.absolutePath }
            .toList()
    }
}

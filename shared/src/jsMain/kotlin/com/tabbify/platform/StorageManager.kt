package com.tabbify.platform

actual class StorageManager actual constructor() {

    actual fun getRecordingsDir(): String = "/recordings"

    actual fun newRecordingPath(songId: String, trackId: String): String =
        "/recordings/$songId/$trackId.webm"

    actual fun deleteRecording(filePath: String): Boolean {
        js("localStorage.removeItem(filePath)")
        return true
    }

    actual fun recordingExists(filePath: String): Boolean {
        return js("!!localStorage.getItem(filePath)") as Boolean
    }

    actual fun getRecordingSize(filePath: String): Long = 0L

    actual fun getTotalUsedBytes(): Long = 0L

    actual fun listRecordings(): List<String> = emptyList()
}

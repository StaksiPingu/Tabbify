package com.tabbify.platform

expect class StorageManager() {
    fun getRecordingsDir(): String
    fun newRecordingPath(songId: String, trackId: String): String
    fun deleteRecording(filePath: String): Boolean
    fun recordingExists(filePath: String): Boolean
    fun getRecordingSize(filePath: String): Long
    fun getTotalUsedBytes(): Long
    fun listRecordings(): List<String>
}

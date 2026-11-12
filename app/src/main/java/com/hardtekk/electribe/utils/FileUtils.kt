package com.hardtekk.electribe.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * File utilities for handling audio files, Storage Access Framework, etc.
 */
object FileUtils {
    private const val TAG = "FileUtils"

    /**
     * Copy a file from a URI (SAF) to app's internal storage
     */
    fun copyUriToFile(context: Context, uri: Uri, destFile: File): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(destFile)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy URI to file", e)
            false
        }
    }

    /**
     * Get file name from URI
     */
    fun getFileName(context: Context, uri: Uri): String? {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) it.getString(nameIndex) else null
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get file name from URI", e)
            null
        }
    }

    /**
     * Get file size from URI
     */
    fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (sizeIndex >= 0) it.getLong(sizeIndex) else -1
                } else -1
            } ?: -1
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get file size from URI", e)
            -1
        }
    }

    /**
     * Get MIME type from URI
     */
    fun getMimeType(context: Context, uri: Uri): String? {
        return context.contentResolver.getType(uri)
    }

    /**
     * Check if URI is an audio file
     */
    fun isAudioFile(context: Context, uri: Uri): Boolean {
        val mimeType = getMimeType(context, uri)
        return mimeType?.startsWith("audio/") == true
    }

    /**
     * Create a unique file name in a directory
     */
    fun createUniqueFileName(dir: File, baseName: String, extension: String): File {
        var counter = 0
        var file = File(dir, "$baseName.$extension")
        while (file.exists()) {
            counter++
            file = File(dir, "${baseName}_$counter.$extension")
        }
        return file
    }

    /**
     * Get audio files directory
     */
    fun getAudioFilesDir(context: Context): File {
        val dir = File(context.filesDir, "samples/user")
        dir.mkdirs()
        return dir
    }

    /**
     * Get recordings directory
     */
    fun getRecordingsDir(context: Context): File {
        val dir = File(context.filesDir, "recordings")
        dir.mkdirs()
        return dir
    }

    /**
     * Delete a file and log result
     */
    fun deleteFile(file: File): Boolean {
        if (file.exists()) {
            val result = file.delete()
            Log.d(TAG, "Deleted file: ${file.name} - $result")
            return result
        }
        return false
    }

    /**
     * Get file extension
     */
    fun getExtension(file: File): String {
        val name = file.name
        val lastDot = name.lastIndexOf('.')
        return if (lastDot > 0) name.substring(lastDot + 1).lowercase() else ""
    }

    /**
     * Get file name without extension
     */
    fun getNameWithoutExtension(file: File): String {
        val name = file.name
        val lastDot = name.lastIndexOf('.')
        return if (lastDot > 0) name.substring(0, lastDot) else name
    }

    /**
     * Format file size for display
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    /**
     * Format duration in seconds to MM:SS.mmm
     */
    fun formatDuration(seconds: Double): String {
        val totalMs = (seconds * 1000).toLong()
        val minutes = totalMs / 60000
        val secs = (totalMs % 60000) / 1000
        val millis = totalMs % 1000
        return if (minutes > 0) {
            String.format("%d:%02d.%03d", minutes, secs, millis)
        } else {
            String.format("%d.%03d", secs, millis)
        }
    }
}
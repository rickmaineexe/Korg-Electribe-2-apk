package com.hardtekk.electribe.sequencer

import android.content.Context
import android.util.Log
import com.hardtekk.electribe.model.Pattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream

/**
 * Pattern Manager - handles saving/loading patterns to disk
 * Uses JSON serialization for human-readable pattern files
 */
class PatternManager(private val context: Context) {
    private val patternsDir: File
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    init {
        patternsDir = File(context.filesDir, "patterns")
        patternsDir.mkdirs()
    }

    /**
     * Save a pattern to disk
     */
    suspend fun savePattern(pattern: Pattern): Boolean = withContext(Dispatchers.IO) {
        try {
            val fileName = "pattern_${pattern.patternIndex}.json"
            val file = File(patternsDir, fileName)
            val jsonString = json.encodeToString(pattern)
            FileOutputStream(file).use { it.write(jsonString.toByteArray()) }
            Log.d("PatternManager", "Saved pattern ${pattern.patternIndex} to $fileName")
            true
        } catch (e: Exception) {
            Log.e("PatternManager", "Failed to save pattern", e)
            false
        }
    }

    /**
     * Load a pattern from disk
     */
    suspend fun loadPattern(patternIndex: Int): Pattern? = withContext(Dispatchers.IO) {
        try {
            val fileName = "pattern_$patternIndex.json"
            val file = File(patternsDir, fileName)
            if (!file.exists()) {
                return@withContext null
            }
            val jsonString = FileInputStream(file).use { it.readBytes().decodeToString() }
            val pattern = json.decodeFromString<Pattern>(jsonString)
            Log.d("PatternManager", "Loaded pattern $patternIndex from $fileName")
            pattern
        } catch (e: Exception) {
            Log.e("PatternManager", "Failed to load pattern $patternIndex", e)
            null
        }
    }

    /**
     * Load all patterns
     */
    suspend fun loadAllPatterns(): Array<Pattern?> = withContext(Dispatchers.IO) {
        val result = Array<Pattern?>(4) { null }
        for (i in 0..3) {
            result[i] = loadPattern(i)
        }
        result
    }

    /**
     * Delete a pattern file
     */
    suspend fun deletePattern(patternIndex: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val fileName = "pattern_$patternIndex.json"
            val file = File(patternsDir, fileName)
            file.delete()
            true
        } catch (e: Exception) {
            Log.e("PatternManager", "Failed to delete pattern", e)
            false
        }
    }

    /**
     * Export pattern as JSON string (for sharing)
     */
    fun exportPattern(pattern: Pattern): String {
        return json.encodeToString(pattern)
    }

    /**
     * Import pattern from JSON string
     */
    fun importPattern(jsonString: String): Pattern? {
        return try {
            json.decodeFromString<Pattern>(jsonString)
        } catch (e: Exception) {
            Log.e("PatternManager", "Failed to import pattern", e)
            null
        }
    }

    /**
     * Get list of saved pattern files
     */
    fun getSavedPatterns(): List<Int> {
        return patternsDir.listFiles()?.filter { it.name.startsWith("pattern_") && it.name.endsWith(".json") }
            ?.map { it.name.substringAfter("pattern_").substringBefore(".json").toIntOrNull() }
            ?.filterNotNull()
            ?.sorted()
            ?.toList() ?: emptyList()
    }
}
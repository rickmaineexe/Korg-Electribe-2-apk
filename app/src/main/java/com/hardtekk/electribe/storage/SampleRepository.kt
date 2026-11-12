package com.hardtekk.electribe.storage

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.hardtekk.electribe.model.SampleData
import com.hardtekk.electribe.model.SliceData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream

/**
 * Sample Repository - manages sample metadata using Room-like SQLite database
 * Handles built-in and user samples, slices, and categories
 */
class SampleRepository(private val context: Context) {
    private val dbHelper: DatabaseHelper
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    init {
        dbHelper = DatabaseHelper(context)
    }

    // ============================================================
    // SAMPLE MANAGEMENT
    // ============================================================

    suspend fun saveSamples(samples: List<SampleData>) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            for (sample in samples) {
                val jsonString = json.encodeToString(sample)
                val values = android.content.ContentValues().apply {
                    put("id", sample.id)
                    put("name", sample.name)
                    put("file_path", sample.filePath)
                    put("duration_frames", sample.durationFrames)
                    put("sample_rate", sample.sampleRate)
                    put("channels", sample.channels)
                    put("bit_depth", sample.bitDepth)
                    put("is_built_in", if (sample.isBuiltIn) 1 else 0)
                    put("category", sample.category.name)
                    put("date_added", sample.dateAdded)
                    put("json_data", jsonString)
                }
                db.insertWithOnConflict("samples", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    suspend fun getAllSamples(): List<SampleData> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT json_data FROM samples ORDER BY category, name", null)
        val results = mutableListOf<SampleData>()
        try {
            while (cursor.moveToNext()) {
                val jsonString = cursor.getString(0)
                try {
                    val sample = json.decodeFromString<SampleData>(jsonString)
                    results.add(sample)
                } catch (e: Exception) {
                    Log.w("SampleRepository", "Failed to parse sample JSON", e)
                }
            }
        } finally {
            cursor.close()
        }
        results
    }

    suspend fun getSamplesByCategory(category: SampleData.SampleCategory): List<SampleData> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT json_data FROM samples WHERE category = ? ORDER BY name",
            arrayOf(category.name)
        )
        val results = mutableListOf<SampleData>()
        try {
            while (cursor.moveToNext()) {
                val jsonString = cursor.getString(0)
                try {
                    results.add(json.decodeFromString<SampleData>(jsonString))
                } catch (e: Exception) {
                    Log.w("SampleRepository", "Failed to parse sample JSON", e)
                }
            }
        } finally {
            cursor.close()
        }
        results
    }

    suspend fun getSampleById(id: String): SampleData? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT json_data FROM samples WHERE id = ?", arrayOf(id))
        try {
            if (cursor.moveToFirst()) {
                return json.decodeFromString<SampleData>(cursor.getString(0))
            }
            null
        } finally {
            cursor.close()
        }
    }

    suspend fun deleteSample(id: String): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val rows = db.delete("samples", "id = ?", arrayOf(id))
        // Also delete slices
        db.delete("slices", "sample_id = ?", arrayOf(id))
        rows > 0
    }

    suspend fun updateSample(sample: SampleData) = withContext(Dispatchers.IO) {
        saveSamples(listOf(sample))
    }

    // ============================================================
    // SLICE MANAGEMENT
    // ============================================================

    suspend fun saveSlice(slice: SliceData) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = android.content.ContentValues().apply {
            put("id", slice.id)
            put("sample_id", slice.sampleId)
            put("name", slice.name)
            put("start_frame", slice.startFrame)
            put("end_frame", slice.endFrame)
            put("assigned_pad", slice.assignedPad)
            put("gain", slice.gain)
            put("pitch", slice.pitch)
            put("loop_enabled", if (slice.loopEnabled) 1 else 0)
            put("loop_start_frame", slice.loopStartFrame)
            put("loop_end_frame", slice.loopEndFrame)
            put("fade_in_frames", slice.fadeInFrames)
            put("fade_out_frames", slice.fadeOutFrames)
        }
        db.insertWithOnConflict("slices", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    suspend fun getSlicesForSample(sampleId: String): List<SliceData> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM slices WHERE sample_id = ? ORDER BY start_frame",
            arrayOf(sampleId)
        )
        val results = mutableListOf<SliceData>()
        try {
            while (cursor.moveToNext()) {
                results.add(SliceData(
                    id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                    sampleId = cursor.getString(cursor.getColumnIndexOrThrow("sample_id")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    startFrame = cursor.getInt(cursor.getColumnIndexOrThrow("start_frame")),
                    endFrame = cursor.getInt(cursor.getColumnIndexOrThrow("end_frame")),
                    assignedPad = cursor.getInt(cursor.getColumnIndexOrThrow("assigned_pad")),
                    gain = cursor.getFloat(cursor.getColumnIndexOrThrow("gain")),
                    pitch = cursor.getFloat(cursor.getColumnIndexOrThrow("pitch")),
                    loopEnabled = cursor.getInt(cursor.getColumnIndexOrThrow("loop_enabled")) == 1,
                    loopStartFrame = cursor.getInt(cursor.getColumnIndexOrThrow("loop_start_frame")),
                    loopEndFrame = cursor.getInt(cursor.getColumnIndexOrThrow("loop_end_frame")),
                    fadeInFrames = cursor.getInt(cursor.getColumnIndexOrThrow("fade_in_frames")),
                    fadeOutFrames = cursor.getInt(cursor.getColumnIndexOrThrow("fade_out_frames"))
                ))
            }
        } finally {
            cursor.close()
        }
        results
    }

    suspend fun getSliceById(id: String): SliceData? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM slices WHERE id = ?", arrayOf(id))
        try {
            if (cursor.moveToFirst()) {
                return SliceData(
                    id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                    sampleId = cursor.getString(cursor.getColumnIndexOrThrow("sample_id")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    startFrame = cursor.getInt(cursor.getColumnIndexOrThrow("start_frame")),
                    endFrame = cursor.getInt(cursor.getColumnIndexOrThrow("end_frame")),
                    assignedPad = cursor.getInt(cursor.getColumnIndexOrThrow("assigned_pad")),
                    gain = cursor.getFloat(cursor.getColumnIndexOrThrow("gain")),
                    pitch = cursor.getFloat(cursor.getColumnIndexOrThrow("pitch")),
                    loopEnabled = cursor.getInt(cursor.getColumnIndexOrThrow("loop_enabled")) == 1,
                    loopStartFrame = cursor.getInt(cursor.getColumnIndexOrThrow("loop_start_frame")),
                    loopEndFrame = cursor.getInt(cursor.getColumnIndexOrThrow("loop_end_frame")),
                    fadeInFrames = cursor.getInt(cursor.getColumnIndexOrThrow("fade_in_frames")),
                    fadeOutFrames = cursor.getInt(cursor.getColumnIndexOrThrow("fade_out_frames"))
                )
            }
            null
        } finally {
            cursor.close()
        }
    }

    suspend fun deleteSlice(id: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete("slices", "id = ?", arrayOf(id))
    }

    suspend fun updateSlicePadAssignment(sliceId: String, padIndex: Int) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = android.content.ContentValues().apply {
            put("assigned_pad", padIndex)
        }
        db.update("slices", values, "id = ?", arrayOf(sliceId))
    }

    // ============================================================
    // FILE COPY/IMPORT
    // ============================================================

    /**
     * Copy a user sample to app's internal storage and register it
     */
    suspend fun importUserSample(
        sourceFile: File,
        name: String,
        category: SampleData.SampleCategory = SampleData.SampleCategory.CUSTOM
    ): SampleData? = withContext(Dispatchers.IO) {
        try {
            val destDir = File(context.filesDir, "samples/user")
            destDir.mkdirs()

            val destFile = File(destDir, "${System.currentTimeMillis()}_${sourceFile.name}")
            sourceFile.copyTo(destFile)

            // TODO: Read actual audio file metadata (duration, sample rate, etc.)
            // For now, use defaults
            val sample = SampleData(
                name = name,
                filePath = destFile.absolutePath,
                durationFrames = 44100, // 1 second default
                sampleRate = 44100,
                channels = 1,
                bitDepth = 16,
                isBuiltIn = false,
                category = category
            )

            saveSamples(listOf(sample))
            return sample
        } catch (e: Exception) {
            Log.e("SampleRepository", "Failed to import sample", e)
            return null
        }
    }

    /**
     * Save a slice as a new sample file
     */
    suspend fun saveSliceAsSample(slice: SliceData, sourceSample: SampleData): SampleData? = withContext(Dispatchers.IO) {
        // This would require audio processing to extract the slice
        // For now, return a reference sample
        return SampleData(
            name = slice.name,
            filePath = sourceSample.filePath,
            durationFrames = slice.durationFrames,
            sampleRate = sourceSample.sampleRate,
            channels = sourceSample.channels,
            bitDepth = sourceSample.bitDepth,
            isBuiltIn = false,
            category = SampleData.SampleCategory.SLICED
        )
    }

    fun close() {
        dbHelper.close()
    }

    // ============================================================
    // DATABASE HELPER
    // ============================================================

    private class DatabaseHelper(context: Context) : SQLiteOpenHelper(
        context, "samples.db", null, 1
    ) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE samples (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    file_path TEXT NOT NULL,
                    duration_frames INTEGER,
                    sample_rate INTEGER,
                    channels INTEGER,
                    bit_depth INTEGER,
                    is_built_in INTEGER,
                    category TEXT,
                    date_added INTEGER,
                    json_data TEXT
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE slices (
                    id TEXT PRIMARY KEY,
                    sample_id TEXT NOT NULL,
                    name TEXT NOT NULL,
                    start_frame INTEGER,
                    end_frame INTEGER,
                    assigned_pad INTEGER,
                    gain REAL,
                    pitch REAL,
                    loop_enabled INTEGER,
                    loop_start_frame INTEGER,
                    loop_end_frame INTEGER,
                    fade_in_frames INTEGER,
                    fade_out_frames INTEGER,
                    FOREIGN KEY(sample_id) REFERENCES samples(id)
                )
            """.trimIndent())

            db.execSQL("CREATE INDEX idx_samples_category ON samples(category)")
            db.execSQL("CREATE INDEX idx_slices_sample_id ON slices(sample_id)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // Handle migrations
        }
    }
}
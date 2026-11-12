package com.hardtekk.electribe

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import com.hardtekk.electribe.audio.AudioEngine
import com.hardtekk.electribe.storage.SampleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Main Application class
 * Initializes audio engine and repositories on startup
 */
class HardtekkApplication : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Singleton instance
    companion object {
        @Suppress("UNUSED_PARAMETER")
        private var instance: HardtekkApplication? = null
        fun getInstance(): HardtekkApplication = instance!!
    }

    // Audio Engine
    private var audioEngine: AudioEngine? = null

    // Repositories
    private var sampleRepository: SampleRepository? = null

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize repositories
        sampleRepository = SampleRepository(this)

        // Initialize audio engine (will generate built-in sounds)
        initializeAudioEngine()

        // Keep screen on for performance
        // Note: This requires WAKE_LOCK permission
    }

    private fun initializeAudioEngine() {
        scope.launch {
            try {
                audioEngine = AudioEngine(this@HardtekkApplication)
                Log.d("HardtekkApplication", "Audio engine initialized")

                // Set up audio engine callbacks
                audioEngine?.onLoadCompleteCallback = {
                    Log.d("HardtekkApplication", "Built-in sounds loaded")
                }

                audioEngine?.onStepCallback = { step ->
                    // Step callback handled by sequencer
                }

                audioEngine?.onPatternChangeCallback = { patternIndex ->
                    Log.d("HardtekkApplication", "Pattern changed to $patternIndex")
                }
            } catch (e: Exception) {
                Log.e("HardtekkApplication", "Failed to initialize audio engine", e)
            }
        }
    }

    fun getAudioEngine(): AudioEngine? = audioEngine

    fun getSampleRepository(): SampleRepository? = sampleRepository

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            // App going to background - could pause audio
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        // Release non-critical resources
    }

    override fun onTerminate() {
        // Shutdown audio engine
        audioEngine?.shutdown()
        sampleRepository?.close()
        scope.cancel()
        super.onTerminate()
    }
}
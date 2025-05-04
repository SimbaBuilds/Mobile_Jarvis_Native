package com.cameronhightower.mobilejarvisnative.modules.voice.processors

import android.Manifest
import android.content.Context
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import com.cameronhightower.mobilejarvisnative.utils.PermissionUtils
import com.cameronhightower.mobilejarvisnative.utils.SpeechUtils
import com.cameronhightower.mobilejarvisnative.utils.UIUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Utility class to manage speech recognition
 */
object SpeechRecognitionManager {
    private const val TAG = "SpeechRecognitionMgr"
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var lastRecognitionStartTime = 0L
    private const val RECOGNITION_DEBOUNCE_MS = 1000L // Prevent duplicate starts within 1 second
    private var resultCallback: ((String) -> Unit)? = null
    private var retryCount = 0
    private const val MAX_RETRY_COUNT = 3
    private var noSpeechRetryCount = 0
    private const val MAX_NO_SPEECH_RETRY = 2
    
    // For capturing raw audio
    private var audioRecord: AudioRecord? = null
    private var isCapturingAudio = false
    private var isAudioRecordReleased = true
    private val SAMPLE_RATE = 16000 // 16kHz
    private val ENCODING = android.media.AudioFormat.ENCODING_PCM_16BIT
    private val CHANNEL = android.media.AudioFormat.CHANNEL_IN_MONO
    private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING) * 4
    
    private var capturedAudioData: ByteArray? = null
    
    /**
     * Check if speech recognition is already active
     */
    fun isRecognitionActive(): Boolean {
        return isListening || isCapturingAudio
    }
    
    /**
     * Initialize the speech recognizer
     */
    fun initialize(context: Context): Boolean {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                Log.d(TAG, "Speech recognizer initialized")
                return true
            } else {
                Log.e(TAG, "Speech recognition not available on this device")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing speech recognizer", e)
            return false
        }
    }
    
    /**
     * Set up the recognition listener
     */
    fun setupRecognitionListener(callback: (String) -> Unit) {
        resultCallback = callback
        
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
                isListening = true
                retryCount = 0 // Reset retry count on successful start
                
                // Start audio capture if needed
                if (!isCapturingAudio) {
                    startAudioCapture()
                }
            }
            
            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Beginning of speech")
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                // Not used
            }
            
            override fun onBufferReceived(buffer: ByteArray?) {
                // Not used
            }
            
            override fun onEndOfSpeech() {
                Log.d(TAG, "End of speech")
                isListening = false
                
                // Stop audio capture if needed with delay
                if (isCapturingAudio) {
                    // Add small delay to ensure all audio is captured
                    Handler(Looper.getMainLooper()).postDelayed({
                        stopAudioCapture()
                    }, 300) // 300ms delay
                }
            }
            
            override fun onError(error: Int) {
                val errorMessage = SpeechUtils.getSpeechRecognitionErrorMessage(error)
                Log.e(TAG, "Speech recognition error: $errorMessage")
                
                // Reset state flags
                isListening = false
                
                // Handle permission error by retrying after a delay (up to MAX_RETRY_COUNT times)
                if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS && retryCount < MAX_RETRY_COUNT) {
                    retryCount++
                    Log.w(TAG, "Permission error, will retry speech recognition (attempt $retryCount/$MAX_RETRY_COUNT)")
                    
                    // Notify callback with error
                    resultCallback?.invoke("")
                    return
                }
                
                // Special handling for "No speech detected" errors
                if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    Log.i(TAG, "No speech detected, checking if we should retry (count: $noSpeechRetryCount)")
                    
                    if (noSpeechRetryCount < MAX_NO_SPEECH_RETRY) {
                        noSpeechRetryCount++
                        Log.i(TAG, "Retrying speech recognition after no speech detected (attempt $noSpeechRetryCount/$MAX_NO_SPEECH_RETRY)")
                        
                        // Wait a short moment before restarting
                        Handler(Looper.getMainLooper()).postDelayed({
                            try {
                                // Create a new intent with the same parameters
                                val retryIntent = SpeechUtils.createSpeechRecognitionIntent().apply {
                                    // Add additional parameters for better performance
                                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000)
                                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 8000)
                                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 200)
                                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                                }
                                
                                // Restart listening
                                speechRecognizer?.startListening(retryIntent)
                                isListening = true
                                Log.i(TAG, "Speech recognition restarted after no speech detected")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error restarting speech recognition after no speech detected", e)
                                resultCallback?.invoke("")
                            }
                        }, 3000) // Short delay before retry
                        
                        return // Don't notify with empty result yet
                    } else {
                        Log.i(TAG, "No speech detected after $MAX_NO_SPEECH_RETRY retries, notifying with empty result")
                        noSpeechRetryCount = 0 // Reset the counter
                        resultCallback?.invoke("")
                        return
                    }
                }
                
                // Otherwise notify with empty result
                resultCallback?.invoke("")
            }
            
            override fun onResults(results: Bundle?) {
                // Reset recognition state
                isListening = false
                
                // Reset retry counter when we get results
                noSpeechRetryCount = 0
                
                try {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val spokenText = matches[0]
                        Log.i(TAG, "Speech recognized: \"$spokenText\"")
                        
                        // Handle spoken text via callback
                        resultCallback?.invoke(spokenText)
                    } else {
                        Log.w(TAG, "No speech recognized")
                        resultCallback?.invoke("")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing speech results", e)
                    resultCallback?.invoke("")
                }
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                // Not used
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {
                // Not used
            }
        })
        
        Log.d(TAG, "Recognition listener set up")
    }
    
    /**
     * Start listening for speech
     */
    fun startListening(context: Context) {
        val currentTime = System.currentTimeMillis()
        
        // Debounce mechanism - prevent duplicate starts in quick succession
        if (currentTime - lastRecognitionStartTime < RECOGNITION_DEBOUNCE_MS) {
            Log.w(TAG, "Ignoring start request due to debounce (${currentTime - lastRecognitionStartTime}ms < ${RECOGNITION_DEBOUNCE_MS}ms)")
            return
        }
        
        // Reset the no speech retry counter when starting a new listening session
        noSpeechRetryCount = 0
        
        // Only log warning if already listening - don't return, as we should restart listening
        if (isListening) {
            Log.w(TAG, "Already listening - restarting speech recognition")
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping existing speech recognition", e)
            }
        }
        
        try {
            Log.i(TAG, "Starting speech recognition")
            
            // First, ensure we have audio permission
            if (!PermissionUtils.hasPermission(context, Manifest.permission.RECORD_AUDIO)) {
                Log.e(TAG, "Missing RECORD_AUDIO permission, cannot start speech recognition")
                
                // Show error message
                UIUtils.showToast(context, "Microphone permission required", Toast.LENGTH_SHORT)
                
                // Notify with empty result
                resultCallback?.invoke("")
                return
            }
            
            // Check if speechRecognizer is null and try to reinitialize
            if (speechRecognizer == null) {
                Log.w(TAG, "Speech recognizer is null, trying to reinitialize")
                initialize(context)
                
                if (speechRecognizer == null) {
                    Log.e(TAG, "Failed to reinitialize speech recognizer")
                    UIUtils.showToast(context, "Failed to initialize speech recognition", Toast.LENGTH_SHORT)
                    resultCallback?.invoke("")
                    return
                }
            }
            
            // Update timestamp first, before any operations that might fail
            lastRecognitionStartTime = currentTime
            
            // Set flag to indicate we're now listening
            isListening = true
            
            // Start capturing audio if not already capturing
            if (!isCapturingAudio) {
                Log.i(TAG, "Starting audio capture")
                startAudioCapture()
            }
            
            // Create intent with recognition parameters
            val intent = SpeechUtils.createSpeechRecognitionIntent().apply {
                // Add additional parameters for better performance
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 8000)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 200)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            }
            
            // Start the speech recognizer with our intent
            speechRecognizer?.startListening(intent)
            
            Log.i(TAG, "Speech recognition started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition", e)
            isListening = false
            
            // Notify with empty result
            resultCallback?.invoke("")
            
            // Show error via toast
            try {
                UIUtils.showToast(context, "Speech recognition error: ${e.message}", Toast.LENGTH_SHORT)
            } catch (ignored: Exception) {
                // Ignore toast errors
            }
        }
    }
    
    /**
     * Get the captured audio data
     */
    fun getCapturedAudioData(): ByteArray? {
        val data = capturedAudioData
        capturedAudioData = null // Clear after retrieval to avoid reuse
        return data
    }
    
    /**
     * Start capturing raw audio
     */
    private fun startAudioCapture() {
        if (isCapturingAudio) {
            Log.w(TAG, "Already capturing audio, not starting another capture")
            return // Don't restart capture if already running
        }
        
        try {
            Log.d(TAG, "Creating AudioRecord with buffer size: $BUFFER_SIZE")
            
            // Create and configure AudioRecord instance
            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL,
                    ENCODING,
                    BUFFER_SIZE
                )
                isAudioRecordReleased = false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create AudioRecord instance: ${e.message}", e)
                return
            }
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord not initialized properly")
                audioRecord?.release()
                audioRecord = null
                isAudioRecordReleased = true
                return
            }
            
            val buffer = ByteArray(BUFFER_SIZE)
            val capturedAudio = ByteBuffer.allocate(BUFFER_SIZE * 20) // Max ~10 seconds @ 16kHz
            
            // Start recording
            try {
                audioRecord?.startRecording()
                isCapturingAudio = true
                Log.i(TAG, "Audio recording started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start audio recording: ${e.message}", e)
                audioRecord?.release()
                audioRecord = null
                isAudioRecordReleased = true
                return
            }
            
            // Run audio capture in a background thread
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d(TAG, "Started audio capture background processing")
                    var readCount = 0
                    var totalBytesRead = 0
                    
                    while (isCapturingAudio && audioRecord != null) {
                        val bytesRead = try {
                            audioRecord?.read(buffer, 0, buffer.size) ?: 0
                        } catch (e: Exception) {
                            Log.e(TAG, "Error reading from AudioRecord: ${e.message}")
                            break
                        }
                        
                        readCount++
                        
                        if (bytesRead > 0) {
                            try {
                                if (capturedAudio.remaining() >= bytesRead) {
                                    capturedAudio.put(buffer, 0, bytesRead)
                                    totalBytesRead += bytesRead
                                } else {
                                    Log.w(TAG, "Buffer full, cannot store more audio")
                                    break
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error storing audio data: ${e.message}")
                                break
                            }
                        }
                        
                        // Small delay to prevent CPU overuse
                        try {
                            kotlinx.coroutines.delay(5)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error during delay: ${e.message}")
                            break
                        }
                    }
                    
                    // Prepare the final audio data
                    try {
                        capturedAudio.flip()
                        val audioData = ByteArray(capturedAudio.limit())
                        capturedAudio.get(audioData)
                        
                        Log.i(TAG, "Audio capture completed: ${audioData.size} bytes from $readCount reads")
                        
                        // Store the captured audio for later processing
                        storeAudioData(audioData)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error finalizing audio data: ${e.message}")
                    }
                    
                    // Ensure we stop and release resources
                    try {
                        stopAudioCapture()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error stopping audio capture after completion: ${e.message}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during audio capture", e)
                    
                    try {
                        stopAudioCapture()
                    } catch (ignored: Exception) {
                        // Ignore cleanup errors
                    }
                }
            }
            
            Log.d(TAG, "Started audio capture for processing")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting audio capture", e)
            isCapturingAudio = false
            audioRecord?.release()
            audioRecord = null
        }
    }
    
    /**
     * Store audio data for later processing
     */
    private fun storeAudioData(audioData: ByteArray) {
        if (audioData.size < 1000) {
            Log.w(TAG, "Audio data too small (${audioData.size} bytes), not storing")
            return
        }
        
        capturedAudioData = audioData
        Log.d(TAG, "Stored ${audioData.size} bytes of audio data for later processing")
    }
    
    /**
     * Stop capturing raw audio
     */
    private fun stopAudioCapture() {
        Log.d(TAG, "Stopping audio capture")
        isCapturingAudio = false
        
        if (audioRecord == null || isAudioRecordReleased) {
            Log.d(TAG, "AudioRecord already released, nothing to do")
            return
        }
        
        try {
            if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                try {
                    if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        audioRecord?.stop()
                        Log.d(TAG, "AudioRecord stopped")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping AudioRecord", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking AudioRecord state", e)
        } finally {
            try {
                audioRecord?.release()
                Log.d(TAG, "AudioRecord released")
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing AudioRecord", e)
            } finally {
                audioRecord = null
                isAudioRecordReleased = true
            }
        }
    }
    
    /**
     * Cleanup resources
     */
    fun destroy() {
        Log.d(TAG, "Cleaning up speech recognition resources")
        
        // First reset state variables
        isListening = false
        resultCallback = null
        
        // Handle speechRecognizer cleanup
        if (speechRecognizer != null) {
            try {
                speechRecognizer?.destroy()
                Log.d(TAG, "Speech recognizer destroyed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error destroying speech recognizer", e)
            } finally {
                speechRecognizer = null
            }
        }
        
        // Handle audio recording cleanup
        if (isCapturingAudio || audioRecord != null) {
            try {
                stopAudioCapture()
                Log.d(TAG, "Audio capture stopped during cleanup")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping audio capture during cleanup", e)
                // Even if stopAudioCapture fails, try to reset the state
                isCapturingAudio = false
                audioRecord = null
            }
        }
        
        Log.d(TAG, "Speech recognition resources cleanup completed")
    }
    
    /**
     * Force restart speech recognition
     */
    fun forceRestart(context: Context) {
        Log.i(TAG, "Forcing restart of speech recognition")
        
        // First, stop any ongoing recognition
        try {
            if (isListening) {
                speechRecognizer?.stopListening()
                isListening = false
            }
            
            // Stop audio capture if active
            if (isCapturingAudio) {
                stopAudioCapture()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping existing speech recognition", e)
        }
        
        // Short delay before restarting
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                // Start listening again
                startListening(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error restarting speech recognition", e)
            }
        }, 300) // 300ms delay
    }
} 
package com.anonymous.MobileJarvisNative.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.anonymous.MobileJarvisNative.ConfigManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Client for Deepgram API for text-to-speech conversion
 */
class DeepgramClient(private val context: Context) {
    private val TAG = "DeepgramClient"
    private var isInitialized = false
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var configManager: ConfigManager
    private var mediaPlayer: MediaPlayer? = null
    
    /**
     * Initialize the Deepgram client
     */
    fun initialize() {
        try {
            Log.d(TAG, "Initializing Deepgram client")
            
            // Initialize OkHttp client with longer timeouts for audio processing
            okHttpClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
            
            // Get config manager for API keys
            configManager = ConfigManager.getInstance()
            
            // Initialize media player
            mediaPlayer = MediaPlayer()
            
            isInitialized = true
            Log.i(TAG, "Deepgram client initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Deepgram client: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Convert text to speech and play the audio
     */
    suspend fun speak(text: String) = withContext(Dispatchers.IO) {
        if (!isInitialized) {
            Log.e(TAG, "Deepgram client not initialized")
            throw IllegalStateException("Deepgram client not initialized")
        }
        
        Log.d(TAG, "Converting text to speech: '$text'")
        
        try {
            // Get Deepgram API key from config
            val apiKey = configManager.getDeepgramApiKey()
            if (apiKey.isBlank()) {
                Log.e(TAG, "Deepgram API key not found in config")
                throw IllegalStateException("Deepgram API key not found")
            }
            
            // Create request JSON
            val requestJson = JSONObject().apply {
                put("text", text)
                put("voice", "aura-professional")  // Using a professional voice
                put("model", "aura-asteria-en")    // High-quality English model
                put("sample_rate", 24000)          // Higher sample rate for better quality
                put("encoding", "mp3")             // MP3 encoding for better quality
            }
            
            // Create request
            val request = Request.Builder()
                .url("https://api.deepgram.com/v1/speak")
                .header("Authorization", "Token $apiKey")
                .header("Content-Type", "application/json")
                .post(requestJson.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()
            
            Log.d(TAG, "Sending TTS request to Deepgram")
            
            // Execute request
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                val errorCode = response.code
                Log.e(TAG, "Error from Deepgram API: $errorCode, body: $errorBody")
                
                // Parse error details if available
                try {
                    val errorJson = JSONObject(errorBody)
                    val errorMessage = errorJson.optString("error", "Unknown error")
                    val errorDetails = errorJson.optString("message", "")
                    Log.e(TAG, "Deepgram error details: $errorMessage - $errorDetails")
                } catch (e: Exception) {
                    Log.e(TAG, "Could not parse error response: ${e.message}")
                }
                
                throw IOException("Deepgram API error: $errorCode")
            }
            
            // Save audio to temporary file
            val audioBytes = response.body?.bytes() ?: throw IOException("Empty response from Deepgram")
            val tempFile = File(context.cacheDir, "tts_${UUID.randomUUID()}.mp3")
            
            FileOutputStream(tempFile).use { fos ->
                fos.write(audioBytes)
                fos.flush()
            }
            
            Log.d(TAG, "TTS audio saved to: ${tempFile.absolutePath}")
            
            // Play the audio
            withContext(Dispatchers.Main) {
                try {
                    // Reset media player if it's already playing
                    mediaPlayer?.apply {
                        reset()
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                        )
                        setDataSource(tempFile.absolutePath)
                        setOnPreparedListener { it.start() }
                        setOnCompletionListener {
                            // Delete the temp file after playback
                            tempFile.delete()
                            Log.d(TAG, "TTS audio playback completed and file deleted")
                        }
                        prepareAsync()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error playing TTS audio: ${e.message}", e)
                    tempFile.delete()
                    throw e
                }
            }
            
            Log.i(TAG, "TTS request successful and playback started")
        } catch (e: Exception) {
            Log.e(TAG, "Error in TTS process: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Release resources
     */
    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        isInitialized = false
        Log.d(TAG, "Deepgram client resources released")
    }
} 
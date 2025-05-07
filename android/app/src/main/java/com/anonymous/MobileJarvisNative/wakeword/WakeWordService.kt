package com.anonymous.MobileJarvisNative.wakeword

import ai.picovoice.porcupine.PorcupineActivationException
import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
import ai.picovoice.porcupine.Porcupine
import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import android.content.pm.ServiceInfo
import android.content.SharedPreferences
import com.anonymous.MobileJarvisNative.MainActivity
import com.anonymous.MobileJarvisNative.utils.PermissionUtils
import com.anonymous.MobileJarvisNative.modules.voice.VoiceProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.Properties

/**
 * Service that listens for the wake word "Jarvis" in the background
 */
class WakeWordService : Service() {
    
    private val TAG = "WakeWordService"
    private var porcupineManager: PorcupineManager? = null
    private var isRunning = false
    private var serviceScope = CoroutineScope(Dispatchers.Main)
    private var stateMonitorJob: Job? = null
    private lateinit var voiceProcessor: VoiceProcessor
    private lateinit var prefs: SharedPreferences
    
    override fun onCreate() {
        try {
            super.onCreate()
            Log.i(TAG, "Service onCreate called")
            
            // Initialize SharedPreferences
            prefs = getSharedPreferences("wakeword_prefs", Context.MODE_PRIVATE)
            
            // Check if wake word detection is enabled
            if (!isWakeWordEnabled()) {
                Log.i(TAG, "Wake word detection is disabled")
                stopSelf()
                return
            }
            
            createNotificationChannel()
            
            // Starting foreground service with type on Android 10+ (API 29+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            } else {
                startForeground(NOTIFICATION_ID, createNotification())
            }
            
            Toast.makeText(this, "Jarvis detection service starting...", Toast.LENGTH_SHORT).show()
            
            // Check if we can access the Porcupine BuiltInKeyword class
            if (!checkPorcupineLibrary()) {
                Log.e(TAG, "Cannot access Porcupine library classes")
                Toast.makeText(this, "Porcupine library not properly initialized", Toast.LENGTH_LONG).show()
                stopSelf()
                return
            }
            
            // Initialize voice processor
            voiceProcessor = VoiceProcessor()
            voiceProcessor.initialize(this)
            
            // Set up voice state monitoring
            setupVoiceStateMonitoring()
            
            initWakeWordDetection()
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Native library error: ${e.message}", e)
            Toast.makeText(this, "Device compatibility issue: Missing native library", Toast.LENGTH_LONG).show()
            stopSelf()
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error in service onCreate: ${e.message}", e)
            Toast.makeText(this, "Service startup failed: ${e.message}", Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }
    
    private fun isWakeWordEnabled(): Boolean {
        return prefs.getBoolean("wake_word_enabled", false)
    }
    
    private fun setupVoiceStateMonitoring() {
        stateMonitorJob?.cancel()
        
        stateMonitorJob = serviceScope.launch {
            try {
                // Monitor voice processor state
                if (voiceProcessor.isProcessing() || voiceProcessor.isSpeaking()) {
                    pauseWakeWordDetection()
                } else {
                    resumeWakeWordDetection()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in voice state monitoring", e)
            }
        }
    }
    
    private fun pauseWakeWordDetection() {
        if (isRunning) {
            try {
                Log.i(TAG, "Pausing wake word detection during conversation")
                porcupineManager?.stop()
            } catch (e: Exception) {
                Log.e(TAG, "Error pausing wake word detection", e)
            }
        }
    }
    
    private fun resumeWakeWordDetection() {
        if (isRunning) {
            try {
                serviceScope.launch {
                    delay(500)
                    Log.i(TAG, "Resuming wake word detection after conversation")
                    try {
                        porcupineManager?.start()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error resuming wake word detection", e)
                        initWakeWordDetection()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling wake word detection resume", e)
            }
        }
    }
    
    private fun checkPorcupineLibrary(): Boolean {
        return try {
            val keywordValue = Porcupine.BuiltInKeyword.JARVIS.name
            Log.d(TAG, "Successfully accessed Porcupine BuiltInKeyword: $keywordValue")
            true
        } catch (e: NoClassDefFoundError) {
            Log.e(TAG, "Failed to find Porcupine classes: ${e.message}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error checking Porcupine library: ${e.message}", e)
            false
        }
    }
    
    private fun getPicovoiceAccessKey(): String {
        return try {
            val inputStream: InputStream = assets.open("config.properties")
            val properties = Properties()
            properties.load(inputStream)
            properties.getProperty("picovoice_access_key", "")
        } catch (e: Exception) {
            Log.e(TAG, "Error reading config.properties: ${e.message}", e)
            ""
        }
    }
    
    private fun initWakeWordDetection() {
        try {
            if (!PermissionUtils.hasPermission(this, Manifest.permission.RECORD_AUDIO)) {
                Log.e(TAG, "Missing RECORD_AUDIO permission")
                Toast.makeText(
                    this, 
                    "Microphone permission required for wake word detection", 
                    Toast.LENGTH_LONG
                ).show()
                
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra("REQUEST_AUDIO_PERMISSION", true)
                }
                startActivity(intent)
                
                stopSelf()
                return
            }
            
            val accessKey = getPicovoiceAccessKey()
            
            if (accessKey.isEmpty()) {
                Log.e(TAG, "Access key is empty")
                Toast.makeText(
                    this, 
                    "Picovoice access key is not set", 
                    Toast.LENGTH_LONG
                ).show()
                stopSelf()
                return
            }

            try {
                Log.d(TAG, "Initializing PorcupineManager")
                porcupineManager = PorcupineManager.Builder()
                    .setAccessKey(accessKey)
                    .setKeyword(Porcupine.BuiltInKeyword.JARVIS)
                    .setSensitivity(0.7f)
                    .build(this, wakeWordCallback)

                Log.d(TAG, "PorcupineManager initialized successfully")
                Toast.makeText(this, "Wake word detection initialized", Toast.LENGTH_SHORT).show()

                porcupineManager?.start()
                isRunning = true
                Log.i(TAG, "Wake word detection started")
                Toast.makeText(this, "Now listening for 'Jarvis'", Toast.LENGTH_SHORT).show()
            } catch (e: PorcupineActivationException) {
                handlePorcupineError("Failed to initialize Porcupine: ${e.message}", e)
            } catch (e: Exception) {
                handlePorcupineError("Error initializing wake word detection: ${e.message}", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in initWakeWordDetection: ${e.message}", e)
            Toast.makeText(this, "Unexpected error: ${e.message}", Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }
    
    private fun handlePorcupineError(message: String, error: Exception) {
        Log.e(TAG, message, error)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        stopSelf()
    }
    
    private val wakeWordCallback = PorcupineManagerCallback { keywordIndex ->
        try {
            Log.i(TAG, "Wake word 'Jarvis' detected!")
            
            // Start voice recognition
            voiceProcessor.startListening()
            
            // Bring application to foreground
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("WAKE_WORD_ACTIVATED", true)
            }
            startActivity(intent)
            
            Toast.makeText(this, "Jarvis activated! Listening...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error in wake word callback: ${e.message}", e)
        }
    }
    
    private fun createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Wake Word Detection",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Background service for wake word detection"
                    setShowBadge(false)
                }
                
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
                Log.d(TAG, "Notification channel created")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating notification channel: ${e.message}", e)
        }
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Voice Assistant")
            .setContentText("Listening for 'Jarvis'")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Service onStartCommand called")
        
        if (!isRunning) {
            initWakeWordDetection()
        }
        
        return START_STICKY
    }
    
    override fun onDestroy() {
        Log.i(TAG, "Service onDestroy called")
        Toast.makeText(this, "Stopping Jarvis detection", Toast.LENGTH_SHORT).show()
        try {
            isRunning = false
            stateMonitorJob?.cancel()
            porcupineManager?.stop()
            porcupineManager?.delete()
            porcupineManager = null
            voiceProcessor.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up wake word detection: ${e.message}", e)
        }
        Log.i(TAG, "Wake word detection service stopped")
        super.onDestroy()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    companion object {
        private const val CHANNEL_ID = "wake_word_channel"
        private const val NOTIFICATION_ID = 1
    }
} 
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
import com.anonymous.MobileJarvisNative.voice.VoiceManager
import com.anonymous.MobileJarvisNative.ConfigManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Service that listens for the wake word "Jarvis" in the background
 */
class WakeWordService : Service() {
    
    private val TAG = "WakeWordService"
    private var porcupineManager: PorcupineManager? = null
    private var isRunning = false
    private var serviceScope = CoroutineScope(Dispatchers.Main)
    private var stateMonitorJob: Job? = null
    private lateinit var voiceManager: VoiceManager
    private lateinit var prefs: SharedPreferences
    private lateinit var configManager: ConfigManager
    
    // Notification constants
    private val NOTIFICATION_CHANNEL_ID = "wake_word_channel"
    private val NOTIFICATION_ID = 1001

    companion object {
        private var isServiceRunning = false

        fun isRunning(): Boolean {
            return isServiceRunning
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service onCreate called")
        
        // First create the notification channel
        createNotificationChannel()
        
        // Start foreground immediately
        startForegroundWithNotification()
        
        // Initialize minimal requirements
        prefs = getSharedPreferences("wakeword_prefs", Context.MODE_PRIVATE)
    }

    /**
     * Helper method to start foreground service with notification
     */
    private fun startForegroundWithNotification() {
        try {
            val notification = createNotification()
            
            // Starting foreground service with type on Android 10+ (API 29+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            
            Log.d(TAG, "Successfully called startForeground() with notification")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground: ${e.message}", e)
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Service onStartCommand called")
        
        // Start foreground again in case onCreate didn't complete properly
        startForegroundWithNotification()
        
        // Complete initialization in background thread
        Thread {
            try {
                initializeService()
            } catch (e: Exception) {
                Log.e(TAG, "Error in service initialization: ${e.message}", e)
                stopSelf()
            }
        }.start()
        
        return START_STICKY
    }

    /**
     * Initialize the service after it has been started in foreground
     */
    private fun initializeService() {
        if (isServiceRunning) {
            Log.w(TAG, "Service already running, stopping duplicate service")
            stopSelf()
            return
        }
        
        try {
            // Initialize ConfigManager
            ConfigManager.init(this)
            configManager = ConfigManager.getInstance()
            
            // Check if wake word detection is enabled
            if (!isWakeWordEnabled()) {
                Log.i(TAG, "Wake word detection is disabled")
                stopSelf()
                return
            }

            isServiceRunning = true
            isRunning = true
            
            Toast.makeText(this, "Jarvis detection service starting...", Toast.LENGTH_SHORT).show()
            
            // Initialize remaining components
            initializeComponents()
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error in service initialization: ${e.message}", e)
            Toast.makeText(this, "Service startup failed: ${e.message}", Toast.LENGTH_LONG).show()
            cleanup()
            stopSelf()
        }
    }

    private fun initializeComponents() {
        try {
            // Check if we can access the Porcupine BuiltInKeyword class
            if (!checkPorcupineLibrary()) {
                Log.e(TAG, "Cannot access Porcupine library classes")
                Toast.makeText(this, "Porcupine library not properly initialized", Toast.LENGTH_LONG).show()
                cleanup()
                stopSelf()
                return
            }
            
            // Initialize voice manager
            voiceManager = VoiceManager.getInstance()
            
            // Set up voice state monitoring
            setupVoiceStateMonitoring()
            
            initWakeWordDetection()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing components: ${e.message}", e)
            cleanup()
            stopSelf()
        }
    }

    private fun cleanup() {
        try {
            stateMonitorJob?.cancel()
            porcupineManager?.stop()
            porcupineManager = null
            isRunning = false
            isServiceRunning = false
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup: ${e.message}", e)
        }
    }

    private fun isWakeWordEnabled(): Boolean {
        return prefs.getBoolean("wake_word_enabled", false)
    }
    
    private fun setupVoiceStateMonitoring() {
        stateMonitorJob?.cancel()
        
        stateMonitorJob = serviceScope.launch {
            try {
                // Check voice state every second
                while (true) {
                    delay(1000)
                    
                    val currentState = voiceManager.voiceState.value
                    if (currentState !is VoiceManager.VoiceState.IDLE) {
                        pauseWakeWordDetection()
                    } else {
                        resumeWakeWordDetection()
                    }
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
        return configManager.getPicovoiceAccessKey()
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
            Log.d(TAG, "Initializing Picovoice with access key length: ${accessKey.length}")
            
            if (accessKey.isEmpty()) {
                Log.e(TAG, "Access key is empty")
                Toast.makeText(
                    this, 
                    "Picovoice access key not found. Wake word detection disabled.", 
                    Toast.LENGTH_LONG
                ).show()
                stopSelf()
                return
            }
            
            // Keyword callback
            val porcupineCallback = object : PorcupineManagerCallback {
                override fun invoke(keywordIndex: Int) {
                    try {
                        onWakeWordDetected(keywordIndex)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in wake word callback: ${e.message}", e)
                    }
                }
            }
            
            // Define keywords - for now just use "Jarvis"
            val keywords = arrayOf(Porcupine.BuiltInKeyword.JARVIS)
            
            // Sensitivity (0.0-1.0), higher means more sensitive but more false positives
            val sensitivities = floatArrayOf(0.7f)
            
            try {
                Log.d(TAG, "Creating PorcupineManager with sensitivity: 0.7f")
                // Initialize porcupine manager
                porcupineManager = PorcupineManager.Builder()
                    .setAccessKey(accessKey)
                    .setKeywords(keywords)
                    .setSensitivities(sensitivities)
                    .build(this, porcupineCallback)
                
                Log.d(TAG, "Starting PorcupineManager wake word detection")
                // Start listening for wake word
                porcupineManager?.start()
                isRunning = true
                
                Log.i(TAG, "Wake word detection started successfully")
            } catch (e: PorcupineActivationException) {
                Log.e(TAG, "Porcupine activation error: ${e.message}", e)
                Toast.makeText(
                    this, 
                    "Invalid Picovoice access key. Wake word detection disabled.", 
                    Toast.LENGTH_LONG
                ).show()
                stopSelf()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing wake word detection: ${e.message}", e)
            Toast.makeText(
                this, 
                "Error setting up wake word detection: ${e.message}", 
                Toast.LENGTH_LONG
            ).show()
            stopSelf()
        }
    }
    
    private fun onWakeWordDetected(keywordIndex: Int) {
        Log.d(TAG, "Wake word detected with keyword index: $keywordIndex at time: ${System.currentTimeMillis()}")
        
        try {
            val timestamp = System.currentTimeMillis()
            
            // Trigger voice manager
            voiceManager.onWakeWordDetected(timestamp)
            
            // Send broadcast for optional UI update
            val intent = Intent("com.anonymous.MobileJarvisNative.WAKE_WORD_DETECTED")
            intent.putExtra("timestamp", timestamp)
            sendBroadcast(intent)
            
            // Also notify React Native side via broadcast
            try {
                val context = applicationContext
                val reactIntent = Intent("com.anonymous.MobileJarvisNative.WAKE_WORD_DETECTED_RN")
                reactIntent.setPackage(context.packageName)
                context.sendBroadcast(reactIntent)
                Log.d(TAG, "Sent wake word detection broadcast to React Native")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending wake word broadcast to React Native: ${e.message}", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling wake word detection: ${e.message}", e)
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Wake Word Detection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Used for wake word detection service"
                enableLights(false)
                enableVibration(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Wake Word Detection Active")
            .setContentText("Listening for 'Jarvis'")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
        
        return builder.build()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cleanup()
        Log.i(TAG, "Service destroyed")
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
} 
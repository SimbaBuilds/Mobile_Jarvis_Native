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
import android.content.IntentFilter
import android.content.BroadcastReceiver
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
import com.anonymous.MobileJarvisNative.utils.Constants
import kotlinx.coroutines.SupervisorJob

/**
 * Service that listens for the wake word "Jarvis" in the background
 */
class WakeWordService : Service() {
    
    private val TAG = "WakeWordService"
    private var porcupineManager: PorcupineManager? = null
    private var isRunning = false
    private var isPaused = false
    private var isServiceRunning = false
    private var serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var stateMonitorJob: Job? = null
    private lateinit var voiceManager: VoiceManager
    private lateinit var prefs: SharedPreferences
    private lateinit var configManager: ConfigManager
    private lateinit var brWakeWordPause: BroadcastReceiver
    private var wakeWordCallback: PorcupineManagerCallback? = null
    
    // Notification constants
    private val NOTIFICATION_CHANNEL_ID = "wake_word_channel"
    private val NOTIFICATION_ID = 1001

    companion object {
        fun isRunning(): Boolean {
            return instance?.isServiceRunning ?: false
        }
        
        @Volatile
        private var instance: WakeWordService? = null
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service onCreate called (WakeWordService)")
        instance = this
        isServiceRunning = true
        prefs = getSharedPreferences("wakeword_prefs", Context.MODE_PRIVATE)
        createNotificationChannel()
        serviceScope.launch(Dispatchers.Main) {
            startForegroundWithNotification()
        }
        registerPauseResumeReceiver()
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
        Log.i(TAG, "Service onStartCommand called (WakeWordService)")
        Log.i(TAG, "Launching foreground notification and initialization coroutine")
        serviceScope.launch(Dispatchers.Main) {
            try {
                startForegroundWithNotification()
                Toast.makeText(this@WakeWordService, "Jarvis detection service starting...", Toast.LENGTH_SHORT).show()
                Log.i(TAG, "Foreground started, launching initializeService() in IO")
                serviceScope.launch(Dispatchers.IO) {
                    try {
                        Log.i(TAG, "Calling initializeService()...")
                        initializeService()
                        Log.i(TAG, "initializeService() completed")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in service initialization: ${e.message}", e)
                        serviceScope.launch(Dispatchers.Main) {
                            Toast.makeText(this@WakeWordService, "Service startup failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                        stopSelf()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in onStartCommand: ${e.message}", e)
                stopSelf()
            }
        }
        return START_STICKY
    }

    /**
     * Initialize the service after it has been started in foreground
     */
    private fun initializeService() {
        Log.i(TAG, "Entered initializeService()")
        try {
            ConfigManager.init(this)
            configManager = ConfigManager.getInstance()
            Log.i(TAG, "ConfigManager initialized")
            if (!isWakeWordEnabled()) {
                Log.i(TAG, "Wake word detection is disabled")
                stopSelf()
                Log.i(TAG, "Exiting initializeService() because wake word is disabled")
                return
            }
            Log.i(TAG, "Calling initWakeWordDetection() from initializeService()")
            
            isServiceRunning = true
            isRunning = true
            
            // Initialize remaining components
            initializeComponents()
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error in service initialization: ${e.message}", e)
            serviceScope.launch(Dispatchers.Main) {
                Toast.makeText(this@WakeWordService, "Service startup failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
            cleanup()
            stopSelf()
        }
    }

    private fun initializeComponents() {
        try {
            // Check if we can access the Porcupine BuiltInKeyword class
            if (!checkPorcupineLibrary()) {
                Log.e(TAG, "Cannot access Porcupine library classes")
                serviceScope.launch(Dispatchers.Main) {
                    Toast.makeText(this@WakeWordService, "Porcupine library not properly initialized", Toast.LENGTH_LONG).show()
                }
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
                        Log.d(TAG, "🎙️ Raw Porcupine callback received for keyword index: $keywordIndex")
                        onWakeWordDetected(keywordIndex)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in wake word callback: ${e.message}", e)
                    }
                }
            }
            
            // Define keywords - for now just use "Jarvis"
            val keywords = arrayOf(Porcupine.BuiltInKeyword.JARVIS)
            Log.d(TAG, "Setting up with keyword: ${keywords[0].name}")
            
            // Sensitivity (0.0-1.0), higher means more sensitive but more false positives
            val sensitivities = floatArrayOf(0.7f)
            Log.d(TAG, "Setting sensitivity to: ${sensitivities[0]}")
            
            try {
                Log.d(TAG, "Creating PorcupineManager...")
                // Initialize porcupine manager
                porcupineManager = PorcupineManager.Builder()
                    .setAccessKey(accessKey)
                    .setKeywords(keywords)
                    .setSensitivities(sensitivities)
                    .build(this, porcupineCallback)
                
                Log.d(TAG, "🚀 PorcupineManager created successfully! Starting detection...")
                // Start listening for wake word
                porcupineManager?.start()
                isRunning = true
                
                Log.i(TAG, "✅ Wake word detection started successfully ✅")
                serviceScope.launch(Dispatchers.Main) {
                    Toast.makeText(
                        applicationContext,
                        "Wake word detection started - listening for 'Jarvis'",
                        Toast.LENGTH_SHORT
                    ).show()
                }
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
        val timestamp = System.currentTimeMillis()
        val timeString = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date(timestamp))
        
        Log.d(TAG, "-----------------------------------------------------")
        Log.d(TAG, "🎤 WAKE WORD DETECTED! 🎤 at $timeString (index: $keywordIndex)")
        Log.d(TAG, "-----------------------------------------------------")
        
        try {
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
                reactIntent.putExtra("timestamp", timestamp)
                reactIntent.setPackage(context.packageName)
                context.sendBroadcast(reactIntent)
                Log.d(TAG, "Sent wake word detection broadcast to React Native")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending wake word broadcast to React Native: ${e.message}", e)
            }
            
            // Also show a toast notification for debugging
            try {
                serviceScope.launch(Dispatchers.Main) {
                    Toast.makeText(
                        applicationContext,
                        "Wake word 'Jarvis' detected at $timeString",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error showing toast: ${e.message}", e)
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
    
    /**
     * Register broadcast receiver for pause/resume commands
     */
    private fun registerPauseResumeReceiver() {
        brWakeWordPause = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Constants.Actions.PAUSE_WAKE_WORD_KEEP_LISTENING) {
                    Log.i(TAG, "Received broadcast to pause wake word detection but keep mic active")
                    pauseWakeWordButKeepMicActive()
                } else if (intent.action == Constants.Actions.RESUME_WAKE_WORD) {
                    Log.i(TAG, "Received broadcast to resume wake word detection")
                    resumeWakeWordDetectionFromPaused()
                }
            }
        }
        
        val filter = IntentFilter().apply {
            addAction(Constants.Actions.PAUSE_WAKE_WORD_KEEP_LISTENING)
            addAction(Constants.Actions.RESUME_WAKE_WORD)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(brWakeWordPause, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(brWakeWordPause, filter)
        }
        
        Log.d(TAG, "Registered pause/resume broadcast receiver")
    }
    
    /**
     * Pause wake word detection but keep microphone active for other voice processing
     */
    private fun pauseWakeWordButKeepMicActive() {
        if (isPaused) {
            Log.d(TAG, "Wake word detection already paused")
            return
        }
        
        try {
            Log.i(TAG, "Pausing wake word detection but keeping mic active")
            
            // Set paused flag
            isPaused = true
            
            // Update notification to show paused state
            updateNotification("Voice active", "Listening for your command...")
            
            // Note: We're not stopping the recorder or detector, just ignoring wake word events
            // This allows the microphone to remain active for voice processing
            try {
                porcupineManager?.stop()
                Log.d(TAG, "Explicitly stopped porcupineManager to pause wake word detection")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping porcupineManager during pause: ${e.message}", e)
            }
            
            // Disable the callback to prevent wake word triggers
            wakeWordCallback = null
            
            Log.d(TAG, "Wake word detection paused, microphone still active")
            
            // Set a timer to automatically resume wake word detection if it's stuck in paused state
            serviceScope.launch {
                try {
                    // After 2 minutes, check if we're still paused and should auto-resume
                    delay(2 * 60 * 1000L) // 2 minutes
                    if (isPaused) {
                        Log.w(TAG, "Wake word detection stuck in paused state for 2 minutes, auto-resuming")
                        resumeWakeWordDetectionFromPaused()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in auto-resume timer: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing wake word detection: ${e.message}", e)
        }
    }
    
    /**
     * Resume wake word detection with paused state handling
     */
    private fun resumeWakeWordDetectionFromPaused() {
        if (!isPaused) {
            Log.d(TAG, "Wake word detection not paused")
            return
        }
        
        try {
            Log.i(TAG, "Resuming wake word detection from paused state")
            
            // Reset paused flag
            isPaused = false
            
            // Update notification
            updateNotification("Listening for wake word", "Say 'Jarvis' to activate")
            
            // Restore wake word callback
            wakeWordCallback = createWakeWordCallback()
            
            // Explicitly restart the porcupine manager
            try {
                porcupineManager?.start()
                Log.d(TAG, "Successfully restarted porcupineManager")
            } catch (e: Exception) {
                Log.e(TAG, "Error restarting porcupineManager, will try to reinitialize: ${e.message}", e)
                
                // If starting fails, try to reinitialize
                serviceScope.launch(Dispatchers.IO) {
                    try {
                        // Short delay before reinitializing
                        delay(500)
                        Log.d(TAG, "Reinitializing wake word detection after failed resume")
                        initWakeWordDetection()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reinitializing wake word detection: ${e.message}", e)
                    }
                }
            }
            
            Log.d(TAG, "Wake word detection resumed")
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming wake word detection: ${e.message}", e)
            
            // If resuming fails, attempt to reinitialize the whole wake word detection
            serviceScope.launch(Dispatchers.IO) {
                try {
                    delay(1000)
                    Log.w(TAG, "Attempting to reinitialize wake word detection after resume failure")
                    initWakeWordDetection()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to reinitialize wake word detection: ${e.message}", e)
                }
            }
        }
    }
    
    /**
     * Update notification with new title and text
     */
    private fun updateNotification(title: String, text: String) {
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(Constants.NOTIFICATION_ID, builder.build())
    }

    /**
     * Create wake word callback
     */
    private fun createWakeWordCallback(): PorcupineManagerCallback {
        return object : PorcupineManagerCallback {
            override fun invoke(keywordIndex: Int) {
                try {
                    Log.d(TAG, "🎙️ Wake word callback received for keyword index: $keywordIndex")
                    onWakeWordDetected(keywordIndex)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in wake word callback: ${e.message}", e)
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cleanup()
        
        // Unregister pause/resume receiver
        try {
            unregisterReceiver(brWakeWordPause)
            Log.d(TAG, "Unregistered pause/resume broadcast receiver")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering pause/resume receiver: ${e.message}", e)
        }
        
        Log.i(TAG, "Service destroyed")
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
} 
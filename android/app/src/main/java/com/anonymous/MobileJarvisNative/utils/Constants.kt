package com.cameronhightower.aivoiceassistant.utils

/**
 * Application-wide constants
 */
object Constants {
    // Wake word constants
    const val WAKE_WORD_ACTION = "com.cameronhightower.aivoiceassistant.WAKE_WORD_DETECTED"
    
    // Notification constants
    const val NOTIFICATION_CHANNEL_ID = "voice_assistant_channel"
    const val NOTIFICATION_ID = 1001
    
    // Shared preferences keys
    object Prefs {
        const val WAKE_WORD_ENABLED = "wake_word_enabled"
        const val ASSISTANT_VOLUME = "assistant_volume"
        const val USE_CUSTOM_LLM = "use_custom_llm"
        const val CUSTOM_LLM_URL = "custom_llm_url"
        const val CUSTOM_LLM_KEY = "custom_llm_key"
    }
    
    // Config property keys
    object Config {
        const val PICOVOICE_ACCESS_KEY = "picovoice_access_key"
        const val OPENAI_API_KEY = "openai_api_key"
        const val GOOGLE_API_KEY = "google_api_key"
        const val WEATHER_API_KEY = "weather_api_key"
        const val NEWS_API_KEY = "news_api_key"
        const val VAPI_API_KEY = "vapi_api_key"
        const val VAPI_ASSISTANT_ID = "vapi_assistant_id"
        const val MISTRAL_API_KEY = "mistral_api_key"
        const val ELEVENLABS_API_KEY = "elevenlabs_api_key"
        const val DEEPGRAM_API_KEY = "deepgram_api_key"
    }
    
    // Intent extras
    object Extras {
        const val COMMAND_TEXT = "command_text"
        const val RESPONSE_TEXT = "response_text"
    }
    
    // Request codes
    object RequestCodes {
        const val SPEECH_RECOGNITION = 1001
        const val PERMISSIONS = 1002
        const val BATTERY_OPTIMIZATION = 1003
    }
    
    // Permissions
    object Permissions {
        val REQUIRED_PERMISSIONS = arrayOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.INTERNET
        )
        
        val OPTIONAL_PERMISSIONS = arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.READ_CONTACTS
        )
    }
} 
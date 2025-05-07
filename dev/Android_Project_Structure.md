## The Android project structure for MobileJarvisNative is organized as follows:

android/app/src/main/
├── java/
│   └── com/
│       └── anonymous/
│           └── MobileJarvisNative/
│               ├── MainActivity.kt
│               ├── MainApplication.kt
│               ├── ConfigManager.kt
│               ├── voice/
│               │   ├── VoiceManager.kt
│               │   └── VoiceProcessor.kt
│               ├── wakeword/
│               │   ├── WakeWordService.kt
│               │   ├── WakeWordModule.kt
│               │   └── WakeWordPackage.kt
│               └── utils/
│                   ├── SpeechUtils.kt
│                   ├── Constants.kt
│                   ├── ServiceUtils.kt
│                   ├── TextToSpeechManager.kt
│                   └── PermissionUtils.kt
├── res/
│   ├── layout/
│   ├── values/
│   ├── drawable/
│   └── mipmap/
├── assets/
└── AndroidManifest.xml 
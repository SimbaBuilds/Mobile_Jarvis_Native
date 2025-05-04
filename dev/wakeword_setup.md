# Wake Word Detection Setup

This document explains how to set up and use the wake word detection feature in the MobileJarvisNative app.

## Overview

Wake word detection allows the app to listen for the keyword "Jarvis" in the background, activating the voice assistant when detected. The implementation uses the [Picovoice Porcupine](https://picovoice.ai/docs/porcupine/) wake word engine.

## Setup Instructions

### 1. Add Picovoice Access Key

You need to obtain an access key from the [Picovoice Console](https://console.picovoice.ai/):

1. Create a free Picovoice account at https://console.picovoice.ai/
2. Create a new access key in the console
3. Add the key to the `android/app/src/main/assets/config.properties` file:

```
picovoice.accessKey=YOUR_PICOVOICE_ACCESS_KEY_HERE
```

### 2. Android Permissions

The app needs microphone permission to detect wake words. This is implemented in the `MainActivity.java` and `WakeWordService.kt` files. The permission will be requested at runtime.

### 3. Adding to React Native

The wake word functionality is exposed to React Native through the following files:

- `WakeWordModule.kt`: React Native bridge for wake word functionality
- `WakeWordPackage.kt`: Package registration for React Native
- `WakeWordService.ts`: TypeScript interface for the native module
- `useWakeWord.ts`: Custom hook for wake word detection
- `WakeWordToggle.tsx`: UI component to toggle wake word detection

## How It Works

1. The `WakeWordService` is a foreground service that runs in the background listening for the wake word "Jarvis"
2. When detected, it notifies the `VoiceManager` which coordinates with the voice processing pipeline
3. The `VoiceContext` in React Native manages the voice state and allows components to react to voice events
4. The `useWakeWord` hook provides a simple interface for React components to start/stop wake word detection

## Testing

To test wake word detection:

1. Build and run the app on an Android device
2. Enable wake word detection using the toggle
3. Say "Jarvis" near your device
4. Verify that the app activates and starts listening

## Troubleshooting

- **Permission Issues**: Ensure microphone permission is granted
- **Wake Word Not Detected**: Try adjusting the sensitivity in `WakeWordService.kt`
- **Background Service Stopping**: Check battery optimization settings on the device

## Additional Resources

- [Picovoice Porcupine Documentation](https://picovoice.ai/docs/porcupine/)
- [Android Foreground Services](https://developer.android.com/guide/components/foreground-services)
- [React Native Native Modules](https://reactnative.dev/docs/native-modules-intro) 
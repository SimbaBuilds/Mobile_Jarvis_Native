# Feature to File Mapping

This document maps each feature to its associated Kotlin native modules and React Native TypeScript files.

## 1. Wake Word Detection

### Kotlin Native Files
- `android/app/src/main/java/com/cameronhightower/mobilejarvisnative/modules/wakeword/WakeWordModule.kt`: React Native bridge for wake word functionality
- `android/app/src/main/java/com/cameronhightower/mobilejarvisnative/modules/wakeword/WakeWordPackage.kt`: Package registration for React Native
- `android/app/src/main/java/com/cameronhightower/mobilejarvisnative/modules/wakeword/WakeWordService.kt`: Background service for wake word detection
- `android/app/src/main/java/com/cameronhightower/mobilejarvisnative/utils/ConfigManager.kt`: Configuration management for wake word settings
- `android/app/src/main/assets/config.properties`: Configuration file storing wake word settings and API keys

### React Native Files
- `src/services/NativeModules/WakeWordService.ts`: TypeScript interface for the native module
- `src/features/voice/hooks/useWakeWord.ts`: Custom hook for wake word detection
- `src/features/voice/components/WakeWordToggle.tsx`: UI component to toggle wake word detection
- `src/types/voice.ts`: TypeScript types for wake word functionality
- `src/features/voice/context/VoiceContext.tsx`: Context provider for voice state management
- `src/features/voice/hooks/useVoiceState.ts`: Hook for managing voice state

### Documentation
- `dev/wakeword_setup.md`: Setup instructions and documentation for wake word detection

## 2. Voice Processing

### Kotlin Native Files
- `android/app/src/main/java/com/cameronhightower/mobilejarvisnative/modules/voice/VoiceModule.kt`: React Native bridge for voice functionality
- `android/app/src/main/java/com/cameronhightower/mobilejarvisnative/modules/voice/VoicePackage.kt`: Package registration for React Native
- `android/app/src/main/java/com/cameronhightower/mobilejarvisnative/modules/voice/VoiceProcessor.kt`: Main voice processing implementation
- `android/app/src/main/java/com/cameronhightower/mobilejarvisnative/modules/voice/processors/VoiceProcessor.kt`: Specialized voice processors

### React Native Files
- `src/services/NativeModules/VoiceService.ts`: TypeScript interface for the native module
- `src/features/voice/hooks/useVoiceRecognition.ts`: Custom hook for voice recognition
- `src/features/voice/hooks/useVoiceState.ts`: Hook for managing voice state
- `src/features/voice/context/VoiceContext.tsx`: Context for voice state management
- `src/features/voice/components/VoiceStatusIndicator.tsx`: UI component showing voice status
- `src/components/VoiceAssistant/VoiceAssistant.tsx`: Main voice assistant component
- `src/components/VoiceAssistant/VoiceButton.tsx`: Button for activating voice input
- `src/components/VoiceAssistant/VoiceResponseDisplay.tsx`: Component for displaying responses
- `src/components/ErrorBoundary/VoiceErrorBoundary.tsx`: Error boundary for handling voice feature errors

## 3. Permissions Management

### Kotlin Native Files
- `android/app/src/main/java/com/cameronhightower/mobilejarvisnative/modules/permissions/PermissionsModule.kt`: React Native bridge for permissions
- `android/app/src/main/java/com/cameronhightower/mobilejarvisnative/modules/permissions/PermissionsPackage.kt`: Package registration for React Native
- `android/app/src/main/java/com/cameronhightower/mobilejarvisnative/utils/PermissionUtils.kt`: Utility functions for handling permissions
- `android/app/src/main/java/com/cameronhightower/mobilejarvisnative/utils/Constants.kt`: Constants for permission request codes

### React Native Files
- `src/services/NativeModules/PermissionsService.ts`: TypeScript interface for the native module
- `src/hooks/usePermissions.ts`: Custom hook for managing permissions
- `src/utils/permissions.ts`: Utility functions for handling permissions
- `src/types/permissions.ts`: TypeScript types for permissions

### Integration Points
- `android/app/src/main/java/com/cameronhightower/mobilejarvisnative/MainActivity.java`: Permission handling in main activity
- `App.tsx`: Permission UI and state management
- `src/features/voice/components/WakeWordToggle.tsx`: Integration with wake word feature

### Features
- Microphone permission management for voice recognition
- Battery optimization exemption for background services
- Permission state management and UI feedback
- Integration with wake word detection feature
- Cross-platform support (Android/iOS)
- TypeScript type safety throughout the React Native layer

## 4. Audio Processing

### Kotlin Native Files
- `android/app/src/main/java/com/cameronhightower/mobilejarvisnative/modules/audio/AudioModule.kt`: React Native bridge for audio functionality
- `android/app/src/main/java/com/cameronhightower/mobilejarvisnative/modules/audio/AudioPackage.kt`: Package registration for React Native
- `android/app/src/main/java/com/cameronhightower/mobilejarvisnative/modules/audio/TextToSpeechManager.kt`: Text-to-speech implementation
- `android/app/src/main/java/com/cameronhightower/mobilejarvisnative/utils/SpeechUtils.kt`: Speech-related utility functions

### React Native Files
- `src/services/NativeModules/VoiceService.ts`: Also includes audio functionality interfaces


## 5. Utilities & Core Functionality

### Kotlin Native Files
- `android/app/src/main/java/com/cameronhightower/mobilejarvisnative/MainActivity.java`: Main activity for the React Native app
- `android/app/src/main/java/com/cameronhightower/mobilejarvisnative/MainApplication.java`: Application class for React Native
- `android/app/src/main/java/com/cameronhightower/mobilejarvisnative/utils/Constants.kt`: Constants used across native modules
- `android/app/src/main/java/com/cameronhightower/mobilejarvisnative/utils/NetworkUtils.kt`: Network-related utility functions
- `android/app/src/main/java/com/cameronhightower/mobilejarvisnative/utils/UIUtils.kt`: UI-related utility functions
- `android/app/src/main/java/com/cameronhightower/mobilejarvisnative/utils/ServiceUtils.kt`: Service-related utility functions

### React Native Files
- `App.tsx`: Main application component
- `index.ts`: Entry point for the React Native app 
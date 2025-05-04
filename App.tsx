import React, { useEffect } from 'react';
import { View, Text, StyleSheet, SafeAreaView, ScrollView, Platform, ActivityIndicator } from 'react-native';
import { VoiceProvider } from './src/features/voice/context/VoiceContext';
import { WakeWordToggle } from './src/features/voice/components/WakeWordToggle';
import { usePermissions } from './src/hooks/usePermissions';
import { VoiceAssistant } from './src/components/VoiceAssistant/VoiceAssistant';
import { VoiceErrorBoundary } from './src/components/ErrorBoundary/VoiceErrorBoundary';
import WakeWordService from './src/services/NativeModules/WakeWordService';

const PermissionsSection = () => {
  const {
    permissions,
    loading,
    requestMicrophone,
    requestBatteryExemption,
    hasMicrophonePermission,
    hasBatteryOptimizationExemption,
  } = usePermissions();

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#0000ff" />
        <Text style={styles.loadingText}>Checking permissions...</Text>
      </View>
    );
  }

  return (
    <View style={styles.permissionsSection}>
      <Text style={styles.sectionTitle}>Required Permissions</Text>
      
      <View style={styles.permissionItem}>
        <Text style={styles.permissionTitle}>Microphone Access</Text>
        <Text style={styles.permissionStatus}>
          Status: {hasMicrophonePermission ? '✅ Granted' : '❌ Not Granted'}
        </Text>
        {!hasMicrophonePermission && (
          <Text 
            style={styles.permissionButton}
            onPress={requestMicrophone}
          >
            Grant Microphone Permission
          </Text>
        )}
      </View>

      {Platform.OS === 'android' && (
        <View style={styles.permissionItem}>
          <Text style={styles.permissionTitle}>Battery Optimization</Text>
          <Text style={styles.permissionStatus}>
            Status: {hasBatteryOptimizationExemption ? '✅ Optimized' : '❌ Not Optimized'}
          </Text>
          {!hasBatteryOptimizationExemption && (
            <Text 
              style={styles.permissionButton}
              onPress={requestBatteryExemption}
            >
              Optimize Battery Usage
            </Text>
          )}
        </View>
      )}
    </View>
  );
};

export default function App() {
  // Initialize any needed configurations here
  useEffect(() => {
    // Initialize app settings
    const initializeApp = async () => {
      try {
        // Check if wake word detection is available
        const isAvailable = await WakeWordService.isAvailable();
        console.log(`Wake word detection available: ${isAvailable}`);
      } catch (error) {
        console.error('Error initializing app:', error);
      }
    };
    
    initializeApp();
  }, []);

  return (
    <VoiceProvider>
      <SafeAreaView style={styles.container}>
        <ScrollView contentContainerStyle={styles.scrollContent}>
          <View style={styles.header}>
            <Text style={styles.title}>Voice Assistant</Text>
            <Text style={styles.subtitle}>Say "Jarvis" to activate</Text>
          </View>
          
          <PermissionsSection />
          
          <View style={styles.settingsSection}>
            <Text style={styles.sectionTitle}>Settings</Text>
            <WakeWordToggle label="Wake Word Detection (Jarvis)" />
          </View>

          <VoiceErrorBoundary>
            <View style={styles.voiceAssistantContainer}>
              <VoiceAssistant onSpeechResult={(text) => console.log('Speech recognized:', text)} />
            </View>
          </VoiceErrorBoundary>
          
          <View style={styles.infoSection}>
            <Text style={styles.infoText}>
              This app uses wake word detection to listen for "Jarvis" in the background.
              When detected, the app will activate voice recognition automatically.
            </Text>
            {Platform.OS === 'android' && (
              <Text style={styles.infoText}>
                Note: On Android, a service runs in the background with a notification while
                wake word detection is active. Your wake word preference will be remembered
                between app sessions.
              </Text>
            )}
          </View>
        </ScrollView>
      </SafeAreaView>
    </VoiceProvider>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  scrollContent: {
    padding: 16,
  },
  header: {
    marginVertical: 24,
    alignItems: 'center',
  },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#333',
  },
  subtitle: {
    fontSize: 16,
    color: '#666',
    marginTop: 8,
  },
  loadingContainer: {
    padding: 20,
    alignItems: 'center',
  },
  loadingText: {
    marginTop: 10,
    color: '#666',
  },
  permissionsSection: {
    marginVertical: 16,
    backgroundColor: '#fff',
    borderRadius: 8,
    padding: 16,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
  },
  permissionItem: {
    marginVertical: 8,
  },
  permissionTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
  },
  permissionStatus: {
    fontSize: 14,
    color: '#666',
    marginTop: 4,
  },
  permissionButton: {
    color: '#007AFF',
    marginTop: 8,
    fontSize: 14,
    fontWeight: '500',
  },
  settingsSection: {
    marginVertical: 16,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '600',
    marginBottom: 8,
    color: '#333',
  },
  infoSection: {
    backgroundColor: '#e6f2ff',
    padding: 16,
    borderRadius: 8,
    marginTop: 16,
  },
  infoText: {
    fontSize: 14,
    color: '#444',
    marginBottom: 8,
  },
  voiceAssistantContainer: {
    minHeight: 200,
    marginVertical: 16,
  },
});

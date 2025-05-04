import React, { useEffect } from 'react';
import { View, Text, StyleSheet, SafeAreaView, ScrollView, Platform } from 'react-native';
import { VoiceProvider } from './src/features/voice/context/VoiceContext';
import { WakeWordToggle } from './src/features/voice/components/WakeWordToggle';

export default function App() {
  // Initialize any needed configurations here
  useEffect(() => {
    // Initialize app settings
  }, []);

  return (
    <VoiceProvider>
      <SafeAreaView style={styles.container}>
        <ScrollView contentContainerStyle={styles.scrollContent}>
          <View style={styles.header}>
            <Text style={styles.title}>Voice Assistant</Text>
            <Text style={styles.subtitle}>Say "Jarvis" to activate</Text>
          </View>
          
          <View style={styles.settingsSection}>
            <Text style={styles.sectionTitle}>Settings</Text>
            <WakeWordToggle label="Wake Word Detection (Jarvis)" />
          </View>
          
          <View style={styles.infoSection}>
            <Text style={styles.infoText}>
              This app uses wake word detection to listen for "Jarvis" in the background.
              When detected, the app will activate voice recognition automatically.
            </Text>
            {Platform.OS === 'android' && (
              <Text style={styles.infoText}>
                Note: On Android, a service runs in the background with a notification while
                wake word detection is active.
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
});

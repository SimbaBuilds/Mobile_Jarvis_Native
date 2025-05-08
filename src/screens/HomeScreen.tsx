import React from 'react';
import { View, Text, StyleSheet, ScrollView, SafeAreaView, Platform } from 'react-native';
import { WakeWordStatus } from '../features/wakeword/components/WakeWordStatus';
import { WakeWordToggle } from '../features/wakeword/components/WakeWordToggle';
import { VoiceAssistant } from '../shared/components/VoiceAssistant/VoiceAssistant';
import { VoiceErrorBoundary } from '../shared/components/ErrorBoundary/VoiceErrorBoundary';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';

type RootStackParamList = {
  Home: undefined;
  Settings: undefined;
};

type HomeScreenNavigationProp = NativeStackNavigationProp<RootStackParamList, 'Home'>;

type Props = {
  navigation: HomeScreenNavigationProp;
};

export const HomeScreen: React.FC<Props> = ({ navigation }) => {
  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.scrollContent}>
        <View style={styles.header}>
          <Text style={styles.title}>Voice Assistant</Text>
          <Text style={styles.subtitle}>Say "Jarvis" to activate</Text>
        </View>
        
        <View style={styles.settingsSection}>
          <WakeWordToggle />
          <WakeWordStatus />
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
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#121212',
  },
  scrollContent: {
    padding: 16,
  },
  header: {
    alignItems: 'center',
    marginBottom: 24,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#FFFFFF',
    marginBottom: 8,
  },
  subtitle: {
    fontSize: 16,
    color: '#B0B0B0',
  },
  settingsSection: {
    marginBottom: 24,
  },
  voiceAssistantContainer: {
    marginBottom: 24,
  },
  infoSection: {
    backgroundColor: '#1E1E1E',
    padding: 16,
    borderRadius: 8,
  },
  infoText: {
    color: '#B0B0B0',
    fontSize: 14,
    lineHeight: 20,
    marginBottom: 8,
  },
}); 
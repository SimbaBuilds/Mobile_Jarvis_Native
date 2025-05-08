import React from 'react';
import { View, Text, StyleSheet, ScrollView, SafeAreaView, ActivityIndicator, Platform } from 'react-native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';

import { WakeWordToggle } from '../wakeword/components/WakeWordToggle';
import { WakeWordStatus } from '../wakeword/components/WakeWordStatus';
import { usePermissions } from './usePermissions';

type RootStackParamList = {
  Home: undefined;
  Settings: undefined;
};

type SettingsScreenNavigationProp = NativeStackNavigationProp<RootStackParamList, 'Settings'>;

type Props = {
  navigation: SettingsScreenNavigationProp;
};

export const SettingsScreen: React.FC<Props> = ({ navigation }) => {
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
      <View style={[styles.container, styles.loadingContainer]}>
        <ActivityIndicator size="large" color="#FFFFFF" />
        <Text style={styles.loadingText}>Checking permissions...</Text>
      </View>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.scrollContent}>
        <View style={styles.header}>
          <Text style={styles.title}>Settings</Text>
        </View>
        
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Wake Word</Text>
          <WakeWordToggle />
          <WakeWordStatus />
        </View>
        
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Permissions</Text>
          
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
        
        <View style={styles.infoSection}>
          <Text style={styles.infoText}>
            Wake word detection allows the app to listen for "Jarvis" in the background.
            When enabled, a notification will appear to indicate that the service is running.
          </Text>
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
  loadingContainer: {
    justifyContent: 'center',
    alignItems: 'center',
  },
  loadingText: {
    color: '#FFFFFF',
    marginTop: 16,
  },
  header: {
    marginBottom: 24,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#FFFFFF',
  },
  section: {
    marginBottom: 24,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '500',
    color: '#FFFFFF',
    marginBottom: 16,
  },
  permissionItem: {
    marginBottom: 16,
  },
  permissionTitle: {
    fontSize: 16,
    fontWeight: '500',
    color: '#FFFFFF',
  },
  permissionStatus: {
    fontSize: 14,
    color: '#B0B0B0',
    marginTop: 4,
  },
  permissionButton: {
    color: '#4A90E2',
    marginTop: 8,
    fontSize: 14,
    fontWeight: '500',
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
  },
}); 
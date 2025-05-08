import React, { useEffect, useState } from 'react';
import { View, Text, Switch, StyleSheet, Platform, Alert } from 'react-native';
import WakeWordService from '../../wakeword/WakeWordService';
import { usePermissions } from '../../settings/usePermissions';

interface WakeWordToggleProps {
  label: string;
}

/**
 * Toggle switch for enabling/disabling wake word detection
 */
export const WakeWordToggle: React.FC<WakeWordToggleProps> = ({ label }) => {
  const [isEnabled, setIsEnabled] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const { hasMicrophonePermission, hasBatteryOptimizationExemption, requestMicrophone } = usePermissions();

  // Only allow enabling wake word detection if necessary permissions are granted
  const canEnableWakeWord = Platform.OS === 'ios' || 
    (hasMicrophonePermission && hasBatteryOptimizationExemption);

  // Initialize state from persistence
  useEffect(() => {
    const loadStatus = async () => {
      try {
        setIsLoading(true);
        const status = await WakeWordService.getStatus();
        setIsEnabled(status);
      } catch (error) {
        console.error('Error loading wake word status:', error);
      } finally {
        setIsLoading(false);
      }
    };

    loadStatus();
  }, []);

  const handleToggle = async () => {
    if (!canEnableWakeWord) {
      if (Platform.OS === 'android' && !hasMicrophonePermission) {
        Alert.alert(
          'Permission Required',
          'Microphone permission is required for wake word detection.',
          [
            { text: 'Cancel', style: 'cancel' },
            { text: 'Grant Permission', onPress: requestMicrophone }
          ]
        );
      }
      return;
    }

    try {
      setIsLoading(true);
      
      if (isEnabled) {
        // Turn off wake word detection
        const result = await WakeWordService.stopDetection();
        if (!result.success) {
          console.error('Failed to stop wake word detection:', result.error);
          Alert.alert('Error', `Failed to disable wake word detection: ${result.error}`);
          return;
        }
      } else {
        // Turn on wake word detection
        const result = await WakeWordService.startDetection();
        if (!result.success) {
          console.error('Failed to start wake word detection:', result.error);
          Alert.alert('Error', `Failed to enable wake word detection: ${result.error}`);
          return;
        }
      }
      
      // Toggle state
      setIsEnabled(!isEnabled);
    } catch (error) {
      console.error('Error toggling wake word detection:', error);
      Alert.alert('Error', `An error occurred: ${error}`);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <View style={styles.row}>
        <Text style={styles.label}>{label}</Text>
        <Switch
          value={isEnabled}
          onValueChange={handleToggle}
          disabled={isLoading || !canEnableWakeWord}
        />
      </View>
      
      {!canEnableWakeWord && Platform.OS === 'android' && (
        <Text style={styles.warning}>
          Please grant microphone permission and optimize battery usage to enable wake word detection.
        </Text>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    marginVertical: 8,
  },
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 8,
  },
  label: {
    fontSize: 16,
    color: '#333',
    flex: 1,
    marginRight: 16,
  },
  warning: {
    fontSize: 12,
    color: '#ff6b6b',
    marginTop: 4,
  },
});

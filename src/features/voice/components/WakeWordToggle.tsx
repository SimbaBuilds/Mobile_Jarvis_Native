import React from 'react';
import { View, Text, Switch, StyleSheet, Platform } from 'react-native';
import { useWakeWord } from '../hooks/useWakeWord';
import { usePermissions } from '../../settings/hooks/usePermissions';

interface WakeWordToggleProps {
  label: string;
}

/**
 * Toggle switch for enabling/disabling wake word detection
 */
export const WakeWordToggle: React.FC<WakeWordToggleProps> = ({ label }) => {
  const { isEnabled, toggleWakeWord } = useWakeWord();
  const { hasMicrophonePermission, hasBatteryOptimizationExemption } = usePermissions();

  const canEnableWakeWord = Platform.OS === 'ios' || 
    (hasMicrophonePermission && hasBatteryOptimizationExemption);

  const handleToggle = () => {
    if (canEnableWakeWord) {
      toggleWakeWord();
    }
  };

  return (
    <View style={styles.container}>
      <View style={styles.row}>
        <Text style={styles.label}>{label}</Text>
        <Switch
          value={isEnabled}
          onValueChange={handleToggle}
          disabled={!canEnableWakeWord}
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

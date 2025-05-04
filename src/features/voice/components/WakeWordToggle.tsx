import React, { useEffect } from 'react';
import { View, Switch, Text, StyleSheet, ActivityIndicator } from 'react-native';
import { useWakeWord } from '../hooks/useWakeWord';
import { useVoiceState } from '../hooks/useVoiceState';

interface WakeWordToggleProps {
  label?: string;
}

/**
 * Toggle switch for enabling/disabling wake word detection
 */
export const WakeWordToggle: React.FC<WakeWordToggleProps> = ({
  label = 'Wake Word Detection',
}) => {
  const {
    isAvailable,
    isActive,
    isLoading,
    error,
    toggleDetection,
  } = useWakeWord();
  
  const { setWakeWordEnabled } = useVoiceState();
  
  // Update the global voice state when wake word is toggled
  useEffect(() => {
    setWakeWordEnabled(isActive);
  }, [isActive, setWakeWordEnabled]);
  
  if (!isAvailable) {
    return (
      <View style={styles.container}>
        <Text style={styles.label}>{label}</Text>
        <Text style={styles.errorText}>
          Not available on this device
        </Text>
      </View>
    );
  }
  
  return (
    <View style={styles.container}>
      <View style={styles.labelContainer}>
        <Text style={styles.label}>{label}</Text>
        {error && <Text style={styles.errorText}>{error}</Text>}
      </View>
      
      <View style={styles.controlContainer}>
        {isLoading ? (
          <ActivityIndicator size="small" color="#0066cc" />
        ) : (
          <Switch
            value={isActive}
            onValueChange={toggleDetection}
            disabled={isLoading}
            trackColor={{ false: '#767577', true: '#81b0ff' }}
            thumbColor={isActive ? '#0066cc' : '#f4f3f4'}
          />
        )}
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: 16,
    backgroundColor: '#ffffff',
    borderRadius: 8,
    marginVertical: 8,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 2,
  },
  labelContainer: {
    flex: 1,
  },
  label: {
    fontSize: 16,
    fontWeight: '500',
  },
  errorText: {
    color: 'red',
    fontSize: 12,
    marginTop: 4,
  },
  controlContainer: {
    marginLeft: 16,
  },
});

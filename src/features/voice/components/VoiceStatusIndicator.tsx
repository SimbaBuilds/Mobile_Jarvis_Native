import React from 'react';
import { View, Text, StyleSheet, ActivityIndicator } from 'react-native';
import { useVoiceState } from '../hooks/useVoiceState';
import { VoiceState } from '../../../services/NativeModules/VoiceService';

/**
 * Component that displays the current voice assistant state
 */
export const VoiceStatusIndicator: React.FC = () => {
  const { voiceState, isListening, isSpeaking, isError } = useVoiceState();
  
  // Determine indicator color based on state
  const getStatusColor = () => {
    if (isError) return 'red';
    if (isSpeaking) return 'blue';
    if (isListening) return 'green';
    return 'gray';
  };
  
  // Get status text to display
  const getStatusText = () => {
    switch (voiceState) {
      case VoiceState.IDLE:
        return 'Ready';
      case VoiceState.WAKE_WORD_DETECTED:
        return 'Wake word detected';
      case VoiceState.LISTENING:
        return 'Listening...';
      case VoiceState.PROCESSING:
        return 'Processing...';
      case VoiceState.SPEAKING:
        return 'Speaking...';
      case VoiceState.ERROR:
        return 'Error';
      default:
        return 'Unknown state';
    }
  };
  
  return (
    <View style={styles.container}>
      <View style={[styles.indicator, { backgroundColor: getStatusColor() }]}>
        {(isListening || voiceState === VoiceState.PROCESSING) && (
          <ActivityIndicator color="white" size="small" />
        )}
      </View>
      <Text style={styles.statusText}>{getStatusText()}</Text>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 8,
  },
  indicator: {
    width: 16,
    height: 16,
    borderRadius: 8,
    marginRight: 8,
    justifyContent: 'center',
    alignItems: 'center',
  },
  statusText: {
    fontSize: 16,
    fontWeight: '500',
  },
});

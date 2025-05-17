import React from 'react';
import { View, StyleSheet, ActivityIndicator, FlatList, Text, TouchableOpacity } from 'react-native';
import { VoiceButton } from './VoiceButton';
import { VoiceResponseDisplay } from './VoiceResponseDisplay';
import { useVoice } from '../VoiceContext';
import { VoiceStatusIndicator } from './VoiceStatusIndicator';
import { VoiceState } from '../types/voice';
import { Ionicons } from '@expo/vector-icons';

interface VoiceAssistantProps {
  onSpeechResult?: (text: string) => void;
}

/**
 * Main component for voice assistant functionality
 * Combines voice button, response display, and status indicator
 */
export const VoiceAssistant: React.FC<VoiceAssistantProps> = ({ 
  onSpeechResult 
}) => {
  const { 
    isListening,
    isSpeaking,
    transcript,
    voiceState,
    isError,
    chatHistory,
    setTranscript,
    interruptSpeech
  } = useVoice();

  // When a speech result is received, call the callback
  React.useEffect(() => {
    if (transcript && onSpeechResult) {
      onSpeechResult(transcript);
    }
  }, [transcript, onSpeechResult]);
  
  // Format timestamp to readable time
  const formatTime = (timestamp: number) => {
    return new Date(timestamp).toLocaleTimeString([], { 
      hour: '2-digit', 
      minute: '2-digit' 
    });
  };

  /**
   * Handle interrupt button press
   * This stops the current TTS playback and changes the state from SPEAKING/RESPONDING to LISTENING
   * allowing the user to speak again without waiting for the current response to finish
   */
  const handleInterrupt = async () => {
    console.log('Interrupting speech...');
    const result = await interruptSpeech();
    console.log('Interrupt result:', result);
  };

  if (isError) {
    throw isError; // This will be caught by the error boundary
  }
  
  return (
    <View style={styles.container}>
      <VoiceStatusIndicator />
      
      {chatHistory.length > 0 ? (
        <FlatList
          data={chatHistory}
          keyExtractor={(item, index) => `chat-${index}-${item.timestamp}`}
          style={styles.chatList}
          renderItem={({ item }) => (
            <View style={[
              styles.chatBubble, 
              item.role === 'user' ? styles.userBubble : styles.assistantBubble
            ]}>
              <Text style={styles.chatText}>{item.content}</Text>
              <Text style={styles.timeText}>{formatTime(item.timestamp)}</Text>
            </View>
          )}
        />
      ) : (
        <View style={styles.emptyChatContainer}>
          <Text style={styles.emptyChatText}>Say "Jarvis" to activate the assistant</Text>
        </View>
      )}
      
      {/* Interrupt button - only shown when the assistant is speaking */}
      {isSpeaking && (
        <TouchableOpacity 
          style={styles.interruptButton}
          onPress={handleInterrupt}
          activeOpacity={0.7}
          accessibilityLabel="Stop speaking"
          accessibilityHint="Stops the current speech and allows you to speak again"
        >
          <Ionicons name="stop-circle" size={24} color="white" />
          <Text style={styles.interruptButtonText}>Tap to Interrupt</Text>
        </TouchableOpacity>
      )}
      
      <View style={styles.buttonContainer}>
        <VoiceButton />
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'space-between',
    padding: 16,
  },
  chatList: {
    flex: 1,
    marginVertical: 16,
  },
  chatBubble: {
    padding: 12,
    borderRadius: 16,
    marginVertical: 6,
    maxWidth: '80%',
    minWidth: 100,
  },
  userBubble: {
    backgroundColor: '#3B82F6',
    alignSelf: 'flex-end',
    marginLeft: 40,
    borderBottomRightRadius: 4,
  },
  assistantBubble: {
    backgroundColor: '#262626',
    alignSelf: 'flex-start',
    marginRight: 40,
    borderBottomLeftRadius: 4,
  },
  chatText: {
    color: '#FFFFFF',
    fontSize: 16,
  },
  timeText: {
    color: 'rgba(255, 255, 255, 0.6)',
    fontSize: 11,
    marginTop: 4,
    textAlign: 'right',
  },
  buttonContainer: {
    alignItems: 'center',
    padding: 20,
  },
  emptyChatContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  emptyChatText: {
    color: '#888888',
    fontSize: 16,
    textAlign: 'center',
  },
  interruptButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#e74c3c',
    paddingVertical: 12,
    paddingHorizontal: 20,
    borderRadius: 25,
    marginBottom: 20,
    alignSelf: 'center',
    elevation: 4,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.3,
    shadowRadius: 4,
  },
  interruptButtonText: {
    color: 'white',
    fontWeight: '600',
    fontSize: 16,
    marginLeft: 8,
  },
});

import React from 'react';
import { View, StyleSheet, ActivityIndicator, FlatList, Text } from 'react-native';
import { VoiceButton } from './VoiceButton';
import { VoiceResponseDisplay } from './VoiceResponseDisplay';
import { useVoice } from '../VoiceContext';
import { VoiceStatusIndicator } from './VoiceStatusIndicator';

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
    setTranscript
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
});

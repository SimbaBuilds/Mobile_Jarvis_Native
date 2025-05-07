import { NativeModules, NativeEventEmitter, EmitterSubscription } from 'react-native';

// Get the native module
const { VoiceModule } = NativeModules;

// Voice state enum that matches the native implementation
export enum VoiceState {
  IDLE = 'IDLE',
  WAKE_WORD_DETECTED = 'WAKE_WORD_DETECTED',
  LISTENING = 'LISTENING',
  PROCESSING = 'PROCESSING',
  SPEAKING = 'SPEAKING',
  ERROR = 'ERROR'
}

// Event type for voice state changes
export interface VoiceStateChangeEvent {
  state: VoiceState;
}

// Create event emitter for the native module
const voiceEventEmitter = new NativeEventEmitter(VoiceModule);

/**
 * Service wrapper for the native voice module
 */
class VoiceService {
  private listeners: EmitterSubscription[] = [];

  /**
   * Start listening for voice input
   * @returns Promise resolving to true if successful
   */
  public startListening(): Promise<boolean> {
    return VoiceModule.startListening();
  }

  /**
   * Stop listening for voice input
   * @returns Promise resolving to true if successful
   */
  public stopListening(): Promise<boolean> {
    return VoiceModule.stopListening();
  }

  /**
   * Interrupt any ongoing speech
   * @returns Promise resolving to true if speech was interrupted
   */
  public interruptSpeech(): Promise<boolean> {
    return VoiceModule.interruptSpeech();
  }

  /**
   * Get the current voice state
   * @returns Promise resolving to the current VoiceState
   */
  public getVoiceState(): Promise<VoiceState> {
    return VoiceModule.getVoiceState();
  }

  /**
   * Register a listener for voice state changes
   * @param callback Function to call when voice state changes
   * @returns Function to unregister the listener
   */
  public onVoiceStateChange(callback: (event: VoiceStateChangeEvent) => void): () => void {
    const subscription = voiceEventEmitter.addListener('onVoiceStateChange', callback);
    this.listeners.push(subscription);
    
    return () => {
      subscription.remove();
      this.listeners = this.listeners.filter(listener => listener !== subscription);
    };
  }

  /**
   * Remove all listeners
   */
  public removeAllListeners(): void {
    this.listeners.forEach(listener => listener.remove());
    this.listeners = [];
  }
}

// Export a singleton instance
export default new VoiceService();

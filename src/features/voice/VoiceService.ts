import { NativeModules, NativeEventEmitter, EmitterSubscription } from 'react-native';

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

// Event types
export interface VoiceStateChangeEvent {
    state: VoiceState;
}

export interface SpeechResultEvent {
    text: string;
}

class VoiceService {
    private static instance: VoiceService;
    private eventEmitter: NativeEventEmitter;
    private listeners: EmitterSubscription[] = [];

    private constructor() {
        this.eventEmitter = new NativeEventEmitter(VoiceModule);
    }

    public static getInstance(): VoiceService {
        if (!VoiceService.instance) {
            VoiceService.instance = new VoiceService();
        }
        return VoiceService.instance;
    }

    public async startListening(): Promise<boolean> {
        try {
            return await VoiceModule.startListening();
        } catch (error) {
            console.error('Error starting voice recognition:', error);
            throw error;
        }
    }

    public async stopListening(): Promise<boolean> {
        try {
            return await VoiceModule.stopListening();
        } catch (error) {
            console.error('Error stopping voice recognition:', error);
            throw error;
        }
    }

    public async interruptSpeech(): Promise<boolean> {
        try {
            return await VoiceModule.interruptSpeech();
        } catch (error) {
            console.error('Error interrupting speech:', error);
            throw error;
        }
    }

    public async getVoiceState(): Promise<VoiceState> {
        try {
            return await VoiceModule.getVoiceState();
        } catch (error) {
            console.error('Error getting voice state:', error);
            throw error;
        }
    }

    public onVoiceStateChange(callback: (event: VoiceStateChangeEvent) => void): () => void {
        const subscription = this.eventEmitter.addListener('onVoiceStateChange', callback);
        this.listeners.push(subscription);
        
        return () => {
            subscription.remove();
            this.listeners = this.listeners.filter(listener => listener !== subscription);
        };
    }

    public onSpeechResult(callback: (event: SpeechResultEvent) => void): () => void {
        const subscription = this.eventEmitter.addListener('speechResult', callback);
        this.listeners.push(subscription);
        
        return () => {
            subscription.remove();
            this.listeners = this.listeners.filter(listener => listener !== subscription);
        };
    }

    public removeAllListeners(): void {
        this.listeners.forEach(listener => listener.remove());
        this.listeners = [];
    }
}

export default VoiceService; 
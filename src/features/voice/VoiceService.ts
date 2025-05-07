import { NativeModules, NativeEventEmitter } from 'react-native';

const { VoiceModule } = NativeModules;

class VoiceService {
    private static instance: VoiceService;
    private eventEmitter: NativeEventEmitter;

    private constructor() {
        this.eventEmitter = new NativeEventEmitter(VoiceModule);
    }

    public static getInstance(): VoiceService {
        if (!VoiceService.instance) {
            VoiceService.instance = new VoiceService();
        }
        return VoiceService.instance;
    }

    public async startListening(): Promise<void> {
        try {
            await VoiceModule.startListening();
        } catch (error) {
            console.error('Error starting voice recognition:', error);
            throw error;
        }
    }

    public async stopListening(): Promise<void> {
        try {
            await VoiceModule.stopListening();
        } catch (error) {
            console.error('Error stopping voice recognition:', error);
            throw error;
        }
    }

    public async interruptSpeech(): Promise<void> {
        try {
            await VoiceModule.interruptSpeech();
        } catch (error) {
            console.error('Error interrupting speech:', error);
            throw error;
        }
    }

    public async getVoiceState(): Promise<string> {
        try {
            return await VoiceModule.getVoiceState();
        } catch (error) {
            console.error('Error getting voice state:', error);
            throw error;
        }
    }

    public addVoiceStateChangeListener(callback: (state: string) => void): { remove: () => void } {
        return this.eventEmitter.addListener('voiceStateChanged', callback);
    }

    public addSpeechResultListener(callback: (text: string) => void): { remove: () => void } {
        return this.eventEmitter.addListener('speechResult', callback);
    }
}

export default VoiceService; 
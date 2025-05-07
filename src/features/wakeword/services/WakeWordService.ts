import { NativeModules, NativeEventEmitter } from 'react-native';

const { WakeWordModule } = NativeModules;

class WakeWordService {
    private static instance: WakeWordService;
    private eventEmitter: NativeEventEmitter;

    private constructor() {
        this.eventEmitter = new NativeEventEmitter(WakeWordModule);
    }

    public static getInstance(): WakeWordService {
        if (!WakeWordService.instance) {
            WakeWordService.instance = new WakeWordService();
        }
        return WakeWordService.instance;
    }

    public async startWakeWordDetection(): Promise<boolean> {
        try {
            return await WakeWordModule.startWakeWordDetection();
        } catch (error) {
            console.error('Error starting wake word detection:', error);
            throw error;
        }
    }

    public async stopWakeWordDetection(): Promise<boolean> {
        try {
            return await WakeWordModule.stopWakeWordDetection();
        } catch (error) {
            console.error('Error stopping wake word detection:', error);
            throw error;
        }
    }

    public async isWakeWordDetectionRunning(): Promise<boolean> {
        try {
            return await WakeWordModule.isWakeWordDetectionRunning();
        } catch (error) {
            console.error('Error checking wake word detection status:', error);
            throw error;
        }
    }

    public async setWakeWordEnabled(enabled: boolean): Promise<boolean> {
        try {
            return await WakeWordModule.setWakeWordEnabled(enabled);
        } catch (error) {
            console.error('Error setting wake word enabled state:', error);
            throw error;
        }
    }

    public async isWakeWordEnabled(): Promise<boolean> {
        try {
            return await WakeWordModule.isWakeWordEnabled();
        } catch (error) {
            console.error('Error getting wake word enabled state:', error);
            throw error;
        }
    }

    public addWakeWordDetectedListener(callback: () => void): { remove: () => void } {
        return this.eventEmitter.addListener('wakeWordDetected', callback);
    }
}

export default WakeWordService; 
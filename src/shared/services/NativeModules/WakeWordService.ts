import { NativeModules, NativeEventEmitter, EmitterSubscription, Platform } from 'react-native';

// Define the interface for responses from the native module
interface WakeWordAvailabilityResponse {
  available: boolean;
  reason?: string;
}

interface WakeWordActionResponse {
  success: boolean;
  error?: string;
}

interface WakeWordStatusResponse {
  enabled: boolean;
}

// Define the interface for the native module
interface WakeWordModuleInterface {
  isAvailable(): Promise<WakeWordAvailabilityResponse>;
  startDetection(): Promise<WakeWordActionResponse>;
  stopDetection(): Promise<WakeWordActionResponse>;
  getStatus(): Promise<WakeWordStatusResponse>;
  setAccessKey(accessKey: string): Promise<WakeWordActionResponse>;
}

// Define event names
export const WakeWordEvents = {
  SERVICE_RESTORED: 'wakeWordServiceRestored'
};

// Get the native module or create a mock for platforms that don't support it
const WakeWordModule: WakeWordModuleInterface = Platform.OS === 'android'
  ? NativeModules.WakeWordModule
  : {
      // Mock implementation for iOS or other platforms
      isAvailable: async () => ({ available: false, reason: 'Platform not supported' }),
      startDetection: async () => ({ success: false, error: 'Platform not supported' }),
      stopDetection: async () => ({ success: false, error: 'Platform not supported' }),
      getStatus: async () => ({ enabled: false }),
      setAccessKey: async () => ({ success: false, error: 'Platform not supported' }),
    };

// Create an event emitter for the module
const wakeWordEmitter = new NativeEventEmitter(
  // Handle the case where the module might not be available
  Platform.OS === 'android' ? NativeModules.WakeWordModule : null
);

/**
 * Provides access to wake word detection functionality
 */
class WakeWordService {
  /**
   * Check if wake word detection is available on this device
   */
  static async isAvailable(): Promise<boolean> {
    try {
      const result = await WakeWordModule.isAvailable();
      return result.available;
    } catch (error) {
      console.error('Error checking wake word availability:', error);
      return false;
    }
  }

  /**
   * Start wake word detection
   */
  static async startDetection(): Promise<WakeWordActionResponse> {
    try {
      return await WakeWordModule.startDetection();
    } catch (error) {
      console.error('Error starting wake word detection:', error);
      return {
        success: false,
        error: `Failed to start wake word detection: ${error}`,
      };
    }
  }

  /**
   * Stop wake word detection
   */
  static async stopDetection(): Promise<WakeWordActionResponse> {
    try {
      return await WakeWordModule.stopDetection();
    } catch (error) {
      console.error('Error stopping wake word detection:', error);
      return {
        success: false,
        error: `Failed to stop wake word detection: ${error}`,
      };
    }
  }

  /**
   * Get the current wake word detection status
   */
  static async getStatus(): Promise<boolean> {
    try {
      const result = await WakeWordModule.getStatus();
      return result.enabled;
    } catch (error) {
      console.error('Error getting wake word status:', error);
      return false;
    }
  }

  /**
   * Set the Picovoice access key
   */
  static async setAccessKey(accessKey: string): Promise<boolean> {
    try {
      const result = await WakeWordModule.setAccessKey(accessKey);
      return result.success;
    } catch (error) {
      console.error('Error setting access key:', error);
      return false;
    }
  }

  /**
   * Add a listener for wake word events
   */
  static addListener(
    eventType: string,
    listener: (event: any) => void
  ): EmitterSubscription {
    return wakeWordEmitter.addListener(eventType, listener);
  }
}

export default WakeWordService;

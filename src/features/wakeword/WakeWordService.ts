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
  startDetection(serviceClass?: string): Promise<WakeWordActionResponse>;
  stopDetection(): Promise<WakeWordActionResponse>;
  getStatus(): Promise<WakeWordStatusResponse>;
  setAccessKey(accessKey: string): Promise<WakeWordActionResponse>;
}

// Define event names
export const WakeWordEvents = {
  SERVICE_RESTORED: 'wakeWordServiceRestored'
};

// Debug logs to help identify issues
console.log('Available native modules:', Object.keys(NativeModules));

// Extract the WakeWordModule from NativeModules
const nativeWakeWordModule = Platform.OS === 'android' ? NativeModules.WakeWordModule : null;

console.log('WakeWordModule available:', nativeWakeWordModule ? 'Yes' : 'No');
if (!nativeWakeWordModule && Platform.OS === 'android') {
  console.error('WARNING: WakeWordModule not found on Android platform!');
  console.error('Available modules:', Object.keys(NativeModules));
}

// Get the native module or create a mock for platforms that don't support it
const WakeWordModule: WakeWordModuleInterface = nativeWakeWordModule
  ? nativeWakeWordModule
  : {
      // Mock implementation for iOS or other platforms where module is not available
      isAvailable: async () => ({ available: false, reason: 'Platform not supported or module not found' }),
      startDetection: async () => ({ success: false, error: 'Platform not supported or module not found' }),
      stopDetection: async () => ({ success: false, error: 'Platform not supported or module not found' }),
      getStatus: async () => ({ enabled: false }),
      setAccessKey: async () => ({ success: false, error: 'Platform not supported or module not found' }),
    };

// Create an event emitter for the module
const eventEmitter = nativeWakeWordModule 
  ? new NativeEventEmitter(nativeWakeWordModule)
  : null;

/**
 * Provides access to wake word detection functionality
 */
class WakeWordService {
  private static instance: WakeWordService | null = null;
  private static eventSubscription: EmitterSubscription | null = null;

  // Define the correct fully qualified service class name
  private static ANDROID_SERVICE_CLASS = 'com.anonymous.MobileJarvisNative.wakeword.WakeWordService';

  constructor() {
    // Initialize event listener if emitter is available
    if (eventEmitter && !WakeWordService.eventSubscription) {
      WakeWordService.eventSubscription = eventEmitter.addListener(
        'wakeWordDetected',
        (event) => {
          console.log('Wake word detected:', event);
          // Handle wake word detection event
          this.handleWakeWordDetected(event);
        }
      );
    }
  }

  private handleWakeWordDetected(event: any) {
    const timestamp = event.timestamp || Date.now();
    const timeString = new Date(timestamp).toLocaleTimeString();
    
    // Log the detection with emoji for visibility
    console.log('🎤 Wake word detected!');
    console.log(`⏰ Timestamp: ${timeString}`);
    console.log(`📍 Confidence: ${event.confidence || 'N/A'}`);
    
    // Additional debug information if available
    if (event.debug) {
      console.log('🔍 Debug info:', event.debug);
    }
  }

  /**
   * Clean up event subscription
   */
  static cleanup() {
    if (WakeWordService.eventSubscription) {
      WakeWordService.eventSubscription.remove();
      WakeWordService.eventSubscription = null;
    }
  }

  /**
   * Get singleton instance (for compatibility with old code)
   */
  static getInstance(): WakeWordService {
    if (!WakeWordService.instance) {
      WakeWordService.instance = new WakeWordService();
    }
    return WakeWordService.instance;
  }

  /**
   * For compatibility with older implementation
   */
  async isWakeWordEnabled(): Promise<boolean> {
    try {
      console.log('Calling getStatus() on WakeWordModule');
      const response = await WakeWordModule.getStatus();
      console.log('getStatus result:', response);
      return response.enabled;
    } catch (error) {
      console.error('Error getting wake word status:', error);
      return false;
    }
  }

  /**
   * For compatibility with older implementation
   */
  async setWakeWordEnabled(enabled: boolean): Promise<boolean> {
    try {
      if (enabled) {
        // Try to ensure permissions are granted first
        if (Platform.OS === 'android') {
          try {
            const { PermissionsService } = require('../settings/PermissionsService');
            // Check wake word permissions directly using the PermissionsService
            const hasPermissions = await PermissionsService.checkWakeWordPermissions();
            
            if (!hasPermissions) {
              console.log('Need to request wake word permissions before enabling');
              const granted = await PermissionsService.requestWakeWordPermissions();
              
              if (!granted) {
                console.error('Permission request denied');
                throw new Error('Required permissions were denied');
              }
            }
          } catch (permError) {
            console.error('Error checking/requesting permissions:', permError);
            // Continue anyway, the native side will handle permission errors
          }
        }
        
        // Pass the correct service class name
        const result = await WakeWordModule.startDetection(WakeWordService.ANDROID_SERVICE_CLASS);
        
        // Add a small delay to give the service time to start
        if (result.success && Platform.OS === 'android') {
          await new Promise(resolve => setTimeout(resolve, 1000));
        }
        
        return result.success;
      } else {
        const result = await WakeWordModule.stopDetection();
        return result.success;
      }
    } catch (error) {
      console.error('Error setting wake word enabled state:', error);
      return false;
    }
  }

  /**
   * For compatibility with older implementation
   */
  async startWakeWordDetection(): Promise<boolean> {
    try {
      // Pass the service class explicitly
      const result = await WakeWordModule.startDetection(WakeWordService.ANDROID_SERVICE_CLASS);
      
      // Add a small delay to give the service time to start
      if (result.success && Platform.OS === 'android') {
        await new Promise(resolve => setTimeout(resolve, 1000));
      }
      
      return result.success;
    } catch (error) {
      console.error('Error starting wake word detection:', error);
      return false;
    }
  }

  /**
   * For compatibility with older implementation
   */
  async stopWakeWordDetection(): Promise<boolean> {
    const result = await WakeWordModule.stopDetection();
    return result.success;
  }

  /**
   * For compatibility with older implementation
   */
  async isWakeWordDetectionRunning(): Promise<boolean> {
    return WakeWordService.getStatus();
  }

  /**
   * Check if wake word detection is available on this device
   */
  static async isAvailable(): Promise<boolean> {
    try {
      console.log('Calling isAvailable() on WakeWordModule');
      const result = await WakeWordModule.isAvailable();
      console.log('isAvailable result:', result);
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
      if (!nativeWakeWordModule) {
        console.error('WakeWordModule is not available on this platform', 
          { modules: Object.keys(NativeModules) });
        return {
          success: false,
          error: 'WakeWordModule is not available on this platform',
        };
      }

      console.log('Calling startDetection() on WakeWordModule with service class:', WakeWordService.ANDROID_SERVICE_CLASS);
      // Explicitly pass the service class name to the native module
      const result = await WakeWordModule.startDetection(WakeWordService.ANDROID_SERVICE_CLASS);
      console.log('startDetection result:', result);
      
      // Add a small delay to give the service time to start
      if (result.success && Platform.OS === 'android') {
        await new Promise(resolve => setTimeout(resolve, 1000));
      }
      
      return result;
    } catch (error) {
      console.error('Error starting wake word detection:', error);
      return {
        success: false,
        error: String(error),
      };
    }
  }

  /**
   * Stop wake word detection
   */
  static async stopDetection(): Promise<WakeWordActionResponse> {
    try {
      if (!nativeWakeWordModule) {
        console.error('WakeWordModule is not available on this platform',
          { modules: Object.keys(NativeModules) });
        return {
          success: false,
          error: 'WakeWordModule is not available on this platform',
        };
      }
      
      console.log('Calling stopDetection() on WakeWordModule');
      const result = await WakeWordModule.stopDetection();
      console.log('stopDetection result:', result);
      
      return result;
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
      if (!nativeWakeWordModule) {
        console.warn('WakeWordModule is not available, returning default status (false)', 
          { available_modules: Object.keys(NativeModules) });
        return false;
      }
      
      console.log('Calling getStatus() on WakeWordModule');
      const result = await WakeWordModule.getStatus();
      console.log('getStatus result:', result);
      
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
      if (!nativeWakeWordModule) {
        console.warn('WakeWordModule is not available, cannot set access key');
        return false;
      }
      
      console.log('Calling setAccessKey() on WakeWordModule');
      const result = await WakeWordModule.setAccessKey(accessKey);
      console.log('setAccessKey result:', result);
      
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
  ): EmitterSubscription | null {
    if (!eventEmitter) {
      console.warn('WakeWordEmitter is not available, cannot add listener');
      return null;
    }
    return eventEmitter.addListener(eventType, listener);
  }
}

export default WakeWordService;

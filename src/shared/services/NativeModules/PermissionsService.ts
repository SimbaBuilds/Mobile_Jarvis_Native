import { NativeEventEmitter, NativeModules, Platform } from 'react-native';
import { PermissionEvent } from '../../../shared/types/permissions';

const { PermissionsModule } = NativeModules;

/**
 * Interface for the native PermissionsModule
 */
interface IPermissionsModule {
  checkAudioPermission(): Promise<boolean>;
  requestAudioPermission(): Promise<boolean>;
  isBatteryOptimizationExempt(): Promise<boolean>;
  requestBatteryOptimizationExemption(): Promise<boolean>;
  addListener(eventName: string): void;
  removeListeners(count: number): void;
}

// Mock implementation for iOS or when native module is not available
const mockPermissionsModule: IPermissionsModule = {
  checkAudioPermission: async () => true,
  requestAudioPermission: async () => true,
  isBatteryOptimizationExempt: async () => true,
  requestBatteryOptimizationExemption: async () => true,
  addListener: () => {},
  removeListeners: () => {},
};

// Use the native module if available, otherwise use the mock
const module: IPermissionsModule = PermissionsModule || mockPermissionsModule;

// Create event emitter for permission events
const permissionEventEmitter = PermissionsModule
  ? new NativeEventEmitter(PermissionsModule)
  : null;

/**
 * Service for handling permissions in the app
 */
export const PermissionsService = {
  /**
   * Check if the app has microphone permission
   */
  checkAudioPermission: async (): Promise<boolean> => {
    try {
      return await module.checkAudioPermission();
    } catch (error) {
      console.error('Error checking audio permission:', error);
      return false;
    }
  },

  /**
   * Request microphone permission
   */
  requestAudioPermission: async (): Promise<boolean> => {
    try {
      return await module.requestAudioPermission();
    } catch (error) {
      console.error('Error requesting audio permission:', error);
      return false;
    }
  },

  /**
   * Check if the app is exempt from battery optimization
   */
  isBatteryOptimizationExempt: async (): Promise<boolean> => {
    if (Platform.OS !== 'android') return true;
    
    try {
      return await module.isBatteryOptimizationExempt();
    } catch (error) {
      console.error('Error checking battery optimization:', error);
      return false;
    }
  },

  /**
   * Request exemption from battery optimization
   */
  requestBatteryOptimizationExemption: async (): Promise<boolean> => {
    if (Platform.OS !== 'android') return true;
    
    try {
      return await module.requestBatteryOptimizationExemption();
    } catch (error) {
      console.error('Error requesting battery optimization exemption:', error);
      return false;
    }
  },

  /**
   * Add listener for permission results
   */
  addPermissionListener: (
    callback: (event: PermissionEvent) => void
  ): (() => void) => {
    if (!permissionEventEmitter) return () => {};
    
    const subscription = permissionEventEmitter.addListener(
      'onPermissionResult',
      callback
    );
    
    return () => subscription.remove();
  },
};

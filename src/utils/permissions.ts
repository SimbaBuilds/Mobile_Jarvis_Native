import { Platform } from 'react-native';
import { PermissionsService } from '../services/NativeModules/PermissionsService';
import { PermissionResult, PermissionStatus, PermissionType } from '../types/permissions';

/**
 * Utility functions for working with permissions
 */

/**
 * Check if the app has required audio permissions
 * 
 * @returns Promise<PermissionResult> Result of permission check
 */
export const checkMicrophonePermission = async (): Promise<PermissionResult> => {
  try {
    const granted = await PermissionsService.checkAudioPermission();
    return {
      granted,
      status: granted ? PermissionStatus.GRANTED : PermissionStatus.DENIED,
      permission: PermissionType.MICROPHONE
    };
  } catch (error) {
    console.error('Error checking microphone permission:', error);
    return {
      granted: false,
      status: PermissionStatus.UNAVAILABLE,
      permission: PermissionType.MICROPHONE
    };
  }
};

/**
 * Request microphone permission
 * 
 * @returns Promise<PermissionResult> Result of permission request
 */
export const requestMicrophonePermission = async (): Promise<PermissionResult> => {
  try {
    const granted = await PermissionsService.requestAudioPermission();
    return {
      granted,
      status: granted ? PermissionStatus.GRANTED : PermissionStatus.DENIED,
      permission: PermissionType.MICROPHONE
    };
  } catch (error) {
    console.error('Error requesting microphone permission:', error);
    return {
      granted: false,
      status: PermissionStatus.UNAVAILABLE,
      permission: PermissionType.MICROPHONE
    };
  }
};

/**
 * Check if the app is exempt from battery optimization
 * 
 * @returns Promise<PermissionResult> Result of battery optimization check
 */
export const checkBatteryOptimization = async (): Promise<PermissionResult> => {
  // Only relevant for Android
  if (Platform.OS !== 'android') {
    return {
      granted: true,
      status: PermissionStatus.GRANTED,
      permission: PermissionType.BATTERY_OPTIMIZATION
    };
  }
  
  try {
    const isExempt = await PermissionsService.isBatteryOptimizationExempt();
    return {
      granted: isExempt,
      status: isExempt ? PermissionStatus.GRANTED : PermissionStatus.DENIED,
      permission: PermissionType.BATTERY_OPTIMIZATION
    };
  } catch (error) {
    console.error('Error checking battery optimization status:', error);
    return {
      granted: false,
      status: PermissionStatus.UNAVAILABLE,
      permission: PermissionType.BATTERY_OPTIMIZATION
    };
  }
};

/**
 * Request exemption from battery optimization
 * 
 * @returns Promise<PermissionResult> Result of battery optimization request
 */
export const requestBatteryOptimizationExemption = async (): Promise<PermissionResult> => {
  // Only relevant for Android
  if (Platform.OS !== 'android') {
    return {
      granted: true,
      status: PermissionStatus.GRANTED,
      permission: PermissionType.BATTERY_OPTIMIZATION
    };
  }
  
  try {
    const requested = await PermissionsService.requestBatteryOptimizationExemption();
    // Note: this doesn't guarantee the permission was granted, just that the request was shown
    return {
      granted: requested,
      status: requested ? PermissionStatus.GRANTED : PermissionStatus.DENIED,
      permission: PermissionType.BATTERY_OPTIMIZATION
    };
  } catch (error) {
    console.error('Error requesting battery optimization exemption:', error);
    return {
      granted: false,
      status: PermissionStatus.UNAVAILABLE,
      permission: PermissionType.BATTERY_OPTIMIZATION
    };
  }
};

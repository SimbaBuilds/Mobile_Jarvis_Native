/**
 * Permission types used throughout the application
 */

/**
 * Permission status enum
 */
export enum PermissionStatus {
  GRANTED = 'granted',
  DENIED = 'denied',
  NEVER_ASK_AGAIN = 'never_ask_again',
  UNAVAILABLE = 'unavailable',
}

/**
 * Permission types that can be requested
 */
export enum PermissionType {
  MICROPHONE = 'microphone',
  BATTERY_OPTIMIZATION = 'battery_optimization',
}

/**
 * Permission result object
 */
export interface PermissionResult {
  granted: boolean;
  status: PermissionStatus;
  permission: PermissionType;
}

/**
 * Permission event coming from native module
 */
export interface PermissionEvent {
  isGranted: boolean;
  permission: string;
}

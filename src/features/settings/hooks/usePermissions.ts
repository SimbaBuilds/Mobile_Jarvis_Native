import { useCallback, useEffect, useState } from 'react';
import { PermissionEvent, PermissionResult, PermissionType } from '../types/permissions';
import { 
  checkBatteryOptimization, 
  checkMicrophonePermission, 
  requestBatteryOptimizationExemption, 
  requestMicrophonePermission 
} from '../types/permissions';
import { PermissionsService } from '../services/NativeModules/PermissionsService';

/**
 * Hook for managing permissions in React components
 */
export const usePermissions = () => {
  const [microphonePermission, setMicrophonePermission] = useState<PermissionResult | null>(null);
  const [batteryOptimization, setBatteryOptimization] = useState<PermissionResult | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  /**
   * Check all required permissions
   */
  const checkPermissions = useCallback(async () => {
    setLoading(true);
    try {
      const [micPermission, batteryPermission] = await Promise.all([
        checkMicrophonePermission(),
        checkBatteryOptimization()
      ]);
      
      setMicrophonePermission(micPermission);
      setBatteryOptimization(batteryPermission);
    } catch (error) {
      console.error('Error checking permissions:', error);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Request microphone permission
   */
  const requestMicrophone = useCallback(async () => {
    try {
      const result = await requestMicrophonePermission();
      setMicrophonePermission(result);
      return result;
    } catch (error) {
      console.error('Error requesting microphone permission:', error);
      throw error;
    }
  }, []);

  /**
   * Request battery optimization exemption
   */
  const requestBatteryExemption = useCallback(async () => {
    try {
      const result = await requestBatteryOptimizationExemption();
      setBatteryOptimization(result);
      return result;
    } catch (error) {
      console.error('Error requesting battery optimization:', error);
      throw error;
    }
  }, []);

  /**
   * Handle permission changes from native module
   */
  const handlePermissionChange = useCallback((event: PermissionEvent) => {
    if (event.permission === 'android.permission.RECORD_AUDIO') {
      checkMicrophonePermission().then(setMicrophonePermission);
    }
  }, []);

  // Check permissions on component mount
  useEffect(() => {
    checkPermissions();
  }, [checkPermissions]);

  // Set up permission change listener
  useEffect(() => {
    const unsubscribe = PermissionsService.addPermissionListener(handlePermissionChange);
    return unsubscribe;
  }, [handlePermissionChange]);

  return {
    permissions: {
      microphone: microphonePermission,
      batteryOptimization: batteryOptimization
    },
    loading,
    checkPermissions,
    requestMicrophone,
    requestBatteryExemption,
    hasMicrophonePermission: microphonePermission?.granted ?? false,
    hasBatteryOptimizationExemption: batteryOptimization?.granted ?? false,
    hasAllRequiredPermissions: 
      (microphonePermission?.granted ?? false) && 
      (batteryOptimization?.granted ?? false)
  };
};

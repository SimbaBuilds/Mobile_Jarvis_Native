package com.anonymous.MobileJarvisNative.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Utility class for handling permissions
 */
object PermissionUtils {
    
    /**
     * Check if the app has been granted a specific permission
     * 
     * @param context Application context
     * @param permission The permission to check
     * @return True if permission is granted, false otherwise
     */
    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if the app has been granted all permissions in the given list
     * 
     * @param context Application context
     * @param permissions List of permissions to check
     * @return True if all permissions are granted, false otherwise
     */
    fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
        permissions.forEach { permission ->
            if (!hasPermission(context, permission)) {
                return false
            }
        }
        return true
    }

    /**
     * Check if the app has audio recording permission
     * 
     * @param context Application context
     * @return True if audio permission is granted, false otherwise
     */
    fun hasAudioPermission(context: Context): Boolean {
        return hasPermission(context, Manifest.permission.RECORD_AUDIO)
    }
} 
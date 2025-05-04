package com.cameronhightower.mobilejarvisnative;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.cameronhightower.mobilejarvisnative.utils.PermissionUtils;
import com.facebook.react.ReactActivity;
import com.facebook.react.ReactActivityDelegate;
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint;
import com.facebook.react.defaults.DefaultReactActivityDelegate;
import com.facebook.react.modules.core.PermissionAwareActivity;
import com.facebook.react.modules.core.PermissionListener;

public class MainActivity extends ReactActivity implements PermissionAwareActivity {

  private static final String TAG = "MainActivity";
  private PermissionListener permissionListener;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    // Check initial permissions
    PermissionUtils.checkAudioPermission(this);
    PermissionUtils.checkAndRequestBatteryOptimization(this);
  }

  /**
   * Returns the name of the main component registered from JavaScript.
   */
  @Override
  protected String getMainComponentName() {
    return "MobileJarvisNative";
  }

  /**
   * Returns the instance of the {@link ReactActivityDelegate}.
   */
  @Override
  protected ReactActivityDelegate createReactActivityDelegate() {
    return new DefaultReactActivityDelegate(
        this,
        getMainComponentName(),
        // If you opted-in for the New Architecture, we enable the Fabric Renderer.
        DefaultNewArchitectureEntryPoint.getFabricEnabled());
  }

  @Override
  public void requestPermissions(String[] permissions, int requestCode, PermissionListener listener) {
    permissionListener = listener;
    requestPermissions(permissions, requestCode);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    
    // Handle permission results through PermissionUtils
    PermissionUtils.handlePermissionResult(this, requestCode, permissions, grantResults);
    
    // Also notify React Native permission listener if set
    if (permissionListener != null && 
        permissionListener.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
      permissionListener = null;
    }
  }
}

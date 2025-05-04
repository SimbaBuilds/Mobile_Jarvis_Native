package com.cameronhightower.mobilejarvisnative;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.facebook.react.ReactActivity;
import com.facebook.react.ReactActivityDelegate;
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint;
import com.facebook.react.defaults.DefaultReactActivityDelegate;

public class MainActivity extends ReactActivity {

  private static final String TAG = "MainActivity";
  private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    // Check if we were opened to request audio permission
    if (getIntent() != null && getIntent().hasExtra("REQUEST_AUDIO_PERMISSION")) {
      requestAudioPermission();
    }
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

  /**
   * Request audio permission for wake word detection
   */
  private void requestAudioPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
        Log.i(TAG, "Requesting RECORD_AUDIO permission for wake word detection");
        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
      } else {
        Log.d(TAG, "RECORD_AUDIO permission already granted");
      }
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    
    if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        Log.i(TAG, "RECORD_AUDIO permission granted");
        Toast.makeText(this, "Microphone permission granted. You can now use wake word detection.", Toast.LENGTH_LONG).show();
      } else {
        Log.w(TAG, "RECORD_AUDIO permission denied");
        Toast.makeText(this, "Microphone permission denied. Wake word detection will not work.", Toast.LENGTH_LONG).show();
      }
    }
  }
}

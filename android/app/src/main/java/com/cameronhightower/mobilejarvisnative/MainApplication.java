package com.cameronhightower.mobilejarvisnative;

import android.app.Application;
import android.util.Log;

import com.cameronhightower.mobilejarvisnative.modules.wakeword.WakeWordPackage;
import com.cameronhightower.mobilejarvisnative.modules.permissions.PermissionsPackage;
import com.cameronhightower.mobilejarvisnative.modules.voice.VoicePackage;
import com.cameronhightower.mobilejarvisnative.utils.ConfigManager;
import com.facebook.react.PackageList;
import com.facebook.react.ReactApplication;
import com.facebook.react.ReactNativeHost;
import com.facebook.react.ReactPackage;
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint;
import com.facebook.react.defaults.DefaultReactNativeHost;
import com.facebook.soloader.SoLoader;
import java.util.List;

public class MainApplication extends Application implements ReactApplication {

  private static final String TAG = "MainApplication";

  private final ReactNativeHost mReactNativeHost =
      new DefaultReactNativeHost(this) {
        @Override
        public boolean getUseDeveloperSupport() {
          return BuildConfig.DEBUG;
        }

        @Override
        protected List<ReactPackage> getPackages() {
          @SuppressWarnings("UnnecessaryLocalVariable")
          List<ReactPackage> packages = new PackageList(this).getPackages();
          // Add our native packages
          packages.add(new WakeWordPackage());
          packages.add(new PermissionsPackage());
          packages.add(new VoicePackage());
          return packages;
        }

        @Override
        protected String getJSMainModuleName() {
          return "index";
        }
      };

  @Override
  public ReactNativeHost getReactNativeHost() {
    return mReactNativeHost;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    SoLoader.init(this, false);
    
    // Initialize our ConfigManager
    try {
      ConfigManager.getInstance().initialize(this);
      Log.i(TAG, "ConfigManager initialized");
    } catch (Exception e) {
      Log.e(TAG, "Error initializing ConfigManager: " + e.getMessage(), e);
    }
    
    if (BuildConfig.IS_NEW_ARCHITECTURE_ENABLED) {
      // If you opted-in for the New Architecture, we enable the TurboModule system
      DefaultNewArchitectureEntryPoint.enable();
    }
  }
}

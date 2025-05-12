const { withProjectBuildGradle, withAppBuildGradle, withMainApplication } = require('@expo/config-plugins');

// Plugin to configure native modules for Expo
const withNativeModules = (config) => {
  // Add proper project build.gradle configuration
  config = withProjectBuildGradle(config, (config) => {
    if (!config.modResults.contents.includes('maven { url "https://www.jitpack.io" }')) {
      const pattern = /allprojects\s*\{\s*repositories\s*\{/;
      config.modResults.contents = config.modResults.contents.replace(
        pattern,
        `allprojects {
    repositories {
        // Add JitPack repository if needed for your native modules
        maven { url 'https://www.jitpack.io' }`
      );
    }
    return config;
  });

  // Configure app build.gradle
  config = withAppBuildGradle(config, (config) => {
    // Make sure any needed dependency declarations are included
    if (!config.modResults.contents.includes('// Native Module Dependencies')) {
      const pattern = /dependencies\s*\{/;
      config.modResults.contents = config.modResults.contents.replace(
        pattern,
        `dependencies {
    // Native Module Dependencies`
      );
    }
    return config;
  });

  // Ensure main application properly adds custom packages
  config = withMainApplication(config, (config) => {
    // Only add if the packages aren't already configured
    if (!config.modResults.contents.includes('WakeWordPackage()')) {
      // Find the line where packages are created
      const importSection = `import com.anonymous.MobileJarvisNative.wakeword.WakeWordPackage
import com.anonymous.MobileJarvisNative.voice.VoicePackage
import com.anonymous.MobileJarvisNative.settings.AppConfigPackage
import com.anonymous.MobileJarvisNative.permissions.PermissionsPackage`;

      // Add imports if they don't exist
      if (!config.modResults.contents.includes('import com.anonymous.MobileJarvisNative.wakeword.WakeWordPackage')) {
        const lastImportIndex = config.modResults.contents.lastIndexOf('import ');
        const endOfLastImport = config.modResults.contents.indexOf('\n', lastImportIndex) + 1;
        
        config.modResults.contents = 
          config.modResults.contents.substring(0, endOfLastImport) + 
          importSection + '\n' + 
          config.modResults.contents.substring(endOfLastImport);
      }

      // Update the packages method to include the custom packages
      if (config.modResults.contents.includes('val packages = PackageList(this).packages')) {
        const pattern = /val packages = PackageList\(this\).packages[\s\S]*?return packages/;
        config.modResults.contents = config.modResults.contents.replace(
          pattern,
          `val packages = PackageList(this).packages
            // Packages that cannot be autolinked yet can be added manually here
            packages.add(WakeWordPackage())
            // Add other packages as needed
            packages.add(VoicePackage())
            packages.add(PermissionsPackage())
            return packages`
        );
      }
    }
    
    return config;
  });

  return config;
};

module.exports = withNativeModules; 
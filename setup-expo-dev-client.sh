#!/bin/bash

# Exit on error
set -e

echo "🚀 Setting up Expo Dev Client for MobileJarvisNative"

# Install dependencies if node_modules doesn't exist
if [ ! -d "node_modules" ]; then
  echo "📦 Installing dependencies..."
  npm install
fi

# Clean build caches
echo "🧹 Cleaning up build caches..."
rm -rf android/app/build
npx expo prebuild --clean

# Enable development build
echo "🔧 Configuring development build..."
npx expo prebuild -p android

# Install expo development client
echo "📱 Setting up Expo Development Client..."
npx expo install expo-dev-client

# Start development server with Expo Dev Client
echo "✅ Setup complete! Starting Expo Dev Client..."
echo ""
echo "🔍 Run the app on your Android device with:"
echo "npx expo run:android"
echo ""
echo "🌐 Or start the development server with:"
echo "npm start"

# Make this script executable
chmod +x ./setup-expo-dev-client.sh 
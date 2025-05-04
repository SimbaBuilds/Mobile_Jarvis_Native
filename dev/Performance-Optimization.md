# React Native Performance Optimization Best Practices

> **When to read:** When optimizing React Native app performance, addressing performance bottlenecks, or preparing an application for production deployment with performance in mind.

## Core Performance Optimizations

### Enable Hermes JavaScript Engine
- Always enable Hermes for both Android and iOS for better performance.
- Hermes reduces startup time, memory usage, and app size.

```javascript
// In android/app/build.gradle
project.ext.react = [
  enableHermes: true
];

// In Podfile for iOS
use_react_native!(
  :path => config[:reactNativePath],
  :hermes_enabled => true
)
```

### Optimize List Rendering
- Use FlatList or SectionList instead of ScrollView for long lists.
- Implement list item recycling with proper key extraction.
- Use getItemLayout when item dimensions are known.

```typescript
<FlatList
  data={items}
  renderItem={renderItem}
  keyExtractor={(item) => item.id.toString()}
  initialNumToRender={10}
  maxToRenderPerBatch={10}
  windowSize={5}
  getItemLayout={(data, index) => ({
    length: ITEM_HEIGHT,
    offset: ITEM_HEIGHT * index,
    index,
  })}
/>
```

### Memoize Components and Callbacks
- Use React.memo for pure functional components.
- Use useCallback for event handlers passed as props.
- Use useMemo for expensive calculations.

```typescript
// Memoize component
const Item = React.memo(({ title, onPress }) => (
  <TouchableOpacity onPress={onPress}>
    <Text>{title}</Text>
  </TouchableOpacity>
));

// In parent component
const ItemList = ({ items }) => {
  const handlePress = useCallback((id) => {
    // Handle press logic
  }, []);
  
  return (
    <FlatList
      data={items}
      renderItem={({ item }) => (
        <Item 
          title={item.title} 
          onPress={() => handlePress(item.id)}
        />
      )}
    />
  );
};
```

### Optimize Images
- Use proper image resolutions for different screen densities.
- Compress images before bundling.
- Consider using a library like react-native-fast-image for caching.

### Reduce JavaScript Bridge Traffic
- Batch API calls and state updates.
- Use debounce for user inputs (like search).
- Minimize JSON stringify/parse operations.

### Monitor and Address Performance Issues
- Use Flipper or the React Native Debugger for performance monitoring.
- Implement Firebase Performance Monitoring or other analytics tools.

## Animation Performance

### Use Reanimated for Complex Animations
- Use react-native-reanimated for complex animations that run on the UI thread.
- Use react-native-gesture-handler for handling complex gestures.

```typescript
import Animated, { 
  useSharedValue, 
  useAnimatedStyle, 
  withSpring 
} from 'react-native-reanimated';
import { Gesture, GestureDetector } from 'react-native-gesture-handler';

const AnimatedComponent = () => {
  const offset = useSharedValue(0);
  
  const animatedStyle = useAnimatedStyle(() => {
    return {
      transform: [{ translateX: offset.value }],
    };
  });
  
  const gesture = Gesture.Pan()
    .onUpdate((e) => {
      offset.value = e.translationX;
    })
    .onEnd(() => {
      offset.value = withSpring(0);
    });
  
  return (
    <GestureDetector gesture={gesture}>
      <Animated.View style={[styles.box, animatedStyle]} />
    </GestureDetector>
  );
};
```

### Keep Animations at 60 FPS
- Measure animation performance with tools like Flipper.
- Optimize animations that drop below 60 FPS.

## Build Optimizations

### Optimize Bundle Size
- Use Hermes to reduce bundle size.
- Remove unused dependencies.
- Replace large libraries with smaller alternatives (e.g., day.js instead of moment.js).

### Enable Proguard for Android
- Configure ProGuard to reduce APK size.

### Code Splitting
- Use dynamic imports and React.lazy for code splitting when applicable.

### Clean Up Console Statements for Production
- Remove console.log statements in production builds.
- Use a babel plugin to automatically remove console statements.

```javascript
// In babel.config.js
module.exports = {
  presets: ['module:metro-react-native-babel-preset'],
  env: {
    production: {
      plugins: ['transform-remove-console'],
    },
  },
};
```

## Navigation Performance

### Optimize Navigation Performance
- Use screen options to control when screens are mounted/unmounted.
- Avoid deep navigation hierarchies.

```typescript
<Stack.Navigator
  screenOptions={{
    headerShown: false,
    unmountOnBlur: true, // Unmount screen when navigating away
  }}
>
  {/* Screen definitions */}
</Stack.Navigator>
``` 
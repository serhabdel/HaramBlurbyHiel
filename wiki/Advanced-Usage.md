# Advanced Usage Guide 🚀

<div align="center">

**Master HaramBlur - unlock advanced features and optimize performance**

[⬅️ Home](Home.md) • [⚙️ Settings](Settings-and-Configuration.md) • [🔧 Troubleshooting](Troubleshooting.md) • [💡 Tips & Tricks](#power-user-tips)

</div>

---

## 🎯 For Power Users

This guide is for users who want to:
- **Optimize performance** for their specific device
- **Create custom filtering rules** and configurations
- **Integrate HaramBlur** with other apps and workflows
- **Monitor and analyze** usage patterns
- **Customize behavior** beyond basic settings
- **Troubleshoot advanced issues** and edge cases

**⚠️ Note**: Advanced features require technical knowledge and may affect app stability if misconfigured.

---

## 📋 Table of Contents

- [⚡ Performance Optimization](#-performance-optimization)
- [🎨 Custom Filtering Rules](#-custom-filtering-rules)
- [🔧 Developer Mode Features](#-developer-mode-features)
- [📊 Monitoring & Analytics](#-monitoring--analytics)
- [🔗 Integration with Other Apps](#-integration-with-other-apps)
- [🎭 Advanced Customization](#-advanced-customization)
- [💡 Power User Tips](#-power-user-tips)
- [🛠️ Command Line Tools](#-command-line-tools)
- [🔍 Advanced Troubleshooting](#-advanced-troubleshooting)
- [🚀 Experimental Features](#-experimental-features)

---

## ⚡ Performance Optimization

### Device-Specific Tuning

**For High-End Devices (Snapdragon 8xx, Exynos 2xxx):**
```bash
# Enable maximum performance
Settings → Performance → High-Quality Mode
Settings → Detection → Processing Speed → 1000ms
Settings → Detection → Face Threshold → 0.6
```

**For Mid-Range Devices (Snapdragon 7xx, Exynos 1xxx):**
```bash
# Balanced performance
Settings → Performance → Balanced Mode
Settings → Detection → Processing Speed → 1500ms
Settings → Detection → Face Threshold → 0.7
```

**For Budget Devices (Snapdragon 6xx, older):**
```bash
# Maximum efficiency
Settings → Performance → Ultra-Fast Mode
Settings → Detection → Processing Speed → 3000ms
Settings → Detection → Face Threshold → 0.8
```

### Memory Optimization

**Advanced Memory Management:**
1. **Enable Bitmap Pooling** (automatic)
2. **Clear Cache Regularly**: Settings → Privacy → Clear Cache
3. **Monitor Memory Usage**:
   ```bash
   adb shell dumpsys meminfo com.hieltech.haramblur
   ```
4. **Optimize Model Loading**:
   - Use smaller models on low-RAM devices
   - Enable model caching for faster startup

### Battery Optimization

**Advanced Battery Settings:**
- **Adaptive Processing**: Automatically adjusts based on battery level
- **GPU Acceleration**: Enable for better efficiency on supported devices
- **Background Limits**: Configure processing when screen is off

**Battery Monitoring:**
```bash
# Check battery impact
adb shell dumpsys battery | grep "HaramBlur"
```

### GPU Acceleration Tuning

**Enable GPU Support:**
1. **Check Device Compatibility**:
   ```bash
   adb shell getprop ro.hardware
   ```
2. **Force GPU Rendering** (Developer Options):
   ```
   Developer Options → Force GPU rendering
   ```
3. **Monitor GPU Usage**:
   ```bash
   adb shell dumpsys gpu
   ```

---

## 🎨 Custom Filtering Rules

### Advanced Detection Configuration

**Custom Detection Thresholds:**
```kotlin
// Advanced detection settings (requires developer mode)
data class AdvancedDetectionConfig(
    val faceThreshold: Float = 0.7f,      // 0.1-0.9
    val nsfwThreshold: Float = 0.6f,      // 0.1-0.9
    val genderThreshold: Float = 0.8f,    // 0.1-0.9
    val minFaceSize: Int = 64,            // pixels
    val maxFaceSize: Int = 512,           // pixels
    val contentDensityThreshold: Float = 0.3f,
    val temporalConsistency: Int = 3       // frames
)
```

### Content Category Customization

**Custom Categories:**
- **Override Default Rules**: Create exceptions for specific content
- **Pattern Matching**: Use regex for complex filtering
- **Context Awareness**: Different rules for different apps
- **Time-Based Rules**: Different filtering at different times

### False Positive Management

**Advanced False Positive Handling:**
1. **Feedback System**: Report false positives in-app
2. **Pattern Learning**: App learns from your corrections
3. **Custom Whitelists**: Domain-specific exceptions
4. **Context Rules**: Different rules for work vs personal use

---

## 🔧 Developer Mode Features

### Enable Developer Mode

**Unlock Advanced Features:**
1. **Install ADB** on your computer
2. **Connect Device**:
   ```bash
   adb devices
   ```
3. **Enable Developer Options**:
   ```
   Settings → About Phone → Tap Build Number 7 times
   ```
4. **Enable USB Debugging**:
   ```
   Developer Options → USB debugging → ON
   ```

### Debug Logging

**Advanced Logging:**
```bash
# Enable verbose logging
adb shell setprop log.tag.HaramBlur VERBOSE

# Monitor specific components
adb logcat | grep -E "(HaramBlur|ContentDetection|FaceDetection)"

# Save logs to file
adb logcat -f /sdcard/haramblur_logs.txt
```

### Performance Profiling

**Profile App Performance:**
```bash
# CPU profiling
adb shell am profile start --cpu-sample com.hieltech.haramblur

# Memory profiling
adb shell am profile start --heap com.hieltech.haramblur

# Stop profiling
adb shell am profile stop
```

### Component Testing

**Test Individual Components:**
```bash
# Test accessibility service
adb shell settings get secure enabled_accessibility_services

# Test overlay permissions
adb shell pm list permissions -g | grep SYSTEM_ALERT_WINDOW

# Test model loading
adb logcat | grep "Model loaded successfully"
```

---

## 📊 Monitoring & Analytics

### Usage Statistics

**Built-in Analytics:**
- **Detection Metrics**: Faces detected, content blocked
- **Performance Stats**: Processing time, battery usage
- **Islamic Features**: Dhikr shown, verses displayed
- **Error Tracking**: Crash reports and issues

**Access Statistics:**
```
HaramBlur → Settings → About → Usage Statistics
```

### Performance Metrics

**Monitor App Performance:**
- **Average Detection Time**: How fast content is processed
- **Memory Usage**: RAM consumption over time
- **Battery Impact**: Percentage used per hour
- **False Positive Rate**: Accuracy of detection

### Export Data

**Export Your Data:**
1. **Settings** → **Privacy** → **Data Management**
2. **Export Statistics** - Get usage reports
3. **Export Settings** - Backup configurations
4. **Export Logs** - Debug information

---

## 🔗 Integration with Other Apps

### Tasker Integration

**Automate HaramBlur:**
- **Location-Based**: Enable/disable based on location
- **Time-Based**: Different settings for work/home
- **App-Based**: Change settings when specific apps open
- **Battery-Based**: Adjust performance based on battery level

**Tasker Profile Example:**
```
Profile: Work Mode
Context: App Changed → Owned → [Work Apps]
Task: HaramBlur → Performance Mode → Ultra-Fast
```

### MacroDroid Automation

**Advanced Automation:**
- **Screen Content Triggers**: React to specific content
- **Notification Actions**: Respond to HaramBlur notifications
- **Location Contexts**: Different settings by location
- **Time Contexts**: Schedule setting changes

### Custom Launchers

**Launcher Integration:**
- **Custom Icons**: Quick access to HaramBlur controls
- **Gesture Support**: Custom gestures for controls
- **Widget Support**: Home screen widgets for quick actions
- **Shortcut Support**: Direct access to specific features

---

## 🎭 Advanced Customization

### Custom Blur Effects

**Advanced Blur Options:**
```kotlin
enum class AdvancedBlurEffect {
    RECTANGLE,      // Simple rectangle
    GAUSSIAN,       // Smooth gaussian blur
    PIXELATE,       // Blocky pixelation
    MOSAIC,         // Artistic mosaic
    CUSTOM_SHADER   // Custom OpenGL shader
}
```

**Shader Customization:**
- **Custom Fragment Shaders** for unique effects
- **Color Filtering** beyond simple blur
- **Animation Effects** for dynamic overlays
- **Performance Considerations** for custom shaders

### Islamic Content Customization

**Custom Quranic Content:**
- **Add Personal Verses**: Include favorite verses
- **Custom Dhikr Phrases**: Add personal remembrances
- **Language Preferences**: Mix languages in displays
- **Display Timing**: Control when content appears

### Notification Customization

**Advanced Notifications:**
- **Custom Sounds**: Different sounds for different alerts
- **Vibration Patterns**: Custom vibration sequences
- **LED Colors**: Choose notification light colors
- **Priority Levels**: Control notification importance

---

## 💡 Power User Tips

### Efficiency Tips

**Maximize Performance:**
- **Use Fast Mode** on modern devices for 90% of balanced accuracy
- **Enable GPU acceleration** on supported devices
- **Clear cache weekly** to maintain performance
- **Restart app monthly** for optimal memory usage

### Accuracy Tips

**Improve Detection:**
- **Adjust sensitivity** based on your content types
- **Use feedback system** to train the app
- **Report false positives** to improve accuracy
- **Update regularly** for latest model improvements

### Battery Tips

**Minimize Battery Usage:**
- **Ultra-Fast mode** for maximum battery life
- **Disable unused features** (notifications, location)
- **Use airplane mode** when possible
- **Close background apps** to reduce interference

### Privacy Tips

**Advanced Privacy:**
- **Regular cache clearing** removes temporary data
- **Check permissions** monthly for changes
- **Use offline mode** when possible
- **Monitor app updates** for privacy changes

---

## 🛠️ Command Line Tools

### ADB Commands

**Service Management:**
```bash
# Check service status
adb shell settings get secure enabled_accessibility_services

# Force restart service
adb shell am broadcast -a com.hieltech.haramblur.SERVICE_RESTART

# Emergency stop
adb shell am broadcast -a com.hieltech.haramblur.EMERGENCY_RESET
```

**Debug Commands:**
```bash
# Enable debug mode
adb shell setprop debug.haramblur true

# Monitor performance
adb shell am profile start --cpu-sample com.hieltech.haramblur

# Dump app state
adb shell dumpsys activity services com.hieltech.haramblur
```

**Testing Commands:**
```bash
# Test detection
adb shell am broadcast -a com.hieltech.haramblur.TEST_DETECTION

# Reset settings
adb shell pm clear com.hieltech.haramblur

# Force model reload
adb shell am broadcast -a com.hieltech.haramblur.RELOAD_MODELS
```

### Log Analysis

**Parse Logs:**
```bash
# Extract detection events
adb logcat | grep "FaceDetectionManager" | grep "detected"

# Monitor performance
adb logcat | grep "Processing completed in"

# Check for errors
adb logcat | grep "HaramBlur.*ERROR"
```

---

## 🔍 Advanced Troubleshooting

### Deep Diagnostics

**System-Level Checks:**
1. **Check Android Version**:
   ```bash
   adb shell getprop ro.build.version.release
   ```

2. **Verify Permissions**:
   ```bash
   adb shell pm list permissions -g | grep haramblur
   ```

3. **Monitor System Resources**:
   ```bash
   adb shell top | grep haramblur
   ```

### Memory Leak Detection

**Advanced Memory Analysis:**
```bash
# Dump heap
adb shell am dumpheap com.hieltech.haramblur /sdcard/heap.hprof

# Analyze memory usage
adb shell dumpsys meminfo com.hieltech.haramblur
```

### Network Analysis

**Monitor Network Activity:**
```bash
# Check network usage
adb shell dumpsys netstats | grep haramblur

# Monitor connections
adb shell netstat | grep haramblur
```

### Performance Bottleneck Analysis

**Identify Slow Components:**
- **Model Loading**: Check model file integrity
- **Image Processing**: Monitor bitmap operations
- **UI Rendering**: Check overlay performance
- **Database Access**: Monitor query performance

---

## 🚀 Experimental Features

### Beta Features (Use with Caution)

**Advanced Detection Models:**
- **Enhanced NSFW Detection**: More accurate but resource-intensive
- **Video Content Analysis**: Process video streams (experimental)
- **OCR Integration**: Text content analysis (future)
- **Multi-language Detection**: Beyond English/Arabic

**Enable Experimental Features:**
```bash
# Warning: May affect stability
adb shell setprop haramblur.experimental true
```

### Custom Model Integration

**Use Custom TensorFlow Lite Models:**
1. **Prepare Model**: Train or obtain custom model
2. **Convert to TFLite**: Use TensorFlow tools
3. **Place in Assets**: Add to `app/src/main/assets/models/`
4. **Update Configuration**: Modify model loading code
5. **Test Thoroughly**: Validate performance and accuracy

### API Integration

**Custom API Endpoints:**
- **Prayer Times**: Alternative to Aladhan API
- **Quranic Content**: Custom Islamic content sources
- **Location Services**: Alternative geolocation providers
- **Translation Services**: Custom translation APIs

---

<div align="center">

## 🏆 Power User Mastery

**Congratulations on reaching advanced usage!**

You've mastered:
- ✅ **Performance optimization** for your device
- ✅ **Custom filtering rules** and configurations
- ✅ **Developer tools** and debugging
- ✅ **System integration** and automation
- ✅ **Advanced troubleshooting** techniques

---

## ⚠️ Advanced User Warnings

**Important Notes:**
- **Backup regularly** before making advanced changes
- **Test thoroughly** after configuration changes
- **Monitor performance** for unexpected impacts
- **Report issues** if you find bugs in advanced features
- **Use experimental features** at your own risk

**Need help with advanced features?**
- **[🐛 Report Issues](https://github.com/serhabdel/HaramBlur/issues)** - For bugs and suggestions
- **[💬 Community Support](Community-and-Support.md)** - Connect with other advanced users
- **[📚 Documentation](https://github.com/serhabdel/HaramBlur)** - Technical documentation

---

**Remember:** With great power comes great responsibility. Use advanced features wisely!

[⬆️ Back to Top](#advanced-usage-guide-) • [🏠 Home](Home.md) • [⚙️ Settings](Settings-and-Configuration.md) • [🔧 Troubleshooting](Troubleshooting.md)

</div>

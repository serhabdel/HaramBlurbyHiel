# Settings and Configuration Guide ⚙️

<div align="center">

**Customize HaramBlur to fit your needs and preferences**

[⬅️ Home](Home.md) • [✨ Features](Features-Overview.md) • [🕌 Islamic Settings](#islamic-settings) • [🔧 Detection Settings](#detection-settings)

</div>

---

## 🎯 Understanding Settings

HaramBlur offers extensive customization options to ensure the app works perfectly for your specific needs, device, and preferences. This guide will help you understand and configure every aspect of the app.

**Settings Philosophy:**
- **User-Centric**: Organized by what you want to achieve, not technical details
- **Progressive Disclosure**: Start with basics, access advanced options as needed
- **Safety First**: Changes require confirmation to prevent accidental misconfiguration

---

## 📋 Table of Contents

- [🏠 Accessing Settings](#-accessing-settings)
- [🔍 Detection Settings](#-detection-settings)
- [⚡ Performance Settings](#-performance-settings)
- [🕌 Islamic Settings](#-islamic-settings)
- [🎨 Appearance Settings](#-appearance-settings)
- [📱 App Management](#-app-management)
- [🔒 Privacy Settings](#-privacy-settings)
- [🚨 Emergency Controls](#-emergency-controls)
- [💾 Backup & Reset](#-backup--reset)

---

## 🏠 Accessing Settings

### How to Open Settings

**Method 1: From Home Screen**
1. Open HaramBlur app
2. Tap the **Settings** icon (gear) in the top-right corner
3. Browse through different setting categories

**Method 2: Quick Access**
1. Swipe down from the top of any screen
2. Tap the HaramBlur notification
3. Select "Open Settings" from the menu

**Method 3: Emergency Access**
1. Long-press the HaramBlur notification
2. Select "Emergency Settings" for quick access

### Settings Categories

The settings are organized into logical groups:

- **🔍 Detection**: Control what and how content is detected
- **⚡ Performance**: Optimize for your device and usage
- **🕌 Islamic**: Configure spiritual features
- **🎨 Appearance**: Customize look and feel
- **📱 Apps**: Choose which apps to monitor
- **🔒 Privacy**: Control data and permissions
- **🚨 Emergency**: Quick disable options

---

## 🔍 Detection Settings

### Detection Sensitivity

Control how aggressively HaramBlur detects and blurs content.

**Sensitivity Levels:**

**Conservative (Most Protective)**
- **Best for**: Those seeking maximum protection
- **Detection**: More likely to blur borderline content
- **False Positives**: May blur some appropriate content
- **Use when**: You prefer caution over convenience

**Balanced (Recommended)**
- **Best for**: Most users
- **Detection**: Good balance of protection and accuracy
- **False Positives**: Minimal inappropriate content missed
- **Use when**: You want reliable protection without over-blocking

**Relaxed (Less Protective)**
- **Best for**: Those comfortable with more content
- **Detection**: Less likely to blur borderline content
- **False Positives**: More appropriate content may pass through
- **Use when**: You prefer convenience over strict filtering

### Content Categories

Choose which types of content HaramBlur should detect:

**Face Detection**
- **What it does**: Blurs human faces in images and videos
- **Default**: Enabled
- **Customize**: Adjust face size sensitivity

**NSFW Detection**
- **What it does**: Identifies inappropriate or explicit content
- **Default**: Enabled
- **Customize**: Set confidence threshold (60-90%)

**Gender-Specific Content**
- **What it does**: Handles gender-specific filtering rules
- **Default**: Enabled
- **Customize**: Adjust classification sensitivity

**Site Blocking**
- **What it does**: Blocks access to inappropriate websites
- **Default**: Enabled
- **Customize**: Add custom blocked domains

### Advanced Detection Options

**Face Detection Threshold**
- **Range**: 0.1 to 0.9 (default: 0.7)
- **Higher values**: Fewer false positives, may miss some faces
- **Lower values**: More detections, may blur non-face objects

**Minimum Face Size**
- **Range**: 32 to 256 pixels (default: 64)
- **Larger values**: Only blur large, clear faces
- **Smaller values**: Blur smaller faces (more detections)

**Processing Speed**
- **Range**: 500ms to 5000ms (default: 2000ms)
- **Faster**: More responsive but higher battery usage
- **Slower**: Less battery usage but delayed detection

---

## ⚡ Performance Settings

### Performance Modes

HaramBlur adapts to your device and usage patterns:

**Ultra-Fast Mode**
- **Best for**: Older devices or maximum battery life
- **Detection**: Basic face detection only
- **Resolution**: Lower quality analysis
- **Battery Impact**: Minimal (~2-3% per hour)
- **Accuracy**: Good for obvious content

**Fast Mode**
- **Best for**: Balanced performance on mid-range devices
- **Detection**: Face + basic NSFW detection
- **Resolution**: Medium quality analysis
- **Battery Impact**: Low (~5-7% per hour)
- **Accuracy**: Good balance of speed and detection

**Balanced Mode (Recommended)**
- **Best for**: Most modern devices
- **Detection**: Full feature set
- **Resolution**: Native screen resolution
- **Battery Impact**: Moderate (~8-12% per hour)
- **Accuracy**: High accuracy with good performance

**High-Quality Mode**
- **Best for**: Powerful devices where accuracy matters most
- **Detection**: Enhanced analysis with multiple models
- **Resolution**: High-resolution processing
- **Battery Impact**: Higher (~15-20% per hour)
- **Accuracy**: Maximum detection accuracy

### Adaptive Performance

**Smart Adjustments:**
- **Battery Level**: Reduces performance when battery is low
- **Device Temperature**: Slows down if device is overheating
- **Usage Patterns**: Learns your typical usage and optimizes
- **Background Apps**: Adjusts when other apps need resources

### Memory Management

**Automatic Optimization:**
- **Bitmap Pooling**: Reuses memory for better performance
- **Cache Management**: Clears old data to free memory
- **Resource Cleanup**: Automatic cleanup of unused resources
- **Leak Prevention**: Prevents memory leaks that slow down your device

---

## 🕌 Islamic Settings

### Dhikr Configuration

**Reminder Schedule:**
- **Time Windows**: Choose when to show dhikr reminders
  - Fajr (Dawn)
  - Dhuhr (Noon)
  - Asr (Afternoon)
  - Maghrib (Evening)
  - Isha (Night)
- **Frequency**: How often during each time window
- **Duration**: How long each dhikr displays (10-60 seconds)

**Display Options:**
- **Method**: Overlay, Notification, or Both
- **Position**: Top-right, Center, Bottom-right
- **Language**: Arabic only, with translation, with transliteration
- **Size**: Small, Medium, Large display

**Dhikr Content:**
- **Categories**: Morning, Evening, General, Protection
- **Custom Phrases**: Add your own dhikr phrases
- **Translation Language**: English, French, or both

### Prayer Times Setup

**Location Settings:**
- **Automatic Detection**: Use GPS for precise location
- **Manual Location**: Set city/country if GPS unavailable
- **Time Zone**: Automatic or manual adjustment

**Calculation Method:**
- **Muslim World League** (default - most common)
- **Islamic Society of North America (ISNA)**
- **Egyptian General Authority**
- **Umm Al-Qura University**
- **University of Islamic Sciences, Karachi**

**Notification Preferences:**
- **Advance Time**: 5-30 minutes before prayer
- **Prayer Selection**: Choose which prayers to notify
- **Quiet Mode**: Disable during certain hours
- **Sound**: Choose notification sound or silent

### Quranic Guidance

**Verse Display:**
- **Language**: Arabic, English, French
- **Translation**: Multiple translation options
- **Display Duration**: How long verses show
- **Font Size**: Adjust for readability

**Content Categories:**
- **Protection**: Verses about guarding eyes and heart
- **Remembrance**: Verses about dhikr and remembrance
- **Patience**: Verses for difficult moments
- **Gratitude**: Verses for thankfulness

---

## 🎨 Appearance Settings

### Blur Effects

**Blur Styles:**
- **Rectangle**: Simple gray rectangles (default)
- **Gaussian**: Smooth blur effect
- **Pixelate**: Blocky pixelation effect
- **Mosaic**: Artistic mosaic pattern

**Customization:**
- **Blur Intensity**: Light, Medium, Strong (default)
- **Color**: Gray (default), Black, White, Custom
- **Opacity**: 70-100% (default: 80%)
- **Border**: With or without border

### Interface Theme

**Visual Themes:**
- **Light Theme**: Clean, bright interface
- **Dark Theme**: Easy on eyes, saves battery
- **System Default**: Follows your phone's theme
- **Islamic Theme**: Special theme with Islamic motifs

**Language & Localization:**
- **App Language**: Arabic, English, French
- **Text Direction**: Left-to-right or right-to-left
- **Font**: Default or custom Islamic fonts

### Notifications

**Notification Style:**
- **Minimal**: Just icons and basic info
- **Detailed**: Full information and actions
- **Custom**: Personalized notification content

**Alert Preferences:**
- **Sound**: Choose notification sound
- **Vibration**: Pattern and intensity
- **LED**: Color for notification light

---

## 📱 App Management

### App Whitelisting

**Protection Scope:**
- **All Apps**: Monitor everything (recommended)
- **Selected Apps**: Choose specific apps to monitor
- **Exclude Apps**: Skip certain apps entirely

**App Categories:**
- **Social Media**: Facebook, Instagram, Twitter, etc.
- **Browsers**: Chrome, Firefox, Samsung Internet
- **Messaging**: WhatsApp, Telegram, Messenger
- **Gallery**: Photos, Google Photos, Gallery apps

### Website Blocking

**Domain Management:**
- **Pre-configured Lists**: Built-in inappropriate site lists
- **Custom Domains**: Add your own blocked sites
- **Allow Lists**: Override blocking for specific sites

**Blocking Methods:**
- **Complete Block**: Prevent access entirely
- **Blur Content**: Allow access but blur inappropriate content
- **Warn Only**: Show warning without blocking

### Emergency Apps

**Always Allowed:**
- **Phone**: Emergency calls always work
- **Messages**: SMS for urgent communication
- **Maps**: Navigation for getting help
- **Custom Apps**: Add your own emergency apps

---

## 🔒 Privacy Settings

### Data Collection

**What We Track:**
- **Usage Statistics**: How often you use the app (optional)
- **Detection Metrics**: Success rates and performance
- **Crash Reports**: Automatic error reporting (optional)

**Privacy Controls:**
- **Analytics**: Enable/disable usage tracking
- **Crash Reporting**: Send error reports to improve app
- **Location Data**: Only used for prayer times

### Permissions

**Required Permissions:**
- **Accessibility Service**: Required for content detection
- **Overlay Permission**: Required for blur effects
- **Storage**: For caching AI models (optional)

**Optional Permissions:**
- **Location**: For accurate prayer times
- **Notifications**: For prayer and dhikr reminders
- **Camera**: For testing detection (optional)

### Data Management

**Local Storage:**
- **Cache**: Temporary files for performance
- **Settings**: Your preferences and configuration
- **Logs**: Debug information (optional)

**Data Controls:**
- **Clear Cache**: Free up storage space
- **Export Settings**: Backup your configuration
- **Reset App**: Return to default settings

---

## 🚨 Emergency Controls

### Quick Disable

**Emergency Toggle:**
- **From Notification**: Quick toggle in notification shade
- **From App**: Large emergency button on home screen
- **From Settings**: One-tap disable option

**Temporary Override:**
- **Duration**: 5 minutes, 15 minutes, 1 hour, custom
- **Scope**: All apps or specific apps only
- **Auto-reenable**: Automatically turn back on

### Parent Controls

**Family Safety:**
- **PIN Protection**: Require PIN to change settings
- **Time Limits**: Set usage time limits
- **Emergency Contacts**: Quick access to important numbers

### Recovery Options

**If Something Goes Wrong:**
- **Force Stop**: Complete shutdown of protection
- **Safe Mode**: Basic protection only
- **Factory Reset**: Return to original settings

---

## 💾 Backup & Reset

### Backup Your Settings

**Export Configuration:**
1. Go to Settings → Privacy → Data Management
2. Tap "Export Settings"
3. Choose what to backup:
   - Detection settings
   - Islamic preferences
   - App whitelist
   - Custom configurations
4. Save to file or cloud storage

### Restore from Backup

**Import Configuration:**
1. Go to Settings → Privacy → Data Management
2. Tap "Import Settings"
3. Select your backup file
4. Review changes before applying
5. Confirm restoration

### Reset Options

**Partial Reset:**
- **Detection Settings**: Reset to defaults
- **Islamic Settings**: Reset dhikr and prayer preferences
- **Appearance**: Return to default theme
- **App List**: Clear custom whitelist

**Complete Reset:**
- **Factory Reset**: Return to initial setup state
- **Clear All Data**: Remove all settings and cache
- **Reinstall**: Fresh installation experience

---

<div align="center">

## 🎯 Settings Summary

**Most Important Settings for New Users:**

1. **Detection Sensitivity**: Start with "Balanced"
2. **Performance Mode**: Choose based on your device
3. **Islamic Features**: Enable dhikr and prayer times
4. **App Whitelist**: Keep "All Apps" for full protection

**Remember:** You can always change settings later. Start simple and customize as you learn what works best for you.

---

**Need Help Configuring?**
- **[❓ FAQ](FAQ.md)** - Common configuration questions
- **[🔧 Troubleshooting](Troubleshooting.md)** - Fix configuration issues
- **[💬 Community Support](Community-and-Support.md)** - Get advice from other users

[⬆️ Back to Top](#settings-and-configuration-guide-) • [🏠 Home](Home.md) • [✨ Features](Features-Overview.md) • [🕌 Islamic Features](Islamic-Features-Guide.md)

</div>

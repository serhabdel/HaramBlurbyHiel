# Getting Started with HaramBlur 📚

<div align="center">

**Your complete guide to setting up HaramBlur for the first time**

[⬅️ Home](Home.md) • [✨ Features Overview](Features-Overview.md) • [❓ Troubleshooting](Troubleshooting.md)

</div>

---

## 🎯 Welcome to Your Journey

**Assalamu Alaikum!** Congratulations on taking the first step towards protecting your faith in the digital world. This guide will walk you through everything you need to know to get HaramBlur up and running on your Android device.

Whether you're new to Android apps or a tech-savvy user, we'll guide you through each step with clear, simple instructions. By the end of this guide, you'll have HaramBlur working to help you maintain your spiritual well-being.

---

## 📋 Table of Contents

- [📱 System Requirements](#-system-requirements)
- [⬇️ Download & Installation](#️-download--installation)
- [⚙️ First-Time Setup](#️-first-time-setup)
- [🔧 Permission Setup](#-permission-setup)
- [✅ Verification](#-verification)
- [🎉 You're All Set!](#-youre-all-set)
- [📞 Need Help?](#-need-help)

---

## 📱 System Requirements

Before you begin, make sure your device meets these minimum requirements:

### Android Version
- **Minimum**: Android 7.1 (API Level 25)
- **Recommended**: Android 9.0+ (API Level 28)
- **Why?** HaramBlur uses modern Android features for optimal performance

### Hardware Requirements
- **RAM**: Minimum 2GB, Recommended 4GB+
- **Storage**: 100MB free space for the app and its AI models
- **Processor**: Any modern Android processor (works on most devices from 2016+)

### Compatible Devices
✅ **Smartphones & Tablets**: All modern Android devices
✅ **Custom ROMs**: Works with most custom Android versions
❌ **Rooted Devices**: May have compatibility issues (contact support)

---

## ⬇️ Download & Installation

### Option 1: Download from GitHub (Recommended for Latest Version)

1. **Visit the Repository**
   - Open your browser and go to: `https://github.com/serhabdel/HaramBlur`
   - Scroll down to the **Releases** section

2. **Download the APK**
   - Look for the latest release (usually at the top)
   - Download the file ending in `.apk` (e.g., `haramblur-v1.0.0.apk`)

3. **Install the APK**
   - Locate the downloaded file in your Downloads folder
   - Tap on it to start installation
   - If prompted about "Unknown sources," tap **"Settings"** → Enable **"Install unknown apps"** for your browser

### Option 2: Build from Source (Advanced Users)

If you prefer to build the app yourself:

```bash
# 1. Download the project
git clone https://github.com/serhabdel/HaramBlur.git
cd HaramBlur

# 2. Build the app
./gradlew assembleDebug

# 3. Install on your device
adb install app/build/outputs/apk/debug/app-debug.apk
```

> **Note**: This option requires Android development tools and is recommended only for advanced users or developers.

---

## ⚙️ First-Time Setup

### Step 1: Launch HaramBlur

1. **Find the App**
   - Open your app drawer
   - Look for the HaramBlur icon (usually shows a blue shield with Islamic design)
   - Tap to open

2. **Welcome Screen**
   - You'll see a welcome message in Arabic and English
   - Read the brief introduction about the app's purpose
   - Tap **"Next"** or **"ابدأ"** (Start) to continue

### Step 2: Language Selection

HaramBlur supports multiple languages to serve the global Muslim community:

- **العربية** (Arabic)
- **English**
- **Français** (French)

Choose your preferred language and tap **"Continue"**.

### Step 3: App Introduction

You'll see a series of screens explaining:
- How HaramBlur protects your faith
- The Islamic principles behind the app
- Privacy and security features
- Performance and battery optimization

Read through each screen and tap **"Next"** to proceed.

---

## 🔧 Permission Setup

HaramBlur needs certain permissions to work effectively. Don't worry - all processing happens on your device and your privacy is protected.

### Step 1: Accessibility Service Permission

This is the most important permission for HaramBlur to work:

1. **Tap "Enable Accessibility Service"**
   - The app will guide you to Android Settings
   - Navigate to **Settings** → **Accessibility**

2. **Find HaramBlur**
   - Scroll through the list of services
   - Look for **"HaramBlur"** or **"HaramBlur Accessibility Service"**

3. **Enable the Service**
   - Tap on HaramBlur in the list
   - Toggle the switch to **ON** (green)
   - Tap **"Allow"** if prompted for permissions

4. **Return to App**
   - Press the back button or use the navigation gesture
   - Return to HaramBlur app

> **Why this permission?** The accessibility service allows HaramBlur to safely capture screen content for analysis while maintaining your privacy.

### Step 2: Overlay Permission

For blur effects to appear over other apps:

1. **Grant Overlay Permission**
   - HaramBlur will prompt you to grant this permission
   - Tap **"Allow"** or **"Grant"**

2. **System Settings Method**
   - If the automatic prompt doesn't work:
   - Go to **Settings** → **Apps** → **HaramBlur**
   - Tap **"Advanced"** or **"More options"**
   - Select **"Display over other apps"**
   - Toggle to **ON**

> **Why this permission?** Allows HaramBlur to show protective blur overlays when inappropriate content is detected.

### Step 3: Additional Permissions (Optional)

Depending on features you want to use:

- **Location**: For accurate prayer times (optional)
- **Notifications**: For prayer reminders and dhikr (optional)
- **Storage**: For caching AI models (optional)

You can grant these later in settings if needed.

---

## ✅ Verification

Let's make sure HaramBlur is working correctly:

### Check 1: Service Status

1. **Open HaramBlur**
2. **Look at the Home Screen**
   - You should see **"HaramBlur Active"** in green
   - A status indicator showing the service is running
   - Real-time statistics (may show zeros initially)

### Check 2: Test Detection

1. **Open Your Camera App**
   - Point it at your face or open a photo with faces
   - You should see gray blur rectangles appear over detected faces

2. **Browse Different Apps**
   - Try opening your gallery, browser, or social media
   - The app should process content without noticeable lag

### Check 3: Monitor Activity

You can check if HaramBlur is working by monitoring logs (advanced):

```bash
# Connect your phone to computer and run:
adb logcat | grep "HaramBlur"
```

Look for messages like:
- `"HaramBlurService: Service Connected"`
- `"FaceDetectionManager: Face detection completed"`
- `"ContentDetectionEngine: Analysis result"`

---

## 🎉 You're All Set!

Congratulations! HaramBlur is now protecting you. Here's what happens next:

### What HaramBlur Does Now
- **Automatic Protection**: Scans your screen every 1-2 seconds
- **Smart Detection**: Uses AI to identify faces and inappropriate content
- **Islamic Guidance**: Shows relevant Quranic verses when needed
- **Dhikr Reminders**: Provides Islamic remembrance throughout the day
- **Prayer Notifications**: Alerts you to prayer times

### Next Steps
1. **[Customize Settings](Settings-and-Configuration.md)** - Adjust detection sensitivity and preferences
2. **[Explore Islamic Features](Islamic-Features-Guide.md)** - Learn about spiritual guidance features
3. **[Read the Features Guide](Features-Overview.md)** - Discover everything HaramBlur can do

### Daily Usage Tips
- **Keep the app running** - HaramBlur works best when active
- **Check notifications** - Prayer times and dhikr reminders
- **Monitor statistics** - See how much protection you've received
- **Update regularly** - Keep the app updated for best performance

---

## 📞 Need Help?

### Common Issues

**Service Won't Start**
- Make sure accessibility service is enabled
- Restart your phone and try again
- Check if you have any battery optimization settings blocking the app

**No Blur Effects**
- Verify overlay permission is granted
- Try restarting the app
- Check if the service is active in settings

**App Crashes**
- Clear app cache and data
- Reinstall the app
- Check device compatibility

### Get Support

- **[Troubleshooting Guide](Troubleshooting.md)** - Detailed solutions
- **[FAQ](FAQ.md)** - Common questions and answers
- **[Community Support](Community-and-Support.md)** - Connect with other users
- **GitHub Issues** - Report bugs and get help

---

<div align="center">

**🎊 Welcome to the HaramBlur Family!**

*May HaramBlur help you maintain your faith and spiritual well-being*

**الحمد لله رب العالمين**

---

[⬆️ Back to Top](#getting-started-with-haramblur-) • [🏠 Home](Home.md) • [✨ Features](Features-Overview.md) • [❓ FAQ](FAQ.md)

</div>

# Privacy and Security Guide 🔒

<div align="center">

**Your privacy is our priority - understand how HaramBlur protects your data**

[⬅️ Home](Home.md) • [✨ Features](Features-Overview.md) • [⚙️ Settings](Settings-and-Configuration.md) • [❓ FAQ](FAQ.md)

</div>

---

## 🛡️ Privacy First Philosophy

**At HaramBlur, your privacy is not just a feature - it's our core principle.** We believe that technology should protect your faith without compromising your personal privacy. This guide explains exactly how we protect your data and maintain your trust.

> *"And whoever fears Allah - He will make for him a way out and will provide for him from where he does not expect."*
>
> **— Quran 65:2-3**

---

## 📋 Table of Contents

- [🔐 Core Privacy Principles](#-core-privacy-principles)
- [📱 On-Device Processing](#-on-device-processing)
- [🚫 No Data Collection](#-no-data-collection)
- [🔑 Permissions Explained](#-permissions-explained)
- [🗂️ Data Storage](#-data-storage)
- [🔒 Security Measures](#-security-measures)
- [🌍 Third-Party Services](#-third-party-services)
- [📊 Analytics & Tracking](#-analytics--tracking)
- [🛠️ Open Source Transparency](#-open-source-transparency)
- [⚖️ Your Rights & Controls](#-your-rights--controls)

---

## 🔐 Core Privacy Principles

### 1. **Zero Data Transmission**
- **Nothing leaves your device** - All processing happens locally
- **No cloud storage** - Your photos and content stay on your phone
- **No user tracking** - We don't collect usage statistics
- **No personal data** - We don't ask for emails, names, or identifiers

### 2. **Islamic Privacy Ethics**
- **Modesty protection** - Technology that respects Islamic values
- **Family privacy** - Safe for use by entire families
- **Community trust** - Built by Muslims for the Muslim community
- **Ethical development** - Guided by Islamic principles

### 3. **Transparency**
- **Open source code** - Anyone can verify our privacy claims
- **Clear documentation** - No hidden features or backdoors
- **User control** - You decide what features to enable
- **Regular audits** - Community review of privacy practices

---

## 📱 On-Device Processing

### How HaramBlur Works Locally

**Complete Local Processing:**
- **AI Models**: All machine learning runs on your device
- **Content Analysis**: Photos and videos never leave your phone
- **Face Detection**: Uses Google ML Kit locally
- **NSFW Detection**: TensorFlow Lite processes locally
- **Quranic Database**: Verses stored locally on device

**What This Means:**
```
Your Photo → HaramBlur AI (on device) → Blur Applied → Nothing Sent Anywhere
```

### Hardware Acceleration

**GPU & Neural Processing:**
- **GPU Acceleration**: Uses your phone's graphics processor
- **NNAPI Support**: Android's Neural Networks API
- **DSP Optimization**: Qualcomm Hexagon DSP when available
- **CPU Fallback**: Works on any Android device

**Performance Benefits:**
- **Faster processing** without compromising privacy
- **Lower battery usage** through efficient hardware use
- **Better accuracy** with dedicated AI processors

---

## 🚫 No Data Collection

### What We Don't Collect

**Zero Data Collection Policy:**
- ❌ **No photos or images** from your device
- ❌ **No browsing history** or website tracking
- ❌ **No personal information** (name, email, location*)
- ❌ **No usage statistics** or analytics
- ❌ **No crash reports** unless you explicitly enable
- ❌ **No device identifiers** or advertising IDs

*Location is only used for prayer times and stored locally*

### What We Don't Share

**No External Communications:**
- **No internet uploads** of any kind
- **No API calls** to external servers (except prayer times)
- **No telemetry** or error reporting
- **No user feedback** sent automatically
- **No community data** shared

### Exceptions (Optional)

**User-Controlled Features:**
- **Prayer Times**: Optional API call to Aladhan (location-based only)
- **Crash Reporting**: Can be enabled in settings (anonymous)
- **Bug Reports**: Manual submission only
- **Feature Feedback**: User-initiated contact

---

## 🔑 Permissions Explained

### Required Permissions

**Accessibility Service** (Required)
- **What it does**: Monitors screen content for inappropriate material
- **Why needed**: Core functionality for content detection
- **Privacy impact**: Only sees screen content, nothing else
- **Data access**: Temporary screenshots (not stored)

**Display Over Other Apps** (Required)
- **What it does**: Shows blur overlays on detected content
- **Why needed**: To apply protective blur effects
- **Privacy impact**: None (just displays overlays)
- **Data access**: None

**Storage** (Optional)
- **What it does**: Caches AI models and temporary files
- **Why needed**: Improves performance and offline functionality
- **Privacy impact**: None (only app data)
- **Data access**: App's own storage only

### Optional Permissions

**Location** (Optional)
- **What it does**: Gets location for accurate prayer times
- **Why needed**: Calculates prayer times based on your position
- **Privacy impact**: Location data stored locally only
- **Data access**: GPS/network location (not sent externally)

**Notifications** (Optional)
- **What it does**: Shows prayer reminders and dhikr notifications
- **Why needed**: Islamic features (prayer times, dhikr)
- **Privacy impact**: None
- **Data access**: None (just shows local notifications)

---

## 🗂️ Data Storage

### Local Storage Only

**What HaramBlur Stores Locally:**

**App Settings**
- Detection preferences
- Islamic feature settings
- Performance configurations
- User customizations

**AI Models** (Cached)
- TensorFlow Lite models (~50MB)
- Face detection models
- Language models for content analysis

**Islamic Content**
- Quranic verses database
- Dhikr phrases
- Prayer time calculations

**Temporary Files**
- Screenshot buffers (cleared immediately)
- Processing cache (auto-cleaned)

### Storage Locations

**Android Storage:**
```
/data/data/com.hieltech.haramblur/
├── shared_prefs/     # App settings
├── databases/        # Local databases
├── cache/           # Temporary files
└── files/           # Cached models
```

**External Storage (Optional):**
- Backup files (user-initiated only)
- Exported settings (user choice)

### Data Cleanup

**Automatic Cleanup:**
- **Cache clearing**: Old temporary files removed
- **Screenshot cleanup**: Immediate deletion after processing
- **Memory management**: Automatic cleanup of unused data
- **Storage optimization**: Smart cache management

---

## 🔒 Security Measures

### Code Security

**Open Source Security:**
- **Public code review** - Community audits security
- **Regular updates** - Security patches and improvements
- **No obfuscation** - Transparent code for verification
- **Peer review** - Multiple developers review changes

### Android Security Integration

**Platform Security:**
- **Android Keystore** - Secure key storage
- **App sandboxing** - Isolated from other apps
- **Permission model** - Granular access controls
- **SELinux** - Kernel-level security

### Network Security

**Secure Communications:**
- **HTTPS only** - All external requests encrypted
- **Certificate pinning** - Prevents man-in-the-middle attacks
- **Minimal network** - Very few external connections
- **No tracking** - No analytics or telemetry

---

## 🌍 Third-Party Services

### Prayer Times (Aladhan API)

**What it provides:**
- Accurate Islamic prayer times
- Location-based calculations
- Multiple calculation methods

**Privacy Impact:**
- **Data sent**: Only latitude/longitude (no personal info)
- **Data stored**: Nothing stored externally
- **Frequency**: Once daily or on location change
- **Purpose**: Prayer time accuracy only

**Example API Call:**
```
GET https://api.aladhan.com/v1/timings?latitude=XX.XXXX&longitude=YY.YYYY
```

### Google ML Kit (Face Detection)

**What it provides:**
- Advanced face detection
- Landmark identification
- Real-time processing

**Privacy Impact:**
- **Local processing**: All analysis on device
- **No data sent**: Nothing leaves your device
- **Offline capable**: Works without internet
- **Google's privacy**: Subject to Google's privacy policy

### TensorFlow Lite

**What it provides:**
- On-device machine learning
- NSFW content detection
- Optimized for mobile

**Privacy Impact:**
- **Local models**: All processing on device
- **No cloud**: No data sent to Google/TensorFlow
- **Open source**: Transparent model training
- **User control**: Can disable specific detections

---

## 📊 Analytics & Tracking

### Our Stance on Analytics

**No Analytics By Default:**
- We don't track how you use the app
- No usage statistics collected
- No performance metrics sent
- No user behavior analysis

### Optional Analytics (User Choice)

**Crash Reporting (Optional):**
- **What it collects**: Anonymous crash logs only
- **Purpose**: Help fix bugs and improve stability
- **Data sent**: Technical error information, no personal data
- **User control**: Completely optional, can be disabled

**Example Crash Report:**
```
App Version: 1.0.0
Android Version: 12
Device: Samsung Galaxy S21
Error: NullPointerException in ContentDetectionEngine
Stack Trace: [technical details only]
```

### Why We Don't Track Usage

**Privacy Philosophy:**
- **Trust**: We respect your privacy completely
- **Ethics**: No hidden data collection
- **Transparency**: Open source means you can verify
- **Islamic values**: Respect for personal privacy

---

## 🛠️ Open Source Transparency

### Code Availability

**Full Transparency:**
- **Source code**: Publicly available on GitHub
- **Build process**: Reproducible builds
- **Dependencies**: All open source libraries
- **Licensing**: Clear open source licenses

### Community Verification

**Independent Audits:**
- **Code reviews**: Community reviews all changes
- **Security audits**: Regular security assessments
- **Bug bounties**: Community testing and reporting
- **Transparency reports**: Regular privacy updates

### How to Verify Privacy Claims

**For Advanced Users:**
1. **Review source code** on GitHub
2. **Check network traffic** with monitoring tools
3. **Examine app permissions** in Android settings
4. **Verify no data collection** through testing

**Tools for Verification:**
- Android Studio profiler
- Network monitoring apps
- File system analysis
- Log analysis tools

---

## ⚖️ Your Rights & Controls

### Data Control Rights

**Your Rights:**
- **Access**: View all data stored by HaramBlur
- **Deletion**: Delete any stored data
- **Portability**: Export your settings and preferences
- **Correction**: Modify your settings anytime

### Privacy Controls

**Built-in Controls:**
- **Disable features**: Turn off any feature you don't want
- **Clear data**: Remove all stored information
- **Reset app**: Return to factory defaults
- **Uninstall**: Complete removal of all data

### How to Exercise Your Rights

**Access Your Data:**
1. **Settings** → **Privacy** → **Data Management**
2. **Export Settings** - Get copy of your preferences
3. **View Storage** - See what data is stored
4. **Clear Cache** - Remove temporary files

**Delete Your Data:**
1. **Android Settings** → **Apps** → **HaramBlur** → **Storage**
2. **Clear Data** - Remove all app data
3. **Clear Cache** - Remove temporary files
4. **Uninstall** - Complete removal

**Control Features:**
- **Disable Islamic features** if not wanted
- **Turn off notifications** anytime
- **Disable location access** for prayer times
- **Use offline mode** completely

---

<div align="center">

## 🔒 Privacy Comparison

**HaramBlur vs Other Content Filters:**

| Feature | HaramBlur | Other Filters |
|---------|-----------|---------------|
| **Local Processing** | ✅ 100% | ❌ Often cloud-based |
| **No Data Collection** | ✅ None | ❌ Usually collect data |
| **Open Source** | ✅ Fully | ❌ Often proprietary |
| **Islamic Focus** | ✅ Spiritual guidance | ❌ Generic filtering |
| **Offline Capable** | ✅ Complete | ❌ Limited offline |

---

## 🛡️ Your Privacy is Protected

HaramBlur represents a new approach to content filtering - one that respects your privacy as much as it protects your faith. With complete local processing, no data collection, and open source transparency, you can use HaramBlur with complete confidence.

**Questions about privacy?** Check our [FAQ](FAQ.md) or [contact the community](Community-and-Support.md).

---

**Remember:** *"The best of affairs is the moderate course."* (Sahih Muslim)

*HaramBlur follows the moderate, balanced approach to technology and privacy.*

[⬆️ Back to Top](#privacy-and-security-guide-) • [🏠 Home](Home.md) • [⚙️ Settings](Settings-and-Configuration.md) • [❓ FAQ](FAQ.md)

</div>

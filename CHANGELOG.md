# HaramBlur - Release Changelog

## Version 1.2.0 - Prayer Notification Fixes

### 🕌 Prayer Notification System - Fixed & Improved
- **Fixed timing issues** - Increased notification tolerance from 1 to 3 minutes
- **Enhanced reliability** - Better error handling and API fallback logic  
- **Runtime permission check** - Proper Android 13+ notification permission handling
- **Comprehensive logging** - Added detailed tracking for debugging
- **Manual testing option** - Added trigger method for testing notifications

### 🛠️ Technical Improvements
- Improved prayer time API fallback chains
- Enhanced worker scheduling with retry mechanism
- Better error recovery for local calculation failures
- Optimized notification tracking and duplicate prevention

### 🐛 Bug Fixes
- Fixed prayer notifications not appearing due to strict timing windows
- Resolved permission-related silent failures
- Improved stability of background notification service

---

## Previous Features (Included)

### 🛡️ Content Protection
- AI-powered adult content detection and blocking
- Real-time website filtering with Islamic guidelines
- Customizable blur intensity and detection sensitivity

### 🕌 Islamic Features
- Prayer times with notifications (now fixed)
- Quranic verses and guidance
- Dhikr reminders and tracking
- Islamic content recommendations

### ⚡ Performance
- Optimized detection algorithms
- Battery-efficient background processing
- Fast response times for content blocking

### 🔒 Privacy & Security
- All processing done locally on device
- No data sent to external servers
- Secure and private user experience

---

**Release App Bundle:** `./app/build/outputs/bundle/release/app-release.aab`
**Size:** 42MB (optimized for Play Store)
**Build Date:** November 2, 2025
**Format:** Android App Bundle (.aab) - Recommended for Play Store

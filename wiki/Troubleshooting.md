# Troubleshooting Guide 🔧

<div align="center">

**Solve common issues and get HaramBlur working perfectly**

[⬅️ Home](Home.md) • [📚 Getting Started](Getting-Started.md) • [❓ FAQ](FAQ.md) • [💬 Support](Community-and-Support.md)

</div>

---

## 🚨 Quick Diagnosis

**Having trouble with HaramBlur?** Start here for the fastest solutions to common problems.

### Is the Service Running?
**Check Status:**
1. Open HaramBlur app
2. Look for **"HaramBlur Active"** in green
3. If red or missing, tap to enable service

### Are Permissions Granted?
**Verify Permissions:**
1. Go to **Settings** → **Accessibility**
2. Find **HaramBlur** and ensure it's **ON**
3. Check **Settings** → **Apps** → **HaramBlur** → **Display over other apps** is **ON**

---

## 📋 Table of Contents

- [🚫 Service Won't Start](#-service-wont-start)
- [👁️ No Blur Effects](#️-no-blur-effects)
- [🔋 High Battery Usage](#-high-battery-usage)
- [🐌 Slow Performance](#-slow-performance)
- [❌ App Crashes](#-app-crashes)
- [🕌 Islamic Features Not Working](#-islamic-features-not-working)
- [🌐 Website Blocking Issues](#-website-blocking-issues)
- [📱 Device-Specific Issues](#-device-specific-issues)
- [🔧 Advanced Troubleshooting](#-advanced-troubleshooting)
- [📞 When to Seek Help](#-when-to-seek-help)

---

## 🚫 Service Won't Start

### Problem: "Service Not Active" or "Accessibility Service Disabled"

**Solution Steps:**

1. **Check Accessibility Settings**
   ```
   Settings → Accessibility → HaramBlur
   ```
   - Ensure the toggle is **ON**
   - If it's off, tap it to enable

2. **Restart the Service**
   - Close HaramBlur app completely
   - Reopen the app
   - The service should start automatically

3. **Restart Your Phone**
   - Sometimes a simple restart fixes accessibility issues
   - After restart, check if service is active

4. **Check for Conflicts**
   - Disable other accessibility services temporarily
   - Some apps (like screen readers) can conflict
   - Re-enable HaramBlur after testing

### Problem: Service Starts but Stops Immediately

**Possible Causes:**
- **Battery Optimization**: Android is killing the service
- **Memory Pressure**: Device running low on RAM
- **System Restrictions**: Some manufacturers limit background services

**Solutions:**

1. **Disable Battery Optimization**
   ```
   Settings → Apps → HaramBlur → Battery → Don't optimize
   ```

2. **Check Memory Usage**
   - Close other apps to free up memory
   - Restart your device
   - Check for memory-intensive apps

3. **Manufacturer-Specific Settings**
   - **Samsung**: Disable "Auto optimization" for HaramBlur
   - **Xiaomi**: Add to "Protected apps" list
   - **Huawei**: Disable "Power Genius" for the app

---

## 👁️ No Blur Effects

### Problem: Faces/Content Not Being Blurred

**Diagnosis Steps:**

1. **Verify Service is Active**
   - Check notification area for HaramBlur icon
   - Open app to confirm "Active" status

2. **Test Detection**
   - Open camera or photo app with faces
   - Take a photo or view existing photos
   - Look for gray blur rectangles

3. **Check Permissions**
   - Accessibility service enabled
   - Display over other apps permission granted

**Common Solutions:**

### Overlay Permission Missing
```
Settings → Apps → HaramBlur → Advanced → Display over other apps
```
- Toggle **ON** if disabled
- Grant permission when prompted

### App Not in Whitelist
- Open HaramBlur → Settings → App Management
- Ensure the app you're testing is included
- Or set to "Monitor All Apps"

### Detection Sensitivity Too Low
- Go to Settings → Detection → Sensitivity
- Try "Conservative" mode temporarily
- Or increase face detection threshold

---

## 🔋 High Battery Usage

### Problem: HaramBlur Draining Battery Too Fast

**Normal Usage:**
- **Balanced Mode**: ~8-12% battery per hour
- **Fast Mode**: ~5-7% battery per hour
- **Ultra-Fast Mode**: ~2-3% battery per hour

**If Higher Than Expected:**

### Change Performance Mode
1. Open HaramBlur → Settings → Performance
2. Select **"Ultra-Fast"** or **"Fast"** mode
3. Test battery usage for a few hours

### Adjust Detection Frequency
1. Settings → Detection → Processing Speed
2. Increase interval from 2000ms to 3000ms or higher
3. This reduces how often the app analyzes content

### Check for Background Activity
```bash
# Monitor app activity (requires developer options)
adb shell dumpsys battery | grep "HaramBlur"
```

### Battery Optimization Issues
- **Android Settings**: Add HaramBlur to "Don't optimize" list
- **Device Settings**: Check manufacturer battery settings
- **Background Apps**: Ensure app isn't being killed unnecessarily

---

## 🐌 Slow Performance

### Problem: App is Laggy or Unresponsive

**Performance Optimization:**

### Clear Cache and Restart
1. **Android Settings** → **Apps** → **HaramBlur** → **Storage**
2. Tap **"Clear Cache"**
3. **Force Stop** the app
4. Restart HaramBlur

### Adjust Performance Settings
1. **HaramBlur Settings** → **Performance** → **Mode**
2. Try **"Ultra-Fast"** mode for better responsiveness
3. Or **"Fast"** mode for balanced performance

### Check Device Resources
- **Free RAM**: Close unnecessary apps
- **Storage Space**: Ensure at least 500MB free
- **CPU Usage**: Check for other demanding apps

### Update the App
- Check for updates in Google Play Store
- Latest versions include performance improvements
- Clear app data if problems persist (backup settings first)

---

## ❌ App Crashes

### Problem: HaramBlur Keeps Crashing

**Immediate Actions:**

1. **Clear App Data** (Caution: This resets settings)
   ```
   Settings → Apps → HaramBlur → Storage → Clear Data
   ```

2. **Reinstall the App**
   - Uninstall HaramBlur
   - Restart device
   - Reinstall from trusted source

3. **Check System Compatibility**
   - Ensure Android 7.1+ (API 25)
   - Check device meets minimum requirements

### Common Crash Scenarios

**Crash on Startup:**
- **Cause**: Corrupted settings or cache
- **Solution**: Clear data and reinstall

**Crash During Use:**
- **Cause**: Memory issues or conflicts
- **Solution**: Free up RAM, restart device

**Crash After Update:**
- **Cause**: Settings incompatibility
- **Solution**: Reset to default settings

---

## 🕌 Islamic Features Not Working

### Problem: Dhikr Reminders Not Showing

**Check Dhikr Settings:**
1. **HaramBlur** → **Settings** → **Islamic** → **Dhikr**
2. Ensure **"Enable Dhikr"** is **ON**
3. Check **Time Windows** - select prayer times
4. Verify **Display Method** (Overlay/Notification)

**Common Issues:**
- **Time Windows**: No prayer times selected
- **Display Method**: Set to "None" accidentally
- **Permissions**: Notification permission missing

### Problem: Prayer Times Not Accurate

**Location Issues:**
1. **Settings** → **Location** → Enable location access
2. Or manually set your city in Islamic settings
3. Check calculation method (try different ones)

**Time Zone Problems:**
- Ensure automatic time zone is enabled
- Check system time settings
- Restart device to refresh location

### Problem: Quranic Verses Not Showing

**Language Settings:**
- Check selected language supports verses
- Try switching between Arabic/English/French
- Clear app cache if verses don't load

---

## 🌐 Website Blocking Issues

### Problem: Sites Not Being Blocked

**Check Site Blocking:**
1. **Settings** → **Detection** → **Site Blocking**
2. Ensure **"Enable Site Blocking"** is **ON**
3. Check if site is in blocked list

**Custom Blocking:**
- Add specific domains to block list
- Use wildcards: `*.example.com`
- Check pattern matching syntax

### Problem: False Positives (Good Sites Blocked)

**Adjust Blocking Rules:**
1. **Settings** → **Site Blocking** → **Exceptions**
2. Add allowed sites to whitelist
3. Use exact domain matching

**Report False Positives:**
- Use in-app reporting feature
- Or submit via GitHub issues
- Include URL and why it should be allowed

---

## 📱 Device-Specific Issues

### Samsung Devices

**Common Samsung Issues:**
- **One UI**: Disable "Auto optimize" for HaramBlur
- **Battery**: Add to "Sleeping apps" exception
- **Security**: Allow "Unknown sources" for APK installation

### Xiaomi/MIUI Devices

**MIUI Specific Fixes:**
- **Autostart**: Enable in Security → Permissions
- **Battery Saver**: Add to "Protected apps"
- **App Locker**: Ensure not locked

### Huawei/EMUI Devices

**Huawei Solutions:**
- **Power Genius**: Disable for HaramBlur
- **App Launch**: Enable "Manage manually"
- **Notifications**: Allow notification access

### Google Pixel/Stock Android

**Pixel Issues:**
- Usually most compatible
- Check **Adaptive Battery** settings
- Ensure **Play Protect** isn't blocking

---

## 🔧 Advanced Troubleshooting

### Check Logs for Errors

**Monitor App Activity:**
```bash
# Connect device to computer
adb logcat | grep "HaramBlur"
```

**Look for Error Patterns:**
- `HaramBlurService: Error` - Service issues
- `ContentDetectionEngine: Failed` - Detection problems
- `BlurOverlayManager: Exception` - Overlay issues

### Reset to Factory Settings

**Complete Reset:**
1. **Settings** → **Privacy** → **Data Management**
2. **Export Settings** (backup first!)
3. **Reset App** → **Factory Reset**
4. **Reconfigure** from scratch

### Developer Options (Advanced)

**Enable Developer Mode:**
```
Settings → About Phone → Tap Build Number 7 times
```

**Useful Developer Settings:**
- **Don't keep activities**: Test memory issues
- **Background process limit**: Set to "No background processes"
- **Force GPU rendering**: Enable for better performance

---

## 📞 When to Seek Help

### Try These First
1. **Restart Device**: Simple but effective
2. **Update App**: Latest version may have fixes
3. **Clear Cache/Data**: Fresh start often works
4. **Check Permissions**: Most common cause

### When to Contact Support
- **Persistent Crashes**: App won't run at all
- **Detection Not Working**: Even after trying all solutions
- **Device Incompatibility**: Confirmed device-specific issues
- **Feature Requests**: Ideas for improvements

### How to Report Issues Effectively

**Include This Information:**
- **Device Model**: Samsung Galaxy S21, etc.
- **Android Version**: Android 12, API 31, etc.
- **HaramBlur Version**: Version number
- **Steps to Reproduce**: Exact steps that cause the problem
- **Expected vs Actual**: What should happen vs what does happen
- **Screenshots/Logs**: If possible, include error logs

**Best Places to Report:**
- **[GitHub Issues](https://github.com/serhabdel/HaramBlur/issues)** - For bugs and technical issues
- **[Community Discussions](Community-and-Support.md)** - For general questions
- **Email Support** - For private issues

---

<div align="center">

## 🎯 Quick Fix Checklist

**Before contacting support, try:**

✅ **Restart your device**  
✅ **Check accessibility service is enabled**  
✅ **Verify overlay permission granted**  
✅ **Clear app cache**  
✅ **Update to latest version**  
✅ **Check device compatibility**  
✅ **Try different performance mode**  

**Still having issues?** [Get Help from Community](Community-and-Support.md)

---

**Remember:** Most issues have simple solutions. Start with the basics and work through systematically.

[⬆️ Back to Top](#troubleshooting-guide-) • [🏠 Home](Home.md) • [📚 Getting Started](Getting-Started.md) • [❓ FAQ](FAQ.md)

</div>

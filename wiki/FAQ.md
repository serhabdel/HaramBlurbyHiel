# Frequently Asked Questions ❓

<div align="center">

**Your questions answered - find quick solutions to common concerns**

[⬅️ Home](Home.md) • [🔧 Troubleshooting](Troubleshooting.md) • [🕌 Islamic Questions](#islamic-questions) • [🔒 Privacy Questions](#privacy-questions)

</div>

---

## 📋 Quick Navigation

- [🚀 Getting Started](#-getting-started)
- [👁️ Detection & Features](#️-detection--features)
- [🔋 Performance & Battery](#-performance--battery)
- [🕌 Islamic Questions](#-islamic-questions)
- [🔒 Privacy & Security](#-privacy--security)
- [📱 Device Compatibility](#-device-compatibility)
- [💰 Cost & Licensing](#-cost--licensing)
- [🆘 Support & Help](#-support--help)

---

## 🚀 Getting Started

### Q: How do I install HaramBlur?

**A:** Download the APK from our GitHub releases page and install it like any other Android app. Make sure to enable "Install unknown apps" for your browser first. For detailed steps, see our [Getting Started Guide](Getting-Started.md).

### Q: Why do I need to enable accessibility service?

**A:** The accessibility service allows HaramBlur to safely monitor screen content for inappropriate material. This permission is required for the app to work, but all processing happens locally on your device - nothing is sent to external servers.

### Q: Do I need to grant overlay permission?

**A:** Yes, the overlay permission allows HaramBlur to display blur effects over other apps. Without it, the app can detect content but cannot apply the protective blur overlays.

### Q: Is HaramBlur difficult to set up?

**A:** Not at all! The app includes a step-by-step setup wizard that guides you through the entire process. Most users are up and running in under 5 minutes.

### Q: Can I use HaramBlur without an internet connection?

**A:** Yes! Once installed and set up, HaramBlur works completely offline. It only needs internet for initial model downloads and prayer time updates (optional).

---

## 👁️ Detection & Features

### Q: What exactly does HaramBlur detect?

**A:** HaramBlur uses advanced AI to detect:
- Human faces in photos and videos
- Potentially inappropriate content (NSFW material)
- Gender-specific content for appropriate filtering
- Islamic content to avoid false positives

### Q: Does HaramBlur block websites?

**A:** Yes, HaramBlur includes comprehensive website blocking with:
- Pre-configured lists of inappropriate sites
- Custom domain blocking
- Pattern matching for similar sites
- Quranic guidance when blocking sites

### Q: Can HaramBlur work on any app?

**A:** HaramBlur works on most Android apps including:
- Browsers (Chrome, Firefox, etc.)
- Social media (Instagram, Facebook, etc.)
- Messaging apps (WhatsApp, Telegram, etc.)
- Photo galleries and camera apps

Some system apps or highly customized apps may have limitations.

### Q: How accurate is the detection?

**A:** Detection accuracy varies by content type:
- **Face detection**: 85-95% accuracy (using Google ML Kit)
- **NSFW detection**: 70-85% accuracy (depends on content)
- **False positives**: Can be minimized by adjusting sensitivity settings

### Q: What happens when HaramBlur detects inappropriate content?

**A:** When inappropriate content is detected:
1. Gray blur rectangles appear over the content
2. A Quranic verse may be displayed for spiritual guidance
3. The content remains accessible but visually obscured
4. You can adjust blur intensity in settings

---

## 🔋 Performance & Battery

### Q: Does HaramBlur drain my battery?

**A:** Battery usage depends on your settings:
- **Ultra-Fast mode**: ~2-3% per hour
- **Fast mode**: ~5-7% per hour
- **Balanced mode**: ~8-12% per hour
- **High-Quality mode**: ~15-20% per hour

Most users find the balanced mode provides good protection with reasonable battery usage.

### Q: Will HaramBlur slow down my phone?

**A:** HaramBlur is designed for minimal performance impact:
- Uses GPU acceleration when available
- Processes content in the background
- Includes adaptive performance modes
- Most users report no noticeable slowdown

### Q: Can I use HaramBlur on older devices?

**A:** Yes, but with some limitations:
- **Minimum**: Android 7.1 (API 25)
- **Recommended**: Android 9.0+ for best performance
- Use "Ultra-Fast" or "Fast" mode on older devices
- Some advanced features may be limited on very old devices

### Q: Does HaramBlur use a lot of data?

**A:** Minimal data usage:
- **Initial setup**: Downloads AI models (~50MB)
- **Prayer times**: Small updates daily (~1KB)
- **Normal usage**: No data required
- **Offline capable**: Works without internet after setup

---

## 🕌 Islamic Questions

### Q: Is HaramBlur permissible in Islam?

**A:** HaramBlur is designed according to Islamic principles:
- Helps implement "lowering the gaze" (Quran 24:30)
- Protects modesty and purity
- Uses technology for spiritual benefit
- Respects Islamic content and avoids false positives

However, consult with your local Islamic scholar for personal circumstances.

### Q: What Islamic features does HaramBlur include?

**A:** HaramBlur includes comprehensive Islamic features:
- **Quranic verses** with Arabic text and translations
- **Dhikr reminders** throughout the day
- **Prayer time notifications** with location-based accuracy
- **Islamic guidance** when blocking inappropriate content
- **Multi-language support** (Arabic, English, French)

### Q: Can HaramBlur show Quranic verses in Arabic?

**A:** Yes! HaramBlur includes:
- Authentic Arabic Quranic text
- English and French translations
- Phonetic transliteration for pronunciation
- Multiple translation options
- Beautiful Islamic calligraphy fonts

### Q: How does HaramBlur help with Islamic remembrance (Dhikr)?

**A:** Dhikr features include:
- **Scheduled reminders** at prayer times
- **Beautiful displays** with Arabic text and translation
- **Customizable phrases** for different times of day
- **Non-intrusive notifications** that don't interrupt your flow
- **Educational value** for learning Islamic remembrance

### Q: Does HaramBlur work during Ramadan or other Islamic months?

**A:** Yes, HaramBlur works throughout the Islamic calendar:
- Prayer times automatically adjust for Ramadan
- Dhikr reminders continue as configured
- No special configuration needed
- Respects Islamic timing and traditions

---

## 🔒 Privacy & Security

### Q: Does HaramBlur send my photos to the cloud?

**A:** **Absolutely not!** HaramBlur processes everything locally on your device:
- All AI analysis happens on your phone
- No photos, screenshots, or personal data leave your device
- Zero telemetry or data collection
- Your privacy is completely protected

### Q: Can HaramBlur access my personal files?

**A:** No, HaramBlur only accesses:
- **Screen content** (through accessibility service)
- **App settings** (for configuration)
- **Location** (optional, for prayer times only)

It cannot read your personal files, messages, or other private data.

### Q: Is HaramBlur secure?

**A:** HaramBlur follows security best practices:
- **Open source** code for transparency
- **Local processing** only
- **No data collection** policy
- **Regular security updates**
- **Respects Android security model**

### Q: What permissions does HaramBlur need?

**A:** Required permissions:
- **Accessibility Service**: For content monitoring (core functionality)
- **Display Over Apps**: For blur overlays
- **Storage**: For caching AI models (optional)

Optional permissions:
- **Location**: For prayer times
- **Notifications**: For Islamic reminders

### Q: Can I use HaramBlur on a work/school device?

**A:** Check your organization's policies:
- Some workplaces may restrict accessibility services
- School devices may have admin restrictions
- Consider using "Ultra-Fast" mode for minimal impact
- Always respect your organization's IT policies

---

## 📱 Device Compatibility

### Q: Which Android versions are supported?

**A:** HaramBlur supports:
- **Minimum**: Android 7.1 (API Level 25)
- **Recommended**: Android 9.0+ (API Level 28)
- **Latest**: Compatible with Android 14+

Older versions may have reduced functionality.

### Q: Does HaramBlur work on Samsung devices?

**A:** Yes, with excellent compatibility:
- Works on all Samsung Galaxy devices
- Supports One UI customization
- Includes Samsung-specific optimizations
- May need battery optimization adjustments

### Q: What about Xiaomi/Huawei/OnePlus devices?

**A:** Full support for all major manufacturers:
- **Xiaomi**: Add to "Protected apps" list
- **Huawei**: Disable "Power Genius" restrictions
- **OnePlus**: Standard Android behavior
- All include manufacturer-specific setup guidance

### Q: Can I use HaramBlur on tablets?

**A:** Yes! HaramBlur works on Android tablets:
- Same features as phone version
- Optimized for larger screens
- Touch and stylus support
- All Islamic features available

### Q: Does HaramBlur work on foldable phones?

**A:** Yes, with some considerations:
- Supports Samsung Galaxy Fold/Z Flip
- Handles screen transitions
- May need performance adjustments
- All features work on both screens

---

## 💰 Cost & Licensing

### Q: Is HaramBlur free?

**A:** Yes! HaramBlur is completely free:
- No ads or in-app purchases
- No premium features locked behind paywalls
- Open source and community-supported
- Donations appreciated but not required

### Q: What is the "Islamic Open Source License"?

**A:** The IOSL allows:
- ✅ **Free personal use** for all Muslims
- ✅ **Educational use** in Islamic institutions
- ✅ **Non-profit distribution** by Islamic organizations
- ❌ **Commercial use** not permitted

### Q: Can I donate to support HaramBlur?

**A:** Yes, donations are welcome:
- Support ongoing development
- Help maintain Islamic features
- Contribute to community improvements
- Contact developers for donation information

### Q: Is HaramBlur really open source?

**A:** Yes, fully open source:
- **Source code** available on GitHub
- **Community contributions** welcome
- **Transparent development** process
- **No hidden features** or backdoors

---

## 🆘 Support & Help

### Q: Where can I get help?

**A:** Multiple support options:
- **[Troubleshooting Guide](Troubleshooting.md)** - Solve common issues
- **[Community Support](Community-and-Support.md)** - Connect with other users
- **GitHub Issues** - Report bugs and request features
- **Documentation** - Comprehensive wiki guides

### Q: How do I report a bug?

**A:** To report bugs effectively:
1. Check [Troubleshooting Guide](Troubleshooting.md) first
2. Include device model and Android version
3. Describe steps to reproduce the issue
4. Include screenshots if possible
5. Submit via GitHub Issues

### Q: Can I suggest new features?

**A:** Absolutely! Feature requests are welcome:
- Use GitHub Issues with "enhancement" label
- Describe the feature and its benefits
- Explain how it would help the Muslim community
- Community voting helps prioritize features

### Q: How do I contribute to HaramBlur?

**A:** Ways to contribute:
- **Code contributions** via GitHub pull requests
- **Bug reports** and testing
- **Documentation** improvements
- **Translation** help for new languages
- **Community support** for other users

### Q: What if I have a unique situation?

**A:** For unique circumstances:
- Check our comprehensive documentation
- Search existing GitHub issues
- Contact the development team
- Join community discussions

---

<div align="center">

## 🤔 Still Have Questions?

**Can't find what you're looking for?**

- **[🔧 Check Troubleshooting](Troubleshooting.md)** - For technical issues
- **[💬 Community Support](Community-and-Support.md)** - Connect with other users
- **[🐛 Report Issues](https://github.com/serhabdel/HaramBlur/issues)** - For bugs and suggestions

---

**Remember:** HaramBlur is built by the Muslim community, for the Muslim community. Your questions help us improve!

[⬆️ Back to Top](#frequently-asked-questions-) • [🏠 Home](Home.md) • [🔧 Troubleshooting](Troubleshooting.md) • [🕌 Islamic Features](Islamic-Features-Guide.md)

</div>

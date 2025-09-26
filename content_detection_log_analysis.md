# HaramBlur Content Detection Log Analysis

## Overview
HaramBlur logs content detection events in both Android logcat and its internal SQLite database. Detection logs are categorized as "DETECTION" and contain detailed information about face detection, NSFW content analysis, and performance metrics.

## Log Structure
Detection logs in the database follow this structure:
- **Tag**: "ContentDetectionEngine"
- **Category**: "DETECTION" 
- **Message Format**: Structured key-value pairs
  - `DETECTION|faces:X|nsfw:true|false|processing_time:Xms|success:true|false|performance_mode:X`
  - `DETECTION_DETAILED|...` (more detailed information)
  - `PERFORMANCE|detection_time:Xms|mode:X|success:true|false|faces:X|regions:X`

## How to Access Logs

### 1. Via Android Logcat
```bash
# View real-time content detection logs
adb logcat -s "ContentDetectionEngine:D"

# View recent content detection logs
adb logcat -s "ContentDetectionEngine:D" -d | tail -50
```

### 2. Via Internal Database (Offline Analysis)
```bash
# Pull the database from the device
adb shell run-as com.hieltech.haramblur cat databases/site_blocking_database > local_database.db

# Query detection logs
sqlite3 local_database.db "SELECT timestamp, tag, message FROM logs WHERE category = 'DETECTION' ORDER BY timestamp DESC LIMIT 50;"
```

## Key Log Messages to Look For

### Content Analysis Events
- `📸 Starting content analysis` - Beginning of image analysis
- `👤 Starting face detection` - Face detection process
- `🔞 Starting NSFW content detection` - NSFW content analysis
- `📊 Detection results summary` - Summary of results
- `✅ Content analysis completed` - Successful completion
- `❌ Content analysis failed` - Error occurred

### Performance and Optimization
- `Performance updated` - Frame optimization adjustments
- `GPU acceleration enabled` - GPU acceleration status
- `Frame skipped` - Frame skipping for performance

### Region and Blur Analysis
- `🎯 Generated X blur regions` - Number of regions to blur
- `Region-based full-screen blur` - Triggering full-screen blur
- `Using cached detection result` - Cache hits for performance

## Sample Log Analysis

### Normal Detection Event
```
[timestamp] ContentDetectionEngine/DETECTION: DETECTION|faces:2|nsfw:false|processing_time:45ms|success:true|performance_mode:BALANCED|app:com.twitter.android
```
This indicates 2 faces detected in Twitter app, took 45ms, successful.

### NSFW Detection Event
```
[timestamp] ContentDetectionEngine/DETECTION: DETECTION|faces:1|nsfw:true|processing_time:78ms|success:true|performance_mode:BALANCED|app:com.chrome.beta
```
This indicates NSFW content detected in Chrome, took 78ms, successful.

### Performance Issues
```
[timestamp] ContentDetectionEngine/DETECTION: DETECTION|faces:0|nsfw:false|processing_time:156ms|success:true|performance_mode:FAST|error:Timeout approaching
```
This indicates performance issues, took 156ms, near timeout.

## Common Error Patterns

### Memory Issues
Look for:
- `❌ Content analysis failed`
- `OutOfMemoryError` in stack trace
- `Bitmap is recycled` messages

### ML Model Issues
Look for:
- `ML model initialization failed`
- `NSFW detection failed`
- `Face detection failed`

### App Filtering Issues
Look for:
- `Content detection skipped for app`
- `App not in monitored categories`

## Performance Analysis

### Metrics to Monitor
1. **Average Processing Time** - Should be < 100ms
2. **Success Rate** - Should be > 95%
3. **Cache Hit Rate** - Higher is better
4. **Frame Skip Rate** - Should be minimal
5. **Error Rate** - Should be < 1%

### Performance Modes
- **ULTRA_FAST** - Maximum performance, reduced accuracy
- **FAST** - Balanced performance/accuracy
- **BALANCED** - Default mode
- **HIGH_QUALITY** - Maximum accuracy, reduced performance
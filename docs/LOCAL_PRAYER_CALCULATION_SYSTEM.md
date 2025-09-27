# Local Prayer Time Calculation System

## Overview

The Local Prayer Time Calculation System is an offline-first prayer time calculation engine that provides accurate prayer times without requiring API calls. This system is particularly designed for Morocco, with official calculation methods endorsed by the Ministry of Islamic Affairs, but also works globally.

## Features

### 1. Morocco-Specific Calculation Method
- **Official Method**: Uses Morocco's official prayer time calculation method (18° Fajr, 17° Isha) as endorsed by the Ministry of Islamic Affairs
- **Regional Adjustments**: Implements city-specific and regional prayer time adjustments for all 12 Moroccan regions
- **Automatic Detection**: Automatically detects when users are in Morocco and applies the appropriate calculation method

### 2. Offline-First Architecture
- **No API Dependency**: Works completely offline without requiring internet connectivity
- **Fallback Mechanism**: Seamlessly falls back to local calculations when API calls fail
- **User Preference**: Allows users to choose between API-first, local-first, or API-only modes

### 3. Enhanced Location Support
- **Comprehensive Coverage**: Includes 100+ Moroccan cities across all 12 regions
- **Regional Grouping**: Cities are organized by administrative regions for accurate adjustments
- **Coordinate-Based**: Automatically adjusts prayer times based on user's coordinates

### 4. User Interface Integration
- **Visual Indicators**: Shows whether prayer times are calculated locally or from API
- **Settings Control**: Users can enable/disable local calculations and set preferences
- **Real-time Updates**: Prayer times update automatically when settings change

## Technical Implementation

### Core Components

#### 1. LocalPrayerCalculator (`LocalPrayerCalculator.kt`)
- **Purpose**: Core prayer time calculation engine
- **Methods**: 
  - `compute()`: Generic prayer time calculation
  - `computeForMorocco()`: Morocco-specific calculation with adjustments
  - `getMoroccanCityAdjustments()`: Get adjustments for specific cities
  - `getMoroccanAdjustmentsForCoordinates()`: Get adjustments based on coordinates

#### 2. MoroccanLocationHelper (`MoroccanLocationHelper.kt`)
- **Purpose**: Location detection and regional adjustments for Morocco
- **Features**:
  - Location detection within Morocco's boundaries
  - 100+ Moroccan cities with coordinates
  - Regional prayer time adjustments for all 12 regions
  - City lookup and suggestion functionality

#### 3. PrayerTimesRepository (`PrayerTimesRepository.kt`)
- **Purpose**: Orchestrates between API and local calculations
- **Features**:
  - Fallback mechanism between API and local calculations
  - Caching system for performance
  - Settings-based calculation preferences
  - Integration with Moroccan location support

#### 4. Settings Integration
- **New Settings**:
  - `enableLocalCalculations`: Enable/disable local calculation feature
  - `preferLocalOverApi`: Choose between local-first or API-first
  - `showCalculationMethod`: Show calculation source indicator
  - `moroccoSpecificAdjustments`: Enable Morocco-specific adjustments

### Regional Adjustments

The system implements regional prayer time adjustments for all 12 Moroccan regions:

| Region | Fajr Adjustment | Dhuhr Adjustment | Asr Adjustment | Maghrib Adjustment | Isha Adjustment |
|--------|----------------|-----------------|---------------|-------------------|----------------|
| Casablanca-Settat | -1 min | 0 min | 0 min | 0 min | -1 min |
| Rabat-Salé-Kénitra | -1 min | 0 min | 0 min | 0 min | -1 min |
| Fès-Meknès | -2 min | -1 min | -1 min | 0 min | -2 min |
| Marrakech-Safi | -2 min | -1 min | -1 min | 0 min | -2 min |
| Tanger-Tétouan-Al Hoceïma | -2 min | -1 min | -1 min | 0 min | -2 min |
| Oriental | -3 min | -1 min | -1 min | 0 min | -3 min |
| Béni Mellal-Khénifra | -2 min | -1 min | -1 min | 0 min | -2 min |
| Drâa-Tafilalet | -3 min | -1 min | -1 min | 0 min | -3 min |
| Souss-Massa | -2 min | -1 min | -1 min | 0 min | -2 min |
| Guelmim-Oued Noun | -3 min | -1 min | -1 min | 0 min | -3 min |
| Laâyoune-Sakia El Hamra | -4 min | -2 min | -2 min | 0 min | -4 min |
| Dakhla-Oued Ed Dahab | -5 min | -2 min | -2 min | 0 min | -5 min |

## Usage

### For Users

1. **Enable Local Calculations**:
   - Go to Settings → Prayer Times → Enable Local Calculations
   - Choose preference: API-first or Local-first

2. **View Calculation Source**:
   - The prayer times widget shows whether times are from API or calculated locally
   - Look for the "API" or "Local" indicator next to the calculation method

3. **Morocco-Specific Features**:
   - When in Morocco, the system automatically uses the official calculation method
   - Regional adjustments are applied based on your location
   - No manual configuration required

### For Developers

#### Using LocalPrayerCalculator Directly

```kotlin
// Calculate prayer times for Morocco
val calendar = Calendar.getInstance()
val timings = LocalPrayerCalculator.computeForMorocco(
    calendar = calendar,
    latitude = 33.5731, // Casablanca
    longitude = -7.5898,
    asrFactor = 1, // Shafi (1) or Hanafi (2)
    adjustmentsMinutes = LocalPrayerCalculator.getMoroccanCityAdjustments("Casablanca")
)
```

#### Using MoroccanLocationHelper

```kotlin
// Check if coordinates are in Morocco
val isInMorocco = MoroccanLocationHelper.isInMorocco(latitude, longitude)

// Get nearest city
val nearestCity = MoroccanLocationHelper.findNearestMoroccanCity(latitude, longitude)

// Get regional adjustments
val adjustments = MoroccanLocationHelper.getRegionalAdjustments("Casablanca-Settat")
```

#### Using PrayerTimesRepository with Local Calculations

```kotlin
// The repository automatically handles local vs API based on settings
val result = prayerTimesRepository.getPrayerTimes()
if (result.isSuccess) {
    val prayerData = result.getOrThrow()
    // Use prayer data
}
```

## Testing

### Unit Tests

1. **LocalPrayerCalculatorMoroccoTest**: Tests Morocco-specific calculation methods
2. **MoroccanLocationHelperTest**: Tests location detection and regional adjustments
3. **PrayerTimesRepositoryLocalCalculationTest**: Tests integration between components

### Integration Tests

1. **Settings Integration**: Tests that settings changes affect calculation behavior
2. **Fallback Mechanism**: Tests API to local and local to API fallback
3. **Regional Adjustments**: Tests that correct regional adjustments are applied

### Performance Tests

1. **Calculation Speed**: Local calculations are typically faster than API calls
2. **Memory Usage**: Local calculations use minimal memory
3. **Battery Impact**: Reduced battery usage compared to frequent API calls

## Benefits

### For Users

1. **Offline Access**: Prayer times work without internet connectivity
2. **Faster Performance**: No network latency for prayer time calculations
3. **Reduced Data Usage**: Less data consumption when using local calculations
4. **Morocco-Specific Accuracy**: Official calculation methods with regional adjustments

### For the System

1. **Reduced API Dependency**: Less reliance on external services
2. **Improved Reliability**: System continues to work when API is unavailable
3. **Better Performance**: Faster response times for prayer time requests
4. **Scalability**: Reduced load on API servers

## Limitations

1. **Simplified Hijri Calendar**: Local calculations use a simplified Hijri calendar algorithm
2. **Limited Advanced Features**: Some advanced features like Qibla direction still require API calls
3. **Initial Setup**: Requires accurate location coordinates for best results

## Future Enhancements

1. **Enhanced Hijri Calendar**: Implement more accurate Hijri calendar calculations
2. **User Customizable Adjustments**: Allow users to customize prayer time adjustments
3. **Historical Data**: Store historical prayer times for analysis
4. **Prayer Time Notifications**: Enhanced notification system with local calculations
5. **More Regional Support**: Add support for other countries with official calculation methods

## Migration Guide

### For Existing Users

1. **No Breaking Changes**: Existing functionality remains unchanged
2. **Opt-in Feature**: Local calculations are disabled by default
3. **Seamless Transition**: Users can enable local calculations at any time

### For Developers

1. **New Dependencies**: The system uses LocalPrayerCalculator and MoroccanLocationHelper
2. **Settings Migration**: Settings are automatically migrated with version control
3. **Testing**: Ensure comprehensive testing of local calculation scenarios

## Conclusion

The Local Prayer Time Calculation System provides a robust, offline-first solution for prayer time calculations with special support for Morocco's official calculation methods. It enhances reliability, performance, and user experience while maintaining full compatibility with existing API-based functionality.

## Support

For questions, issues, or contributions related to the Local Prayer Time Calculation System, please refer to the project documentation or contact the development team.
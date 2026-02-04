# Release Signing Configuration

## Overview

This document explains how to configure release signing for HaramBlur securely.

## Security Principles

1. **Never commit credentials** - Passwords must never be in version control
2. **Environment variables preferred** - For CI/CD, use environment variables
3. **Local properties for development** - Use `local.properties` for local builds

## Setup for Local Development

### 1. Create local.properties from template

```bash
cp local.properties.template local.properties
```

### 2. Configure your signing credentials

Edit `local.properties`:

```properties
sdk.dir=/path/to/android/sdk
RELEASE_STORE_FILE=../haramblur-release-key.keystore
RELEASE_STORE_PASSWORD=your_actual_password
RELEASE_KEY_ALIAS=haramblur
RELEASE_KEY_PASSWORD=your_actual_password
```

### 3. Build release APK

```bash
./gradlew assembleRelease
```

## CI/CD Setup (GitHub Actions Example)

### 1. Add secrets to your repository

Go to Settings → Secrets and variables → Actions:

- `STORE_PASSWORD`: Your keystore password
- `KEY_PASSWORD`: Your key password
- `KEY_ALIAS`: Your key alias (e.g., `haramblur`)

### 2. Example workflow

```yaml
name: Build Release APK

on:
  push:
    tags:
      - 'v*'

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Build Release APK
        env:
          STORE_PASSWORD: ${{ secrets.STORE_PASSWORD }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
        run: ./gradlew assembleRelease
      
      - name: Upload APK
        uses: actions/upload-artifact@v3
        with:
          name: release-apk
          path: app/build/outputs/apk/release/*.apk
```

## Security Checklist

- [ ] `local.properties` is in `.gitignore`
- [ ] Keystore file (`*.keystore`, `*.jks`) is in `.gitignore`
- [ ] No hardcoded passwords in any `.gradle` or `.kt` files
- [ ] CI/CD uses repository secrets, not hardcoded values
- [ ] Keystore is backed up securely (password manager, encrypted storage)

## Troubleshooting

### "Keystore file not found"
Ensure the keystore path in `local.properties` is correct relative to the app directory.

### "Cannot recover key"
The password is incorrect. Check your `RELEASE_KEY_PASSWORD`.

### "Using debug signing credentials" warning
You see this when building release without proper credentials. The build will use debug keys.

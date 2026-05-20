# APK Build Instructions

## Android Studio
1. Open project in Android Studio
2. Wait for Gradle sync
3. Build > Build APK(s)

## GitHub Actions Automatic APK Build
1. Upload project to GitHub
2. Create workflow file:
.github/workflows/android.yml

Use:

name: Android Build

on:
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17

      - name: Setup Android SDK
        uses: android-actions/setup-android@v3

      - name: Build APK
        run: ./gradlew assembleDebug

      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: app-debug
          path: app/build/outputs/apk/debug/*.apk
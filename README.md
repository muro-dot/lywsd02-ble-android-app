# 📡 LYWSD02 BLE Tool (Android) 🌡️

<p align="center">
  <strong>Native Android application for Xiaomi Mijia LYWSD02 Bluetooth Temperature & Humidity Clock</strong>
</p>

<p align="center">
  <a href="https://github.com/muro-dot/lywsd02-ble-android-app/releases/latest"><img src="https://img.shields.io/github/v/release/muro-dot/lywsd02-ble-android-app?color=blue&label=Latest%20Release" alt="Latest Release"></a>
  <a href="https://github.com/muro-dot/lywsd02-ble-android-app/releases"><img src="https://img.shields.io/github/downloads/muro-dot/lywsd02-ble-android-app/total?color=blueviolet&logo=github&label=Downloads" alt="Total Downloads"></a>
  <a href="https://github.com/muro-dot/lywsd02-ble-android-app/releases/latest/download/lywsd02-ble-android-app-v1.2.9.apk"><img src="https://img.shields.io/badge/Download-APK-success?logo=android" alt="Download APK"></a>
  <img src="https://img.shields.io/badge/License-GPL--3.0-orange" alt="License">
</p>
<p align="center">
<img width="360" alt="LYWSD02" src="https://github.com/user-attachments/assets/fbd66c98-02ff-4f2d-ad43-b1087f127c95" />
</p>
<p align="center">
  <img src="img/screenshot_dashboard_v1.2.6_hd.jpg" alt="LYWSD02 BLE Tool Android App Screenshot" width="320">
</p>

---

## 📖 Overview

**LYWSD02 BLE Tool** is a modern native Android application built with Jetpack Compose, designed to monitor and configure the **Xiaomi Mijia LYWSD02** (and LYWSD02MMC) Bluetooth Low Energy (BLE) temperature and humidity clock in real time. It delivers a clean, high-contrast light UI optimized for readability and responsive hardware interaction.

---

## ✨ Key Features

| Feature | Description |
| :--- | :--- |
| 🌐 **Multi-Language Support** | Full English fallback support for non-Korean system locales and in-app manual language switching (System / 한국어 / English). |
| ⏰ **Clock Synchronization** | Synchronizes device time down to the second using your phone's local time and selected timezone offset. Automatically prompts or recalibrates when a clock drift exceeding 10 seconds is detected. |
| ⚙️ **Display & Unit Settings** | Permanently configure Celsius (°C) or Fahrenheit (°F) display preferences stored directly in device memory, and toggle between 12-hour and 24-hour clock formats. |
| 🌡️ **Real-Time Sensor Monitoring** | Live streaming of ambient temperature, relative humidity, and battery percentage with animated progress indicators and packet reception counters. |
| 📊 **Historical Records & CSV Export** | Fetch and visualize up to 96 hours of hourly min/max temperature & humidity records directly from the sensor's internal flash memory, with instant CSV export and sharing. |
| 🏷️ **Device Management & Aliases** | Filter devices starting with `LYWSD02`, assign custom friendly names (e.g., *Living Room*, *Bedroom*), and quickly reconnect via the recent device list. |
| 📋 **Light Theme Event Console** | High-contrast diagnostic log console displaying BLE connection state changes, GATT notifications, time synchronization results, and packet details. |

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 2.3.20 (Toolchain Java 17)
- **UI Framework**: Jetpack Compose, Material 3
- **Architecture**: MVVM + Clean Architecture, Coroutines & Flow
- **Minimum SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 16 (API 36)
- **Bluetooth**: Android BLE API with full Android 12+ runtime permission handling (`BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`)

---

## 📥 Download & Install

You can download the pre-compiled APK directly from the [GitHub Releases](https://github.com/muro-dot/lywsd02-ble-android-app/releases/latest) page:

- 📦 **Latest APK**: [lywsd02-ble-android-app-v1.2.9.apk](https://github.com/muro-dot/lywsd02-ble-android-app/releases/latest/download/lywsd02-ble-android-app-v1.2.9.apk)
- Requires Android 7.0 (API 24) or higher.

---

## 🚀 Building & Testing

### Prerequisites
- Android Studio Ladybug / Meerkat or later
- Android SDK 36
- JDK 17

### Build Commands

```bash
# Build Debug APK
./gradlew assembleDebug

# Run Unit Tests
./gradlew testDebugUnitTest
```

---

## 📜 Attribution & License

- Based on the BLE communication protocol and web dashboard concept of [drslid/LYWSD02_BLE_Dashboard](https://github.com/drslid/LYWSD02_BLE_Dashboard).
- License: **GNU General Public License v3.0 (GPL-3.0)**

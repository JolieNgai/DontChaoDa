# DontChaoDa

DontChaoDa is an Android home-safety application built with Kotlin and Jetpack Compose. It helps users manage rooms, scan for potential household hazards, review risk summaries, store scan history, receive reminders, and view safety recommendations.
## Overview

The app combines a modern Compose UI with local persistence and Firebase authentication:

- `Firebase Authentication` for registration, login, logout, and password reset
- `Room` for storing rooms, scans, and linked recommendations
- `Jetpack Compose` for the full UI and navigation flow
- `CameraX`, `ML Kit`, `TensorFlow Lite`, and `OpenCV` for hazard-scanning capabilities

The current navigation flow includes:

- Welcome
- Login / Register
- Home
- Room Selection
- AR / Scan
- Scan Summary
- Room History
- Recommendations
- Reminders
- Profile
- FAQ

## Main Features

- Account creation and login with Firebase Auth
- Default room seeding and custom room management
- Hazard scan result handling with overall risk scoring
- Recommendation generation based on detected hazards
- Saved scan history per room
- Hazard-fixing workflow for previously detected issues
- Reminder generation for daily checks and hazard follow-up
- FAQ and help/support access inside the app

## Tech Stack

- `Kotlin`
- `Android SDK 36`
- `Jetpack Compose`
- `Navigation Compose`
- `ViewModel`
- `Room`
- `Firebase Auth`
- `CameraX`
- `ML Kit Object Detection`
- `TensorFlow Lite`
- `OpenCV`
- `JUnit`
- `AndroidJUnit4`
- `Compose UI Test`

## Project Structure

```text
DontChaoDa/
|-- app/
|   |-- src/
|   |   |-- main/
|   |   |   |-- java/com/example/dontchaoda/
|   |   |   |   |-- navigation/
|   |   |   |   |-- screens/
|   |   |   |   |-- ui/theme/
|   |   |   |   |-- model/
|   |   |   |   `-- viewmodel/
|   |   |-- test/
|   |   `-- androidTest/
|   |-- build.gradle.kts
|   `-- google-services.json
|-- build.gradle.kts
|-- settings.gradle.kts
`-- gradlew.bat
```

Key areas:

- `navigation/` defines the app routes and navigation host
- `screens/` contains Compose screens, Room entities, DAOs, and UI-specific logic
- `viewmodel/` contains the main state and business logic in `AppViewModel`
- `test/` contains JVM unit tests
- `androidTest/` contains instrumentation and integration tests

## Requirements

- Android Studio with Android SDK support
- JDK 11
- An Android emulator or device for instrumentation tests

## Getting Started

### 1. Clone the project

```powershell
git clone <your-repo-url>
cd Mobile-Project
```

### 2. Open in Android Studio

Open the project folder in Android Studio and let Gradle sync complete.

### 3. Verify Firebase configuration

The app module already contains `app/google-services.json`. If you are using a different Firebase project, replace that file with your own configuration.

### 4. Build and run

From Android Studio:

- select an emulator or connected device
- click `Run`

Or from the terminal:

```powershell
.\gradlew.bat assembleDebug
```

## Running Tests

### Unit tests

```powershell
.\gradlew.bat testDebugUnitTest
```

### Instrumentation and integration tests

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

### Android test source currently includes

- `15` unit tests in `AppViewModelTest`
- `3` Room integration tests in `ScanDaoIntegrationTest`
- `11` instrumentation/UI tests in `AppInstrumentedUiTest`

The Gradle configuration also includes custom summary tasks that print readable test results to the terminal after unit and connected Android test runs.

## Testing Scope

The implemented tests cover:

- authentication validation and state changes
- room creation and renaming
- risk score calculation
- recommendation generation
- reminders
- scan persistence and history
- Room DAO operations and joins
- Compose navigation and UI flows
- FAQ and profile interactions

## Architecture Notes

The app centers around `AppViewModel`, which coordinates:

- authentication state
- room lists
- scan history
- current hazard results
- reminder generation
- recommendation generation

Persistence is handled with `Room`, while user-facing flows are implemented with Compose screens and `Navigation Compose`.

## Known Notes

- The project contains hazard-scanning dependencies and screens, but README behaviour should be validated against the actual scanning implementation before presenting it as production-ready computer vision.
- Recorded test XML artifacts in `app/build/` may reflect earlier runs and should not be treated as automatically up to date unless the test suite is rerun.
- Instrumentation tests require an emulator or physical Android device.

## Useful Commands

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
.\gradlew.bat :app:compileDebugAndroidTestKotlin
```

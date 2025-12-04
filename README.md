# TOTP Authenticator App

A professional Flutter-based TOTP (Time-based One-Time Password) authenticator application with a clean, organized codebase.

## Project Structure

The project has been restructured into a professional, maintainable architecture:

```
lib/
├── core/                   # Core app configuration
│   └── app.dart           # Main app widget and theme configuration
├── models/                # Data models
│   └── totp_account.dart  # TOTP account data model
├── screens/               # UI screens/pages
│   ├── authenticator_home.dart  # Main home screen
│   └── add_account_page.dart    # Add new account screen
├── services/              # Business logic and services
│   ├── database_helper.dart     # SQLite database operations
│   └── totp_generator.dart      # TOTP code generation logic
├── utils/                 # Utility functions
│   └── cupertino_snackbar.dart  # Custom snackbar utility
├── widgets/               # Reusable UI components
│   └── account_card.dart        # Account display card widget
└── main.dart             # App entry point
```

## Features

- **TOTP Code Generation**: Generate time-based one-time passwords
- **Account Management**: Add, view, and delete authenticator accounts
- **Search Functionality**: Real-time search through accounts with smooth animations
- **Data Persistence**: SQLite database for secure local storage
- **Import/Export**: Backup and restore accounts via JSON files
- **iOS-style UI**: Native Cupertino design components
- **Real-time Updates**: Live countdown timers and code refresh
- **Arabic Support**: RTL text support and Arabic interface

## Architecture Benefits

### 1. **Separation of Concerns**
- Models handle data structure
- Services manage business logic
- Screens focus on UI presentation
- Widgets provide reusable components

### 2. **Maintainability**
- Easy to locate and modify specific functionality
- Clear dependencies between components
- Simplified testing and debugging

### 3. **Scalability**
- Easy to add new features
- Modular structure supports team development
- Clean import statements and dependencies

### 4. **Professional Standards**
- Follows Flutter best practices
- Consistent naming conventions
- Proper file organization

## Getting Started

1. **Prerequisites**
   ```bash
   flutter --version  # Ensure Flutter is installed
   ```

2. **Install Dependencies**
   ```bash
   flutter pub get
   ```

3. **Run the App**
   ```bash
   flutter run
   ```

## Key Components

### Models
- `TOTPAccount`: Data structure for authenticator accounts with JSON serialization

### Services
- `TOTPGenerator`: Handles TOTP algorithm implementation and code generation
- `DatabaseHelper`: Manages SQLite operations for data persistence

### Screens
- `AuthenticatorHome`: Main interface showing all accounts and management options
- `AddAccountPage`: Form for manually adding new authenticator accounts

### Widgets
- `AccountCard`: Displays individual account with live TOTP codes and controls

## Development Setup

### Setting up Icon

```bash
flutter pub add flutter_launcher_icons
```

Add to `pubspec.yaml`:
```yaml
flutter_launcher_icons:
  android: "launcher_icon"
  ios: true
  image_path: "assets/icons/icon.png"
  min_sdk_android: 21
  web:
    generate: true
    image_path: "assets/icons/icon.png"
    background_color: "#hexcode"
    theme_color: "#hexcode"
  windows:
    generate: true
    image_path: "assets/icons/icon.png"
    icon_size: 48
  macos:
    generate: true
    image_path: "assets/icons/icon.png"
```

Generate icons:
```bash
dart run flutter_launcher_icons
```

### Build for Android

```bash
flutter build apk --split-per-abi
```

## Technical Details

- **Database**: SQLite with sqflite package
- **UI Framework**: Flutter Cupertino (iOS-style)
- **Cryptography**: HMAC-SHA1 for TOTP generation
- **File Operations**: Import/export functionality with file_picker
- **State Management**: StatefulWidget with proper lifecycle management

## TODO

- [X] Making the full app in Arabic
- [X] Restructuring code into professional architecture
- [X] Adding search functionality with smooth animations
- [ ] Adding QR code scanning
- [ ] Implementing biometric authentication
- [ ] Adding cloud backup options

## Development

The restructured codebase makes it easy to:
- Add new authenticator features
- Implement additional security measures
- Extend UI components
- Add new import/export formats
- Integrate with cloud services

This professional structure ensures the app is maintainable, testable, and ready for production deployment. 
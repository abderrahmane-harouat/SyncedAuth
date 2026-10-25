# SyncedAuth

A TOTP (Time-based One-Time Password) authenticator for Android, built entirely with Jetpack Compose.

## Features

- **TOTP codes**: 6/8-digit codes with live countdown ring and "next code" preview
- **QR setup**: scan two-factor setup QR codes (`otpauth://`) or Google Authenticator
  transfer QR codes (`otpauth-migration://`) with the camera, or decode a QR from a
  screenshot; HOTP and SHA-256/512 entries are skipped with an explanation
- **Real brand logos**: every known service shows its authentic logo, bundled in the APK (no network)
- **Pin & reorder**: pin accounts to the top; long-press a card to drag-reorder
- **Swipe actions**: swipe a card left to delete, right to pin/unpin; deletion asks for confirmation
- **Search**: instant filtering with match highlighting
- **Import/Export**: backup and restore accounts as JSON via the system file picker
- **Light & dark theme**

## Project structure

```
app/src/main/kotlin/com/harouat/syncedauth/
├── MainActivity.kt          # Entry point + simple home/add/scan routing
├── MainViewModel.kt         # Account list state, reorder, import/export
├── data/                    # Account model, storage, backup codec
├── otpauth/                 # QR payload parsing (otpauth:// + GA migration protobuf)
├── totp/Totp.kt             # TOTP (HMAC-SHA1) implementation
└── ui/
    ├── theme/               # Design tokens (colors, typography)
    ├── components/          # CodeCard, ServiceTile, CountdownRing, buttons…
    └── screens/             # HomeScreen, AddAccountScreen, QrScanScreen
```

Brand assets live in `app/src/main/res/` (`drawable/ic_brand_*.xml` vector marks,
`drawable-nodpi/brand_*.png` full-color logos).

## Build

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Credits

Brand logos: [Simple Icons](https://simpleicons.org) (CC0) and
[gilbarbara/logos](https://github.com/gilbarbara/logos) (CC0). Drag-to-reorder uses
[Reorderable](https://github.com/Calvin-LL/Reorderable) by Calvin Lau.

## License

SyncedAuth is released under the [MIT License](LICENSE). Brand names and logos are
trademarks of their respective owners and are not covered by this license.

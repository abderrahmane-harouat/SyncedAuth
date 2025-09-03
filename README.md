# authenticator

## SettingUP icon

```zsh
flutter pub add flutter_launcher_icons
```

```zsh
# Then you copy someting like this to pubspec.yaml
flutter_launcher_icons:
  android: "launcher_icon"
  ios: true
  image_path: "assets/icons/icon.png"
  min_sdk_android: 21 # android min sdk min:16, default 21
  web:
    generate: true
    image_path: "assets/icons/icon.png"
    background_color: "#hexcode"
    theme_color: "#hexcode"
  windows:
    generate: true
    image_path: "assets/icons/icon.png"
    icon_size: 48 # min:48, max:256, default: 48
  macos:
    generate: true
    image_path: "assets/icons/icon.png"
```

```zsh
# then you add your icon to assets/icons/icon.png
```


```zsh
dart run flutter_launcher_icons
```

## build for android

```zsh
flutter build apk --split-per-abi
```
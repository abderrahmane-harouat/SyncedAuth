# OneUI Design System

A comprehensive design system library mixing Samsung OneUI and Cupertino design styles for Flutter applications.

## Overview

This design system provides a consistent set of colors, typography, spacing, and components that can be easily reused across multiple Flutter projects. It's designed to be portable and maintainable.

## Installation

1. Copy `lib/theme.dart` to your project
2. Import the theme in your app:

```dart
import 'package:your_app/theme.dart';
```

## Usage

### Colors

```dart
// Primary colors
Container(color: AppColors.primary)
Container(color: AppColors.primaryLight)
Container(color: AppColors.primaryDark)

// Text colors
Text('Hello', style: TextStyle(color: AppColors.textPrimary))
Text('Secondary', style: TextStyle(color: AppColors.textSecondary))

// Status colors
Container(color: AppColors.success)
Container(color: AppColors.error)
Container(color: AppColors.warning)

// With opacity
Container(color: AppColors.primaryWithOpacity(0.1))
```

### Typography

```dart
// Display styles
Text('Large Title', style: AppTextStyles.displayLarge)
Text('Medium Title', style: AppTextStyles.displayMedium)

// Headlines
Text('Headline', style: AppTextStyles.headlineLarge)
Text('Subtitle', style: AppTextStyles.headlineMedium)

// Body text
Text('Body text', style: AppTextStyles.bodyLarge)
Text('Small text', style: AppTextStyles.bodyMedium)

// Labels
Text('Label', style: AppTextStyles.labelLarge)
Text('Caption', style: AppTextStyles.labelMedium)

// Special styles
Text('123 456', style: AppTextStyles.code)
Text('Button', style: AppTextStyles.button)
```

### Spacing

```dart
// Predefined spacing
SizedBox(height: AppSpacing.xs)     // 4.0
SizedBox(height: AppSpacing.sm)     // 8.0
SizedBox(height: AppSpacing.md)     // 16.0
SizedBox(height: AppSpacing.lg)     // 24.0
SizedBox(height: AppSpacing.xl)     // 32.0
SizedBox(height: AppSpacing.xxl)    // 48.0

// Semantic spacing
SizedBox(height: AppSpacing.small)
SizedBox(height: AppSpacing.medium)
SizedBox(height: AppSpacing.large)

// Padding
Padding(
  padding: EdgeInsets.all(AppSpacing.medium),
  child: YourWidget(),
)
```

### Border Radius

```dart
// Predefined radius
BorderRadius.circular(AppRadius.small)    // 4.0
BorderRadius.circular(AppRadius.medium)   // 12.0
BorderRadius.circular(AppRadius.large)    // 16.0

// Semantic radius
BorderRadius.circular(AppRadius.card)     // 12.0
BorderRadius.circular(AppRadius.button)   // 12.0
BorderRadius.circular(AppRadius.input)    // 6.0
```

### Shadows

```dart
// Predefined shadows
Container(
  decoration: BoxDecoration(
    boxShadow: AppShadows.small,
  ),
)

Container(
  decoration: BoxDecoration(
    boxShadow: AppShadows.medium,
  ),
)

Container(
  decoration: BoxDecoration(
    boxShadow: AppShadows.card,
  ),
)
```

### Icon Sizes

```dart
Icon(
  CupertinoIcons.heart,
  size: AppIconSizes.small,    // 16.0
)

Icon(
  CupertinoIcons.heart,
  size: AppIconSizes.medium,   // 20.0
)

Icon(
  CupertinoIcons.heart,
  size: AppIconSizes.large,    // 24.0
)
```

### Complete Theme

Apply the complete theme to your app:

```dart
class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return CupertinoApp(
      theme: AppTheme.lightTheme,
      darkTheme: AppTheme.darkTheme,
      home: YourHomePage(),
    );
  }
}
```

## Design Tokens

### Color Palette

| Token | Value | Usage |
|-------|-------|-------|
| `AppColors.primary` | `CupertinoColors.systemBlue` | Primary brand color |
| `AppColors.secondary` | `CupertinoColors.systemGreen` | Secondary actions |
| `AppColors.error` | `CupertinoColors.destructiveRed` | Error states |
| `AppColors.warning` | `CupertinoColors.systemYellow` | Warning states |
| `AppColors.success` | `CupertinoColors.systemGreen` | Success states |

### Typography Scale

| Token | Size | Weight | Usage |
|-------|------|--------|-------|
| `displayLarge` | 40sp | Light (300) | Large titles |
| `headlineLarge` | 24sp | Medium (500) | Section headers |
| `titleLarge` | 18sp | Regular (400) | Card titles |
| `bodyLarge` | 16sp | Regular (400) | Body text |
| `labelLarge` | 14sp | Medium (500) | Form labels |

### Spacing Scale

| Token | Value | Usage |
|-------|-------|-------|
| `xs` | 4px | Minimal spacing |
| `sm` | 8px | Small spacing |
| `md` | 16px | Standard spacing |
| `lg` | 24px | Large spacing |
| `xl` | 32px | Extra large spacing |

## Best Practices

1. **Consistency**: Always use design tokens instead of hardcoded values
2. **Semantic naming**: Use semantic names (e.g., `AppSpacing.medium`) over specific values
3. **Color accessibility**: Ensure sufficient contrast ratios
4. **Responsive design**: Test on different screen sizes
5. **Dark mode**: Consider both light and dark themes

## Migration Guide

### From hardcoded values:

```dart
// Before
Container(
  margin: EdgeInsets.all(16),
  decoration: BoxDecoration(
    color: CupertinoColors.systemBlue.withOpacity(0.1),
    borderRadius: BorderRadius.circular(12),
  ),
  child: Text(
    'Hello',
    style: TextStyle(
      fontSize: 18,
      fontWeight: FontWeight.w500,
      color: CupertinoColors.label,
    ),
  ),
)

// After
Container(
  margin: EdgeInsets.all(AppSpacing.medium),
  decoration: BoxDecoration(
    color: AppColors.primaryWithOpacity(0.1),
    borderRadius: BorderRadius.circular(AppRadius.card),
  ),
  child: Text(
    'Hello',
    style: AppTextStyles.titleLarge.copyWith(
      color: AppColors.textPrimary,
    ),
  ),
)
```

## Customization

To customize the design system for your brand:

1. Update color values in `AppColors`
2. Modify typography in `AppTextStyles`
3. Adjust spacing in `AppSpacing`
4. Update the font family in `AppConstants.fontFamily`

## Contributing

When adding new design tokens:

1. Follow the existing naming conventions
2. Add documentation and usage examples
3. Update this README
4. Test with both light and dark themes
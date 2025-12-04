/// OneUI Design System Library
/// A comprehensive design system mixing Samsung OneUI and Cupertino styles
/// 
/// Usage:
/// ```dart
/// import 'package:your_app/theme.dart';
/// 
/// // Use predefined colors
/// Container(color: AppColors.primary)
/// 
/// // Use typography
/// Text('Hello', style: AppTextStyles.titleLarge)
/// 
/// // Use spacing
/// SizedBox(height: AppSpacing.medium)
/// ```

library oneui_design_system;

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

/// Design System Constants
class AppConstants {
  static const String fontFamily = 'Roboto';
  static const String monoFontFamily = 'SF Mono';
  
  // Animation durations
  static const Duration fastAnimation = Duration(milliseconds: 150);
  static const Duration normalAnimation = Duration(milliseconds: 300);
  static const Duration slowAnimation = Duration(milliseconds: 500);
}

/// Color Palette
class AppColors {
  // Primary Colors
  static const Color primary = CupertinoColors.systemBlue;
  static const Color primaryLight = Color(0xFF64B5F6);
  static const Color primaryDark = Color(0xFF1976D2);
  
  // Secondary Colors
  static const Color secondary = CupertinoColors.systemGreen;
  static const Color accent = CupertinoColors.systemOrange;
  
  // Neutral Colors
  static const Color background = CupertinoColors.systemGroupedBackground;
  static const Color surface = CupertinoColors.systemBackground;
  static const Color cardBackground = CupertinoColors.systemGroupedBackground;
  
  // Text Colors
  static const Color textPrimary = CupertinoColors.label;
  static const Color textSecondary = CupertinoColors.secondaryLabel;
  static const Color textTertiary = CupertinoColors.tertiaryLabel;
  static const Color textDisabled = CupertinoColors.systemGrey;
  
  // Status Colors
  static const Color success = CupertinoColors.systemGreen;
  static const Color warning = CupertinoColors.systemYellow;
  static const Color error = CupertinoColors.destructiveRed;
  static const Color info = CupertinoColors.systemBlue;
  
  // Interactive Colors
  static const Color divider = CupertinoColors.separator;
  static const Color border = CupertinoColors.systemGrey4;
  static const Color shadow = CupertinoColors.systemGrey6;
  
  // Utility Colors
  static Color primaryWithOpacity(double opacity) => primary.withValues(alpha: opacity);
  static Color blackWithOpacity(double opacity) => CupertinoColors.black.withValues(alpha: opacity);
  static Color whiteWithOpacity(double opacity) => CupertinoColors.white.withValues(alpha: opacity);
}

/// Typography System
class AppTextStyles {
  // Display Styles
  static const TextStyle displayLarge = TextStyle(
    fontFamily: AppConstants.fontFamily,
    fontWeight: FontWeight.w300,
    fontSize: 40.0,
    height: 1.2,
    letterSpacing: -0.5,
  );
  
  static const TextStyle displayMedium = TextStyle(
    fontFamily: AppConstants.fontFamily,
    fontWeight: FontWeight.w300,
    fontSize: 32.0,
    height: 1.25,
    letterSpacing: -0.25,
  );
  
  // Headline Styles
  static const TextStyle headlineLarge = TextStyle(
    fontFamily: AppConstants.fontFamily,
    fontWeight: FontWeight.w500,
    fontSize: 24.0,
    height: 1.3,
  );
  
  static const TextStyle headlineMedium = TextStyle(
    fontFamily: AppConstants.fontFamily,
    fontWeight: FontWeight.w500,
    fontSize: 20.0,
    height: 1.3,
  );
  
  static const TextStyle headlineSmall = TextStyle(
    fontFamily: AppConstants.fontFamily,
    fontWeight: FontWeight.w400,
    fontSize: 19.0,
    height: 1.35,
  );
  
  // Title Styles
  static const TextStyle titleLarge = TextStyle(
    fontFamily: AppConstants.fontFamily,
    fontWeight: FontWeight.w400,
    fontSize: 18.0,
    height: 1.4,
  );
  
  static const TextStyle titleMedium = TextStyle(
    fontFamily: AppConstants.fontFamily,
    fontWeight: FontWeight.w400,
    fontSize: 17.0,
    height: 1.4,
  );
  
  static const TextStyle titleSmall = TextStyle(
    fontFamily: AppConstants.fontFamily,
    fontWeight: FontWeight.w500,
    fontSize: 16.0,
    height: 1.4,
  );
  
  // Body Styles
  static const TextStyle bodyLarge = TextStyle(
    fontFamily: AppConstants.fontFamily,
    fontWeight: FontWeight.w400,
    fontSize: 16.0,
    height: 1.5,
  );
  
  static const TextStyle bodyMedium = TextStyle(
    fontFamily: AppConstants.fontFamily,
    fontWeight: FontWeight.w400,
    fontSize: 15.0,
    height: 1.5,
  );
  
  static const TextStyle bodySmall = TextStyle(
    fontFamily: AppConstants.fontFamily,
    fontWeight: FontWeight.w400,
    fontSize: 14.0,
    height: 1.5,
  );
  
  // Label Styles
  static const TextStyle labelLarge = TextStyle(
    fontFamily: AppConstants.fontFamily,
    fontWeight: FontWeight.w500,
    fontSize: 14.0,
    height: 1.4,
  );
  
  static const TextStyle labelMedium = TextStyle(
    fontFamily: AppConstants.fontFamily,
    fontWeight: FontWeight.w400,
    fontSize: 13.0,
    height: 1.4,
  );
  
  static const TextStyle labelSmall = TextStyle(
    fontFamily: AppConstants.fontFamily,
    fontWeight: FontWeight.w400,
    fontSize: 12.0,
    height: 1.4,
  );
  
  // Special Styles
  static const TextStyle code = TextStyle(
    fontFamily: AppConstants.monoFontFamily,
    fontWeight: FontWeight.w600,
    fontSize: 24.0,
    letterSpacing: 4.0,
    height: 1.2,
  );
  
  static const TextStyle button = TextStyle(
    fontFamily: AppConstants.fontFamily,
    fontWeight: FontWeight.w600,
    fontSize: 16.0,
    height: 1.2,
  );
  
  static const TextStyle caption = TextStyle(
    fontFamily: AppConstants.fontFamily,
    fontWeight: FontWeight.w400,
    fontSize: 12.0,
    height: 1.3,
  );
}

/// Spacing System
class AppSpacing {
  static const double xs = 4.0;
  static const double sm = 8.0;
  static const double md = 16.0;
  static const double lg = 24.0;
  static const double xl = 32.0;
  static const double xxl = 48.0;
  
  // Semantic spacing
  static const double tiny = xs;
  static const double small = sm;
  static const double medium = md;
  static const double large = lg;
  static const double extraLarge = xl;
  static const double huge = xxl;
}

/// Border Radius System
class AppRadius {
  static const double xs = 4.0;
  static const double sm = 6.0;
  static const double md = 12.0;
  static const double lg = 16.0;
  static const double xl = 24.0;
  
  // Semantic radius
  static const double small = xs;
  static const double medium = md;
  static const double large = lg;
  static const double card = md;
  static const double button = md;
  static const double input = sm;
}

/// Shadow System
class AppShadows {
  static const List<BoxShadow> small = [
    BoxShadow(
      color: Color(0x1A000000),
      blurRadius: 4,
      offset: Offset(0, 2),
    ),
  ];
  
  static const List<BoxShadow> medium = [
    BoxShadow(
      color: Color(0x1A000000),
      blurRadius: 10,
      offset: Offset(0, 4),
    ),
  ];
  
  static const List<BoxShadow> large = [
    BoxShadow(
      color: Color(0x1A000000),
      blurRadius: 20,
      offset: Offset(0, 8),
    ),
  ];
  
  static const List<BoxShadow> card = medium;
}

/// Icon Sizes
class AppIconSizes {
  static const double xs = 12.0;
  static const double sm = 16.0;
  static const double md = 20.0;
  static const double lg = 24.0;
  static const double xl = 32.0;
  static const double xxl = 48.0;
  
  // Semantic sizes
  static const double small = sm;
  static const double medium = md;
  static const double large = lg;
}

/// Complete Theme Data
class AppTheme {
  static final CupertinoThemeData lightTheme = CupertinoThemeData(
    brightness: Brightness.light,
    primaryColor: AppColors.primary,
    barBackgroundColor: AppColors.surface,
    scaffoldBackgroundColor: AppColors.background,
    textTheme: CupertinoTextThemeData(
      textStyle: AppTextStyles.bodyMedium.copyWith(color: AppColors.textPrimary),
      actionTextStyle: AppTextStyles.titleMedium.copyWith(color: AppColors.primary),
      tabLabelTextStyle: AppTextStyles.labelMedium.copyWith(color: AppColors.textSecondary),
      navTitleTextStyle: AppTextStyles.headlineSmall.copyWith(color: AppColors.textPrimary),
      navLargeTitleTextStyle: AppTextStyles.displayMedium.copyWith(color: AppColors.textPrimary),
      pickerTextStyle: AppTextStyles.bodyMedium.copyWith(color: AppColors.textPrimary),
      dateTimePickerTextStyle: AppTextStyles.bodyMedium.copyWith(color: AppColors.textPrimary),
    ),
  );
  
  static final CupertinoThemeData darkTheme = CupertinoThemeData(
    brightness: Brightness.dark,
    primaryColor: AppColors.primary,
    barBackgroundColor: CupertinoColors.systemBackground.darkColor,
    scaffoldBackgroundColor: CupertinoColors.systemGroupedBackground.darkColor,
    textTheme: CupertinoTextThemeData(
      textStyle: AppTextStyles.bodyMedium.copyWith(color: CupertinoColors.label.darkColor),
      actionTextStyle: AppTextStyles.titleMedium.copyWith(color: AppColors.primary),
      tabLabelTextStyle: AppTextStyles.labelMedium.copyWith(color: CupertinoColors.secondaryLabel.darkColor),
      navTitleTextStyle: AppTextStyles.headlineSmall.copyWith(color: CupertinoColors.label.darkColor),
      navLargeTitleTextStyle: AppTextStyles.displayMedium.copyWith(color: CupertinoColors.label.darkColor),
      pickerTextStyle: AppTextStyles.bodyMedium.copyWith(color: CupertinoColors.label.darkColor),
      dateTimePickerTextStyle: AppTextStyles.bodyMedium.copyWith(color: CupertinoColors.label.darkColor),
    ),
  );
}

/// Legacy support - keeping original textTheme for backward compatibility
@Deprecated('Use AppTextStyles instead')
final TextTheme textTheme = TextTheme(
  displayLarge: AppTextStyles.displayLarge,
  headlineMedium: AppTextStyles.headlineMedium,
  headlineSmall: AppTextStyles.headlineSmall,
  titleLarge: AppTextStyles.titleLarge,
  titleMedium: AppTextStyles.titleMedium,
  bodyLarge: AppTextStyles.bodyLarge,
  bodyMedium: AppTextStyles.bodyMedium,
  labelLarge: AppTextStyles.labelLarge,
  labelMedium: AppTextStyles.labelMedium,
);

@Deprecated('Use AppTheme.lightTheme instead')
final ThemeData oneUiTheme = ThemeData(
  brightness: Brightness.light,
  fontFamily: AppConstants.fontFamily,
  textTheme: textTheme,
);